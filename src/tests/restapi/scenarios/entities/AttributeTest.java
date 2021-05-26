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


public class AttributeTest {
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
	private String attributeID2;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	private String attributeStr;
	private DbHandler dbHandler; 
	private AthenaHandler athenaHandler; 
	private LinkedList<String> athenaColumnsToDelete; 
	
	
	//add attribute type twice with the same name
	//update attribute type twice with the same name
	//add attribute without name, with null name, with empty name
	//add attribute without creator, with null creator, with empty creator
	//add attribute without dbTable, with null dbTable, with empty dbTable
	//add attribute without <nullable/returnedByDSR/dbColumn/dataType/withDefaultValue> with null piState, with empty piState
	//update creator
	//test update name
	//test create returned fields
	
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
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table2");
		
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table2");
		
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table2");
		
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);
		athenaColumnsToDelete = new LinkedList<>();
		athenaColumnsToDelete.add("acol1");
		athenaColumnsToDelete.add("acol2");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);

		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test2", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test2", athenaColumnsToDelete);
	}
	
	
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
		Assert.assertFalse(attributeTypeID2.contains("error"), "Cannot create attributeType2: " + attributeTypeID2);		
		
		attributeType.put("name", "attributeType3");
		attributeType.put("dbTable", "table1");
		attributeTypeID3 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID3.contains("error"), "Cannot create attributeType3:" + attributeTypeID3);
		
	}
	
	@Test (dependsOnMethods = "addAttributeTypes", description="add attribute to entity with wrong attributeTypeId field")
	public void addAttributeTestAttributTypeIdField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.remove("attributeTypeId");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributeTypeId"), "Can create attribute with missing attributeTypeId field");
		
		attribute.put("attributeTypeId", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute with empty attributeTypeId field");
		
		attribute.put("attributeTypeId", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("attributeTypeId"), "Can create attribute with null attributeTypeId field");
		
		attribute.put("attributeTypeId", entityID1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("AttributeType"), "Can create attribute with non existng attributeType");
	}
	
	@Test (dependsOnMethods = "addAttributeTestAttributTypeIdField", description="add attribute to entity with attributeType in different entity")
	public void addAttributeTestAttributTypeInOtherEntity() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID3);
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute with attributeType from another entity");
	}
	
	@Test (dependsOnMethods = "addAttributeTestAttributTypeInOtherEntity", description="add attribute to entity with wrong name field")
	public void addAttributeTestNameField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("name");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute with missing name field");
		
		attribute.put("name", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute with empty name field");
		
		attribute.put("name", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can create attribute with null name field");
	}

	@Test (dependsOnMethods = "addAttributeTestNameField", description="add attribute to entity with wrong dbColumn field")
	public void addAttributeTestDbColumnField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("dbColumn");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbColumn"), "Can create attribute with missing dbColumn field");
		
		attribute.put("dbColumn", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbColumn"), "Can create attribute with empty dbColumn field");
		
		attribute.put("dbColumn", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dbColumn"), "Can create attribute with null dbColumn field");
	}
	
	@Test (dependsOnMethods = "addAttributeTestDbColumnField", description="add attribute to entity with wrong creator field")
	public void addAttributeTestCreatorField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		attribute.remove("creator");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute with missing creator field");
		
		attribute.put("creator", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute with empty creator field");
		
		attribute.put("creator", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can create attribute with null creator field");
	}
	
	@Test (dependsOnMethods = "addAttributeTestCreatorField", description="add attribute to entity with wrong data type field")
	public void addAttributeTestDataTypeField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("dataType");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dataType"), "Can create attribute with missing dataType field");
		
		attribute.put("dataType", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dataType"), "Can create attribute with empty dataType field");
		
		attribute.put("dataType", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dataType"), "Can create attribute with null dataType field");
		
		attribute.put("dataType", "XXX");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("dataType"), "Can create attribute with illegal dataType field");
	}
	
	@Test (dependsOnMethods = "addAttributeTestDataTypeField", description="add attribute to entity with wrong returnedByDSR field")
	public void addAttributeTestReturnedByDSRField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("returnedByDSR");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("returnedByDSR"), "Can create attribute with missing returnedByDSR field");
		
		attribute.put("returnedByDSR", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("returnedByDSR"), "Can create attribute with empty returnedByDSR field");
		
		attribute.put("returnedByDSR", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("returnedByDSR"), "Can create attribute with null returnedByDSR field");
		
		attribute.put("returnedByDSR", "XXX");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("returnedByDSR"), "Can create attribute with illegal returnedByDSR field");
	}
	
	@Test (dependsOnMethods = "addAttributeTestReturnedByDSRField", description="add attribute to entity with wrong deprecated field")
	public void addAttributeTestDeprecatedField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("deprecated");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can create attribute with missing deprecated field");
		
		attribute.put("deprecated", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can create attribute with empty deprecated field");
		
		attribute.put("deprecated", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can create attribute with null deprecated field");
		
		attribute.put("deprecated", "XXX");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can create attribute with illegal deprecated field");
		
		attribute.put("deprecated", true);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("deprecated"), "Can create deprectaed attribute");
	}
	
	@Test (dependsOnMethods = "addAttributeTestDeprecatedField", description="add attribute to entity with wrong nullable field")
	public void addAttributeTestNullableField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("nullable");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("nullable"), "Can create attribute with missing nullable field");
		
		attribute.put("nullable", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("nullable"), "Can create attribute with empty nullable field");
		
		attribute.put("nullable", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("nullable"), "Can create attribute with null nullable field");
		
		attribute.put("nullable", "XXX");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("nullable"), "Can create attribute with illegal nullable field");
	}
	
	@Test (dependsOnMethods = "addAttributeTestNullableField", description="add attribute to entity with wrong withDefaultValue field")
	public void addAttributeTestWithDefaultValueField() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		
		attribute.remove("withDefaultValue");
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("withDefaultValue"), "Can create attribute with missing withDefaultValue field");
		
		attribute.put("withDefaultValue", "");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("withDefaultValue"), "Can create attribute with empty withDefaultValue field");
		
		attribute.put("withDefaultValue", JSONObject.NULL);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("withDefaultValue"), "Can create attribute with null withDefaultValue field");
		
		attribute.put("withDefaultValue", "XXX");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("withDefaultValue"), "Can create attribute with illegal withDefaultValue field");
	}
	
	
	@Test (dependsOnMethods = "addAttributeTestWithDefaultValueField", description="add attribute to entity with existing name")
	public void addAttributeNameTwice() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col1"); 
		attribute.put("athenaColumn", "acol2"); 
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeID1);
		
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can create attribute with name that already exists");
		
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID2); //table2
		attribute.put("dbColumn", "col1"); 
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);		
	}
	
	@Test (dependsOnMethods = "addAttributeNameTwice", description="update Attribute to with existing name")
	public void updateAttributeTypeNameTwice() throws JSONException, IOException{
		String attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		
		JSONObject attributeObj2 = new JSONObject(attribute2);
		attributeObj2.put("name", "attribute1");
		String respone = entitiesApi.updateAttribute(attributeID2, attributeObj2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("name"), "Can update Attribute name to name that already exists");
	}
	
	@Test (dependsOnMethods = "updateAttributeTypeNameTwice", description="update attribute to entity with attributeType in different entity")
	public void updateAttributeTestAttributTypeInOtherEntity() throws JSONException, IOException{
		String attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("attributeTypeId", attributeTypeID3);
		String respone = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error"), "Can update attribute with attributeType from another entity");
	}
	

	@Test (dependsOnMethods = "updateAttributeTestAttributTypeInOtherEntity", description="update attribute creator")
	public void updateAttributeCreator() throws JSONException, IOException{
		String attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		
		JSONObject attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("creator", "aaa");
		String respone = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("creator"), "Can update attribute's creator");
	}

	@Test (dependsOnMethods = "updateAttributeCreator", description="update name of attribute")
	public void updateAttributeName() throws JSONException, IOException{
		String attribute = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute2attribute: " + attribute);
		
		JSONObject attribute2 = new JSONObject(attribute);
		attribute2.put("name", "test3");
		String respone = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error") , "cannot update attribute name: " + respone);
		
		attribute = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute2");
		attribute2 = new JSONObject(attribute);
		Assert.assertTrue(attribute2.getString("name").equals("test3"), "name was not updated");
		Assert.assertTrue(attribute2.getString("entityId").equals(entityID1), "wrong entityId");
	}
	
	@Test (dependsOnMethods = "updateAttributeName", description="update dataType of attribute")
	public void updateAttributeDataType() throws JSONException, IOException{
		String attribute = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute2: " + attribute);
		
		JSONObject attribute2 = new JSONObject(attribute);
		attribute2.put("dataType", "BOOLEAN");
		attribute2.put("defaultValue", true);
		String respone = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("Data type cannot be changed"), "can update attribute dataType");
	}
	
	@Test (dependsOnMethods = "updateAttributeDataType", description="update entity id")
	public void updateAttributeEntityId() throws JSONException, IOException{
		String attribute = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute2: " + attribute);
		
		JSONObject attribute2 = new JSONObject(attribute);
		attribute2.put("entityId", entityID2);
		String respone = entitiesApi.updateAttribute(attributeTypeID2, attribute2.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") , "can update entity id");
	}
	
	@Test (dependsOnMethods = "updateAttributeEntityId", description="update attribute deprecated")
	public void updateAttributeDeprectated() throws JSONException, IOException{
		String attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		JSONObject attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated") == false, "deprecated is not false");
		
		attribute2Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute's deprecated: "+respone);
		attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated") == true, "deprecated is not true");
		
		attribute2Obj.put("deprecated", false);
		respone = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error") , "Cannot update attribute's deprecated:" + respone);
		attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated") == false, "deprecated is not false");
		
	}
	
	@Test (dependsOnMethods = "updateAttributeDeprectated", description="get attributes")
	public void getAttributes() throws JSONException, IOException, JSONException{
		String attributesStr = entitiesApi.getAttributes(entityID1, sessionToken);
		Assert.assertFalse(attributesStr.contains("error"), "Cannot get attributes: " + attributesStr);
		
		JSONObject attributesObj = new JSONObject(attributesStr);
		JSONArray entitiesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(entitiesArr.length() == 2, "wrong numbner of attributes");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("uniqueId").equals(attributeID1), "wrong attribute1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("name").equals("attribute1"), "wrong attribute1");
		Assert.assertTrue(entitiesArr.getJSONObject(0).getString("entityId").equals(entityID1), "wrong attribute1");
		
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("uniqueId").equals(attributeID2), "wrong attribute2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("name").equals("test3"), "wrong attribute2");
		Assert.assertTrue(entitiesArr.getJSONObject(1).getString("entityId").equals(entityID1), "wrong attribute2");
	}
	
	@Test (dependsOnMethods = "getAttributes", description="delete entity with attributes")
	public void deleteEntityWithAttributes() throws JSONException, IOException, JSONException{
		int code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertFalse(code == 200, "can delete entity withh attributes");
	}
	
	@Test (dependsOnMethods = "deleteEntityWithAttributes", description="delete attribute")
	public void deleteAttributes() throws JSONException, IOException, JSONException{
		String attributesStr = entitiesApi.getAttributes(entityID1, sessionToken);
		Assert.assertFalse(attributesStr.contains("error"), "Cannot get attributes: " + attributesStr);
		
		JSONObject attributesObj = new JSONObject(attributesStr);
		JSONArray attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.length() == 2, "wrong numbner of attributes");
		
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
		
		attributesStr = entitiesApi.getAttributes(entityID1, sessionToken);
		Assert.assertFalse(attributesStr.contains("error"), "Cannot get attributes: " + attributesStr);
		
		attributesObj = new JSONObject(attributesStr);
		attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.length() == 1, "wrong numbner of attributes");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("uniqueId").equals(attributeID2), "wrong Attribute2");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("name").equals("test3"), "wrong Attribute2");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("entityId").equals(entityID1), "wrong Attribute2");
		
		String response = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted attribute");
		
		code = entitiesApi.deleteAttribute(attributeID1, sessionToken);
		Assert.assertTrue(code == 400, "can delete attribute1 twice");
		
		//move attribute2 to deprecate
		String attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute1);
		JSONObject attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", true);
		respone = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute to deprecated: "+respone);
		
		//delete attribute2
		code = entitiesApi.deleteAttribute(attributeID2, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete Attribute2");
		
		attributesStr = entitiesApi.getAttributes(entityID1, sessionToken);
		Assert.assertFalse(attributesStr.contains("error"), "Cannot get attributes: " + attributesStr);
		
		attributesObj = new JSONObject(attributesStr);
		attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.length() == 0, "wrong numbner of attributes");
		
		response = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "can get deleted attribute");
		
		code = entitiesApi.deleteAttribute(attributeID2, sessionToken);
		Assert.assertTrue(code == 400, "can delete attributeT2 twice");
	}
	
	@Test (dependsOnMethods = "deleteAttributes", description="delete entity without attributes")
	public void deleteEntityWithoutAttributes() throws JSONException, IOException, JSONException{
		int code = entitiesApi.deleteEntity(entityID1, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete entity without attributes");
	}
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		if (dbHandler!=null) {
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table2");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table2");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table2");
		}
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);

		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test2", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test2", athenaColumnsToDelete);
		
		baseUtils.reset(productID1, sessionToken);	
	}
}
