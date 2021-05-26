package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.db.DbHandler;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EntitiesRestApi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;


public class AttributeDeprecateTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String entityID1;
	private String entityID2;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String attributeTypeID3;
	private String attributeID1;
	
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	private String attributeStr;
	private DbHandler dbHandler;
	private AthenaHandler athenaHandler; 
	private LinkedList<String> athenaColumnsToDelete; 
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw", "athenaRegion", "athenaOutputBucket", "athenaCatalog"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw, String athenaRegion,  String athenaOutputBucket, String athenaCatalog) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		
		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		entityStr = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		attributeTypeStr = FileUtils.fileToString(filePath + "airlytics/entities/attributeType1.txt", "UTF-8", false);
		attributeStr = FileUtils.fileToString(filePath + "airlytics/entities/attribute1.txt", "UTF-8", false);
		
		dbHandler = new DbHandler(dburl, dbuser, dbpsw);
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archhive_test", "table1");
		
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);
		athenaColumnsToDelete = new LinkedList<>();
		athenaColumnsToDelete.add("acol1");
		athenaColumnsToDelete.add("acol2");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
	}
	
	@Test (description="add entities")
	public void addEntities() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
		
		
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
	}
		
	@Test (dependsOnMethods = "addEntities", description="add attribute types")
	public void addAttributeTypes() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
		
		attributeType.put("name", "attributeType2");
		attributeType.put("dbTable", "table2");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType2:" + attributeTypeID2);
		
		attributeType.put("name", "attributeType3");
		attributeType.put("dbTable", "table2");
		attributeTypeID3 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType3:" + attributeTypeID3);
	}
	
	@Test (dependsOnMethods = "addAttributeTypes", description="create deprecated attribute")
	public void addDeprecatedAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true);
		attribute.put("deprecated", true);
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deprecated"), "can create deprecated attribute");
	}
	
	@Test (dependsOnMethods = "addDeprecatedAttribute", description="add attribute")
	public void addAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		attribute.put("athenaColumn", "acol2");
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error") , "cannot create attribute: "  +attributeID1);
	}
	
	@Test (dependsOnMethods = "addAttribute", description="update attribute to deprecated while updating other fields")
	public void updateAttributeToDeprecatedPlusOtherUpdated() throws JSONException, IOException, ClassNotFoundException, SQLException{
		String attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", true);
		
		JSONObject updatedObj = new JSONObject (attribute1Obj);
		updatedObj.put("name", "aaa");
		String respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1Obj);
		updatedObj.put("dbColumn", "aaa");
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1Obj);
		updatedObj.put("defaultValue",1.11);
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1Obj);
		updatedObj.put("returnedByDSR",false);
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1Obj);
		updatedObj.put("withDefaultValue", false);
		updatedObj.remove("defaultValue");
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
	}
	
	@Test (dependsOnMethods = "updateAttributeToDeprecatedPlusOtherUpdated", description="update deprecated attribute")
	public void updateDeprecatedAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		String attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", true);
		
		String respone = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error") , "Cannot update attribute to deprecated: " + respone);
		
		attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		
		JSONObject updatedObj = new JSONObject (attribute1);
		updatedObj.put("name", "aaa");
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1);
		updatedObj.put("dbColumn", "aaa");
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1);
		updatedObj.put("defaultValue",1.11);
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1);
		updatedObj.put("returnedByDSR",false);
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
		updatedObj = new JSONObject (attribute1);
		updatedObj.put("withDefaultValue", false);
		updatedObj.remove("defaultValue");
		respone = entitiesApi.updateAttribute(attributeID1, updatedObj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can update deprecated attribute");
		
	}
	
	@Test (dependsOnMethods = "updateDeprecatedAttribute", description="delete attribute")
	public void deleteAttribute() throws JSONException, IOException, JSONException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 0, "wrong size of deletedAttributesData before deletion");
		
		//delete attribute1
		int code = entitiesApi.deleteAttribute(attributeID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attribute1");
		
		entities = entitiesApi.getProductEntities(productID1, sessionToken);
		entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 1, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
	}
		
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
		
		baseUtils.reset(productID1, sessionToken);
	}
}
