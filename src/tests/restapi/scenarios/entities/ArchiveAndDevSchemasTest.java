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
import com.ibm.airlock.admin.airlytics.athena.AthenaHandler.ColumnData;
import com.ibm.airlock.admin.db.DbHandler;
import com.ibm.airlock.admin.db.DbHandler.TableColumn;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.EntitiesRestApi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class ArchiveAndDevSchemasTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String productID2;
	private String entityID1;
	private String entityID2;
	private String entityID3;
	private String entityID4;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String attributeTypeID3;
	private String attributeTypeID4;
	private String attributeID1;
	private String attributeID2;
	private String attributeID3;
	private String attributeID4;
	private String sessionToken = "";
	private String entityStr;
	private String attributeTypeStr;
	private String attributeStr;
	private DbHandler dbHandler; 
	private AthenaHandler athenaHandler;
	private ArrayList<ColumnData> initialColumnsData;
	private ArrayList<ColumnData> initialDevColumnsData;
	private LinkedList<String> athenaColumnsToDelete; 
	
	//test null dev and archive schemas and null dev Athena database 
	//test not null dev schema and null dev Athena database 
	//test null dev schema and not null dev Athena database 
	//test not null archive schema
	//test 2 attribute types in the same entity.
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw", "athenaRegion", "athenaOutputBucket", "athenaCatalog"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw, String athenaRegion,  String athenaOutputBucket, String athenaCatalog) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);

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
		athenaColumnsToDelete.add("acol11");
		athenaColumnsToDelete.add("acol22");
		athenaColumnsToDelete.add("acol33");
		athenaColumnsToDelete.add("acol44");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
	
		initialColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		initialDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
	}
	
	@Test ( description="add entity")
	public void addEntities() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		
		//null dev schema, archive schema, dev Athena db
		entity.put("name", "entity1");
		entity.put("dbDevSchema", JSONObject.NULL);
		entity.put("dbArchiveSchema",  JSONObject.NULL);
		entity.put("athenaDevDatabase",  JSONObject.NULL);
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1: " + entityID1);
		
		//null archive schema, dev Athena db
		//Not null dev schema
		entity = new JSONObject(entityStr);
		entity.put("name", "entity2");
		entity.put("dbDevSchema", "airlock_dev_test");
		entity.put("dbArchiveSchema",  JSONObject.NULL);
		entity.put("athenaDevDatabase",  JSONObject.NULL);
		entityID2 = entitiesApi.createEntity(productID2, entity.toString(), sessionToken);
		Assert.assertFalse(entityID2.contains("error"), "Cannot create entity2: " + entityID2);
		
		//null dev schema, archive schema, 
		//Not null dev Athena db
		entity = new JSONObject(entityStr);
		entity.put("name", "entity3");
		entity.put("dbDevSchema", JSONObject.NULL);
		entity.put("dbArchiveSchema",  JSONObject.NULL);
		entity.put("athenaDevDatabase", "airlocktestsdbdev");
		entityID3 = entitiesApi.createEntity(productID2, entity.toString(), sessionToken);
		Assert.assertFalse(entityID3.contains("error"), "Cannot create entity3: " + entityID3);
		
		//null dev schema, dev Athena db
		//Not null archive schema
		entity = new JSONObject(entityStr);
		entity.put("name", "entity4");
		entity.put("dbDevSchema", JSONObject.NULL);
		entity.put("dbArchiveSchema",  "airlock_archive_test");
		entity.put("athenaDevDatabase",  JSONObject.NULL);
		entityID4 = entitiesApi.createEntity(productID2, entity.toString(), sessionToken);
		Assert.assertFalse(entityID4.contains("error"), "Cannot create entity4: " + entityID4);	
	}
		
	@Test (dependsOnMethods = "addEntities", description="add attribute types")
	public void addAttributeTypes() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
		
		attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("name", "attributeType2");
		attributeType.put("dbTable", "table1");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID2.contains("error"), "Cannot create attributeType2:" + attributeTypeID2);
		
		attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("name", "attributeType3");
		attributeType.put("dbTable", "table1");
		attributeTypeID3 = entitiesApi.createAttributeType(entityID3, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID3.contains("error"), "Cannot create attributeType3:" + attributeTypeID3);
		
		attributeType = new JSONObject(attributeTypeStr);
		attributeType.put("name", "attributeType4");
		attributeType.put("dbTable", "table1");
		attributeTypeID4 = entitiesApi.createAttributeType(entityID4, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID4.contains("error"), "Cannot create attributeType4:" + attributeTypeID4);
	}
	
	@Test (dependsOnMethods = "addAttributeTypes", description="add attribute to entity 1")
	public void addAttribute1() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col11"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 8.88); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol11"); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeID1);
		
		//validate that the column was added to the base schema
		validateColumnInDatabase("airlock_test", "table1", 1, 0, "col11", DATA_TYPE.DOUBLE, true);
		
		//validate that the column was added to the base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 1, "wrong number of columns in the athena table");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol11", "double", false);
					
		//validate that the column was not added to the dev schema
		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_dev_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the dev database");
		
		//validate that the column was not added to the archive schema
		dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the arhive database");
		
		//validate that the column was not added to the dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 0, "wrong number of columns in the dev athena table");
	}
	
	@Test (dependsOnMethods = "addAttribute1", description="add attribute to entity 2")
	public void addAttribute2() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID2); //table1
		attribute.put("dbColumn", "col22"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("defaultValue", "default value"); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol22"); 
		
		attributeID2 = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);
		
		//validate that the column was added to the base schema
		validateColumnInDatabase("airlock_test", "table1", 2, 0, "col11", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_test", "table1", 2, 1, "col22", DATA_TYPE.STRING, false);
		
		//validate that the column was added to the base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol11", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol22", "varchar", false);
					
		//validate that the column was added to the dev schema
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col22", DATA_TYPE.STRING, false);
		
		//validate that the column was not added to the archive schema
		List<TableColumn> dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the arhive database");
		
		//validate that the column was not added to the dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 0, "wrong number of columns in the dev athena table");
	}
	
	@Test (dependsOnMethods = "addAttribute2", description="add attribute to entity 3")
	public void addAttribute3() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute3");
		attribute.put("attributeTypeId", attributeTypeID3); //table1
		attribute.put("dbColumn", "col33"); 
		attribute.put("dataType", "LONG"); 
		attribute.put("defaultValue", 123L); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol33"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID3, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error"), "Cannot create attribute3:" + attributeID3);
		
		//validate that the column was added to the base schema
		validateColumnInDatabase("airlock_test", "table1", 3, 0, "col11", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_test", "table1", 3, 1, "col22", DATA_TYPE.STRING, false);
		validateColumnInDatabase("airlock_test", "table1", 3, 2, "col33", DATA_TYPE.LONG, true);
		
		//validate that the column was added to the base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 3, "wrong number of columns in the athena table");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol11", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol22", "varchar", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol33", "bigint", false);
					
		//validate that the column was added to the dev schema
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col22", DATA_TYPE.STRING, false);
		
		//validate that the column was not added to the archive schema
		List<TableColumn> dbColumns = dbHandler.getTableCulomnsData("airlock_archive_test", "table1");
		Assert.assertTrue(dbColumns.size() == 0, "wrong number of columns in the arhive database");
		
		//validate that the column was not added to the dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 1, "wrong number of columns in the dev athena table");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol33", "bigint", false);
	}
	
	@Test (dependsOnMethods = "addAttribute3", description="add attribute to entity 4")
	public void addAttribute4() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute4");
		attribute.put("attributeTypeId", attributeTypeID4); //table1
		attribute.put("dbColumn", "col44"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("defaultValue", false); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol44"); 
		
		attributeID4 = entitiesApi.createAttribute(entityID4, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID4.contains("error"), "Cannot create attribute3:" + attributeID4);
		
		//validate that the column was added to the base schema
		validateColumnInDatabase("airlock_test", "table1", 4, 0, "col11", DATA_TYPE.DOUBLE, true);
		validateColumnInDatabase("airlock_test", "table1", 4, 1, "col22", DATA_TYPE.STRING, false);
		validateColumnInDatabase("airlock_test", "table1", 4, 2, "col33", DATA_TYPE.LONG, true);
		validateColumnInDatabase("airlock_test", "table1", 4, 3, "col44", DATA_TYPE.BOOLEAN, false);
		
		//validate that the column was added to the base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 4, "wrong number of columns in the athena table");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol11", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol22", "varchar", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol33", "bigint", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol44", "boolean", false);
					
		//validate that the column was added to the dev schema
		validateColumnInDatabase("airlock_dev_test", "table1", 1, 0, "col22", DATA_TYPE.STRING, false);
		
		//validate that the column was added to the archive schema
		validateColumnInDatabase("airlock_archive_test", "table1", 1, 0, "col44", DATA_TYPE.BOOLEAN, false);
		
		//validate that the column was not added to the dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 1, "wrong number of columns in the dev athena table");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol33", "bigint", false);
	}
	
	private void validateColumnInDatabase(String schema, String table, int expectedNumberOfColumns, int colIndex, String expectedName, DATA_TYPE expectedType, boolean expectedIsNullable) throws ClassNotFoundException, SQLException {
		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData(schema, table);
		Assert.assertTrue(dbColumns.size() == expectedNumberOfColumns, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(colIndex).getName().equals(expectedName), "wrong column name");
		Assert.assertTrue(dbColumns.get(colIndex).getType().equals(expectedType), "wrong column type");
		Assert.assertTrue(dbColumns.get(colIndex).isNullable() == expectedIsNullable, "wrong is nullable");
	}
	
	private void validateAthenaColumn(ArrayList<ColumnData> currentColumnsData, int columnIndex, String expectedName, String expectedType, boolean expecetdIsPartitionKey) {
		Assert.assertTrue(currentColumnsData.get(columnIndex).columnName.equals(expectedName), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(columnIndex).columnType.equals(expectedType), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(columnIndex).isPartitionKey == expecetdIsPartitionKey, "wrong is isPartitionKey");
	}
	
	/*
	@Test (dependsOnMethods = "addAttribute", description="add attribute using already existing athena column")
	public void addAttributeWithExistingColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID2); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol20"); 
		
		//create the attribute in another entity but the column already exists. 
		String response = entitiesApi.createAttribute(entityID2, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("acol20"), "can create attribute using an already existing athena column");
	}
	
	@Test (dependsOnMethods = "addAttributeWithExistingColumn", description="update attribute to already existing athena column")
	public void updateAttributeToExistingColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col21"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol21"); 
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error") , "cannot create attribute :" + attributeID2);
		
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2: " + attributeRes);
		
		JSONObject attribute2 = new JSONObject(attributeRes);
		attribute2.put("athenaColumn", "acol20");
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("acol20"), "can update attribute to an already existing athena column");
	}
	
	@Test (dependsOnMethods = "updateAttributeToExistingColumn", description="update attribute rename athena column")
	public void renameAthenaColumn() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
		//base Athena database
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol20", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol21", "double", false);
		
		//dev Athena database
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 2, "wrong number of columns in the athena table");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol20", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol21", "double", false);
		
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2");
		
		JSONObject attribute2 = new JSONObject(attributeRes);
		attribute2.put("athenaColumn", "acol22");
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot rename athena column");
		
		//base Athena database
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol20", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol22", "double", false);
		
		//dev Athena database
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 2, "wrong number of columns in the athena table");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol20", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol22", "double", false);
	}
	
	
	@Test (dependsOnMethods = "renameAthenaColumn", description="test that delete attribute leads to athena column deletion")
	public void deleteAthenaColumn() throws JSONException, IOException, ClassNotFoundException, SQLException, InterruptedException{
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
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol22", "double", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol22", "double", false);
		
				
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
		
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 0, "wrong number of columns in the athena table after deletion");
	
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 0, "wrong number of columns in the athena table after deletion");
	}
	
	@Test (dependsOnMethods = "deleteAthenaColumn", description="add attribute STRING")
	public void addAttributeString() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeString");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col11"); 
		attribute.put("dataType", "STRING"); 
		attribute.put("defaultValue", "aaa"); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol23"); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeID1);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol23", "varchar", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol23", "varchar", false);
		
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", "bbb");
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol23", "varchar", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 1, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol23", "varchar", false);
	}
	
	@Test (dependsOnMethods = "addAttributeString", description="add attribute BOOLEAN")
	public void addAttributeBoolean() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeBoolean");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col7"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.put("defaultValue", false); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol24"); 
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol24", "boolean", false);
		
		//base Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 2, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol24", "boolean", false);
			
		String attributeRes = entitiesApi.getAttribute(attributeID2, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute2");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", true);
		
		String response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol24", "boolean", false);
		
		//base Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 2, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol24", "boolean", false);
		
	}
	
	@Test (dependsOnMethods = "addAttributeBoolean", description="add attribute INTEGER")
	public void addAttributeInteger() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeInteger");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col12"); 
		attribute.put("dataType", "INTEGER"); 
		attribute.put("defaultValue", 33); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol25"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error"), "Cannot create attribute3:" + attributeID3);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 3, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol25", "integer", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 3, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol25", "integer", false);
				
		
		String attributeRes = entitiesApi.getAttribute(attributeID3, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute3");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44);
		
		String response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 3, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol25", "integer", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 3, "wrong number of columns in the athena table after deletion");
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol25", "integer", false);
				
	}
	
	@Test (dependsOnMethods = "addAttributeInteger", description="add attribute DOUBLE")
	public void addAttributeDouble() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeDouble");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col9"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 33.33); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol26"); 
		
		attributeID4 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID4.contains("error"), "Cannot create attribute4:" + attributeID3);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 4, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol26", "double", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 4, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol26", "double", false);
					
		String attributeRes = entitiesApi.getAttribute(attributeID4, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute4");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44.44);
		
		String response = entitiesApi.updateAttribute(attributeID4, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 4, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol26", "double", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 4, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol26", "double", false);
				
	}
	
	@Test (dependsOnMethods = "addAttributeDouble", description="add attribute LONG")
	public void addAttributeLong() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeLong");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col13"); 
		attribute.put("dataType", "LONG"); 
		attribute.put("defaultValue", 33L); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol27"); 
		
		attributeID5 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID5.contains("error"), "Cannot create attribute5:" + attributeID5);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 5, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol27", "bigint", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 5, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol27", "bigint", false);
		
			
		String attributeRes = entitiesApi.getAttribute(attributeID5, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute5");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("defaultValue", 44L);
		
		String response = entitiesApi.updateAttribute(attributeID5, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 5, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol27", "bigint", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 5, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol27", "bigint", false);			
	}

	
	@Test (dependsOnMethods = "addAttributeLong", description="add attribute JSON")
	public void addAttributeJSON() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeJSON");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col14"); 
		attribute.put("dataType", "JSON"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol28"); 
		
		attributeID6 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID6.contains("error"), "Cannot create attribute6:" + attributeID6);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 6, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-8, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol27", "bigint", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol28", "varchar", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 6, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-8, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol27", "bigint", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol28", "varchar", false);
		
		
		String attributeRes = entitiesApi.getAttribute(attributeID6, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute6");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("description", " desc 12345");
		
		String response = entitiesApi.updateAttribute(attributeID6, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 6, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-8, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol27", "bigint", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol28", "varchar", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 6, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-8, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol27", "bigint", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol28", "varchar", false);		
	}
	
	@Test (dependsOnMethods = "addAttributeJSON", description="add attribute timestamp")
	public void addAttributeTimestamp() throws JSONException, IOException, InterruptedException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeTimestamp");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col15"); 
		attribute.put("dataType", "TIMESTAMP"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol29"); 
		
		attributeID7 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID7.contains("error"), "Cannot create attribute7:" + attributeID7);
		
		//base Athena db
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 7, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-9, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-8, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol27", "bigint", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol28", "varchar", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol29", "bigint", false);
		
		//dev Athena db
		ArrayList<ColumnData> currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 7, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-9, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-8, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol27", "bigint", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol28", "varchar", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol29", "bigint", false);
		
		
		String attributeRes = entitiesApi.getAttribute(attributeID7, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute6");
		
		attribute = new JSONObject(attributeRes);
		attribute.put("description", " desc 12345");
		
		String response = entitiesApi.updateAttribute(attributeID7, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error") , "cannot update attribute: " + response);
		
		//base Athena db
		currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 7, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-9, "acol23", "varchar", false);	
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-8, "acol24", "boolean", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-7, "acol25", "integer", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-6, "acol26", "double", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-5, "acol27", "bigint", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-4, "acol28", "varchar", false);
		validateAthenaColumn (currentColumnsData, currentColumnsData.size()-3, "acol29", "bigint", false);
		
		//dev Athena db
		currentDevColumnsData = athenaHandler.getColumnData("airlocktestsdbdev", "airlock_test1");
		Assert.assertTrue(currentDevColumnsData.size() - initialDevColumnsData.size() == 7, "wrong number of columns in the athena table after deletion");
		
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-9, "acol23", "varchar", false);	
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-8, "acol24", "boolean", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-7, "acol25", "integer", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-6, "acol26", "double", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-5, "acol27", "bigint", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-4, "acol28", "varchar", false);
		validateAthenaColumn (currentDevColumnsData, currentDevColumnsData.size()-3, "acol29", "bigint", false);		
	}
*/
	
	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
		
		baseUtils.reset(productID1, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}


}
