package edu.wpi.cs3733.d20.teamL.services.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import edu.wpi.cs3733.d20.teamL.App;
import edu.wpi.cs3733.d20.teamL.services.users.PasswordManager;
import lombok.extern.slf4j.Slf4j;

import edu.wpi.cs3733.d20.teamL.util.io.CSVHelper;
import edu.wpi.cs3733.d20.teamL.services.Service;

@Slf4j
public class DatabaseService extends Service implements IDatabaseService {
	private Connection connection;
	private final Map<String, ArrayList<String>> tableUpdateMappings = new HashMap<>();

	public enum DB_TYPE {
		MY_SQL,
		DERBY
	}
	private DB_TYPE dbType;

	public DatabaseService() {
		super();
		populateTableUpdateMappings();
		this.serviceName = DBConstants.SERVICE_NAME;
	}

	/**
	 * Starts the database service
	 */
	@Override
	public void startService() {
		if (connection == null) {
			connect();
			rebuildDatabase();
		}
		// Rebuild the database if using Derby
		if (dbType == DB_TYPE.DERBY) {
			rebuildDatabase();
		}
	}

	private void populateTableUpdateMappings() {
		tableUpdateMappings.put("Nodes", new ArrayList<>(Arrays.asList(DBConstants.ADD_NODE, DBConstants.DELETE_ALL_NODES)));
		tableUpdateMappings.put("Edges", new ArrayList<>(Arrays.asList(DBConstants.ADD_EDGE, DBConstants.DELETE_ALL_EDGES)));
		tableUpdateMappings.put("Users", new ArrayList<>(Arrays.asList(DBConstants.ADD_USER, DBConstants.DELETE_ALL_USERS)));
		tableUpdateMappings.put("Doctors", new ArrayList<>(Arrays.asList(DBConstants.ADD_DOCTOR, DBConstants.DELETE_ALL_DOCTORS)));
		tableUpdateMappings.put("Gifts", new ArrayList<>(Arrays.asList(DBConstants.ADD_GIFT, DBConstants.DELETE_ALL_GIFTS)));
	}

	/**
	 * Stops the database service
	 */
	@Override
	protected void stopService() {
		disconnect();
	}

	/**
	 * Attempts to connect to the remote MySQL database and falls back to an embedded Derby database if unsuccessful
	 */
	@Override
	public void connect() {
		try {
			Class.forName(DBConstants.DB_DRIVER);
		} catch (ClassNotFoundException ex) {
			log.error("Encountered ClassNotFoundException", ex);
		}
		try {
			connection = DriverManager.getConnection( DBConstants.DB_PREFIX + DBConstants.DB_URL + DBConstants.DB_PORT + DBConstants.DB_NAME_DEV, DBConstants.DB_USER, DBConstants.DB_PASSWORD);
			log.info("Connection established.");
			dbType = DB_TYPE.MY_SQL;
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		if (dbType != DB_TYPE.MY_SQL) {
			log.info("Unable to connect to remote MySQL database. Attempting to connect to fallback embedded Derby Database...");
			try {
				Class.forName(DerbyConstants.DB_DRIVER);
			} catch (ClassNotFoundException ex) {
				log.error("Encountered ClassNotFoundException", ex);
			}
			try {
				connection = DriverManager.getConnection(DerbyConstants.DB_PREFIX + DerbyConstants.DB_URL);
				log.info("Connection established.");
				dbType = DB_TYPE.DERBY;
			} catch (SQLException ex) {
				log.error("Encountered SQLException.", ex);
			}
		}
	}

	/**
	 * Disconnects from the database if currently connected
	 */
	protected void disconnect() {
		try {
			if (connection != null) {
				connection.commit();
				connection.close();
				connection = null;
			}
			log.info("Connection closed.");
		} catch (SQLException ex) {
			log.error("SQL Exception", ex);
		}
	}

	/**
	 * Executes an SQL query
	 *
	 * @param query The query to be executed, including its values if the query is a prepared statement
	 * @return A result set containing the results of the query
	 */
	@Override
	public ResultSet executeQuery(SQLEntry query) {
		App.allowCacheUpdates = false;
		ResultSet resSet = null;
		try {
			if (query.getValues().size() == 0) {
				Statement stmt;
				stmt = connection.createStatement();
				resSet = stmt.executeQuery(query.getStatement());
			} else {
				PreparedStatement pStmt = fillPreparedStatement(query);
				resSet = pStmt.executeQuery();
			}
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		App.allowCacheUpdates = true;
		return resSet;
	}

	/**
	 * Executes a list of SQL queries
	 *
	 * @param queries A list of queries to be executed, including their values if they are prepared statements
	 * @return A list of result sets containing the results of each query
	 */
	@Override
	public ArrayList<ResultSet> executeQueries(ArrayList<SQLEntry> queries) {
		ArrayList<ResultSet> resSets = new ArrayList<>();
		for (SQLEntry query : queries) {
			resSets.add(executeQuery(query));
		}
		return resSets;
	}

	/**
	 * Executes an SQL update
	 *
	 * @param update The update to be executed, including its values if the update is a prepared statement
	 * @return The number of rows affected by the update
	 */
	@Override
	public int executeUpdate(SQLEntry update) {
		App.allowCacheUpdates = false;
		int numRowsAffected = 0;
		try {
			if (update.getValues().size() == 0) {
				Statement stmt;
				stmt = connection.createStatement();
				numRowsAffected = stmt.executeUpdate(update.getStatement());
				stmt.close();
			} else {
				PreparedStatement pStmt = fillPreparedStatement(update);
				numRowsAffected = pStmt.executeUpdate();
				pStmt.close();
			}
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		App.allowCacheUpdates = true;
		return numRowsAffected;
	}

	/**
	 * Executes a list of SQL updates
	 *
	 * @param updates A list of updates to be executed, including their values if they are prepared statements
	 * @return A list of the number of rows affected by each update in the list
	 */
	@Override
	public ArrayList<Integer> executeUpdates(ArrayList<SQLEntry> updates) {
		ArrayList<Integer> affectedRows = new ArrayList<>();
		for (SQLEntry update : updates) {
			affectedRows.add(executeUpdate(update));
		}
		return affectedRows;
	}

	/**
	 * Fills a prepared statement with the supplied values
	 *
	 * @param command The SQL command to be processed
	 * @return A fully processed prepared statement ready to be executed
	 */
	@Override
	public PreparedStatement fillPreparedStatement(SQLEntry command) {
		PreparedStatement pStmt = null;
		if (command.getValues().size() == 0) {
			// TODO: replace with custom exception
			throw new IllegalArgumentException();
		}
		try {
			pStmt = connection.prepareStatement(command.getStatement());
			for (int i = 0; i < command.getValues().size(); i++) {
				pStmt.setString(i + 1, command.getValues().get(i));
			}
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		return pStmt;
	}

	/**
	 * Rebuilds the database by dropping and re-creating all tables then adding the default nodes and edges from CSV files
	 */
	@Override
	public void rebuildDatabase() {
		dropTables();
		ArrayList<SQLEntry> updates = new ArrayList<>();
		if (dbType == DB_TYPE.MY_SQL) {
			updates.add(new SQLEntry(DBConstants.CREATE_NODE_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_EDGE_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_USER_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_DOCTOR_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_PATIENT_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_GIFT_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_GIFT_DELIVERY_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_MEDICATION_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_SERVICE_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_RESERVATIONS_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_SCREENING_QUESTIONS_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_KIOSK_SETTINGS_TABLE));
			updates.add(new SQLEntry(DBConstants.CREATE_FEEDBACK_TABLE));
		} else if (dbType == DB_TYPE.DERBY) {
			updates.add(new SQLEntry(DerbyConstants.CREATE_NODE_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_EDGE_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_USER_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_DOCTOR_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_PATIENT_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_GIFT_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_GIFT_DELIVERY_REQUEST_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_MEDICATION_REQUEST_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_SERVICE_REQUEST_TABLE));
			updates.add(new SQLEntry(DerbyConstants.CREATE_FEEDBACK_TABLE));
			//TODO: add kiosk and screening and feedback tables
		} else {
			log.error("Invalid database type.");
		}
		executeUpdates(updates);

		// Add nodes and edges from CSV files
		populateFromCSV("MapLAllNodes", "Nodes");
		populateFromCSV("MapLAllEdges", "Edges");

		// Add default users
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("admin", "admin", "admin", PasswordManager.hashPassword("admin"), "3", null, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Nurse", "Joy", "nurse", PasswordManager.hashPassword("nurse"), "1", "pharmacy", null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("staff", "Member", "staff", PasswordManager.hashPassword("staff"), "0", null, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Wilson", "Wong", "doctor", PasswordManager.hashPassword("doctor"), "2", "pharmacy", null))));

		// Managers for each department
		Collection<String> serviceTypes = new ArrayList<>(Arrays.asList("security", "internal_transportation", "external_transportation", "maintenance", "interpreter", "sanitation", "gift_shop", "information_technology"));
		for (String serviceType : serviceTypes) {
			executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList(serviceType, "Manager", serviceType, PasswordManager.hashPassword(serviceType), "0", null, serviceType))));
		}

		// Create test employees for some departments
		String serviceType = "pharmacy";
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Billy", "Joel", serviceType + "_emp1", PasswordManager.hashPassword(serviceType + "_emp1"), "0", serviceType, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Jamie", "Adams", serviceType + "_emp2", PasswordManager.hashPassword(serviceType + "_emp2"), "0", serviceType, null))));

		serviceType = "gift_shop";
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Leon", "Hart", serviceType + "_emp1", PasswordManager.hashPassword(serviceType + "_emp1"), "0", serviceType, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Raymond", "Spencer", serviceType + "_emp2", PasswordManager.hashPassword(serviceType + "_emp2"), "0", serviceType, null))));
		serviceType = "information_technology";
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Spongebob", "Squarepants", serviceType + "_emp1", PasswordManager.hashPassword(serviceType + "_emp1"), "0", serviceType, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Barry", "Benson", serviceType + "_emp2", PasswordManager.hashPassword(serviceType + "_emp2"), "0", serviceType, null))));

		// Interpreters for French and Spanish, the interpreter form does submit them starting with capital letters
		String interpreter = "interpreter";
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Jacques", "Cousteau", interpreter + "_emp1", PasswordManager.hashPassword(interpreter + "_emp1"), "0", interpreter + "(French)", null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Adriana", "Lopez", interpreter + "_emp2", PasswordManager.hashPassword(interpreter + "_emp2"), "0", interpreter + "(Spanish)", null))));

		// Add a user that can do both medication and IT
		executeUpdate(new SQLEntry(DBConstants.ADD_USER, new ArrayList<>(Arrays.asList("Multi", "Boi", "multi", PasswordManager.hashPassword("multi"), "0", "pharmacy;information_technology", null))));

		// Example doctor and patient
		executeUpdate(new SQLEntry(DBConstants.ADD_DOCTOR, new ArrayList<>(Arrays.asList("123", "Wilson", "Wong", "doctor", null, null))));
		executeUpdate(new SQLEntry(DBConstants.ADD_PATIENT, new ArrayList<>(Arrays.asList("456", "Conrad", "Tulig", "123", null, null))));

		// Example Gifts
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Roses", "A vase of 7 roses", "100", "10.00"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Tulips", "A vase of 10 tulip", "100", "4.87"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Dandelion", "A vase of 12 dandelions", "100", "4.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Iris", "A vase of 5 iris", "100","3.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Anemone", "A vase of 5 anemone", "100","4.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Lily", "A vase of 3 lily", "100","4.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Flowers", "Daisy", "A vase of 6 daisy", "100","2.99"))));

		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Building blocks", "A container with ", "10", "14.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Play-Do", "A package of 6 Play-do colors in different containers", "10", "1.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Hot Wheels", "A set of 10 Hot Wheels as well as a small race track.", "10", "24.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Children Scooter", "A Multi-Function children scooter.", "10","29.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Kitchen Playset", "An American plastic kitchen playset", "10","29.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Toys", "Magnetic Balls", "A set of magnetic balls building blocks.", "10","9.99"))));

		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "LOTR", "The three Lord of the Rings books from the trilogy", "5", "19.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "Harry Potter", "The entire Harry Potter series", "3", "39.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "Inheritance", "The 4 books from the Inheritance cycle series", "1", "19.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "1984", "A dystopian novel by George Orwell", "5", "19.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "The Great Gatsby", "A novel by F. Scott Fitzgerald", "5","13.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Books", "The Catcher in the Rye", "A 1945 novel by J.D. Salinger", "5","19.99"))));

		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "LOTR Films", "The three Lord of the Rings movies from the trilogy", "100", "24.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "Star Wars", "All 6 Canon Star Wars movies", "100", "19.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "Pulp Fiction", "A copy of the movie Pulp Fiction", "3", "4.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "Joker", "full movie", "100", "6.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "Parasite", "full movie", "100", "12.99"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_GIFT, new ArrayList<>(Arrays.asList("Movies", "Avengers", "A collection of Marvel Avengers 1~4", "100", "39.99"))));

		executeUpdate(new SQLEntry(DBConstants.ADD_DEFAULT_KIOSK, new ArrayList<>(Collections.singletonList("LKIOS00101"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_DEFAULT_KIOSK, new ArrayList<>(Collections.singletonList("LKIOS00103"))));

		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Select your age range:", "1", "-1", "1"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Are you 18 yrs or under", "1", "0", "1"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Are you 18-64 years old?", "1", "0", "1"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Are you 65+ years old?", "1", "1", "1"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have any of the following symptoms?", "2", "-1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have a fever?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do have a new or worsening cough?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have a sore throat?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have diarrhea?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have nausea or vomiting?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Have you been experiencing chills or sweating?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Have you experiences a new loss of smell or taste?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Are you experiencing full body aches?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have any new difficulty breathing?", "2", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Check which of the following applies to you:", "3", "-1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you work or volunteer in a health care setting", "3", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("In the last two weeks have you been to a place where COVID-19 is widespread?", "3", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("In the last two weeks have you traveled outside the United States?", "3", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Do you have any of the following high-risk conditions?", "4", "-1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Chronic lung disease", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Asthma", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Serious heart condition", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Diabetes", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Weakened ability to cough", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Immunocompromised", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Dialysis", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Liver disease", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Pregnancy", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Severe obesity", "4", "1", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Keep yourself and others safe, stay 6ft away from others", "-3", "0", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Wear a mask in public and wash your hands frequently", "-3", "0", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("Minimize contact with others", "-3", "0", "0"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("You are not at risk however you could be a carrier remain in self isolation", "-3", "2", "-1"))));
		executeUpdate(new SQLEntry(DBConstants.ADD_SCREENING_QUESTION, new ArrayList<>(Arrays.asList("You should contact a staff member for immediate testing", "-3", "3", "1"))));

	}

	/**
	 * Populates a table in the database with data from
	 *
	 * @param csvFile The CSV file to pull the data from
	 * @param tableName The name of the table to add data to
	 */
	@Override
	public void populateFromCSV(String csvFile, String tableName) {
		ArrayList<SQLEntry> updates = new ArrayList<>();
		CSVHelper csvReader = new CSVHelper();
		for (ArrayList<String> row : csvReader.readCSVFile(csvFile, true)) {
			//updates.add(new SQLEntry(getTableUpdateMappings().get(tableName).get(0), row));
			row.add(String.valueOf(0));
			if (tableName.equals("Nodes")) {
				updates.add(new SQLEntry(DBConstants.ADD_NODE, row));
			} else if (tableName.equals("Edges")) {
				updates.add(new SQLEntry(DBConstants.ADD_EDGE, row));
			}
		}
		executeUpdates(updates);
	}

	/**
	 * Populates a table in the database with data from
	 *
	 * @param csvFile The CSV file to pull the data from
	 * @param tableName The name of the table to add data to
	 * @param append Whether to append the data or replace the current contents of the table
	 */
	@Override
	public void populateFromCSV(File csvFile, String tableName, boolean append) throws SQLException {
		ArrayList<SQLEntry> updates = new ArrayList<>();
		CSVHelper csvReader = new CSVHelper();
		ArrayList<ArrayList<String>> csvOutput = csvReader.readCSVFile(csvFile, true);
		if (!append) {
			updates.add(new SQLEntry(tableUpdateMappings.get(tableName).get(1)));
		}
		for (ArrayList<String> row : csvOutput) {
			updates.add(new SQLEntry(tableUpdateMappings.get(tableName).get(0), row));
		}
		for (int rowsAffected : executeUpdates(updates)) {
			if (rowsAffected == 0) {
				throw new SQLException();
			}
		}
	}

	/**
	 * Drops all the tables in the database if they currently exist
	 */
	@Override
	public void dropTables() {
		ArrayList<SQLEntry> updates = new ArrayList<>();
		if (dbType == DB_TYPE.MY_SQL) {
			updates.add(new SQLEntry(DBConstants.DROP_SCREENING_QUESTIONS_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_KIOSK_SETTINGS_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_RESERVATIONS_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_SERVICE_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_MEDICATION_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_GIFT_DELIVER_REQUEST_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_GIFT_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_PATIENT_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_DOCTOR_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_USER_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_EDGE_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_NODE_TABLE));
			updates.add(new SQLEntry(DBConstants.DROP_FEEDBACK_TABLE));
			executeUpdates(updates);
		} else if (dbType == DB_TYPE.DERBY) {
			ResultSet resSet;
			ArrayList<String> dropTableUpdates = new ArrayList<>();
			ArrayList<String> tablesToDrop = new ArrayList<>();
			dropTableUpdates.add(DerbyConstants.DROP_NODE_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_EDGE_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_USER_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_DOCTOR_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_PATIENT_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_GIFT_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_GIFT_DELIVER_REQUEST_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_MEDICATION_REQUEST_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_SERVICE_REQUEST_TABLE);
			dropTableUpdates.add(DerbyConstants.DROP_FEEDBACK_TABLE);
			try {
				for (int i = 0; i < DBConstants.GET_TABLE_NAMES().size(); i++) {
					resSet = connection.getMetaData().getTables(null, "APP", DBConstants.GET_TABLE_NAMES().get(i).toUpperCase(), null);
					if (resSet.next()) {
						tablesToDrop.add(dropTableUpdates.get((dropTableUpdates.size() - 1) - i));
					}
				}
				for (String entry : tablesToDrop) {
					updates.add(new SQLEntry(entry));
				}
				executeUpdates(updates);
			} catch (SQLException ex) {
				log.error("Encountered SQLException.", ex);
			}
		} else {
			log.error("Invalid database type.");
		}
	}

	/**
	 * Determines the titles of the columns of a table based on a result set
	 *
	 * @param resSet The result set that determines the table to find the column titles of
	 * @return A list of the titles of the columns in the table
	 */
	@Override
	public ArrayList<String> getColumnNames(ResultSet resSet) {
		ArrayList<String> colLabels = new ArrayList<>();
		try {
			ResultSetMetaData resSetMD = resSet.getMetaData();
			for (int i = 0; i < resSetMD.getColumnCount(); i++) {
				colLabels.add(resSetMD.getColumnLabel(i + 1));
			}
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		return colLabels;
	}

	/**
	 * Converts a result set into a table
	 *
	 * @param resSet The result set to build the table from
	 * @return The table represented by a list of lists of strings
	 */
	@Override
	public ArrayList<ArrayList<String>> getTableFromResultSet(ResultSet resSet) {
		ArrayList<ArrayList<String>> table = new ArrayList<>();
		try {
			while (resSet.next()) {
				ResultSetMetaData resSetMD = resSet.getMetaData();
				int totalCols = resSetMD.getColumnCount();
				ArrayList<String> row = new ArrayList<>();
				for (int i = 0; i < totalCols; i++) {
					row.add(resSet.getString(i + 1));
				}
				table.add(row);
			}
		} catch (SQLException ex) {
			log.error("Encountered SQLException.", ex);
		}
		return table;
	}

	@Override
	public Map<String, ArrayList<String>> getTableUpdateMappings() {
		return tableUpdateMappings;
	}
}
