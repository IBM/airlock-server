package tests.restapi.scenarios.entities;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.ibm.airlock.Constants.DATA_TYPE;
import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.db.DbHandler;
import com.ibm.airlock.admin.db.DbHandler.TableColumn;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EntitiesRestApi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;


public class AttributeTestDBConnection {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String entityID1;
	private String attributeTypeID1;
	private String attributeID1;
	private String attributeID2;
	private String attributeID3;
	private String attributeID4;
	private String attributeID5;
	private String attributeID6;
	private String attributeID7;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	private String attributeStr;
	private DbHandler dbHandler; 
	private AthenaHandler athenaHandler;
	private LinkedList<String> athenaColumnsToDelete; 
	
	//test using an already existing column in create.
	//test update to already existing column
	//test rename column
	//test deleting attribute deletes the db column
	//test creation of all data types
	//test creation of all default values including null and no default value
	//test update of all data types
	//test update of all default values including null and no default value
	//test column rename
	
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
		athenaColumnsToDelete.add("acol7");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
	}
	
	@Test ( description="add entity")
	public void addEntity() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1: " + entityID1);
	}
		
	@Test (dependsOnMethods = "addEntity", description="add attribute type")
	public void addAttributeType() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
	}
	
	@Test (dependsOnMethods = "addAttributeType", description="add attribute ")
	public void addAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col10"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeTypeID1);
	}
	
	@Test (dependsOnMethods = "addAttribute", description="add attribute using already existing db column")
	public void addAttributeWithExistingColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col10"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("col10"), "can create attribute using an already existing column");
	}
	
	@Test (dependsOnMethods = "addAttributeWithExistingColumn", description="update attribute to already existing db column")
	public void updateAttributeToExistingColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col7"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol2"); 
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error") , "cannot create attribute :" + attributeID2);
		
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2");
		
		JSONObject attribute2 = new JSONObject(attributeRes);
		attribute2.put("dbColumn", "col10");
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("col10"), "can update attribute to an already existing column");
	}
	
	@Test (dependsOnMethods = "updateAttributeToExistingColumn", description="update attribute rename db column")
	public void renameDBColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		//base db schema
		List<TableColumn> dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col7"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		
		//dev db schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_dev_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col7"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		
		//archive db schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col7"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");


		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2");
		
		JSONObject attribute2 = new JSONObject(attributeRes);
		attribute2.put("dbColumn", "col8");
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot rename db column");
		
		//base db schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col8"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		
		//dev db schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_dev_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col8"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		
		//archive db schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col10"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).getName().equals("col8"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
	}
	
	@Test (dependsOnMethods = "renameDBColumn", description="test that delete attribute leads to db column deletion")
	public void deleteDBColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		//move attribute1 to deprecate
		String attribute1 = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1: " + attribute1);
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", true);
		String respone = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute to deprecated: "+respone);
		
		//delete attribute1
		int code = entitiesApi.deleteAttribute(attributeID1, sessionToken);
		Assert.assertTrue(code == 200, "Cannot delete attribute1");
		
		validateColumnInDatabase("airlock_test", "table1", 1, 0, "col8", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col8", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 1, 0, "col8", DATA_TYPE.DOUBLE, true);
		
		//move attribute2 to deprecate
		String attribute2 = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute2: " + attribute2);
		JSONObject attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", true);
		respone = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), sessionToken);
		Assert.assertFalse(respone.contains("error"), "Cannot update attribute to deprecated: "+respone);
		
		//delete attribute2
		code = entitiesApi.deleteAttribute(attributeID2, sessionToken);
		Assert.assertTrue(code == 200, "Cannot delete attribute2");
		
		List<TableColumn> dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the database");
		
		dbColumns = dbHandler.getTableCulomnsData("airlock_dev_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the dev database");
		
		dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the archive database");
	}
	
	private void validateColumnInDatabase(String schema, String table, int expectedNumberOfColumns, int colIndex, String expectedName, DATA_TYPE expectedType, boolean expectedIsNullable) throws ClassNotFoundException, SQLException {
		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData(schema, table);
		Assert.assertTrue(dbColumns.size() == expectedNumberOfColumns, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(colIndex).getName().equals(expectedName), "wrong column name");
		Assert.assertTrue(dbColumns.get(colIndex).getType().equals(expectedType), "wrong column type");
		Assert.assertTrue(dbColumns.get(colIndex).isNullable() == expectedIsNullable, "wrong is nullable");
	}
	
	@Test (dependsOnMethods = "deleteDBColumn", description="add attribute STRING")
	public void addAttributeString() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeString");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col11"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("defaultValue", "aaa"); 
		attribute.put("nullable", true); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeID1);
		
		
		validateColumnInDatabase("airlock_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
		
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", "bbb");
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 1, 0, "col11", DATA_TYPE.STRING, true);
	}
	
	@Test (dependsOnMethods = "addAttributeString", description="add attribute BOOLEAN")
	public void addAttributeBoolean() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeBoolean");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col7"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("defaultValue", false); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol2"); 
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);
		
		validateColumnInDatabase("airlock_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
		
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", true);
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 2, 1, "col7", DATA_TYPE.BOOLEAN, false);
	}
	
	@Test (dependsOnMethods = "addAttributeBoolean", description="add attribute INTEGER")
	public void addAttributeInteger() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeInteger");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col12"); 
		attribute.put("dataType", "INTEGER"); 
		attribute.put("defaultValue", 33); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol3"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error"), "Cannot create attribute3:" + attributeID3);
		
		validateColumnInDatabase("airlock_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);
		
		String attributeRes = entitiesApi.getAttribute(attributeID3, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute3");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44);
		
		String response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 3, 2, "col12", DATA_TYPE.INTEGER, false);	
	}
	
	@Test (dependsOnMethods = "addAttributeInteger", description="add attribute DOUBLE")
	public void addAttributeDouble() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeDouble");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col9"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 33.33); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol4"); 
		
		attributeID4 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID4.contains("error"), "Cannot create attribute4:" + attributeID3);
		
		validateColumnInDatabase("airlock_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
		
		String attributeRes = entitiesApi.getAttribute(attributeID4, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute4");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44.44);
		
		String response = entitiesApi.updateAttribute(attributeID4, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 4, 3, "col9", DATA_TYPE.DOUBLE, true);
	}
	
	@Test (dependsOnMethods = "addAttributeDouble", description="add attribute LONG")
	public void addAttributeLong() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeLong");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col13"); 
		attribute.put("dataType", "LONG"); 
		attribute.put("defaultValue", 33L); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol5"); 
		
		attributeID5 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID5.contains("error"), "Cannot create attribute5:" + attributeID5);
		
		validateColumnInDatabase("airlock_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
		
		String attributeRes = entitiesApi.getAttribute(attributeID5, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute5");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44L);
		
		String response = entitiesApi.updateAttribute(attributeID5, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 5, 4, "col13", DATA_TYPE.LONG, true);
	}

	@Test (dependsOnMethods = "addAttributeLong", description="add attribute JSON")
	public void addAttributeJSON() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeJSON");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col14"); 
		attribute.put("dataType", "JSON"); 
		attribute.put("withDefaultValue", false); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol6"); 
		attribute.remove("defaultValue");
		
		attributeID6 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID6.contains("error"), "Cannot create attribute6:" + attributeID6);
		
		validateColumnInDatabase("airlock_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		
		String attributeRes = entitiesApi.getAttribute(attributeID6, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute6");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("description", "desc123");
		
		String response = entitiesApi.updateAttribute(attributeID6, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		validateColumnInDatabase("airlock_dev_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		validateColumnInDatabase("airlock_archive_test", "table1", 6, 5, "col14", DATA_TYPE.JSON, true);
		
	}
	
	@Test (dependsOnMethods = "addAttributeJSON", description="add attribute timestamp")
	public void addAttributeTimestamp() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeTimestamp");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col15"); 
		attribute.put("dataType", "TIMESTAMP"); 
		attribute.put("withDefaultValue", false); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol7"); 
		attribute.remove("defaultValue");
		
		attributeID7 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID7.contains("error"), "Cannot create attribute7:" + attributeID7);
		
		validateColumnInDatabase("airlock_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
	
		String attributeRes = entitiesApi.getAttribute(attributeID7, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute7");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("description", "desc123");
		
		String response = entitiesApi.updateAttribute(attributeID7, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		validateColumnInDatabase("airlock_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
		validateColumnInDatabase("airlock_dev_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
		validateColumnInDatabase("airlock_archive_test", "table1", 7, 6, "col15", DATA_TYPE.TIMESTAMP, false);
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
