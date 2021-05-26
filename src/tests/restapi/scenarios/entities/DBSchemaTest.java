package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.ibm.airlock.admin.db.DbHandler;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.CohortsRestApi;
import tests.restapi.EntitiesRestApi;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

public class DBSchemaTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID;
	private String sessionToken = "";
	private String entityID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID); 

		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
	}
	
	@Test (description="get all db schemas")
	public void getAllDBSchemas() throws JSONException, IOException{
		String resopnse = entitiesApi.getDbSchemas(sessionToken);
		Assert.assertFalse(resopnse.contains("error"), "fail get all db schemas:" + resopnse);
		JSONObject resObj = new JSONObject(resopnse);
		JSONArray schemas = resObj.getJSONArray("dbSchemas");
		Assert.assertTrue(schemas.size()>0, "wrong schemas size");
		boolean containsTestSchema = false;
		for (int i=0;i<schemas.size(); i++) {
			if(schemas.getString(i).equals("airlock_test")) {
				containsTestSchema = true;
				break;
			}
		}
		
		Assert.assertTrue(containsTestSchema, "airlock_test schemas is missing");
		
		//dev schema
		for (int i=0;i<schemas.size(); i++) {
			if(schemas.getString(i).equals("airlock_dev_test")) {
				containsTestSchema = true;
				break;
			}
		}
		
		Assert.assertTrue(containsTestSchema, "airlock_dev_test schemas is missing");
		
		//archive schema
		for (int i=0;i<schemas.size(); i++) {
			if(schemas.getString(i).equals("airlock_archive_test")) {
				containsTestSchema = true;
				break;
			}
		}
		
		Assert.assertTrue(containsTestSchema, "airlock_archive_test schemas is missing");
	}
	
	@Test (dependsOnMethods = "getAllDBSchemas", description="add entity when no schema configured")
	public void addEntityWithoutDBSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.remove("dbSchema");
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("dbSchema"), "add entity when no schema configured");
	}
	
	@Test (dependsOnMethods = "addEntityWithoutDBSchema", description="add entity when no dev schema configured")
	public void addEntityWithoutDBDevSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.remove("dbDevSchema");
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("dbDevSchema"), "add entity when no dev schema configured");
	}
	
	@Test (dependsOnMethods = "addEntityWithoutDBDevSchema", description="add entity when no archive schema configured")
	public void addEntityWithoutDBArchiveSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.remove("dbArchiveSchema");
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("dbArchiveSchema"), "add entity when no archive schema configured");
	}
	
	@Test (dependsOnMethods = "addEntityWithoutDBArchiveSchema", description="configure non existing schema to product")
	public void addEntityWithNonExistingSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.put("dbSchema", "xxx");
		
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("xxx"), "add entity with non existing schema");
	}
	
	@Test (dependsOnMethods = "addEntityWithNonExistingSchema", description="configure non existing dev schema to product")
	public void addEntityWithNonExistingDevSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.put("dbDevSchema", "xxx");
		
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("xxx"), "add entity with non existing dev schema");
	}
	
	@Test (dependsOnMethods = "addEntityWithNonExistingDevSchema", description="configure non existing archive schema to product")
	public void addEntityWithNonExistingArchiveSchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		eObj.put("dbArchiveSchema", "xxx");
		
		String resopnse = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertTrue(resopnse.contains("error") && resopnse.contains("xxx"), "add entity with non existing archive schema");
	}
	
	@Test (dependsOnMethods = "addEntityWithNonExistingArchiveSchema", description="update schema ")
	public void updateEntitySchema() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		JSONObject eObj = new JSONObject(entity);
		
		eObj.put("dbDevSchema", JSONObject.NULL);
		eObj.put("dbArchiveSchema", JSONObject.NULL);
		
		entityID = entitiesApi.createEntity(productID, eObj.toString(), sessionToken);
		Assert.assertFalse(entityID.contains("error"), "cannot create entity: " + entityID);
		
		entity = entitiesApi.getEntity(entityID, sessionToken);
		Assert.assertFalse(entity.contains("error"), "Cannot get entity");
		
		eObj = new JSONObject(entity);
		eObj.remove("attributes");
		eObj.remove("attributeTypes");
		eObj.put("dbSchema", "airlock_test2");
		String respone = entitiesApi.updateEntity(entityID, eObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbSchema"), "Can update entity dbSchema");
	}
	
	@Test (dependsOnMethods = "updateEntitySchema", description="update dev schema")
	public void updateEntityDevSchema() throws JSONException, IOException{
		String entity = entitiesApi.getEntity(entityID, sessionToken);
		Assert.assertFalse(entity.contains("error"), "Cannot get entity");
		
		JSONObject eObj = new JSONObject(entity);
		eObj.remove("attributes");
		eObj.remove("attributeTypes");
		eObj.put("dbDevSchema", "airlock_test2");
		String respone = entitiesApi.updateEntity(entityID, eObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbDevSchema"), "Can update entity dbDevSchema");
	}
	
	@Test (dependsOnMethods = "updateEntityDevSchema", description="update archive schema")
	public void updateEntityArchhiveSchema() throws JSONException, IOException{
		String entity = entitiesApi.getEntity(entityID, sessionToken);
		Assert.assertFalse(entity.contains("error"), "Cannot get entity");
		
		JSONObject eObj = new JSONObject(entity);
		eObj.remove("attributes");
		eObj.remove("attributeTypes");
		eObj.put("dbArchiveSchema", "airlock_test2");
		String respone = entitiesApi.updateEntity(entityID, eObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbArchiveSchema"), "Can update entity dbArchiveSchema");
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
