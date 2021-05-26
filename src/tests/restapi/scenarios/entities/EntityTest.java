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


public class EntityTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String productID2;
	private String entityID1;
	private String entityID2;
	private String sessionToken = "";
	private String entityStr;
	
	//add entity twice with the same name
	//update entity twice with the same name
	//add entity without name, with null name, with empty name
	//add entity without creator, with null creator, with empty creator
	//update creator
	//add entity with attributes
	//update entity with attributes
	//add entity with attributeTypes
	//update entity with attributeTypes
	//test update name
	//test create returned fields
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);

		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		entityStr = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
	}
	/*
	@Test (description="add db schema to product")
	public void addDBSchema() throws JSONException, IOException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", "airlock_test");
		
		entities = entitiesApi.updateEntities(productID1, entitiesObj.toString(), sessionToken);
		entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}
	*/
	@Test (description="add entity to product with wrong name field")
	public void addEntityTestNameField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("name");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing name field");
		
		entity.put("name", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty name field");
		
		entity.put("name", JSONObject.NULL);
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with null name field");
	}
	
	@Test (dependsOnMethods = "addEntityTestNameField", description="add entity to product with wrong creator field")
	public void addEntityTestCreatorField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("creator");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing creator field");
		
		entity.put("creator", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty creator field");
		
		entity.put("creator", JSONObject.NULL);
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with null creator field");
	}
	
	@Test (dependsOnMethods = "addEntityTestCreatorField", description="add entity to product with wrong dbSchema field")
	public void addEntityTestDBSchemaField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("dbSchema");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing dbSchema field");
		
		entity.put("dbSchema", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty dbSchema field");
		
		entity.put("dbSchema", JSONObject.NULL);
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with null dbSchema field");
	}
	
	@Test (dependsOnMethods = "addEntityTestDBSchemaField", description="add entity to product with wrong dbDevSchema field")
	public void addEntityTestDBDevSchemaField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("dbDevSchema");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing dbDevSchema field");
		
		entity.put("dbDevSchema", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty dbDevSchema field");
	}
	
	@Test (dependsOnMethods = "addEntityTestDBDevSchemaField", description="add entity to product with wrong dbArchiveSchema field")
	public void addEntityTestDBArchiveSchemaField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("dbArchiveSchema");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing dbArchiveSchema field");
		
		entity.put("dbArchiveSchema", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty dbArchiveSchema field");
	}
	
	@Test (dependsOnMethods = "addEntityTestDBArchiveSchemaField", description="add entity to product with wrong athenaDatabase field")
	public void addEntityTestAthenaDatabaseField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("athenaDatabase");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing athenaDatabase field");
		
		entity.put("athenaDatabase", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty athenaDatabase field");
		
		entity.put("athenaDatabase", JSONObject.NULL);
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with null athenaDatabase field");
	}
	
	@Test (dependsOnMethods = "addEntityTestAthenaDatabaseField", description="add entity to product with wrong athenaDevDatabase field")
	public void addEntityTestAthenaDevDatabaseField() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.remove("athenaDevDatabase");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with missing athenaDevDatabase field");
		
		entity.put("athenaDevDatabase", "");
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with empty athenaDevDatabase field");
	}
	
	@Test (dependsOnMethods = "addEntityTestAthenaDevDatabaseField", description="add entity to with existing name")
	public void addEntityNameTwice() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
		
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create entity with name that already exists");
		
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create entity2");		
	}
	
	@Test (dependsOnMethods = "addEntityNameTwice", description="update entity to with existing name")
	public void updateEntityNameTwice() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("name", "entity1");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can update entity name to name that already exists");
	}
	
	@Test (dependsOnMethods = "updateEntityNameTwice", description="update entity's creator")
	public void updateEntityCreator() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("creator", "aaa");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can update entity's creator");
	}
	
	@Test (dependsOnMethods = "updateEntityCreator", description="update entity's dbSchema")
	public void updateEntityDBSchema() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("dbSchema", "aaa");
		entity2Obj.remove("attributes");
		entity2Obj.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbSchema"), "Can update entity's dbSchema");
	}
	
	@Test (dependsOnMethods = "updateEntityDBSchema", description="update entity's dbDevSchema")
	public void updateEntityDBDevSchema() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("dbDevSchema", "airlock_archive_test");
		entity2Obj.remove("attributes");
		entity2Obj.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbDevSchema"), "Can update entity's dbDevSchema");
	}
	
	@Test (dependsOnMethods = "updateEntityDBDevSchema", description="update entity's dbArchiveSchema")
	public void updateEntityDBArchiveSchema() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("dbArchiveSchema", "airlock_dev_test");
		entity2Obj.remove("attributes");
		entity2Obj.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbArchiveSchema"), "Can update entity's dbArchiveSchema");
	}
	
	@Test (dependsOnMethods = "updateEntityDBArchiveSchema", description="update entity's athenaDatabase")
	public void updateEntityAthenaDatabase() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("athenaDatabase", "aaa");
		entity2Obj.remove("attributes");
		entity2Obj.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("database") && respone.contains("Athena"), "Can update entity's athenaDatabase");
	}
	
	@Test (dependsOnMethods = "updateEntityAthenaDatabase", description="update entity's athenaDevDatabase")
	public void updateEntityAthenaDevDatabase() throws JSONException, IOException{
		String entity2 = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entity2.contains("error"), "Cannot get entity2");
		
		JSONObject entity2Obj = new JSONObject(entity2);
		entity2Obj.put("athenaDevDatabase", "airlocktestsdb");
		entity2Obj.remove("attributes");
		entity2Obj.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("database") && respone.contains("Athena"), "Can update entity's athenaArchiveDatabase");
	}
	
	@Test (dependsOnMethods = "updateEntityAthenaDevDatabase", description="add entity with attributes")
	public void createEntityWithAttributes() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("attributes", new JSONArray());
		entity.put("name", "test1");
		String respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributes"), "Can create entity with attributes");
		
		entity.remove("attributes");
		entity.put("attributeTypes", new JSONArray());
		respone = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") & respone.contains("attribute types"), "Can create entity with attribute types");
	}
	
	@Test (dependsOnMethods = "createEntityWithAttributes", description="update entity with attributes")
	public void updateEntityWithAttributes() throws JSONException, IOException{
		String entityStr = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entityStr.contains("error"), "Cannot get entity2");
		
		JSONObject entity2 = new JSONObject(entityStr);
		entity2.remove("attributeTypes");
		String respone = entitiesApi.updateEntity(entityID2, entity2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributes"), "Can update entity with attributes");
		
		entity2 = new JSONObject(entityStr);
		entity2.remove("attributes");
		respone = entitiesApi.updateEntity(entityID2, entity2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") & respone.contains("attribute types"), "Can update entity with attribute types");
	}
	
	@Test (dependsOnMethods = "updateEntityWithAttributes", description="update entity")
	public void updateEntity() throws JSONException, IOException{
		String entityStr = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entityStr.contains("error"), "Cannot get entity2");
		
		JSONObject entity2 = new JSONObject(entityStr);
		entity2.remove("attributeTypes");
		entity2.remove("attributes");
		entity2.remove("deletedAttributesData");
		entity2.put("name", "test3");
		String respone = entitiesApi.updateEntity(entityID2, entity2.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error") , "cannot update entity name: " + respone);
		
		entityStr = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entityStr.contains("error"), "Cannot get entity2");
		entity2 = new JSONObject(entityStr);
		Assert.assertTrue(entity2.getString("name").equals("test3"), "name was not updated");
		Assert.assertTrue(entity2.getString("productId").equals(productID1), "wrong productId");
	}
	
	@Test (dependsOnMethods = "updateEntity", description="update product id")
	public void updateProductId() throws JSONException, IOException{
		String entityStr = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertFalse(entityStr.contains("error"), "Cannot get entity2");
		
		JSONObject entity2 = new JSONObject(entityStr);
		entity2.remove("attributeTypes");
		entity2.remove("attributes");
		entity2.remove("deletedAttributesData");
		entity2.put("productId", productID2);
		String respone = entitiesApi.updateEntity(entityID2, entity2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") , "can update product id");
	}
	
	@Test (dependsOnMethods = "updateProductId", description="get entity")
	public void getEntities() throws JSONException, IOException, JSONException{
		String entitiesStr = entitiesApi.getProductEntities(productID1, sessionToken);
		Assert.assertFalse(entitiesStr.contains("error"), "Cannot get entities: " + entitiesStr);
		
		JSONObject entitiesObj = new JSONObject(entitiesStr);
		JSONArray entitiesArr = entitiesObj.getJSONArray("entities");
		Assert.assertTrue(entitiesArr.length() == 2, "wrong numbner of entities");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("uniqueId").equals(entityID1), "wrong entity1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("name").equals("entity1"), "wrong entity1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("productId").equals(productID1), "wrong entity2");
		
		
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("uniqueId").equals(entityID2), "wrong entity2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("name").equals("test3"), "wrong entity2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("productId").equals(productID1), "wrong entity2");
		
	}
	
	@Test (dependsOnMethods = "getEntities", description="delete entity")
	public void deleteEntities() throws JSONException, IOException, JSONException{
		String entitiesStr = entitiesApi.getProductEntities(productID1, sessionToken);
		Assert.assertFalse(entitiesStr.contains("error"), "Cannot get entities: " + entitiesStr);
		
		JSONObject entitiesObj = new JSONObject(entitiesStr);
		JSONArray entitiesArr = entitiesObj.getJSONArray("entities");
		Assert.assertTrue(entitiesArr.length() == 2, "wrong numbner of entities");
		
		
		int code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entity1");
		
		entitiesStr = entitiesApi.getProductEntities(productID1, sessionToken);
		Assert.assertFalse(entitiesStr.contains("error"), "Cannot get entities: " + entitiesStr);
		
		entitiesObj = new JSONObject(entitiesStr);
		entitiesArr = entitiesObj.getJSONArray("entities");
		Assert.assertTrue(entitiesArr.length() == 1, "wrong numbner of entities");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("uniqueId").equals(entityID2), "wrong entity2");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("name").equals("test3"), "wrong entity2");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("productId").equals(productID1), "wrong entity2");
		
		String response = entitiesApi.getEntity(entityID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted entity");
		
		code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertTrue(code == 400, "can delete entity1 twice");
		
		code = entitiesApi.deleteEntity(entityID2, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entity2");
		
		entitiesStr = entitiesApi.getProductEntities(productID1, sessionToken);
		Assert.assertFalse(entitiesStr.contains("error"), "Cannot get entities: " + entitiesStr);
		
		entitiesObj = new JSONObject(entitiesStr);
		entitiesArr = entitiesObj.getJSONArray("entities");
		Assert.assertTrue(entitiesArr.length() == 0, "wrong numbner of entities");
		
		response = entitiesApi.getEntity(entityID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted entity");
		
		code = entitiesApi.deleteEntity(entityID2, sessionToken);
		Assert.assertTrue(code == 400, "can delete entity2 twice");
	}
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		baseUtils.reset(productID1, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}

	

}
