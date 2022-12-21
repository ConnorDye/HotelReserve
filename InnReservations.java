import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;

/*
Enviroment SetUp:

-- MySQL setup:
drop table if exists hp_goods, hp_customers, hp_items, hp_receipts;
create table hp_goods as select * from BAKERY.goods;
create table hp_customers as select * from BAKERY.customers;
create table hp_items as select * from BAKERY.items;
create table hp_receipts as select * from BAKERY.receipts;


-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/<insert_userName>?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=<insert_userName>
export HP_JDBC_PW=<insert_pwd>
 */
public class InnReservations {
	public static void main(String[] args) {
		try {
			InnReservations hp = new InnReservations();
			int demoNum = Integer.parseInt(args[0]);

			switch (demoNum) {
				case 1:
					hp.popularRooms();
					break;
				case 2:
					hp.availableRooms();
					break;
				case 3:
					hp.updateReservation();
					break;
				case 4:
					hp.deleteReservation();
					break;
				case 5:
					hp.searchReservations();
					break;
				case 6:
					hp.revenueReservations();
					break;
			}

		} catch (SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
		} catch (Exception e2) {
			System.err.println("Exception: " + e2.getMessage());
		}
	}

	// Demo1 - Establish JDBC connection, execute DDL statement
	private void popularRooms() throws SQLException {

		System.out.println("fr1: Output a list of rooms to the user sorted by popularity\r\n");

		// Step 0: Load MySQL JDBC Driver
		// No longer required as of JDBC 2.0 / Java 6
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("MySQL JDBC Driver loaded");
		} catch (ClassNotFoundException ex) {
			System.err.println("Unable to load JDBC Driver");
			System.exit(-1);
		}

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			String sql = """
					with DaysOccupiedLast180 as (
						select Room, SUM(DateDiff(Checkout,
							case
								when CheckIn >=  Current_Date - interval 180 day
								then CheckIn
								else Current_Date - interval 180 day
							end
							)) as DaysOccupied
						from lab7_reservations
						join lab7_rooms on Room = RoomCode
						where CheckOut > Current_Date - interval 180 day
						group by Room
					),
					nextAvailableReservations as (
						select Room, MAX(CheckIn) as MostRecentCheckin, MAX(Checkout) as MostRecentCheckout
						from lab7_reservations
						group by Room
					)
					SELECT DaysOccupiedLast180.Room, lab7_rooms.RoomName, lab7_rooms.Beds, lab7_rooms.bedType, lab7_rooms.maxOcc, lab7_rooms.basePrice, lab7_rooms.decor, DaysOccupied / 180 as PopularityScore, DATE_ADD(MostRecentCheckout, INTERVAL 1 DAY) as nextAvailableCheckIn, DATEDIFF(MostRecentCheckout, MostRecentCheckin) as numDaysOfRecentStay, MostRecentCheckout
					FROM DaysOccupiedLast180 join nextAvailableReservations on DaysOccupiedLast180.Room = nextAvailableReservations.Room
									join lab7_rooms on lab7_rooms.RoomCode = DaysOccupiedLast180.Room
					ORDER BY PopularityScore
							""";
			// Step 3: (omitted in this example) Start transaction

			try (Statement stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(sql)) {

				// Step 5: Receive results
				while (rs.next()) {
					String daysOccupied = rs.getString("DaysOccupiedLast180.Room");
					String RoomName = rs.getString("lab7_rooms.RoomName");
					Integer numBeds = rs.getInt("lab7_rooms.Beds");
					String bedType = rs.getString("lab7_rooms.bedType");
					Integer maxOcc = rs.getInt("lab7_rooms.maxOcc");
					Float price = rs.getFloat("lab7_rooms.basePrice");
					String decor = rs.getString("lab7_rooms.decor");
					Float popScore = rs.getFloat("PopularityScore");
					String nextAvailCheckIn = rs.getString("nextAvailableCheckIn");
					Integer numDaysRecentStay = rs.getInt("numDaysOfRecentStay");
					String mostRecentCheckOut = rs.getString("MostRecentCheckout");

					System.out.format("%s %s %s %s %s ($%.2f) %s %s %s %s %s %n", daysOccupied, RoomName, numBeds,
							bedType, maxOcc, price, decor, popScore, nextAvailCheckIn, numDaysRecentStay,
							mostRecentCheckOut);
				}
			}

			// Step 6: (omitted in this example) Commit or rollback transaction
		}
		// Step 7: Close connection (handled by try-with-resources syntax)
	}

	// Demo4 - Establish JDBC connection, execute DML query (UPDATE) using
	// PreparedStatement / transaction
	private void availableRooms() throws SQLException {

		System.out.println("fr2: Produce a numbered list of available rooms\r\n");

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter your first name: ");
			String firstName = scanner.nextLine();
			System.out.print("Enter your last name: ");
			String lastName = scanner.nextLine();
			System.out
					.print("A room code to indicate the specific room desired (or “Any” to indicate no preference): ");
			String roomCode = scanner.nextLine();
			System.out.print("A desired bed type (or “Any” to indicate no preference): ");
			String bedType = scanner.nextLine();
			System.out.format("Enter a begin date of stay in format (YYYY-MM-DD)?: ");
			LocalDate beginDate = LocalDate.parse(scanner.nextLine());
			System.out.format("Enter a end date of stay in format (YYYY-MM-DD)?: ");
			LocalDate endDate = LocalDate.parse(scanner.nextLine());
			System.out.print("Enter number of children: ");
			Integer numChildren = Integer.valueOf(scanner.nextLine());
			System.out.print("Enter number of adults: ");
			Integer numAdults = Integer.valueOf(scanner.nextLine());
			// System.out.format("Until what date will %s be available (YYYY-MM-DD)? ",
			// flavor);
			// LocalDate availDt = LocalDate.parse(scanner.nextLine());

			// If no available rooms, exit
			int maximumOccupancy = numChildren + numAdults;
			if (maximumOccupancy > 4) {
				System.out.print("There are no available rooms with a occupancy of " + maximumOccupancy);
				System.exit(0);
			}
			List<Object> params = new ArrayList<Object>();
			params.add(beginDate);
			params.add(endDate);
			params.add(beginDate);
			params.add(endDate);
			params.add(numChildren + numAdults);

			// StringBuilder sb = new StringBuilder(
			// "SELECT * FROM lab7_rooms as rm join lab7_reservations as rsv on rm.RoomCode
			// = rsv.Room WHERE CheckIn = ? and CheckOut = ? and maxOcc >= ?");
			StringBuilder sb = new StringBuilder(
					"""
							select lab7_rooms.RoomName
							from lab7_rooms
							join lab7_reservations on lab7_rooms.RoomCode = lab7_reservations.Room
							where (Checkin not between ? and ?) and (CheckOut not between ? and ?) and maxOcc >= ?
								""");
			// CHECK IF USER ENTERED THESE PARAMETERS
			if (!"any".equalsIgnoreCase(bedType)) {
				sb.append(" AND bedType = ?");
				params.add(bedType);
			}
			if (!"any".equalsIgnoreCase(roomCode)) {
				sb.append(" AND RoomCode = ?");
				params.add(roomCode);
			}
			// WE ALWAYS NEED TO APPEND OUR GROUP BY AT THE END
			sb.append(" group by lab7_rooms.RoomName");
			System.out.println(sb.toString());

			// Step 3: Start transaction
			conn.setAutoCommit(false);

			// CREATE LIST TO STORE RESULT SET
			List<String> menuItems = new ArrayList<String>();

			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}
				// System.out.println(pstmt.toString());
				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.println("Available rooms:");

					// CHECK IF THERE ARE ANY MATCHING RESULTS TO USERS INPUT
					Boolean resultsFound = false;
					while (rs.next()) {
						resultsFound = true;
						// System.out.format("%s %n", rs.getString("RoomName"));
						menuItems.add(rs.getString("RoomName"));
					}

					// IF NO MATCHING RESULTS, RUN OUR DEFAULT QUERY
					if (resultsFound == false) {
						String defaultQuery = """
								select lab7_rooms.RoomName
								from lab7_rooms
								join lab7_reservations on lab7_rooms.RoomCode = lab7_reservations.Room
								where (Checkin not between ? and ?) and (CheckOut not between ? and ?) and
								maxOcc >= ?
								group by lab7_rooms.RoomName
								limit 5
								""";
						try (PreparedStatement pstmt2 = conn.prepareStatement(defaultQuery)) {
							// Step 4: Send SQL statement to DBMS
							pstmt2.setDate(1, java.sql.Date.valueOf(beginDate));
							pstmt2.setDate(2, java.sql.Date.valueOf(endDate));
							pstmt2.setDate(3, java.sql.Date.valueOf(beginDate));
							pstmt2.setDate(4, java.sql.Date.valueOf(endDate));
							pstmt2.setInt(5, numChildren + numAdults);

							try (ResultSet rs2 = pstmt2.executeQuery()) {
								// CHECK IF THERE ARE ANY MATCHING RESULTS TO USERS INPUT
								while (rs2.next()) {
									// System.out.format("%s %n", rs2.getString("RoomName"));
									menuItems.add(rs.getString("RoomName"));
								}
							}
						}
					}
				}
				Boolean notChosen = true;

				while (notChosen) {
					System.out.println();
					System.out.println("The available rooms are below.");
					for (int index = 0; index < menuItems.size(); index++) {
						System.out.print(index + 1 + ".) " + menuItems.get(index));
						System.out.println();
					}
					System.out.print(
							"Enter a choice to book or cancel to return to the main command line(enter as format e.g 1): ");
					String choice = scanner.nextLine();
					if (choice.equalsIgnoreCase("cancel")) {
						notChosen = true;
						System.exit(0);
					}

					String roomName = menuItems.get(Integer.parseInt(choice) - 1);
					// System.out.print(roomName);

					// Step 2: Get our room attrbutes for our reservations request
					String sql = "SELECT * FROM lab7_rooms WHERE lab7_rooms.RoomName = " + "'" + roomName + "'";
					// System.out.print(sql);
					String RoomCode = "";
					Float Rate = (float) 0;
					String bedtype = "";
					// Step 4: Send SQL statement to DBMS
					try (Statement stmt = conn.createStatement();
							ResultSet rs = stmt.executeQuery(sql)) {

						// Step 5: Receive results
						while (rs.next()) {
							RoomCode = rs.getString("RoomCode");
							Rate = rs.getFloat("basePrice");
							bedtype = rs.getString("bedType");
						}
					}

					// PRINT THE CONFIRMATION
					System.out.println(firstName + " " + lastName);
					System.out.println(RoomCode + ", " + roomName + ", " + bedType);
					System.out.println(beginDate + ", " + endDate);
					System.out.println(numAdults);
					System.out.println(numChildren);
					long numDays = ChronoUnit.DAYS.between(beginDate, endDate);
					float cost = Rate * ((float) numDays);
					System.out.println("Total cost: $" + cost);
					System.out.print(
							"Confirmation below. Enter cancel to return to the main command line. Else enter any key to confirm: ");
					choice = scanner.nextLine();
					if (choice.equalsIgnoreCase("cancel")) {
						notChosen = true;
						System.exit(0);
					}

					Random rand = new Random();
					int randomCode = rand.nextInt(9000000) + 1000000;
					String updateSql = "insert into lab7_reservations(CODE, Room, Checkin, Checkout, Rate, LastName, FirstName, Adults, Kids) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

					// Step 3: Start transaction
					conn.setAutoCommit(false);

					try (PreparedStatement pstmtInsert = conn.prepareStatement(updateSql)) {

						// Step 4: Send SQL statement to DBMS
						pstmtInsert.setInt(1, randomCode);
						pstmtInsert.setString(2, RoomCode);
						pstmtInsert.setDate(3, java.sql.Date.valueOf(beginDate));
						pstmtInsert.setDate(4, java.sql.Date.valueOf(endDate));
						pstmtInsert.setFloat(5, Rate);
						pstmtInsert.setString(6, lastName);
						pstmtInsert.setString(7, firstName);
						pstmtInsert.setInt(8, numAdults);
						pstmtInsert.setInt(9, numChildren);

						int rowCount = pstmtInsert.executeUpdate();

						// Step 5: Handle results
						System.out.format("Inserted %d reservation for %s%n", rowCount, lastName);
					}
					// String sql2 = "INSERT INTO lab7_reservations (CODE , Room, CheckIn, Checkout,
					// Rate, LastName, FirstName, Adults, Kids) VALUES ("
					// + randomCode + ", "
					// + "'" + RoomCode + "'" + ", "
					// + "'" + beginDate + "'" + ", "
					// + "'" + endDate + "'" + ", "
					// + Rate + ", "
					// + "'" + lastName + "'" + ", "
					// + "'" + firstName + "'" + ", "
					// + numAdults + ", " + numChildren + ");";

					// System.out.println();
					// System.out.println(sql2);

					// // INSERT INTO RESERVATIONS
					// try (Statement stmt = conn.createStatement()) {

					// // Step 4: Send SQL statement to DBMS
					// int rowCount = stmt.executeUpdate(sql2);

					// // Step 5: Handle results
					// System.out.println();
					// System.out.print(rowCount + " is the number of inserted reservations");

					// // commit the change
					// conn.commit();

					// }
					// VALUES (1, '123456', CURRENT_DATE, '2023-05-18')

				}
			}

			// try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

			// // Step 4: Send SQL statement to DBMS
			// pstmt.setDate(1, java.sql.Date.valueOf(availDt));
			// pstmt.setString(2, flavor);
			// int rowCount = pstmt.executeUpdate();

			// // Step 5: Handle results
			// System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);

			// // Step 6: Commit or rollback transaction
			// conn.commit();
			// } catch (SQLException e) {
			// conn.rollback();
			// }

		}
		// Step 7: Close connection (handled implcitly by try-with-resources syntax)
	}

	private void updateReservation() throws SQLException {

		System.out.println("FR3: Make changes to a reservation\r\n");

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter your reservation code to make changes: ");
			String resvCode = scanner.nextLine();
			System.out.print("Enter your first name or 'no change': ");
			String firstName = scanner.nextLine();
			System.out.print("Enter your last name or 'no change': ");
			String lastName = scanner.nextLine();
			System.out.format("Enter a begin date of stay in format (YYYY-MM-DD) or 'no change': ");
			String beginDate = scanner.nextLine();
			// LocalDate beginDate = LocalDate.parse(scanner.nextLine());
			System.out.format("Enter a end date of stay in format (YYYY-MM-DD) or 'no change': ");
			// LocalDate endDate = LocalDate.parse(scanner.nextLine());
			String endDate = scanner.nextLine();
			System.out.print("Enter number of children or 'no change': ");
			String numChildren = scanner.nextLine();
			// Integer numChildren = Integer.valueOf(scanner.nextLine());
			System.out.print("Enter number of adults or 'no change': ");
			String numAdults = scanner.nextLine();
			// Integer numAdults = Integer.valueOf(scanner.nextLine());

			// MAXIMUM OCCUPANCY OF A ROOM IS 4
			// int maximumOccupancy = Integer.valueOf(numChildren) +
			// Integer.valueOf(numAdults);
			// if (maximumOccupancy > 4) {
			// System.out.println("There are no available rooms with a occupancy of " +
			// maximumOccupancy
			// + ". Maximum occupancy for a room is 4.");
			// System.exit(0);
			// }

			List<Object> params = new ArrayList<Object>();
			// params.add(lastName);
			// params.add(beginDate);
			// params.add(endDate);
			// params.add(numChildren);
			// params.add(numAdults);

			// StringBuilder sb = new StringBuilder(
			// "SELECT * FROM lab7_rooms as rm join lab7_reservations as rsv on rm.RoomCode
			// = rsv.Room WHERE CheckIn = ? and CheckOut = ? and maxOcc >= ?");
			StringBuilder sb = new StringBuilder(
					"""
							UPDATE lab7_reservations
							SET
									""");
			// CHECK IF USER ENTERED THESE PARAMETERS
			if (!"no change".equalsIgnoreCase(firstName)) {
				sb.append(" FirstName = ? ,");
				params.add(firstName);
			}
			if (!"no change".equalsIgnoreCase(lastName)) {
				sb.append(" LastName = ? ,");
				params.add(lastName);
			}
			if (!"no change".equalsIgnoreCase(numAdults)) {
				sb.append(" Adults = ? ,");
				params.add(numAdults);
			}
			if (!"no change".equalsIgnoreCase(numChildren)) {
				sb.append(" Kids = ? ,");
				params.add(numChildren);
			}
			if ((!"no change".equalsIgnoreCase(beginDate))) {
				sb.append(" CheckIn = ? ,");
				params.add(beginDate);
			}
			if (!"no change".equalsIgnoreCase(endDate)) {
				sb.append(" CheckOut = ? ,");
				params.add(endDate);
			}

			// APPEND OUR CODE CLAUSE
			sb.setLength(sb.length() - 1); // REMOVE THE TRAILING COMMA
			sb.append(" WHERE CODE = " + resvCode + " ");

			// IF CHECKIN AND CHECKOUT DATES ARE ENTERED, WE NEED TO CHECK IF THESE DATES
			// ARE VALID
			if (!"no change".equalsIgnoreCase(beginDate))
				sb.append("""
						AND ? NOT IN (
							SELECT * FROM
							(
								SELECT Checkin as notValidCheckIn
								FROM lab7_reservations as r1
								where (r1.Checkin between ? and ?) AND r1.CODE != ?
							) invalidCheckIns
						)
							""");

			if (!"no change".equalsIgnoreCase(endDate))
				sb.append("""
						AND ? NOT IN (
							SELECT * FROM
							(
								SELECT Checkout as notValidCheckout
								FROM lab7_reservations as r1
								where (r1.Checkout between ? and ?) AND r1.CODE != ?
							) invalidCheckOuts
						)
							""");
			// System.out.println(sb.toString());

			// Step 3: Start transaction
			conn.setAutoCommit(false);

			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {

				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				// IF BOTH beginning date and enddate are present
				if ((!"no change".equalsIgnoreCase(beginDate)) && (!"no change".equalsIgnoreCase(endDate))) {

					pstmt.setDate(i++, java.sql.Date.valueOf(beginDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(beginDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(endDate));
					pstmt.setString(i++, resvCode);

					pstmt.setDate(i++, java.sql.Date.valueOf(endDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(beginDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(endDate));
					pstmt.setString(i++, resvCode);
				}

				// WE NEED TO QUERY OUR END DATE
				if ((!"no change".equalsIgnoreCase(beginDate)) && ("no change".equalsIgnoreCase(endDate))) {
					String sql = "SELECT Checkout FROM lab7_reservations WHERE CODE = " + "'" + resvCode + "'";
					System.out.print(sql);
					String Checkoutdate = "";
					// Step 4: Send SQL statement to DBMS
					try (Statement stmt = conn.createStatement();
							ResultSet rs = stmt.executeQuery(sql)) {

						// Step 5: Receive results
						while (rs.next()) {
							Checkoutdate = rs.getString("Checkout");
						}
					}

					pstmt.setDate(i++, java.sql.Date.valueOf(Checkoutdate));
					pstmt.setDate(i++, java.sql.Date.valueOf(beginDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(Checkoutdate));
					pstmt.setString(i++, resvCode);
				}

				// IF NO BEGINNING DATE IS CHOSEN QUERY FOR IT
				if (("no change".equalsIgnoreCase(beginDate)) && (!"no change".equalsIgnoreCase(endDate))) {
					String sql = "SELECT CheckIn FROM lab7_reservations WHERE CODE = " + "'" + resvCode + "'";
					System.out.print(sql);
					String CheckInDate = "";
					// Step 4: Send SQL statement to DBMS
					try (Statement stmt = conn.createStatement();
							ResultSet rs = stmt.executeQuery(sql)) {

						// Step 5: Receive results
						while (rs.next()) {
							CheckInDate = rs.getString("CheckIn");
						}
					}

					pstmt.setDate(i++, java.sql.Date.valueOf(CheckInDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(CheckInDate));
					pstmt.setDate(i++, java.sql.Date.valueOf(endDate));
					pstmt.setString(i++, resvCode);
				}
				System.out.println(pstmt);

				int rowCount = 1000;

				rowCount = pstmt.executeUpdate();

				// Step 5: Handle results
				System.out.format("Changed %d reservation for %s%n", rowCount, lastName);

				// Step 6: Commit or rollback transaction
				conn.commit();

			} catch (SQLException e) {
				conn.rollback();
			}

		}
		// Step 7: Close connection (handled implcitly by try-with-resources syntax)
	}

	private void searchReservations() throws SQLException {

		System.out.println("FR%: Search for reservations\r\n");

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			Scanner scanner = new Scanner(System.in);

			System.out.print("Enter a search value for first name or 'Any': ");
			String firstName = scanner.nextLine();

			System.out.print("Enter a search value for last name or 'Any': ");
			String lastName = scanner.nextLine();

			System.out.print("Enter a search value for reservation code or 'Any': ");
			String resvCode = scanner.nextLine();

			System.out.print("Enter a search value for room code or 'Any': ");
			String roomCode = scanner.nextLine();

			System.out.format("Enter a begin date of stay in format (YYYY-MM-DD) or 'Any': ");
			String beginDate = scanner.nextLine();
			// LocalDate beginDate = LocalDate.parse(scanner.nextLine());
			System.out.format("Enter a end date of stay in format (YYYY-MM-DD) or 'Any': ");
			String endDate = scanner.nextLine();

			List<String> params = new ArrayList<String>();
			StringBuilder sb = new StringBuilder(
					"""
							SELECT CODE, Room, RoomName, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids, decor
							FROM lab7_reservations join lab7_rooms on
							lab7_reservations.Room = lab7_rooms.RoomCode
							WHERE
											""");

			// CHECK IF USER ENTERED THESE PARAMETERS
			if (!"Any".equalsIgnoreCase(firstName)) {
				sb.append(" FirstName like ? and ");
				params.add(firstName);
			}
			if (!"Any".equalsIgnoreCase(lastName)) {
				sb.append(" LastName like ? and ");
				params.add(lastName);
			}
			if (!"Any".equalsIgnoreCase(resvCode)) {
				sb.append(" CODE like ? and ");
				params.add(resvCode);
			}
			if (!"Any".equalsIgnoreCase(roomCode)) {
				sb.append(" Room like ? and ");
				params.add(roomCode);
			}
			if ((!"Any".equalsIgnoreCase(beginDate))) {
				sb.append(" CheckIn <= ? and ");
				params.add(beginDate);
			}
			if (!"Any".equalsIgnoreCase(endDate)) {
				sb.append(" CheckOut >= ? and ");
				params.add(endDate);
			}

			// REMOVE TRAILING AND
			sb.setLength(sb.length() - 4);

			// Step 3: Start transaction
			conn.setAutoCommit(false);

			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {

				int i = 1;
				for (String p : params) {
					pstmt.setObject(i++, p + "%");
				}

				System.out.println(pstmt);

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.println("Matching reservations:");
					int matchCount = 0;
					while (rs.next()) {
						// SELECT CODE, Room, RoomName, CheckIn, Checkout, Rate, LastName, FirstName,
						// Adults, Kids, decor
						System.out.format("%s %s %s %s %s ($%.2f) %s %s %s %s %s %n", rs.getString("CODE"),
								rs.getString("Room"),
								rs.getString("RoomName"),
								rs.getString("CheckIn"),
								rs.getString("Checkout"),
								rs.getDouble("Rate"),
								rs.getString("LastName"),
								rs.getString("FirstName"),
								rs.getString("Adults"),
								rs.getString("Kids"),
								rs.getString("decor"));
						matchCount++;
					}
					System.out.format("----------------------%nFound %d match%s %n", matchCount,
							matchCount == 1 ? "" : "es");
				}

			}
			// try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

			// // Step 4: Send SQL statement to DBMS
			// pstmt.setDate(1, java.sql.Date.valueOf(availDt));
			// pstmt.setString(2, flavor);
			// int rowCount = pstmt.executeUpdate();

			// // Step 5: Handle results
			// System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);

			// // Step 6: Commit or rollback transaction
			// conn.commit();
			// } catch (SQLException e) {
			// conn.rollback();
			// }

		}
		// Step 7: Close connection (handled implcitly by try-with-resources syntax)
	}

	private void deleteReservation() throws SQLException {
		System.out.println("fr4: Reservation Cancellation\r\n");

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {

			Scanner scanner = new Scanner(System.in);
			System.out.print("To cancel a reservation enter the reservation code: ");
			String roomCode = scanner.nextLine();

			System.out.print("Are you sure you want to cancel this reservation (yes/no)?: ");
			String confirmResponse = scanner.nextLine();

			// check for cancel
			if ((confirmResponse.equals("no")) || (confirmResponse.equals("No"))) {
				// System.out.println("enter statement.");
				System.out.println("Reservation Cancellation aborted.");
				System.exit(0);
			}

			String sql = "delete from lab7_reservations where lab7_reservations.CODE = ?";

			// Step 3: Start transaction
			conn.setAutoCommit(false);

			try (PreparedStatement pstmtInsert = conn.prepareStatement(sql)) {

				// Step 4: Send SQL statement to DBMS
				pstmtInsert.setString(1, roomCode);

				int rowCount = pstmtInsert.executeUpdate();

				// Step 5: Handle results
				System.out.format("Deleted %d reservation. Reservation Cancelled.%n", rowCount);

				// Step 6: Commit or rollback transaction
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
			}

		}

	}

	private void revenueReservations() throws SQLException {
		System.out.println("FR6: Print out the revenues by month and year for each room\r\n");

		// Step 0: Load MySQL JDBC Driver
		// No longer required as of JDBC 2.0 / Java 6
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("MySQL JDBC Driver loaded");
		} catch (ClassNotFoundException ex) {
			System.err.println("Unable to load JDBC Driver");
			System.exit(-1);
		}

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			String sql = """
					select ifnull(lab7_reservations.Room, "MonthRevenues") as Rooms,
					ifnull(round(SUM(CASE MONTH(Checkout) WHEN  1 THEN Rate END), 0), 0) AS January,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  2 THEN Rate END), 0), 0) AS February,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  3 THEN Rate END), 0), 0) AS March,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  4 THEN Rate END), 0), 0) AS April,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  5 THEN Rate END), 0), 0) AS May,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  6 THEN Rate END), 0), 0) AS June,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  7 THEN Rate END), 0), 0) AS July,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  8 THEN Rate END), 0), 0) AS August,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  9 THEN Rate END), 0), 0) AS September,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN 10 THEN Rate END), 0), 0) AS October,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN 11 THEN Rate END), 0), 0) AS November,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN 12 THEN Rate END), 0), 0) AS December,
					  ifnull(round(SUM(CASE MONTH(Checkout) WHEN  1 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  2 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  3 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  4 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  5 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  6 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  7 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  8 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  9 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  10 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN 11 THEN Rate END), 0), 0)
					  + ifnull(round(SUM(CASE MONTH(Checkout) WHEN  12 THEN Rate END), 0), 0) as "YearlyRevenues"
						from lab7_reservations
						where year(curdate()) = year(checkout)
						group by lab7_reservations.Room with rollup
						""";
			;

			// Step 3: (omitted in this example) Start transaction

			try (Statement stmt = conn.createStatement()) {

				try (ResultSet rs = stmt.executeQuery(sql)) {
					System.out.println("Matching reservations:");
					while (rs.next()) {
						// SELECT CODE, Room, RoomName, CheckIn, Checkout, Rate, LastName, FirstName,
						// Adults, Kids, decor
						System.out.format("%s %s %s %s %s %s %s %s %s %s %s %n",
								rs.getString("Rooms"),
								rs.getString("January"),
								rs.getString("February"),
								rs.getString("March"),
								rs.getString("April"),
								rs.getString("May"),
								rs.getString("June"),
								rs.getString("July"),
								rs.getString("August"),
								rs.getString("November"),
								rs.getString("December"),
								rs.getString("December"),
								rs.getString("YearlyRevenues"));
					}

				}
			}

			// Step 6: (omitted in this example) Commit or rollback transaction
		}

	}

	// Demo5 - Construct a query using PreparedStatement
	private void fr() throws SQLException {

		System.out.println("demo5: Run SELECT query using PreparedStatement\r\n");

		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
				System.getenv("HP_JDBC_USER"),
				System.getenv("HP_JDBC_PW"))) {
			Scanner scanner = new Scanner(System.in);
			System.out.print("Find pastries with price <=: ");
			Double price = Double.valueOf(scanner.nextLine());
			System.out.print("Filter by flavor (or 'Any'): ");
			String flavor = scanner.nextLine();

			List<Object> params = new ArrayList<Object>();
			params.add(price);
			StringBuilder sb = new StringBuilder("SELECT * FROM hp_goods WHERE price <= ?");
			if (!"any".equalsIgnoreCase(flavor)) {
				sb.append(" AND Flavor = ?");
				params.add(flavor);
			}

			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object p : params) {
					pstmt.setObject(i++, p);
				}

				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.println("Matching Pastries:");
					int matchCount = 0;
					while (rs.next()) {
						System.out.format("%s %s ($%.2f) %n", rs.getString("Flavor"), rs.getString("Food"),
								rs.getDouble("price"));
						matchCount++;
					}
					System.out.format("----------------------%nFound %d match%s %n", matchCount,
							matchCount == 1 ? "" : "es");
				}
			}

		}
	}

}
