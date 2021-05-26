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


public class AttributeDeletionTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String productID2;
	private String entityID1;
	private String entityID2;
	private String entityID3;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String attributeTypeID3;
	private String attributeTypeID4;
	private String attributeTypeID5;
	private String attributeID1;
	private String attributeID2;
	private String attributeID3;
	private String attributeID4;
	private String attributeID5;
	private String attributeID6;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	private String attributeStr;
	private DbHandler dbHandler; 
	private AthenaHandler athenaHandler; 
	private LinkedList<String> athenaColumnsToDelete1; 
	private LinkedList<String> athenaColumnsToDelete2; 
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw", "athenaRegion", "athenaOutputBucket", "athenaCatalog"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw, String athenaRegion,  String athenaOutputBucket, String athenaCatalog) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		baseUtils.printProductToFile(productID2);

		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		entityStr = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		attributeTypeStr = FileUtils.fileToString(filePath + "airlytics/entities/attributeType1.txt", "UTF-8", false);
		attributeStr = FileUtils.fileToString(filePath + "airlytics/entities/attribute1.txt", "UTF-8", false);
		
		dbHandler = new DbHandler(dburl, dbuser, dbpsw);
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table2");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table2");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table2");
		

		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);

		athenaColumnsToDelete1 = new LinkedList<>();
		athenaColumnsToDelete1.add("acol1");
		athenaColumnsToDelete1.add("acol2");
		athenaColumnsToDelete1.add("acol3");
		
		athenaColumnsToDelete2 = new LinkedList<>();
		athenaColumnsToDelete2.add("acol22");
		athenaColumnsToDelete2.add("acol2");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete1);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete1);

		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test2", athenaColumnsToDelete2);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test2", athenaColumnsToDelete2);

	}

	@Test (description="add entities")
	public void addEntities() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1: " + entityID1);
		
		
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create entity2: " + entityID2);
		
		entity.put("name", "entity3"); //product2
		entityID3 = entitiesApi.createEntity(productID2, entity.toString(), sessionToken);
		Assert.assertFalse(entityID3.contains("error"), "Cannot create entity3: " + entityID3);
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
		attributeType.put("athenaTable", "airlock_test2");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType2:" + attributeTypeID2);
		
		attributeType.put("name", "attributeType3");
		attributeType.put("dbTable", "table2");
		attributeTypeID3 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType3:" + attributeTypeID3);
		
		attributeType.put("name", "attributeType4");
		attributeType.put("dbTable", "table1");
		attributeTypeID4 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID4.contains("error"), "Cannot create attributeType4:" + attributeTypeID4);
		 
		attributeType.put("name", "attributeType4"); //product2
		attributeType.put("dbTable", "table1");
		attributeTypeID5 = entitiesApi.createAttributeType(entityID3, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID5.contains("error"), "Cannot create attributeType5:" + attributeTypeID5);
	}
	
	@Test (dependsOnMethods = "addAttributeTypes", description="add attribute")
	public void addAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error") , "cannot create attribute: "  +attributeID1);
	}
	
	@Test (dependsOnMethods = "addAttribute", description="delete attribute")
	public void deleteAttribute() throws JSONException, IOException, JSONException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 0, "wrong size of deletedAttributesData before deletion");
		
		//move attribute1 to deprecate
		String attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute to deprecated: "+respone);
		
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
	
	@Test (dependsOnMethods = "deleteAttribute", description="add attribute with deleted column same entity")
	public void addAttributeWithDeletedColumnSameEntity() throws JSONException, IOException, JSONException{
		//different type
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute21");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("defaultValue", "default");
		attribute.put("nullable", true);
		attribute.put("deprecated", false);		
		
		String response  = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("type") , "can create attribute even though this column exists with different type and was deleted.");
		
		//different nullable
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", false);
		attribute.put("deprecated", false);
		
		response  = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("nullable") , "can create attribute even though this column exists with different nullable value and was deleted.");
		
		//different default value
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 111.111); 
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		
		attributeID4 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID4.contains("error") , "cannot create attribute: "  +attributeID4);
	}
	
	@Test (dependsOnMethods = "addAttributeWithDeletedColumnSameEntity", description="delete attribute 4")
	public void deleteAttribute4() throws JSONException, IOException, JSONException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 1, "wrong size of deletedAttributesData before deletion");
		
		//move attribute4 to deprecate
		String attribute4 = entitiesApi.getAttribute(attributeID4, sessionToken);
		Assert.assertFalse(attribute4.contains("error"), "Cannot get attribute4: " + attribute4);
		JSONObject attribute4Obj = new JSONObject(attribute4);
		attribute4Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID4, attribute4Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute4 to deprecated: "+respone);
		
		//delete attribute4
		int code = entitiesApi.deleteAttribute(attributeID4, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attribute4");
		
		entities = entitiesApi.getProductEntities(productID1, sessionToken);
		entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 2, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(1);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
	}
	
	@Test (dependsOnMethods = "deleteAttribute4", description="add attribute with deleted column same product")
	public void addAttributeWithDeletedColumnSameProduct() throws JSONException, IOException, JSONException{
		//different type
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute22");
		attribute.put("attributeTypeId", attributeTypeID4); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("defaultValue", "default");
		attribute.put("nullable", true);
		attribute.put("deprecated", false);		
		
		String response  = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("type") , "can create attribute even though this column exists with different type and was deleted.");
		
		//different nullable
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute12");
		attribute.put("attributeTypeId", attributeTypeID4); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", false);
		attribute.put("deprecated", false);
		
		response  = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("nullable") , "can create attribute even though this column exists with different nullable value and was deleted.");
		
		//different default value
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute11");
		attribute.put("attributeTypeId", attributeTypeID4); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 111.111); 
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		attribute.put("athenaColumn", "acol2"); 
		
		attributeID5 = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID5.contains("error") , "cannot create attribute: "  +attributeID5);
	}
	
	@Test (dependsOnMethods = "addAttributeWithDeletedColumnSameProduct", description="delete attribute 5")
	public void deleteAttribute5() throws JSONException, IOException, JSONException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 2, "wrong size of deletedAttributesData before deletion");
		
		//move attribute5 to deprecate
		String attribute5 = entitiesApi.getAttribute(attributeID5, sessionToken);
		Assert.assertFalse(attribute5.contains("error"), "Cannot get attribute5: " + attribute5);
		JSONObject attribute5Obj = new JSONObject(attribute5);
		attribute5Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID5, attribute5Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute5 to deprecated: "+respone);
		
		//delete attribute5
		int code = entitiesApi.deleteAttribute(attributeID5, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attribute5");
		
		entities = entitiesApi.getProductEntities(productID1, sessionToken);
		entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 3, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(1);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(2);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity2"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
	}
	
	@Test (dependsOnMethods = "deleteAttribute5", description="add attribute with deleted column different product")
	public void addAttributeWithDeletedColumnDifferentProduct() throws JSONException, IOException, JSONException{
		//different type
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute33");
		attribute.put("attributeTypeId", attributeTypeID5); //table1, product2
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "INTEGER"); 
		attribute.put("defaultValue", 1);
		attribute.put("nullable", true);
		attribute.put("deprecated", false);		
		
		String response  = entitiesApi.createAttribute(entityID3, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("type") , "can create attribute even though this column exists with different type and was deleted.");
		
		//different nullable
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute33");
		attribute.put("attributeTypeId", attributeTypeID5); //table1,  product2
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", false);
		attribute.put("deprecated", false);
		
		response  = entitiesApi.createAttribute(entityID3, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("deleted") && response.contains("nullable") , "can create attribute even though this column exists with different nullable value and was deleted.");
		
		//different default value
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute33");
		attribute.put("attributeTypeId", attributeTypeID5); //table1,  product2
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 222.222); 
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		attribute.put("athenaColumn", "acol3");
		
		attributeID6 = entitiesApi.createAttribute(entityID3, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID6.contains("error") , "cannot create attribute6: "  +attributeID6);
	}
	
	@Test (dependsOnMethods = "addAttributeWithDeletedColumnDifferentProduct", description="delete attribute 6")
	public void deleteAttribute6() throws JSONException, IOException, JSONException{
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 3, "wrong size of deletedAttributesData before deletion");
		
		//move attribute6 to deprecate
		String attribute6 = entitiesApi.getAttribute(attributeID6, sessionToken);
		Assert.assertFalse(attribute6.contains("error"), "Cannot get attribute5: " + attribute6);
		JSONObject attribute6Obj = new JSONObject(attribute6);
		attribute6Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID6, attribute6Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute6 to deprecated: "+respone);
		
		//delete attribute6
		int code = entitiesApi.deleteAttribute(attributeID6, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attribute6");
		
		entities = entitiesApi.getProductEntities(productID1, sessionToken);
		entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 3, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(1);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(2);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity2"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
		
		entities = entitiesApi.getProductEntities(productID2, sessionToken);
		entitiesObj = new JSONObject(entities);
		deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 1, "wrong size of deletedAttributesData after deletion");
		
		
		deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity3"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute33"), "wrong deletedAttributes attribute");
	}
	
	@Test (dependsOnMethods = "deleteAttribute6", description="add attributes to multiple entities")
	public void addAttributes() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute11");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col22"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("withDefaultValue", false);
		attribute.remove("defaultValue");
		attribute.put("nullable", false);
		attribute.put("deprecated", false);
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error") , "cannot create attribute: "  +attributeID1);
		
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute12");
		attribute.put("attributeTypeId", attributeTypeID2); //table2
		attribute.put("dbColumn", "col22");
		attribute.put("athenaColumn", "acol22");
		attribute.put("dataType", "LONG"); 
		attribute.put("withDefaultValue", true);
		attribute.put("defaultValue", 1L);
		attribute.put("nullable", false);
		attribute.put("deprecated", false);
		
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error") , "cannot create attribute: "  +attributeID2);
		
		attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute13");
		attribute.put("attributeTypeId", attributeTypeID3); //table2
		attribute.put("dbColumn", "col24"); 
		attribute.put("dataType", "INTEGER"); 
		attribute.put("withDefaultValue", true);
		attribute.put("defaultValue", 111);
		attribute.put("nullable", true);
		attribute.put("deprecated", false);
		attribute.put("athenaColumn", "acol2"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error") , "cannot create attribute: " + attributeID3);
		
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 3, "wrong size of deletedAttributesData after deletion");
	}
	
	@Test (dependsOnMethods = "addAttributes", description="delete entity1")
	public void deleteEntity1() throws JSONException, IOException, JSONException{
		//delete attribute1
		int code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertTrue(code != 200, "can delete entity1 with attributes");
		
		deleteAttribute(attributeID1);
		deleteAttribute(attributeID2);
		
		code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entity1 without attributes");
			
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 5, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(1);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(2);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity2"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(3);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col22"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("STRING"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == false, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(4);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col22"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table2"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("LONG"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == false, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute12"), "wrong deletedAttributes attribute");
	}
	
	@Test (dependsOnMethods = "deleteEntity1", description="delete entity2")
	public void deleteEntity2() throws JSONException, IOException, JSONException{
		//delete attribute1
		int code = entitiesApi.deleteEntity(entityID2, sessionToken);
		Assert.assertTrue(code != 200, "can delete entity2 with attributes");
		
		deleteAttribute(attributeID3);
		//deleteAttribute(attributeID5);
		
		code = entitiesApi.deleteEntity(entityID2, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entity2 without attributes");
		
		
		String entities = entitiesApi.getProductEntities(productID1, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		JSONArray deletedAttributesArr = entitiesObj.getJSONArray("deletedAttributesData");
		Assert.assertTrue(entitiesObj.getJSONArray("deletedAttributesData").size() == 6, "wrong size of deletedAttributesData after deletion");
		
		JSONObject deletedAttData = deletedAttributesArr.getJSONObject(0);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(1);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute1"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(2);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col21"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity2"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("DOUBLE"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(3);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col22"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table1"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("STRING"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == false, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute11"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(4);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col22"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table2"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity1"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("LONG"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == false, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute12"), "wrong deletedAttributes attribute");
		
		deletedAttData = deletedAttributesArr.getJSONObject(5);
		Assert.assertTrue(deletedAttData.getString("dbColumn").equals("col24"), "wrong deletedAttributes dbColumn");
		Assert.assertTrue(deletedAttData.getString("dbTable").equals("table2"), "wrong deletedAttributes dbTable");
		Assert.assertTrue(deletedAttData.getString("dbSchema").equals("airlock_test"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("entity").equals("entity2"), "wrong deletedAttributes dbSchema");
		Assert.assertTrue(deletedAttData.getString("dataType").equals("INTEGER"), "wrong deletedAttributes dataType");
		Assert.assertTrue(deletedAttData.getBoolean("nullable") == true, "wrong deletedAttributes nullable");
		Assert.assertTrue(deletedAttData.getString("attribute").equals("attribute13"), "wrong deletedAttributes attribute");
	}
	
	@Test (dependsOnMethods = "deleteEntity2", description="add entities")
	public void addEntities2() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
		
		
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
	}
		
	@Test (dependsOnMethods = "addEntities2", description="add attribute types")
	public void addAttributeTypes2() throws JSONException, IOException{
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
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table2");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table2");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table2");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete1);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete1);

		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test2", athenaColumnsToDelete2);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test2", athenaColumnsToDelete2);
		
		baseUtils.reset(productID1, sessionToken);
	}

	private void deleteAttribute(String attributeID) throws JSONException, IOException {
		//move attribute to deprecate
		String attribute = entitiesApi.getAttribute(attributeID, sessionToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute: " + attribute);
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID, attributeObj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute to deprecated: "+respone);
		
		//delete attribute
		int code = entitiesApi.deleteAttribute(attributeID, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete attribute");
	}
	
}
