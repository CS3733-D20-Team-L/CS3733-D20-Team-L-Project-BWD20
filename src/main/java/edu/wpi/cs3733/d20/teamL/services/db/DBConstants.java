package edu.wpi.cs3733.d20.teamL.services.db;

import java.util.ArrayList;
import java.util.Arrays;

public class DBConstants {
	// Note that the database connection constants have been removed from this file so that they are not exposed to the public
	// Because of this the entire application will not function properly unless they are replaced

	static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
	static final String DB_PREFIX = "jdbc:mysql://";
	static final String DB_URL = "URL_GOES_HERE";
	static final String DB_PORT = ":PORT_GOES_HERE";
	// Development database
	static final String DB_NAME_DEV = "/bwh_dev";
	// Production database
	static final String DB_NAME_PROD = "/bwh_prod";
	// Bleeding-edge 'canary' database
	static final String DB_NAME_CANARY = "/bwh_canary";
	static final String DB_USER = "USER_GOES_HERE";
	static final String DB_PASSWORD = "PASSWORD_GOES_HERE";
	public static final String SERVICE_NAME = "mysql-db-01";

	public static ArrayList<String> GET_TABLE_NAMES() {
		return new ArrayList<>(Arrays.asList("Nodes", "Edges", "Users", "Doctors", "Patients", "Gifts", "Gift_Delivery_Requests", "Medication_Requests", "Service_Requests",
				"Reservations", "Kiosk_Settings", "Screening Questions", "Feedback"));
	}

	public static final String CREATE_NODE_TABLE =
			"CREATE TABLE Nodes(" +
					"id VARCHAR(16) NOT NULL PRIMARY KEY, " +
					"x_pos DOUBLE NOT NULL, " +
					"y_pos DOUBLE NOT NULL, " +
					"floor CHAR(2) NOT NULL, " +
					"building VARCHAR(64) NOT NULL, " +
					"node_type CHAR(4) NOT NULL, " +
					"l_name VARCHAR(64) NOT NULL, " +
					"s_name VARCHAR(64) NOT NULL, " +
					"freq INT)";

	public static final String CREATE_EDGE_TABLE =
			"CREATE TABLE Edges(" +
					"id VARCHAR(21) NOT NULL PRIMARY KEY, " +
					"node_start VARCHAR(16) NOT NULL REFERENCES Nodes(id), " +
					"node_end VARCHAR(16) NOT NULL REFERENCES Nodes(id), " +
					"freq INT)";

	public static final String CREATE_USER_TABLE =
			"CREATE TABLE Users(" +
					"id INT NOT NULL AUTO_INCREMENT, " +
					"f_name VARCHAR(32) NOT NULL, " +
					"l_name VARCHAR(32) NOT NULL, " +
					"username VARCHAR(32) NOT NULL PRIMARY KEY, " +
					"password VARCHAR(60) NOT NULL, " +
					// 0: staff member, 1: Nurse, 2: Doctor, 3: admin
					"acct_type CHAR(1) NOT NULL, " +
					"services VARCHAR(512), " +
					"manager VARCHAR(32), " +
					"INDEX(id))";

	public static final String CREATE_DOCTOR_TABLE =
			"CREATE TABLE Doctors(" +
					"id INT NOT NULL PRIMARY KEY, " +
					"f_name VARCHAR(32) NOT NULL, " +
					"l_name VARCHAR(32) NOT NULL, " +
					"username VARCHAR(32) REFERENCES Users(username), " +
					"office_id VARCHAR(16) REFERENCES Nodes(id), " +
					"addl_info VARCHAR(256))";

	public static final String CREATE_PATIENT_TABLE =
			"CREATE TABLE Patients(" +
					"id INT NOT NULL PRIMARY KEY, " +
					"f_name VARCHAR(32) NOT NULL, " +
					"l_name VARCHAR(32) NOT NULL, " +
					"doctor_id INT REFERENCES Doctors(id), " +
					"room_id VARCHAR(16) REFERENCES Nodes(id), " +
					"addl_info VARCHAR(256))";

	public static final String CREATE_GIFT_TABLE =
			"CREATE TABLE Gifts(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"type VARCHAR(16) NOT NULL, " +
					"subtype VARCHAR(32) NOT NULL, " +
					"description VARCHAR(128) NOT NULL, " +
					"inventory INT NOT NULL, " +
					"cost DOUBLE NOT NULL)";
					//"url VARCHAR(64))";

	public static final String CREATE_GIFT_DELIVERY_REQUEST_TABLE =
			"CREATE TABLE Gift_Delivery_Requests(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"patient_id INT NOT NULL REFERENCES Patients(id), " +
					"sender_name VARCHAR(32) NOT NULL, " +
					"request_username VARCHAR(32) NOT NULL REFERENCES Users(username), " +
					"assignee_username VARCHAR(32) REFERENCES Users(username), " +
					"gifts VARCHAR(256) NOT NULL, " +
					"message VARCHAR(128), " +
					"notes VARCHAR(256), " +
					// 0: Pending, 1: Approved, 2: Assigned, 3: Denied, 4: Completed
					"status CHAR(1) NOT NULL, " +
					"date_and_time CHAR(19) NOT NULL)";

	public static final String CREATE_MEDICATION_REQUEST_TABLE =
			"CREATE TABLE Medication_Requests(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"patient_id INT NOT NULL REFERENCES Patients(id), " +
					"doctor_id INT NOT NULL REFERENCES Doctors(id), " +
					"nurse_username VARCHAR(32) NOT NULL REFERENCES Users(username), " +
					"deliverer_username VARCHAR(32) REFERENCES Users(username), " +
					"dose VARCHAR(64) NOT NULL, " +
					"type VARCHAR(64) NOT NULL, " +
					"notes VARCHAR(256), " +
					// 0: Pending, 1: Approved, 2: Assigned, 3: Denied, 4: Completed
					"status CHAR(1) NOT NULL, " +
					"date_and_time CHAR(19) NOT NULL)";

	public static final String CREATE_SERVICE_REQUEST_TABLE =
			"CREATE TABLE Service_Requests(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"patient_id INT REFERENCES Patients(id), " +
					"request_username VARCHAR(32) REFERENCES Users(username), " +
					"assignee_username VARCHAR(32) REFERENCES Users(username), " +
					"location VARCHAR(16) REFERENCES Nodes(id), " +
					"service VARCHAR(64) NOT NULL, " +
					"type VARCHAR(64), " +
					"notes VARCHAR(256), " +
					// 0: Pending, 1: Approved, 2: Assigned, 3: Denied, 4: Completed
					"status CHAR(1) NOT NULL, " +
					"date_and_time CHAR(19) NOT NULL)";

	public static final String CREATE_RESERVATIONS_TABLE =
			"CREATE TABLE Reservations(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"username VARCHAR(32) NOT NULL REFERENCES Users(username), " +
					"place VARCHAR(32) NOT NULL, " +
					"date VARCHAR(16) NOT NULL, " +
					"start_time VARCHAR(16) NOT NULL, " +
					"end_time VARCHAR(16))";

	public static final String CREATE_KIOSK_SETTINGS_TABLE =
			"CREATE TABLE Kiosk_Settings(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"node_id VARCHAR(16) REFERENCES Nodes(id), " +
					"logout_timeout BIGINT DEFAULT 60000, " +
					"idle_cache_timeout BIGINT DEFAULT 30000, " +
					"force_cache_timeout BIGINT DEFAULT 120000, " +
					"screen_saver_timeout BIGINT DEFAULT 180000)";

	public static final String CREATE_SCREENING_QUESTIONS_TABLE =
			"CREATE TABLE Screening_Questions(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"question VARCHAR(256) NOT NULL, " +
					"page INT NOT NULL, " +
					"weight INT, " +
					"reqs INT)";

	public static final String CREATE_FEEDBACK_TABLE =
			"CREATE TABLE Feedback(" +
					"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"q1 VARCHAR(3) NOT NULL, " +
					"q2 VARCHAR(3) NOT NULL, " +
					"q3 VARCHAR(3) NOT NULL, " +
					"q4 VARCHAR(3) NOT NULL, " +
					"q5 VARCHAR(256) NOT NULL, " +
					"q6 VARCHAR(256) NOT NULL, " +
					"q7 VARCHAR(256))";

	public static final String DROP_NODE_TABLE =
			"DROP TABLE IF EXISTS Nodes";

	public static final String DROP_EDGE_TABLE =
			"DROP TABLE IF EXISTS Edges";

	public static final String DROP_USER_TABLE =
			"DROP TABLE IF EXISTS Users";

	public static final String DROP_DOCTOR_TABLE =
			"DROP TABLE IF EXISTS Doctors";

	public static final String DROP_PATIENT_TABLE =
			"DROP TABLE IF EXISTS Patients";

	public static final String DROP_GIFT_TABLE =
			"DROP TABLE IF EXISTS Gifts";

	public static final String DROP_GIFT_DELIVER_REQUEST_TABLE =
			"DROP TABLE IF EXISTS Gift_Delivery_Requests";

	public static final String DROP_MEDICATION_REQUEST_TABLE =
			"DROP TABLE IF EXISTS Medication_Requests";

	public static final String DROP_SERVICE_REQUEST_TABLE =
			"DROP TABLE IF EXISTS Service_Requests";

	public static final String DROP_RESERVATIONS_TABLE =
			"DROP TABLE IF EXISTS Reservations";

	public static final String DROP_KIOSK_SETTINGS_TABLE =
			"DROP TABLE IF EXISTS Kiosk_Settings";

	public static final String DROP_SCREENING_QUESTIONS_TABLE =
			"DROP TABLE IF EXISTS Screening_Questions";

	public static final String DROP_FEEDBACK_TABLE =
			"DROP TABLE IF EXISTS Feedback";

	public static final String ADD_NODE =
			"INSERT INTO Nodes(id, x_pos, y_pos, floor, building, node_type, l_name, s_name, freq)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String ADD_EDGE =
			"INSERT INTO Edges(id, node_start, node_end, freq)" +
					"VALUES(?, ?, ?, ?)";

	public static final String ADD_USER =
			"INSERT INTO Users(f_name, l_name, username, password, acct_type, services, manager)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?)";

	public static final String ADD_DOCTOR =
			"INSERT INTO Doctors(id, f_name, l_name, username, office_id, addl_info)" +
					"VALUES(?, ?, ?, ?, ?, ?)";

	public static final String ADD_PATIENT =
			"INSERT INTO Patients(id, f_name, l_name, doctor_id, room_id, addl_info)" +
					"VALUES(?, ?, ?, ?, ?, ?)";

	public static final String ADD_GIFT =
			"INSERT INTO Gifts(type, subtype, description, inventory, cost)" +
					"VALUES(?, ?, ?, ?, ?)";

	public static final String ADD_GIFT_DELIVERY_REQUEST =
			"INSERT INTO Gift_Delivery_Requests(patient_id, sender_name, request_username, assignee_username, gifts, message, notes, status, date_and_time)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String ADD_MEDICATION_REQUEST =
			"INSERT INTO Medication_Requests(doctor_id, patient_id, nurse_username, deliverer_username, dose, type, notes, status, date_and_time)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String ADD_SERVICE_REQUEST =
			"INSERT INTO Service_Requests(patient_id, request_username, assignee_username, location, service, type, notes, status, date_and_time)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String ADD_ROOM_REQUEST =
			"INSERT INTO Reservations(username, place, date, start_time, end_time)" +
					"VALUES(?, ?, ?, ?, ?)";

	public static final String ADD_KIOSK =
			"INSERT INTO Kiosk_Settings(node_id, logout_timeout, idle_cache_timeout, force_cache_timeout, screen_saver_timeout)" +
					"VALUES(?, ?, ?, ?, ?)";

	public static final String ADD_DEFAULT_KIOSK =
			"INSERT INTO Kiosk_Settings(node_id)" +
					"VALUES(?)";

	public static final String ADD_SCREENING_QUESTION =
			"INSERT INTO Screening_Questions(question, page, weight, reqs)" +
					"VALUES(?, ?, ?, ?)";

	public static final String ADD_FEEDBACK =
			"INSERT INTO Feedback(q1, q2, q3, q4, q5, q6, q7)" +
					"VALUES(?, ?, ?, ?, ?, ?, ?)";

	public static final String SELECT_ALL_NODES =
			"SELECT * " +
					"FROM Nodes " +
					"ORDER BY id";

	public static final String GET_NODE =
			"SELECT * " +
					"FROM Nodes " +
					"WHERE id = ?";

	public static final String SELECT_ALL_EDGES =
			"SELECT * " +
					"FROM Edges " +
					"ORDER BY id";

	public static final String SELECT_ALL_USERS =
			"SELECT id, f_name, l_name, username, acct_type, services, manager " +
					"FROM Users " +
					"ORDER BY id";

	public static final String GET_USER =
			"SELECT id, f_name, l_name, username, password, acct_type, services, manager " +
					"FROM Users " +
					"WHERE username = ?";

	public static final String GET_USERNAME_BY_NAME =
			"SELECT username " +
					"FROM Users " +
					"WHERE f_name = ? AND l_name = ?";

	public static final String GET_USER_BY_ID =
			"SELECT id, username, f_name, l_name, acct_type " +
					"FROM Users " +
					"WHERE id = ?";

	public static final String GET_NAME_BY_USERNAME =
			"SELECT f_name, l_name " +
					"FROM Users " +
					"WHERE username = ?";

	public static final String SELECT_ALL_DOCTORS =
			"SELECT * " +
					"FROM Doctors " +
					"ORDER BY id";

	public static final String GET_DOCTOR_NAME =
			"SELECT f_name, l_name " +
					"FROM Doctors " +
					"WHERE id = ?";

	public static final String GET_DOCTOR_ID_BY_NAME =
			"SELECT id " +
					"FROM Doctors " +
					"WHERE f_name = ? AND l_name = ?";

	public static final String GET_DOCTOR_ID_BY_USERNAME =
			"SELECT id " +
					"FROM Doctors " +
					"WHERE username = ?";

	public static final String SELECT_ALL_PATIENTS =
			"SELECT * " +
					"FROM Patients " +
					"ORDER BY id";

	public static final String GET_PATIENT_ID =
			"SELECT id " +
					"FROM Patients " +
					"WHERE f_name = ? AND l_name = ?";

	public static final String GET_PATIENT_NAME =
			"SELECT f_name, l_name " +
					"FROM Patients " +
					"WHERE id = ?";

	public static final String GET_PATIENT_ROOM =
			"SELECT room_id " +
					"FROM Patients " +
					"WHERE id = ?";

	public static final String SELECT_ALL_GIFTS =
			"SELECT * " +
					"FROM Gifts " +
					"ORDER BY id";

	public static final String GET_GIFT =
			"SELECT * " +
					"FROM Gifts " +
					"WHERE id = ?";

	public static final String SELECT_ALL_GIFT_DELIVERY_REQUESTS =
			"SELECT * " +
					"FROM Gift_Delivery_Requests " +
					"ORDER BY id";

	public static final String SELECT_ALL_GIFT_DELIVERY_REQUESTS_FOR_ASSIGNEE =
			"SELECT * " +
					"FROM Gift_Delivery_Requests " +
					"WHERE assignee_username = ?";

	public static final String SELECT_ALL_MEDICATION_REQUESTS =
			"SELECT * " +
					"FROM Medication_Requests " +
					"ORDER BY id";

	public static final String SELECT_ALL_MEDICATION_REQUESTS_FOR_DOCTOR =
			"SELECT * " +
					"FROM Medication_Requests " +
					"WHERE doctor_id = ?";

	public static final String SELECT_ALL_MEDICATION_REQUESTS_FOR_DELIVERER =
			"SELECT * " +
					"FROM Medication_Requests " +
					"WHERE deliverer_username = ?";

	public static final String SELECT_ALL_SERVICE_REQUESTS =
			"SELECT * " +
					"FROM Service_Requests " +
					"ORDER BY id";

	public static final String SELECT_ALL_SERVICE_REQUESTS_FOR_ASSIGNEE =
			"SELECT * " +
					"FROM Service_Requests " +
					"WHERE assignee_username = ?";

	public static final String SELECT_ALL_SERVICE_REQUESTS_FOR_MANAGER =
			"SELECT * " +
					"FROM Service_Requests " +
					"WHERE service = ?";

	public static final String SELECT_ALL_ROOM_REQUESTS =
			"SELECT * " +
					"FROM Reservations " +
					"WHERE place = ? AND date = ?";

	public static final String SELECT_SPECIFIC_ROOM_REQUEST =
			"SELECT * " +
					"FROM Reservations "+
					"WHERE place = ? AND date = ? AND start_time = ?";

	public static final String SELECT_ALL_RESERVATIONS =
			"SELECT * " +
					"FROM Reservations "+
					"ORDER BY id";

	public static final String SELECT_ALL_KIOSK_SETTINGS =
			"SELECT * " +
					"FROM Kiosk_Settings " +
					"ORDER BY id";

	public static final String SELECT_ALL_SCREENING_QUESTIONS =
			"SELECT * " +
					"FROM Screening_Questions " +
					"ORDER BY id";

	public static final String GET_ALL_FEEDBACK =
			"SELECT * " +
					"FROM Feedback " +
					"ORDER BY id";

	public static final String GET_KIOSK_SETTINGS =
			"SELECT * " +
					"FROM Kiosk_Settings " +
					"WHERE id = ?";

	public static final String UPDATE_NODE =
			"UPDATE Nodes " +
					"SET x_pos = ?, y_pos = ?, floor = ?, building = ?, node_type = ?, l_name = ?, s_name = ?, freq = ? " +
					"WHERE id = ?";

	public static final String UPDATE_EDGE =
			"UPDATE Edges " +
					"SET node_start = ?, node_end = ?, freq = ? " +
					"WHERE id = ?";

	public static final String UPDATE_GIFT =
			"UPDATE Gifts " +
					"SET type = ?, subtype = ?, description = ?, inventory = ?" +
					"WHERE id = ?";

	public static final String UPDATE_GIFT_INVENTORY =
			"UPDATE Gifts " +
					"SET inventory = ?" +
					"WHERE id = ?";

	public static final String UPDATE_USER =
			"UPDATE Users " +
					"SET f_name = ?, l_name = ?, username = ?, acct_type = ?, services = ?, manager = ? " +
					"WHERE id = ?";

	public static final String UPDATE_USER_NAME =
			"UPDATE Users " +
					"SET f_name = ?, l_name = ? " +
					"WHERE id = ?";

	public static final String UPDATE_USER_PASSWORD =
			"UPDATE Users " +
					"SET password = ? " +
					"WHERE id = ?";

	public static final String UPDATE_USER_ACCT_TYPE =
			"UPDATE Users " +
					"SET acct_type = ? " +
					"WHERE id = ?";

	public static final String UPDATE_DOCTOR =
			"UPDATE Doctors " +
					"SET f_name = ?, l_name = ?, username = ?, office_id = ?. addl_info = ? " +
					"WHERE id = ?";

	public static final String UPDATE_DOCTOR_USERNAME =
			"UPDATE Doctors " +
					"SET username = ? " +
					"WHERE id = ?";

	public static final String UPDATE_GIFT_DELIVERY_REQUEST =
			"UPDATE Gift_Delivery_Requests " +
					"SET patient_id = ?, request_username = ?, assignee_username = ?, gift_id = ?, message = ?, notes = ?, status = ?, date_and_time = ? " +
					"WHERE id = ?";

	public static final String UPDATE_GIFT_DELIVERY_REQUEST_ASSIGNEE =
			"UPDATE Gift_Delivery_Requests " +
					"SET assignee_username = ? " +
					"WHERE id = ?";

	public static final String UPDATE_GIFT_DELIVERY_REQUEST_STATUS =
			"UPDATE Gift_Delivery_Requests " +
					"SET status = ? " +
					"WHERE id = ?";

	public static final String UPDATE_GIFT_DELIVERY_REQUEST_NOTES =
			"UPDATE Gift_Delivery_Requests " +
					"SET notes = ? " +
					"WHERE id = ?";

	public static final String UPDATE_MEDICATION_REQUEST =
			"UPDATE Medication_Requests " +
					"SET doctor_id = ?, patient_id = ?, nurse_username = ?, deliverer_username = ?, dose = ?, type = ?, notes = ?, status = ?, date_and_time = ? " +
					"WHERE id = ?";

	public static final String UPDATE_MEDICATION_REQUEST_DELIVERER =
			"UPDATE Medication_Requests " +
					"SET deliverer_username = ? " +
					"WHERE id = ?";

	public static final String UPDATE_MEDICATION_REQUEST_STATUS =
			"UPDATE Medication_Requests " +
					"SET status = ? " +
					"WHERE id = ?";

	public static final String UPDATE_MEDICATION_REQUEST_NOTES =
			"UPDATE Medication_Requests " +
					"SET notes = ? " +
					"WHERE id = ?";

	public static final String UPDATE_SERVICE_REQUEST =
			"UPDATE Service_Requests " +
					"SET patient_id = ?, request_username = ?, assignee_username = ?, location = ?, service = ?, type = ?, notes = ?, status = ?, date_and_time = ? " +
					"WHERE id = ?";

	public static final String UPDATE_SERVICE_REQUEST_ASSIGNEE =
			"UPDATE Service_Requests " +
					"SET assignee_username = ? " +
					"WHERE id = ?";

	public static final String UPDATE_SERVICE_REQUEST_STATUS =
			"UPDATE Service_Requests " +
					"SET status = ? " +
					"WHERE id = ?";

	public static final String UPDATE_SERVICE_REQUEST_NOTES =
			"UPDATE Service_Requests " +
					"SET notes = ? " +
					"WHERE id = ?";

	public static final String UPDATE_KIOSK_TIMEOUTS =
			"UPDATE Kiosk_Settings " +
					"SET node_id = ?, logout_timeout = ?, idle_cache_timeout = ?, force_cache_timeout = ?, screen_saver_timeout = ? " +
					"WHERE id = ?";

	public static final String UPDATE_SCREENING_QUESTIONS =
			"UPDATE Screening_Questions " +
					"SET question = ?, page = ?, weight = ?, reqs = ?";

	public static final String REMOVE_NODE =
			"DELETE FROM Nodes " +
					"WHERE id = ?";

	public static final String REMOVE_EDGE =
			"DELETE FROM Edges " +
					"WHERE id = ?";

	public static final String REMOVE_USER =
			"DELETE FROM Users " +
					"WHERE id = ?";

	public static final String REMOVE_DOCTOR =
			"DELETE FROM Doctors " +
					"WHERE id = ?";

	public static final String REMOVE_GIFT =
			"DELETE FROM Gifts " +
					"WHERE id = ?";

	public static final String REMOVE_GIFT_DELIVERY_REQUEST =
			"DELETE FROM Gift_Delivery_Requests " +
					"WHERE id = ?";

	public static final String REMOVE_MEDICATION_REQUEST =
			"DELETE FROM Medication_Requests " +
					"WHERE id = ?";

	public static final String REMOVE_SERVICE_REQUEST =
			"DELETE FROM Service_Requests " +
					"WHERE id = ?";

	public static final String DELETE_ALL_NODES =
			"DELETE FROM Nodes";

	public static final String DELETE_ALL_EDGES =
			"DELETE FROM Edges";

	public static final String DELETE_ALL_USERS =
			"DELETE FROM Users";

	public static final String DELETE_ALL_DOCTORS =
			"DELETE FROM Doctors";

	public static final String DELETE_ALL_PATIENTS =
			"DELETE FROM Patients";

	public static final String DELETE_ALL_GIFTS =
			"DELETE FROM Gifts";

	public static final String DELETE_ALL_GIFT_DELIVERY_REQUESTS =
			"DELETE FROM Gift_Delivery_Requests";

	public static final String DELETE_ALL_MEDICATION_REQUESTS =
			"DELETE FROM Medication_Requests";

	public static final String DELETE_ALL_SERVICE_REQUESTS =
			"DELETE FROM SERVICE_REQUESTS";

	public static final String DELETE_ALL_RESERVATIONS =
			"DELETE FROM Reservations";

	public static final String DELETE_ALL_KIOSK_SETTINGS =
			"DELETE FROM Kiosk_Settings";

	public static final String DELETE_ALL_SCREENING_QUESTIONS =
			"DELETE FROM Screening_Questions";
}
