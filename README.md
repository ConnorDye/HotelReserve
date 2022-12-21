# Hotel Reservation System: MySQL and Java Application

This program consists of a JDBC Java Backend Application to make customer queries to a MySQL database on the cloud.

Created by Connor Dye as a California Polytechnic University Project.

## Environment Set Up:
- export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
- export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/<insert_userName>?autoReconnect=true\&useSSL=false
- export HP_JDBC_USER=<insert_userName>
- export HP_JDBC_PW=<insert_pwd>

## InnReservations.java Usage
- `Usage: java InnReservations.java <menu option> where the main menu option is a number from 1-6

## InnReservations.java Features
- Option 1.) Rooms and Rates.  When this option is selected, the system outputs a list of rooms
to the user sorted by popularity (highest to lowest, where popularity score is number of days the room has been occupied during the previous
180 days divided by 180). Output will include: Next available check-in date and Length in days and check out date of the most recent (completed) stay in the room


