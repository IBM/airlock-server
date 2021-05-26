package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EntitiesRestApi;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;


public class AttributeTypeTestAttributesPermission {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String entityID1;
	private String attributeTypeID;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	
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
	}
	*/
	@Test (description="add entities")
	public void addEntities() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
	}
		
	@Test (dependsOnMethods = "addEntities", description="add attribute type to entity with wrong READ_ONLY permission in attributesPermission field")
	public void addAttributeTypeIllegalAttributeROPermissions() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		JSONObject attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_ONLY");
		attributeType.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_ONLY"), "Can create attribute type with missing READ_ONLY permission");
		
		attributeType = new JSONObject(attributeTypeStr);
		attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_ONLY", "xxx");
		attributeType.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("xxx"), "Can create attribute type with wrong READ_ONLY role");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeIllegalAttributeROPermissions", description="add attribute type to entity with wrong READ_WRITE permission in attributesPermission field")
	public void addAttributeTypeIllegalAttributeRWPermissions() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		JSONObject attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE");
		attributeType.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE"), "Can create attribute type with missing READ_WRITE permission");
		
		attributeType = new JSONObject(attributeTypeStr);
		attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE", "yyy");
		attributeType.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("yyy"), "Can create attribute type with wrong READ_WRITE role");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeIllegalAttributeRWPermissions", description="add attribute type to entity with wrong READ_WRITE_DEPRECATE permission in attributesPermission field")
	public void addAttributeTypeIllegalAttributeRWDepPermissions() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		JSONObject attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE_DEPRECATE");
		attributeType.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE_DEPRECATE"), "Can create attribute type with missing READ_WRITE_DEPRECATE permission");
		
		attributeType = new JSONObject(attributeTypeStr);
		attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "yyy");
		attributeType.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("yyy"), "Can create attribute type with wrong READ_WRITE_DEPRECATE role");
	}
	
	@Test (dependsOnMethods = "addAttributeTypeIllegalAttributeRWDepPermissions", description="add attribute type to entity with wrong READ_WRITE_DELETE permission in attributesPermission field")
	public void addAttributeTypeIllegalAttributeRWDelPermissions() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		JSONObject attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE_DELETE");
		attributeType.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE_DELETE"), "Can create attribute type with missing READ_WRITE_DELETE permission");
		
		attributeType = new JSONObject(attributeTypeStr);
		attributesPermissionObj = attributeType.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE_DELETE", "zzz");
		attributeType.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("zzz"), "Can create attribute type with wrong READ_WRITE_DELETE role");
	}
		
	@Test (dependsOnMethods = "addAttributeTypeIllegalAttributeRWDelPermissions", description="add attribute type to entity")
	public void addAttributeTypeNoPermissions() throws JSONException, IOException{
		JSONObject atObj = new JSONObject(attributeTypeStr);
		atObj.remove("attributesPermission");
		attributeTypeID = entitiesApi.createAttributeType(entityID1, atObj.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID.contains("error"), "Cannot create attribute type: " + attributeTypeID);
		
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		atObj = new JSONObject(attributeType);
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_ONLY").equals("AnalyticsViewer"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE").equals("AnalyticsEditor"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DEPRECATE").equals("AnalyticsPowerUser"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DELETE").equals("Administrator"));	
	}
	
	@Test (dependsOnMethods = "addAttributeTypeNoPermissions", description="update attribute type with wrong READ_ONLY permission in attributesPermission field")
	public void updateAttributeTypeIllegalAttributeROPermissions() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_ONLY");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_ONLY"), "Can update attribute type with missing READ_ONLY permission");
		
		attributeTypeObj = new JSONObject(attributeType);
		attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_ONLY", "xxx");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("xxx"), "Can update attribute type with wrong READ_ONLY role");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeIllegalAttributeROPermissions", description="update attribute type with wrong READ_WRITE permission in attributesPermission field")
	public void updateAttributeTypeIllegalAttributeRWPermissions() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE"), "Can update attribute type with missing READ_WRITE permission");
		
		attributeTypeObj = new JSONObject(attributeType);
		attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE", "yyy");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("yyy"), "Can update attribute type with wrong READ_WRITE role");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeIllegalAttributeRWPermissions", description="update attribute type with wrong READ_WRITE_DEPRECATE permission in attributesPermission field")
	public void updateAttributeTypeIllegalAttributeRWDepPermissions() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE_DEPRECATE");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE_DEPRECATE"), "Can update attribute type with missing READ_WRITE_DEPRECATE permission");
		
		attributeTypeObj = new JSONObject(attributeType);
		attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "eee");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("eee"), "Can update attribute type with wrong READ_WRITE_DEPRECATE role");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeIllegalAttributeRWDepPermissions", description="update attribute type with wrong READ_WRITE_DELETE permission in attributesPermission field")
	public void updateAttributeTypeIllegalAttributeRWDelPermissions() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.remove("READ_WRITE_DELETE");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributesPermission")  && respone.contains("READ_WRITE_DELETE"), "Can update attribute type with missing READ_WRITE_DELETE permission");
		
		attributeTypeObj = new JSONObject(attributeType);
		attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE_DELETE", "ccc");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("ccc"), "Can update attribute type with wrong READ_WRITE_DELETE role");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeIllegalAttributeRWDelPermissions", description="update attribute type attributesPermission field")
	public void updateAttributeTypeAttributesPermission() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = attributeTypeObj.getJSONObject("attributesPermission");
		attributesPermissionObj.put("READ_WRITE_DELETE", "AnalyticsPowerUser");
		attributesPermissionObj.put("READ_WRITE", "AnalyticsViewer");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute type: " + respone); 
		
		attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject atObj = new JSONObject(attributeType);
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_ONLY").equals("AnalyticsViewer"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE").equals("AnalyticsViewer"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DEPRECATE").equals("AnalyticsPowerUser"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DELETE").equals("AnalyticsPowerUser"));	
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeAttributesPermission", description="update attribute type without attributesPermission field")
	public void updateAttributeTypeNoAttributesPermission() throws JSONException, IOException{
		String attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		attributeTypeObj.remove("attributesPermission");
		String respone = entitiesApi.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute type: " + respone); 
		
		attributeType = entitiesApi.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType: " + attributeType);
		
		JSONObject atObj = new JSONObject(attributeType);
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_ONLY").equals("AnalyticsViewer"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE").equals("AnalyticsViewer"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DEPRECATE").equals("AnalyticsPowerUser"));
		assertTrue(atObj.getJSONObject("attributesPermission").getString("READ_WRITE_DELETE").equals("AnalyticsPowerUser"));	
	}
	
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		baseUtils.reset(productID1, sessionToken);
	}

	

}
