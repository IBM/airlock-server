package com.ibm.airlock.admin.airlytics.entities;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.db.DbHandler;

public class EntitiesUtilities {
	public static JSONObject schemasListToJSONObject(ServletContext context) throws ClassNotFoundException, SQLException, JSONException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		ArrayList<String> dbSchemas = dbHandler.getSchemaNames();
		JSONArray schemasArr = new JSONArray();
		for (String schema : dbSchemas) {
			schemasArr.add(schema);
		}
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_DB_SCHEMAS, schemasArr);
		return res;
	}

	//return null if schema exists 
	public static ValidationResults validateSchemaExistance(ServletContext context, String schema) throws ClassNotFoundException, SQLException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		ArrayList<String> dbSchemas = dbHandler.getSchemaNames();
		if (!Utilities.isStringInList(dbSchemas, schema)) {
			return new ValidationResults(String.format(Strings.noDbSchema, schema), Status.BAD_REQUEST);
		}
		return null;
	}
	
	public static JSONObject tablesListToJSONObject(ServletContext context, String schemaName) throws ClassNotFoundException, SQLException, JSONException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		ArrayList<String> dbSchemas = dbHandler.getTableNames(schemaName);
		JSONArray tablesArr = new JSONArray();
		for (String schema : dbSchemas) {
			tablesArr.add(schema);
		}
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_DB_TABLES, tablesArr);
		res.put(Constants.JSON_FIELD_DB_SCHEMA, schemaName);
		return res;
	}
	
	public static boolean compareObjects(Object obj1, Object obj2) {
	    return (obj1 == null ? obj2 == null : obj1.equals(obj2));
	}

}
