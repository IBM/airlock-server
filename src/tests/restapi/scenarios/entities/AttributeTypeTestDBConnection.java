package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EntitiesRestApi;

import java.io.IOException;
import java.sql.SQLException;


public class AttributeTypeTestDBConnection {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String entityID1;
	private String entityID2;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	
	static final String dbSchema = "airlock_test";
	//test piState - create+update / legal+illegal values
	//test dbTable - create+update / legal+illegal values
	//get all schema's tables 
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);

		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		entityStr = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		attributeTypeStr = FileUtils.fileToString(filePath + "airlytics/entities/attributeType1.txt", "UTF-8", false);
	}
	/*
	@Test (description="add db schema to product")
	public void addDBSchema() throws JSONException, IOException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", dbSchema);
		
		entities = entitiesApi.updateEntities(productID1, entitiesObj.toString(), sessionToken);
		entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}
	*/
	@Test (description="add entities")
	public void addEntities() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
		
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create entity2");
	}
		
	@Test (dependsOnMethods = "addEntities", description="add attribute to entity with wrong name field")
	public void addAttributeTypeTestNameField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.remove("name");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute type with missing name field");
		
		attributeType.put("name", "");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute type with empty name field");
		
		attributeType.put("name", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute type with null name field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestNameField", description="list the tables in the db")
	public void listTablesInDB() throws JSONException, IOException{
		String response = entitiesApi.getDbTablesInSchema(dbSchema, sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot list db tables");
		
		JSONObject resObj = new JSONObject(response);
		JSONArray schemas = resObj.getJSONArray("dbTables");
		Assert.assertTrue(schemas.size()>0, "wrong tables size");
		
		//look for table1
		boolean containsTestSchema = false;
		for (int i=0;i<schemas.size(); i++) {
			if(schemas.getString(i).equals("table1")) {
				containsTestSchema = true;
				break;
			}
		}
		Assert.assertTrue(containsTestSchema, "table1 is missing");
		
		//look for table2
		containsTestSchema = false;
		for (int i=0;i<schemas.size(); i++) {
			if(schemas.getString(i).equals("table2")) {
				containsTestSchema = true;
				break;
			}
		}		
		Assert.assertTrue(containsTestSchema, "table2 is missing");
	}
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		baseUtils.reset(productID1, sessionToken);
	}

	@Test (dependsOnMethods = "listTablesInDB", description="create attribute type with not existing table")
	public void createAttributeTypeWithNonExistingTable() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("dbTable", "nonExistingTable");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("does not exist in the database"), "Can create attribute type with non existing dbTable");
	}
	
	@Test (dependsOnMethods = "createAttributeTypeWithNonExistingTable", description="add attribute types")
	public void addAttributeTypes() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
		
		attributeType.put("name", "attributeType2");
		attributeType.put("dbTable", "table2");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create attributeType2");		
	}
	
	@Test (dependsOnMethods = "addAttributeTypes", description="update attribute type to not existing table")
	public void updateAttributeTypeTohNonExistingTable() throws JSONException, IOException{
		String attributeType2 = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType2.contains("error"), "Cannot get attributeType2: " + attributeType2);
		
		JSONObject attributeTypeObj2 = new JSONObject(attributeType2);
		attributeTypeObj2.put("dbTable", "nonExistingTable");
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeTypeObj2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can update attribute type to non existing dbTable");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeTohNonExistingTable", description="update db table")
	public void updateDbTable() throws JSONException, IOException{
		String attributeType1 = entitiesApi.getAttributeType(attributeTypeID1, sessionToken);
		Assert.assertFalse(attributeType1.contains("error"), "Cannot get attributeType1");
		
		JSONObject attributeTypeObj1 = new JSONObject(attributeType1);
		attributeTypeObj1.put("dbTable", "table2");
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeTypeObj1.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can update dbTable");
	}

}
