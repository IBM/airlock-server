package tests.restapi;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.db.DbHandler;
import com.ibm.airlock.admin.db.DbHandler.TableColumn;

import tests.com.ibm.qautils.RestClientUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;


public class EntitiesRestApi
{
	protected String m_url;

	public void setURL(String url) throws IOException
	{
		m_url = url;
	}

	//GET /admin/airlytics/products/{product-id}/entities
	public String getProductEntities(String productID, String sessionToken){
		String response="";
		try { 			
			RestClientUtils.RestCallResults res= RestClientUtils.sendGet(m_url+"/airlytics/products/" + productID + "/entities", sessionToken);
			response = res.message;

		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to get entities. Message: "+e.getMessage()) ;
		}
		return response;
	}
/*
	//PUT /admin/airlytics/products/{product-id}/entities
	public String updateEntities(String productID, String entities, String sessionToken){
		String response="";
		try { 			
			RestClientUtils.RestCallResults res= RestClientUtils.sendPut(m_url+"/airlytics/products/" + productID + "/entities", entities, sessionToken);
			response = res.message;

		} catch (Exception e) {
			System.out.println("An exception was thrown when trying  to update all entities. Message: "+e.getMessage()) ;
		}
		return response;
	}*/

	//POST /admin/airlytics/products/{product-id}/entities
	public String createEntity(String productID, String entity, String sessionToken) throws IOException{
		String entityID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/" + productID + "/entities", entity, sessionToken);
		entityID = parseId(res.message);
		return entityID;	
	}


	//POST /admin/airlytics/products/entities/{entity-id}/attributetypes
	public String createAttributeType(String entityID, String attributeType, String sessionToken) throws IOException{
		String attributeTypeID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/entities/" + entityID + "/attributetypes", attributeType, sessionToken);
		attributeTypeID = parseId(res.message);
		return attributeTypeID;	
	}

	public String updateAttributeType(String attributeTypeID, String attributeType, String sessionToken) throws IOException{
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/products/entities/attributetypes/" + attributeTypeID, attributeType, sessionToken);
			responseID = parseId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to update AttributeType. Message: "+e.getMessage()) ;
		}
		return responseID;	
	}
	
	public String updateAttribute(String attributeID, String attribute, String sessionToken) throws IOException{
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/products/entities/attributes/" + attributeID, attribute, sessionToken);
			responseID = parseId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to update Attribute. Message: "+e.getMessage()) ;
		}
		return responseID;	
	}
	
	public String updateAttributeIgnoreExistence(String attributeID, String attribute, String sessionToken) throws IOException{
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/products/entities/attributes/" + attributeID + "?ignoreexistence=true", attribute, sessionToken);
			responseID = parseId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to update Attribute. Message: "+e.getMessage()) ;
		}
		return responseID;	
	}

	//POST /admin/airlytics/products/entities/{entity-id}/attributes
	public String createAttribute(String entityID, String attribute, String sessionToken) throws IOException{
		String attributeID = "";
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/entities/" + entityID + "/attributes", attribute, sessionToken);
		attributeID = parseId(res.message);
		return attributeID;	
	}
	
	//POST /admin/airlytics/products/entities/{entity-id}/attributes
	public String createAttributeIgnoreExistence(String entityID, String attribute, String sessionToken) throws IOException{
			String attributeID = "";
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/airlytics/products/entities/" + entityID + "/attributes?ignoreexistence=true", attribute, sessionToken);
			attributeID = parseId(res.message);
			return attributeID;	
		}

	// /admin/airlytics/products/entities/{entity-id} - delete , update, get
	public String getEntity(String entityID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/products/entities/" + entityID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get airlock entity. Message: "+e.getMessage()) ;
		}
		return response;
	}

	// /admin/airlytics/products/entities/attributetypes/{attributetype-id} 
	public String getAttributeType(String attributeTypeID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/products/entities/attributetypes/" + attributeTypeID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get airlock attributeType. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public String getAttribute(String attributeID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/products/entities/attributes/" + attributeID, sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get airlock attribute. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public int deleteAttributeType(String attributeTypeID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlytics/products/entities/attributetypes/" + attributeTypeID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to delete airlock attributeType. Message: "+e.getMessage()) ;
		}
		return -1;
	}
	
	public int deleteAttribute(String attributeID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlytics/products/entities/attributes/" + attributeID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to delete airlock attribute. Message: "+e.getMessage()) ;
		}
		return -1;
	}

	public String getAttributeTypes(String entityID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/products/entities/" + entityID + "/attributetypes", sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get attributeTypes. Message: "+e.getMessage()) ;
		}
		return response;
	}
	
	public String getAttributes(String entityID, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/products/entities/" + entityID + "/attributes", sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get attributes. Message: "+e.getMessage()) ;
		}
		return response;
	}

	// /admin/airlytics/dbschemas
	public String getDbSchemas(String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/dbschemas", sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get db schemas. Message: "+e.getMessage()) ;
		}
		return response;
	}

	// /admin/airlytics/schemas/{dbschema}/tables
	public String getDbTablesInSchema(String dbSchema, String sessionToken){
		String response="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/airlytics/dbschemas/" + dbSchema + "/tables", sessionToken);
			response = res.message;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to get db tables. Message: "+e.getMessage()) ;
		}
		return response;
	}

	public int deleteEntity(String entityID, String sessionToken){
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(m_url+"/airlytics/products/entities/" + entityID, sessionToken);
			return res.code;
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to delete airlock entity. Message: "+e.getMessage()) ;
		}
		return -1;
	}

	public String updateEntity(String entityID, String entity, String sessionToken){
		String responseID="";
		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(m_url+"/airlytics/products/entities/" + entityID, entity, sessionToken);
			responseID = parseId(res.message);
		} catch (Exception e) {
			System.out.println("An exception was thrown when trying to update airlock entity. Message: "+e.getMessage()) ;
		}
		return responseID;
	}

	private String parseId(String item){
		String itemID = "";
		JSONObject response = null;
		try {
			response = new JSONObject(item);
			if (response.containsKey("error")){
				itemID = item;
			} else {
				itemID = (String)response.get("uniqueId");
			}
		} catch (JSONException e) {
			itemID = "Invalid response: " + response;
		}

		return itemID;
	}
	
	public void deleteAllColumnsFromTable(DbHandler dbHandler ,String schema, String table) throws ClassNotFoundException, SQLException {
		List<TableColumn> dbColumns = dbHandler.getTableCulomnsData(schema, table);
		for (TableColumn col:dbColumns) {
			try { 
				dbHandler.removeColumnFromTable(schema, table, col.getName());
			} catch (Exception e) {
				//ignore
			}
		}
		
		
	}

	public void deleteColumnsFromAthena(AthenaHandler athenaHandler, String database, String table, LinkedList<String> athenaColumnsToDelete) {
		for (String col:athenaColumnsToDelete) {
			try {
				athenaHandler.deleteColumn(database, table, col);
			} catch (Exception e) {
				//ignore
			}
		}
		
	}

}
