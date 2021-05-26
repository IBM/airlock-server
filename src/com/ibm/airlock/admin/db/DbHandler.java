package com.ibm.airlock.admin.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

import javax.sql.rowset.CachedRowSet;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants.DATA_TYPE;
import com.sun.rowset.CachedRowSetImpl;

public class DbHandler {
	public class TableColumn {
		private String name;
		private DATA_TYPE type;
		private boolean nullable;
		private boolean isArray;
		
		public TableColumn (String name, DATA_TYPE type, boolean nullable, boolean isArray) {
			this.name = name;
			this.type = type;
			this.nullable = nullable;
			this.isArray = isArray;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public DATA_TYPE getType() {
			return type;
		}
		public void setType(DATA_TYPE type) {
			this.type = type;
		}
		public boolean isNullable() {
			return nullable;
		}
		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		public boolean isArray() {
			return isArray;
		}

		public void setArray(boolean isArray) {
			this.isArray = isArray;
		}
	}
	
	private String dbUrl;
    private String userName;
    private String password;
	
    public DbHandler(String dburl, String userName, String password) throws ClassNotFoundException, SQLException {
    		this.dbUrl = dburl;
    		this.userName = userName;
    		this.password = password;
    }
    
    public DbHandler(JSONObject secretJSON) throws JSONException {
    		String host = secretJSON.getString("host");
        String port = secretJSON.getString("port");
        String dbName = secretJSON.getString("dbname");
        this.dbUrl = "jdbc:postgresql://"+host+":"+port+"/"+dbName;
        this.userName = secretJSON.getString("username");
        this.password = secretJSON.getString("password");
	}

	public void checkConnectivity() throws ClassNotFoundException, SQLException {
		try (Connection conn = getDBConnection()) {
			//validate db connectivity
		}
    }
    
    public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDbUrl() {
		return dbUrl;
	}
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	private Connection getDBConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", userName);
        props.setProperty("password", password);
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "verify-full");
        Connection conn = DriverManager.getConnection(dbUrl, props);
        return conn;
    }

	public ArrayList<String> getTableNames(String schema) throws ClassNotFoundException, SQLException{
		Class.forName("org.postgresql.Driver");
		ArrayList<String> res = new ArrayList<String>();
        String select = "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + schema + "';";
        try (Connection conn = getDBConnection();
        		Statement stmt = conn.createStatement();
        		ResultSet rs = stmt.executeQuery(select)) {
        		CachedRowSet rowSet = new CachedRowSetImpl();
            rowSet.populate(rs);
            while (rowSet.next()) {
            		String tableName = rowSet.getString("table_name"); 	
            		res.add(tableName);
            }
            rowSet.close();
        }
		return res;
	}
	
	public ArrayList<String> getSchemaNames() throws ClassNotFoundException, SQLException{
		Class.forName("org.postgresql.Driver");
		ArrayList<String> res = new ArrayList<String>();
        String select = "SELECT schema_name FROM information_schema.schemata;";
        try (Connection conn = getDBConnection();
        		Statement stmt = conn.createStatement();
        		ResultSet rs = stmt.executeQuery(select)) {
        		CachedRowSet rowSet = new CachedRowSetImpl();
            rowSet.populate(rs);
            while (rowSet.next()) {
            		String schemaName = rowSet.getString("schema_name"); 	
            		res.add(schemaName);
            }
            rowSet.close();	
        }
		return res;
	}
	
	public ArrayList<String> getTableCulomns(String schema, String tableName) throws ClassNotFoundException, SQLException {
		ArrayList<String> columns = new ArrayList<>();
		Class.forName("org.postgresql.Driver");

        String select = "SELECT column_name FROM information_schema.columns WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA='" + schema +"'";
        try (Connection conn = getDBConnection();
        		Statement stmt = conn.createStatement();
        		ResultSet rs = stmt.executeQuery(select)) {
        		CachedRowSet rowSet = new CachedRowSetImpl();
            rowSet.populate(rs);
            while (rowSet.next()) {
            		String columnName = rowSet.getString("column_name"); 	
            		columns.add(columnName);
            }
            rowSet.close();
        }
    
		return columns;
	}
	
	public void addColumnToTable(String schema, String tableName, String columnName, DATA_TYPE columnType, boolean nullable, boolean withDefaultValue, Object defaultValue, boolean isArray) throws ClassNotFoundException, SQLException{
		Class.forName("org.postgresql.Driver");

        String cmd = "ALTER TABLE "+ schema +"." + tableName  + " ADD COLUMN " + columnName + " " + convertDataTypeToDBType(columnType);
        if (isArray) {
        		cmd += " ARRAY ";
        }
        if (!nullable) {
        		cmd += " NOT NULL";
        }
        if (withDefaultValue) {
        		cmd += " DEFAULT " + defaultValueToString(columnType, defaultValue);
        }
        
        Connection conn = getDBConnection();
        	Statement stmt = conn.createStatement();
        	stmt.executeUpdate(cmd);
	}
	
	public void updateColumnDefaultValue(String schema, String tableName, String columnName, DATA_TYPE columnType, Object defaultValue) throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");

        String cmd = "ALTER TABLE "+ schema +"." + tableName  + " ALTER COLUMN " + columnName;
        	cmd += " SET DEFAULT " + defaultValueToString(columnType, defaultValue);
        
        Connection conn = getDBConnection();
        	Statement stmt = conn.createStatement();
        	stmt.executeUpdate(cmd);
	}
	
	public void dropColumnDefaultValue(String schema, String tableName, String columnName) throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");

        String cmd = "ALTER TABLE "+ schema +"." + tableName  + " ALTER COLUMN " + columnName + " DROP DEFAULT;";
        
        Connection conn = getDBConnection();
        	Statement stmt = conn.createStatement();
        	stmt.executeUpdate(cmd);
	}
	
	private static String defaultValueToString(DATA_TYPE dataType, Object defaultValue) {
		if (defaultValue == null) {
			return "NULL";
		}
		if (dataType.equals(DATA_TYPE.STRING)) {
			return "'"+defaultValue.toString()+"'";
		}
		return defaultValue.toString();
	}
	
	private static String convertDataTypeToDBType(DATA_TYPE columnType) {
		switch (columnType) {
			case STRING :
				return "VARCHAR";
			case LONG : 
				return "bigint";
			case INTEGER:
				return "integer";
			case BOOLEAN:
				return "boolean";
			case DOUBLE:
				return "decimal";
			case  TIMESTAMP:
				return "timestamp";
			case JSON:
				return "jsonb";
		}
		return null;
	}
	
	private static DATA_TYPE convertDBTypToDataType( String columnDBType) {
		switch (columnDBType) {
			case "character varying" :
				return DATA_TYPE.STRING;
			case "bigint" : 
				return DATA_TYPE.LONG;
			case "integer":
				return DATA_TYPE.INTEGER;
			case "boolean":
				return DATA_TYPE.BOOLEAN;
			case "numeric":
				return DATA_TYPE.DOUBLE;
			case "jsonb":
				return DATA_TYPE.JSON;
			case "timestamp":
			case "timestamp without time zone":
				return DATA_TYPE.TIMESTAMP;	
		}
		return null;
	}
	
	private static DATA_TYPE convertArrayDBTypToDataType( String columnDBArrayType) {
		switch (columnDBArrayType) {
			case "_varchar" :
				return DATA_TYPE.STRING;
			case "_int8" : 
				return DATA_TYPE.LONG;
			case "_int4":
				return DATA_TYPE.INTEGER;
			case "_bool":
				return DATA_TYPE.BOOLEAN;
			case "_numeric":
				return DATA_TYPE.DOUBLE;
			case "_jsonb":
				return DATA_TYPE.JSON;
			case "_timestamp":
			case "_timestamp without time zone":
				return DATA_TYPE.TIMESTAMP;	
		}
		return null;
	}

	public void removeColumnFromTable(String schema, String tableName, String columnName) throws ClassNotFoundException, SQLException{
		Class.forName("org.postgresql.Driver");

        String cmd = "ALTER TABLE "+ schema + "." +tableName  + " DROP COLUMN " + columnName;
        Connection conn = getDBConnection();
        	Statement stmt = conn.createStatement();
        	stmt.executeUpdate(cmd);
	}

	public void updateColumnName(String schema, String tableName, String prevDbColumn, String newDbColumn) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");

        String cmd = "ALTER TABLE "+ schema +"." + tableName  + " RENAME COLUMN " + prevDbColumn + " TO " + newDbColumn;
        	
        Connection conn = getDBConnection();
        	Statement stmt = conn.createStatement();
        	stmt.executeUpdate(cmd);
	}
	
	public ArrayList<TableColumn> getTableCulomnsData(String schema, String tableName) throws ClassNotFoundException, SQLException {
		ArrayList<TableColumn> columns = new ArrayList<>();
		Class.forName("org.postgresql.Driver");

        String select = "SELECT column_name, data_type, is_nullable, column_default, udt_name FROM information_schema.columns WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA='" + schema +"'";
        try (Connection conn = getDBConnection();
        		Statement stmt = conn.createStatement();
        		ResultSet rs = stmt.executeQuery(select)) {
        		CachedRowSet rowSet = new CachedRowSetImpl();
            rowSet.populate(rs);
            while (rowSet.next()) {
            		String columnName = rowSet.getString("column_name"); 
            		String dataType = rowSet.getString("data_type"); 
            		String isNullable = rowSet.getString("is_nullable");
            		if (dataType.equals("ARRAY")) {
            			String udtName = rowSet.getString("udt_name");
            			columns.add(new TableColumn(columnName, convertArrayDBTypToDataType(udtName), "YES".equals(isNullable), true));
            		}
            		else {
            			columns.add(new TableColumn(columnName, convertDBTypToDataType(dataType), "YES".equals(isNullable), false));
            		}
            }
            rowSet.close();
        }
		return columns;
	}
	
}
