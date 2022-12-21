# Hotel Reservation System: MySQL and Java Application

This program consists of a JDBC Java Backend Application to make customer queries to a MySQL database on the cloud.

Created by Connor Dye as a California Polytechnic University Project.

## Environment Set Up:
- export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
- export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/<insert_userName>?autoReconnect=true\&useSSL=false
- export HP_JDBC_USER=<insert_userName>
- export HP_JDBC_PW=<insert_pwd>

## InnReservations.java Usage
- `Usage: java InnReservations.java <menu option>` where the main menu option is a number from 1-6

## InnReservations.java Features
- `Option 1.) Rooms and Rates`  When this option is selected, the system outputs a list of rooms
to the user sorted by popularity (highest to lowest, where popularity score is number of days the room has been occupied during the previous
180 days divided by 180). Output will include: Next available check-in date and Length in days and check out date of the most recent (completed) stay in the room

- `Option 2.) Reservations` When this option is selected, the system will accept from the user the following information: **`1.)`** First Name **`2.)`** Last Name **`3.)`** A room code to indicate the specific  desired (or “Any” to indicate no preference) **`4.)`** A desired bed type (or “Any” to indicate no preference) **`5.)`** Begin date of stay **`6.)`** End date of stay **`7.)`** Number of children **`8.)`** Number of adults. Output will include a list of rooms along with a prompt to book by option number. If no exact matches are found, the system will suggest 5 possibilities for different rooms or dates chosen based on similarity.

- `Option 3.) Reservation Change` When this option is selected, the system allows the user to make changes to an existing reservation. Accepts from the user a reservation code and new values for any of the following: **`1.)`** First Name **`2.)`** Last Name **`3.)`** Begin Date **`4.)`** End Date **`5.)`** Number of Children **`6.)`** Number of adults 

- `Option 4.) Reservation Cancellation` When this option is selected, the system allows the user to cancel an existing reservation. Accepts from the user a unique reservation code, confirms the cancellation, and removes the reservation record from the database.

- `Option 5.) Detailed Reservation Information` When this option is selected, the system presents a search prompt or form that allows the user to enter any combination of the following fields: **`1.)`** First Name **`2.)`** Last Name **`3.)`** Range of dates **`4.)`** Room code **`5.)`** Reservation code.
- The output will be a list of all matching reservations found in the database. Accepts partial SQL like wildcards (e.g Bo for a first name value of Bob)

- `Option 6.) Revenue` When this option is selected, the system provides a month-by-month overview of revenue for the current calendar year, based on SQL‘s CURRENT DATE variable.

## Notes:
- MySQL injection safe queries by utilizing JDBC preparedStatement for security
- Exception handling to prevent resource leaks of JDBC objects (e.g Connection, Statement,  ResultSet)

