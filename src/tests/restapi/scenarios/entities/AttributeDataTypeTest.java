package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
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


public class AttributeDataTypeTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String entityID1;
	private String attributeTypeID1;
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
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);

		athenaColumnsToDelete = new LinkedList<>();
		athenaColumnsToDelete.add("acol1");
		athenaColumnsToDelete.add("acol2");
		athenaColumnsToDelete.add("acol3");
		athenaColumnsToDelete.add("acol4");
		athenaColumnsToDelete.add("acol5");
		athenaColumnsToDelete.add("acol6");
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
	}
	
	@Test (description="add entity")
	public void addEntity() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1");
	}
		
	@Test (dependsOnMethods = "addEntity", description="add attribute type")
	public void addAttributeType() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
	}
	
	@Test (dependsOnMethods = "addAttributeType", description="add attribute with dataType-defaultValue mismatch ")
	public void addAttributeDataTypeDefaultValueMismatch() throws JSONException, IOException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("attributeTypeId", attributeTypeID1);
		attribute.put("withDefaultValue", true);
		attribute.put("dbColumn", "col3");
		
		//dataType = String
		attribute.put("dataType", "STRING");
		
		attribute.put("defaultValue", true);
		String respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = string and boolen defaultValue");
		
		attribute.put("defaultValue", 1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = string and int defaultValue");
		
		attribute.put("defaultValue", 1.1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = string and double defaultValue");
		
		
		//dataType = boolean
		attribute.put("dataType", "BOOLEAN");
		
		attribute.put("defaultValue", "aaa");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = boolean and string defaultValue");
		
		attribute.put("defaultValue", 1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = boolean and int defaultValue");
		
		attribute.put("defaultValue", 1.1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = boolean and double defaultValue");
		
		//dataType = integer
		attribute.put("dataType", "INTEGER");
		
		attribute.put("defaultValue", "aaa");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = integer and string defaultValue");
		
		attribute.put("defaultValue", false);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = integer and boolean defaultValue");
		
		attribute.put("defaultValue", 1.1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = integer and double defaultValue");
		
		//dataType = long
		attribute.put("dataType", "LONG");
		
		attribute.put("defaultValue", "aaa");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = long and string defaultValue");
		
		attribute.put("defaultValue", false);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = long and boolean defaultValue");
		
		attribute.put("defaultValue", 1.1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = long and double defaultValue");
		
		//dataType = double
		attribute.put("dataType", "DOUBLE");
		
		attribute.put("defaultValue", "aaa");
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = double and string defaultValue");
		
		attribute.put("defaultValue", false);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = double and boolean defaultValue");
		
		attribute.put("defaultValue", 1);
		respone = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can create attribute with dataType = double and int defaultValue");
	}
	
	@Test (dependsOnMethods = "addAttributeDataTypeDefaultValueMismatch", description="add attribute ")
	public void addAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col4"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attribute1:" + attributeTypeID1);
	}
	
	@Test (dependsOnMethods = "addAttribute", description="add attribute with dataType-defaultValue mismatch ")
	public void updateAttributeDataTypeDefaultValueMismatch() throws JSONException, IOException {
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		JSONObject attribute = new JSONObject(attributeRes);
		
		attribute.put("attributeTypeId", attributeTypeID1);
		attribute.put("withDefaultValue", true);
		
		//dataType = double
		attribute.put("defaultValue", "aaa");
		String respone = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can update attribute with dataType = double and string defaultValue");
		
		attribute.put("defaultValue", false);
		respone = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can update attribute with dataType = double and boolean defaultValue");
		
		attribute.put("defaultValue", 1);
		respone = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(respone.contains("error") && respone.contains("default value")  && respone.contains("data type") ,"Can update attribute with dataType = double and int defaultValue");
	}
	
	//if withDefaultValue = true defaultValue must be passed
	//if withDefaultValue = false defaultValue should not be passed
	@Test (dependsOnMethods = "updateAttributeDataTypeDefaultValueMismatch", description="add attribute with default value")
	public void testCreateWithDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col2"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("defaultValue", true); 
		attribute.put("withDefaultValue", false); 
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can create attribute with default value");
	}
	
	@Test (dependsOnMethods = "testCreateWithDefaultValue", description="add attribute without default value")
	public void testCreateWithoutDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col2"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("withDefaultValue", true);
		attribute.remove("defaultValue");
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can create attribute without defualtValue");
	}
	
	@Test (dependsOnMethods = "testCreateWithoutDefaultValue", description="add attribute without default value")
	public void testCreateWithNullDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col2"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("withDefaultValue", false);
		attribute.put("defaultValue", JSONObject.NULL);
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can create attribute with null default value");
	}
	@Test (dependsOnMethods = "testCreateWithNullDefaultValue", description="Update attribute with default value")
	public void testUpdateWithDefaultValue() throws JSONException, IOException {
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		JSONObject attribute = new JSONObject(attributeRes);
		
		attribute.put("defaultValue", true); 
		attribute.put("withDefaultValue", false); 
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can create attribute with default value");
	}
	
	@Test (dependsOnMethods = "testUpdateWithDefaultValue", description="Update attribute without default value")
	public void testUpdateWithoutDefaultValue() throws JSONException, IOException {
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		JSONObject attribute = new JSONObject(attributeRes);
		attribute.put("withDefaultValue", true);
		attribute.remove("defaultValue");
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can create attribute without defualtValue");
	}
	
	@Test (dependsOnMethods = "testUpdateWithoutDefaultValue", description="Update attribute without default value")
	public void testUpdateWithNullDefaultValue() throws JSONException, IOException {
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		JSONObject attribute = new JSONObject(attributeRes);
		attribute.put("withDefaultValue", false);
		attribute.put("defaultValue", JSONObject.NULL);
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can create attribute with null default value");
	}
	
	@Test (dependsOnMethods = "testUpdateWithoutDefaultValue", description="Update attribute without default value")
	public void testGetWithDefaultValue() throws JSONException, IOException {
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		JSONObject attribute = new JSONObject(attributeRes);
		Assert.assertTrue(attribute.getBoolean("withDefaultValue") == true, "illegal withDefaultValue");
		Assert.assertTrue(attribute.containsKey("defaultValue") == true, "illegal defaultValue");
		Assert.assertTrue(attribute.getDouble("defaultValue") == 0.99, "illegal defaultValue");
	}
	
	@Test (dependsOnMethods = "testGetWithDefaultValue", description="Update attribute without default value")
	public void testGetWithoutDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col5"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("withDefaultValue", false);
		attribute.remove("defaultValue");
		attribute.put("athenaColumn", "acol2"); 
		
		String attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);
		
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2: " + attributeRes);
		
		attribute = new JSONObject(attributeRes);
		Assert.assertTrue(attribute.getBoolean("withDefaultValue") == false, "illegal withDefaultValue");
		Assert.assertTrue(attribute.containsKey("defaultValue") == false, "illegal defaultValue");
	}
	
	@Test (dependsOnMethods = "testGetWithoutDefaultValue", description="add JSON attribute with default value")
	public void testCreateJSONWithDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeJSON");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col2"); 
		attribute.put("dataType", "JSON"); 
		attribute.put("defaultValue", "{}"); 
		attribute.put("withDefaultValue", true); 
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("JSON") && response.contains("default value"), "Can create JSON attribute with default value");
	}
	
	@Test (dependsOnMethods = "testCreateJSONWithDefaultValue", description="add timestamp attribute with default value")
	public void testCreateTimestampWithDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeTimestamp");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col2"); 
		attribute.put("dataType", "TIMESTAMP"); 
		attribute.put("defaultValue", 33L); 
		attribute.put("withDefaultValue", true); 
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("timestamp") && response.contains("default value"), "Can create timestamp attribute with default value");
	}
	
	@Test (dependsOnMethods = "testCreateTimestampWithDefaultValue", description="update JSON attribute with default value")
	public void testUpdateJSONWithDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeJSON");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col3"); 
		attribute.put("athenaColumn", "acol3"); 
		attribute.put("dataType", "JSON"); 
		attribute.put("withDefaultValue", false); 
		attribute.remove("defaultValue");
		
		String jsonAttID = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(jsonAttID.contains("error"), "Cannot create JSON attribute :" + jsonAttID);
		
		String attributeRes = entitiesApi.getAttribute(jsonAttID, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get json attribute: " + attributeRes);
		
		attribute = new JSONObject(attributeRes);
		attribute.put("withDefaultValue", true); 
		attribute.put("defaultValue", "xxx"); 
		
		String response = entitiesApi.updateAttribute(jsonAttID, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("JSON") && response.contains("default value"), "Can update JSON attribute with default value:" + response);
	}
	
	@Test (dependsOnMethods = "testUpdateJSONWithDefaultValue", description="update timestamp attribute with default value")
	public void testUpdateTimestampWithDefaultValue() throws JSONException, IOException {
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeTimestamp");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col6"); 
		attribute.put("athenaColumn", "acol6"); 
		attribute.put("dataType", "TIMESTAMP"); 
		attribute.put("withDefaultValue", false); 
		attribute.remove("defaultValue");
		
		String timestampAttID = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(timestampAttID.contains("error"), "Cannot create timestamp attribute :" + timestampAttID);
		
		String attributeRes = entitiesApi.getAttribute(timestampAttID, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get timestamp attribute: " + attributeRes);
		
		attribute = new JSONObject(attributeRes);
		attribute.put("withDefaultValue", true); 
		attribute.put("defaultValue", 88L); 
		
		String response = entitiesApi.updateAttribute(timestampAttID, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("timestamp") && response.contains("default value"), "Can update timestamp attribute with default value:" + response);
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
