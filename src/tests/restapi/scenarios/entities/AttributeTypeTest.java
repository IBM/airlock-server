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


public class AttributeTypeTest {
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
	
	//add attribute type twice with the same name
	//update attribute type twice with the same name
	//add attribute type without name, with null name, with empty name
	//add attribute type without creator, with null creator, with empty creator
	//add attribute type without dbTable, with null dbTable, with empty dbTable
	//add attribute type without piState, with null piState, with empty piState
	//update creator
	//test update name
	//test create returned fields
	
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
		entitiesObj.put("dbSchema", "airlock_test");
		
		entities = entitiesApi.updateEntities(productID1, entitiesObj.toString(), sessionToken);
		entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}*/
	
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
		
	@Test (dependsOnMethods = "addEntities", description="add attribute type to entity with wrong name field")
	public void addAttributeTypeTestNameField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.remove("name");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute type with missing name field");
		
		attributeType.put("name", "");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute type with empty name field");
		
		attributeType.put("name", JSONObject.NULL);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute type with null name field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestNameField", description="add attribute type to entity with wrong dbTable field")
	public void addAttributeTypeTestDbTableField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.remove("dbTable");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbTable"), "Can create attribute type with missing dbTable field");
		
		attributeType.put("dbTable", "");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbTable"), "Can create attribute type with empty dbTable field");
		
		attributeType.put("dbTable", JSONObject.NULL);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbTable"), "Can create attribute type with null dbTable field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestDbTableField", description="add attribute type to entity with wrong creator field")
	public void addAttributeTypeTestCreatorField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.remove("creator");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute type with missing creator field");
		
		attributeType.put("creator", "");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute type with empty creator field");
		
		attributeType.put("creator", JSONObject.NULL);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute type with null creator field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestCreatorField", description="add attribute type to entity with wrong piState field")
	public void addAttributeTypeTestPiStateField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.remove("piState");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("piState"), "Can create attribute type with missing piState field");
		
		attributeType.put("piState", "");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("piState"), "Can create attribute type with empty piState field");
		
		attributeType.put("piState", JSONObject.NULL);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("piState"), "Can create attribute type with null piState field");
		
		attributeType.put("piState", "XXX");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("piState"), "Can create attribute type with illegal piState field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestCreatorField", description="add attribute type to entity with wrong piState field")
	public void addAttributeTypeTestAttributesPermissionField() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("attributesPermission", "");
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission"), "Can create attribute type with empty attributesPermission field");
		
		attributeType.put("attributesPermission", JSONObject.NULL);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission"), "Can create attribute type with null attributesPermission field");
		
		attributeType.put("attributesPermission", "XXX");
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission"), "Can create attribute type with illegal attributesPermission field");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeTestAttributesPermissionField", description="add attribute to with existing name")
	public void addAttributeTypeNameTwice() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("name", "attributeType1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
		
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute type with name that already exists");
		
		attributeType.put("name", "attributeType2");
		attributeType.put("piState", "NOT_PI");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create attributeType2");		
	}
	
	@Test (dependsOnMethods = "addAttributeTypeNameTwice", description="update AttributeType to with existing name")
	public void updateAttributeTypeNameTwice() throws JSONException, IOException{
		String attributeType2 = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType2.contains("error"), "Cannot get attributeType2");
		
		JSONObject attributeTypeObj2 = new JSONObject(attributeType2);
		attributeTypeObj2.put("name", "attributeType1");
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeTypeObj2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can update AttributeType name to name that already exists");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeNameTwice", description="update attributeType creator")
	public void updateAttributeTypeCreator() throws JSONException, IOException{
		String attributeType2 = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType2.contains("error"), "Cannot get attributeType2: " + attributeType2);
		
		JSONObject attributeType2Obj = new JSONObject(attributeType2);
		attributeType2Obj.put("creator", "aaa");
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeType2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can update attributeType's creator");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeCreator", description="update attributeType creator")
	public void updateAttributeTypeWrongAttributesPermission() throws JSONException, IOException{
		String attributeType2 = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType2.contains("error"), "Cannot get attributeType2: " + attributeType2);
		
		JSONObject attributeType2Obj = new JSONObject(attributeType2);
		attributeType2Obj.put("attributesPermission", JSONObject.NULL);
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeType2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission"), "Can update attributeType's creator");
		
		attributeType2Obj = new JSONObject(attributeType2);
		attributeType2Obj.put("attributesPermission", "BBB");
		respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeType2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission"), "Can update attributeType's creator");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeWrongAttributesPermission", description="update attribute type")
	public void updateAttributeType() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType2");
		
		JSONObject attributeType2 = new JSONObject(attributeType);
		attributeType2.put("name", "test3");
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeType2.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error") , "cannot update attribute type name: " + respone);
		
		attributeType = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType2");
		attributeType2 = new JSONObject(attributeType);
		Assert.assertTrue(attributeType2.getString("name").equals("test3"), "name was not updated");
		Assert.assertTrue(attributeType2.getString("entityId").equals(entityID1), "wrong entityId");
	}
	
	@Test (dependsOnMethods = "updateAttributeType", description="update entity id")
	public void updateAttributeTypeEntityId() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType2");
		
		JSONObject attributeType2 = new JSONObject(attributeType);
		attributeType2.put("entityId", entityID2);
		String respone = entitiesApi.updateAttributeType(attributeTypeID2, attributeType2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") , "can update entity id");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeEntityId", description="get attribute types")
	public void getAttributeTypes() throws JSONException, IOException, JSONException{
		String attributeTypesStr = entitiesApi.getAttributeTypes(entityID1, sessionToken);
		Assert.assertFalse(attributeTypesStr.contains("error"), "Cannot get attribute types: " + attributeTypesStr);
		
		JSONObject attributeTypesObj = new JSONObject(attributeTypesStr);
		JSONArray entitiesArr = attributeTypesObj.getJSONArray("attributeTypes");
		Assert.assertTrue(entitiesArr.length() == 2, "wrong numbner of attributeTypes");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("uniqueId").equals(attributeTypeID1), "wrong attributeType1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("name").equals("attributeType1"), "wrong attributeType1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("entityId").equals(entityID1), "wrong attributeType1");
		
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("uniqueId").equals(attributeTypeID2), "wrong attributeType2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("name").equals("test3"), "wrong attributeType2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("entityId").equals(entityID1), "wrong attributeType2");
	}
	
	@Test (dependsOnMethods = "getAttributeTypes", description="delete attribute type")
	public void deleteAttributeTypes() throws JSONException, IOException, JSONException{
		String attributeTypesStr = entitiesApi.getAttributeTypes(entityID1, sessionToken);
		Assert.assertFalse(attributeTypesStr.contains("error"), "Cannot get attribute types: " + attributeTypesStr);
		
		JSONObject attributeTypesObj = new JSONObject(attributeTypesStr);
		JSONArray attributeTypesArr = attributeTypesObj.getJSONArray("attributeTypes");
		Assert.assertTrue(attributeTypesArr.length() == 2, "wrong numbner of attribute types");
		
		int code = entitiesApi.deleteAttributeType(attributeTypeID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attributeTypes1");
		
		attributeTypesStr = entitiesApi.getAttributeTypes(entityID1, sessionToken);
		Assert.assertFalse(attributeTypesStr.contains("error"), "Cannot get attribute types: " + attributeTypesStr);
		
		attributeTypesObj = new JSONObject(attributeTypesStr);
		attributeTypesArr = attributeTypesObj.getJSONArray("attributeTypes");
		Assert.assertTrue(attributeTypesArr.length() == 1, "wrong numbner of attribute types");
		Assert.assertTrue(attributeTypesArr.getJSONObject(0).getString("uniqueId").equals(attributeTypeID2), "wrong AttributeType2");
		Assert.assertTrue(attributeTypesArr.getJSONObject(0).getString("name").equals("test3"), "wrong AttributeType2");
		Assert.assertTrue(attributeTypesArr.getJSONObject(0).getString("entityId").equals(entityID1), "wrong AttributeType2");
		
		String response = entitiesApi.getAttributeType(attributeTypeID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted attribute type");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID1, sessionToken);
		Assert.assertTrue(code == 400, "can delete attributeType1 twice");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID2, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete AttributeType2");
		
		attributeTypesStr = entitiesApi.getAttributeTypes(entityID1, sessionToken);
		Assert.assertFalse(attributeTypesStr.contains("error"), "Cannot get attribute types: " + attributeTypesStr);
		
		attributeTypesObj = new JSONObject(attributeTypesStr);
		attributeTypesArr = attributeTypesObj.getJSONArray("attributeTypes");
		Assert.assertTrue(attributeTypesArr.length() == 0, "wrong numbner of attribute types");
		
		response = entitiesApi.getAttributeType(attributeTypeID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted attribute type");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID2, sessionToken);
		Assert.assertTrue(code == 400, "can delete attributeTypes2 twice");
	}
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		baseUtils.reset(productID1, sessionToken);
	}

	

}
