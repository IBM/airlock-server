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


public class AttributeTestArrayType {
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
	private ArrayList<ColumnData> initialColumnsData;
	private LinkedList<String> athenaColumnsToDelete; 
	
	
	//test isArray with def value. create+update
	//test update isArray true=>false and vice versa
	//test creation array of all types. check type in the db and in thena
	
	
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
		athenaColumnsToDelete.add("acol20");
		athenaColumnsToDelete.add("acol21");
		athenaColumnsToDelete.add("acol211");
		athenaColumnsToDelete.add("acol222");
		athenaColumnsToDelete.add("acol233");
		athenaColumnsToDelete.add("acol244");
		athenaColumnsToDelete.add("acol255");
		athenaColumnsToDelete.add("acol266");
		athenaColumnsToDelete.add("acol277");
		
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);

		initialColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
	}
	
	@Test (description="add entity")
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
	
	@Test (dependsOnMethods = "addAttributeType", description="add attribute with no isArray")
	public void addAttributeNoIsArrayInput() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col20"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol20"); 
		attribute.remove("isArray");
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeTypeID1);
		
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1: " + attributeRes);
	
		attribute = new JSONObject(attributeRes);
		Assert.assertFalse(attribute.getBoolean("isArray"), "Creation of attribute without isArray field input. Is array is not false");
	}
	
	@Test (dependsOnMethods = "addAttributeNoIsArrayInput", description="update attribute is array")
	public void updateAttributetoArray() throws JSONException, IOException, ClassNotFoundException, SQLException{
		String attributeRes = entitiesApi.getAttribute(attributeID1, sessionToken);
		Assert.assertFalse(attributeRes.contains("error"), "Cannot get attribute1: " + attributeRes);
	
		JSONObject  attribute = new JSONObject(attributeRes);
		attribute.put("isArray", true);
		
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("isArray"), "Can update attribute1 is array. ");
	}
	
	@Test (dependsOnMethods = "updateAttributetoArray", description="add  array of STRINGS attribute")
	public void addAttributeArrayString() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeStringArray");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col211"); 
		attribute.put("dataType", "STRING"); 
		attribute.remove("defaultValue"); 
		attribute.put("withDefaultValue", false); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol211"); 
		attribute.put("isArray", true);
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID2.contains("error"), "Cannot create attribute2:" + attributeID2);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 2, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");
		
		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 2, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
	}

	@Test (dependsOnMethods = "addAttributeArrayString", description="add BOOLEAN array attribute")
	public void addAttributeArrayBoolean() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeBoolean");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col222"); 
		attribute.put("dataType", "BOOLEAN"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("isArray", true); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol222"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error"), "Cannot create attribute3:" + attributeID3);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 3, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");
		
		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 3, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
	}
	
	@Test (dependsOnMethods = "addAttributeArrayBoolean", description="add INTEGER array attribute")
	public void addAttributeArrayInteger() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeInteger");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col233"); 
		attribute.put("dataType", "INTEGER"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("isArray", true); 
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol233"); 
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID3.contains("error"), "Cannot create attribute3:" + attributeID3);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 4, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol233"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");

		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 4, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(3).getName().equals("col233"), "wrong column name");
		Assert.assertTrue(dbColumns.get(3).getType().equals(DATA_TYPE.INTEGER), "wrong column type");
		Assert.assertTrue(dbColumns.get(3).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(3).isNullable() == false, "wrong is nullable");
	}
	

	
	@Test (dependsOnMethods = "addAttributeArrayInteger", description="add DOUBLE array attribute")
	public void addAttributeArrayDouble() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeDouble");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col244"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("isArray", true); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol244"); 
		
		attributeID4 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID4.contains("error"), "Cannot create attribute4:" + attributeID4);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 5, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol233"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol244"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");

		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 5, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(3).getName().equals("col233"), "wrong column name");
		Assert.assertTrue(dbColumns.get(3).getType().equals(DATA_TYPE.INTEGER), "wrong column type");
		Assert.assertTrue(dbColumns.get(3).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(3).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(4).getName().equals("col244"), "wrong column name");
		Assert.assertTrue(dbColumns.get(4).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(4).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(4).isNullable() == true, "wrong is nullable");
	}
	
	@Test (dependsOnMethods = "addAttributeArrayDouble", description="add LONG array attribute")
	public void addAttributeArrayLong() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeLong");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col255"); 
		attribute.put("dataType", "LONG"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("isArray", true); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol255"); 
		
		attributeID5 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID5.contains("error"), "Cannot create attribute5:" + attributeID5);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 6, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol233"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol244"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol255"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");

		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 6, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(3).getName().equals("col233"), "wrong column name");
		Assert.assertTrue(dbColumns.get(3).getType().equals(DATA_TYPE.INTEGER), "wrong column type");
		Assert.assertTrue(dbColumns.get(3).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(3).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(4).getName().equals("col244"), "wrong column name");
		Assert.assertTrue(dbColumns.get(4).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(4).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(4).isNullable() == true, "wrong is nullable");
	
		Assert.assertTrue(dbColumns.get(5).getName().equals("col255"), "wrong column name");
		Assert.assertTrue(dbColumns.get(5).getType().equals(DATA_TYPE.LONG), "wrong column type");
		Assert.assertTrue(dbColumns.get(5).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(5).isNullable() == true, "wrong is nullable");
	}

	
	@Test (dependsOnMethods = "addAttributeArrayLong", description="add JSON array attribute")
	public void addAttributeArrayJSON() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeJSON");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col266"); 
		attribute.put("dataType", "JSON"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false); 
		attribute.put("isArray", true); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol266"); 
		
		attributeID6 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID6.contains("error"), "Cannot create attribute6:" + attributeID6);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 7, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnName.equals("acol233"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol244"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol255"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol266"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");

		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 7, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(3).getName().equals("col233"), "wrong column name");
		Assert.assertTrue(dbColumns.get(3).getType().equals(DATA_TYPE.INTEGER), "wrong column type");
		Assert.assertTrue(dbColumns.get(3).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(3).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(4).getName().equals("col244"), "wrong column name");
		Assert.assertTrue(dbColumns.get(4).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(4).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(4).isNullable() == true, "wrong is nullable");
	
		Assert.assertTrue(dbColumns.get(5).getName().equals("col255"), "wrong column name");
		Assert.assertTrue(dbColumns.get(5).getType().equals(DATA_TYPE.LONG), "wrong column type");
		Assert.assertTrue(dbColumns.get(5).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(5).isNullable() == true, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(6).getName().equals("col266"), "wrong column name");
		Assert.assertTrue(dbColumns.get(6).getType().equals(DATA_TYPE.JSON), "wrong column type");
		Assert.assertTrue(dbColumns.get(6).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(6).isNullable() == true, "wrong is nullable");
	}
	
	@Test (dependsOnMethods = "addAttributeArrayJSON", description="add  timestamp array attribute")
	public void addAttributeArrayTimestamp() throws JSONException, IOException, InterruptedException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attributeTimestamp");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col277"); 
		attribute.put("dataType", "TIMESTAMP"); 
		attribute.remove("defaultValue");
		attribute.put("withDefaultValue", false);
		attribute.put("isArray", true);
		attribute.put("nullable", false); 
		attribute.put("athenaColumn", "acol277"); 
		
		attributeID7 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID7.contains("error"), "Cannot create attribute7:" + attributeID7);
		
		ArrayList<ColumnData> currentColumnsData = athenaHandler.getColumnData("airlocktestsdb", "airlock_test1");
		Assert.assertTrue(currentColumnsData.size() - initialColumnsData.size() == 8, "wrong number of columns in the athena table ");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-10).columnName.equals("acol20"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-10).columnType.equals("double"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-10).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).columnName.equals("acol211"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-9).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnName.equals("acol222"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-8).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnName.equals("acol233"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-7).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnName.equals("acol244"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-6).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnName.equals("acol255"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-5).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnName.equals("acol266"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-4).isPartitionKey == false, "wrong is isPartitionKey");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnName.equals("acol277"), "wrong column name");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).columnType.equals("varchar"), "wrong column type");
		Assert.assertTrue(currentColumnsData.get(currentColumnsData.size()-3).isPartitionKey == false, "wrong is isPartitionKey");

		List<TableColumn>  dbColumns = dbHandler.getTableCulomnsData("airlock_test", "table1");
		Assert.assertTrue(dbColumns.size() == 8, "wrong number of columns in the database");
		Assert.assertTrue(dbColumns.get(0).getName().equals("col20"), "wrong column name");
		Assert.assertTrue(dbColumns.get(0).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(0).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(0).isArray() == false, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(1).getName().equals("col211"), "wrong column name");
		Assert.assertTrue(dbColumns.get(1).getType().equals(DATA_TYPE.STRING), "wrong column type");
		Assert.assertTrue(dbColumns.get(1).isNullable() == true, "wrong is nullable");
		Assert.assertTrue(dbColumns.get(1).isArray() == true, "wrong is array");
		
		Assert.assertTrue(dbColumns.get(2).getName().equals("col222"), "wrong column name");
		Assert.assertTrue(dbColumns.get(2).getType().equals(DATA_TYPE.BOOLEAN), "wrong column type");
		Assert.assertTrue(dbColumns.get(2).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(2).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(3).getName().equals("col233"), "wrong column name");
		Assert.assertTrue(dbColumns.get(3).getType().equals(DATA_TYPE.INTEGER), "wrong column type");
		Assert.assertTrue(dbColumns.get(3).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(3).isNullable() == false, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(4).getName().equals("col244"), "wrong column name");
		Assert.assertTrue(dbColumns.get(4).getType().equals(DATA_TYPE.DOUBLE), "wrong column type");
		Assert.assertTrue(dbColumns.get(4).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(4).isNullable() == true, "wrong is nullable");
	
		Assert.assertTrue(dbColumns.get(5).getName().equals("col255"), "wrong column name");
		Assert.assertTrue(dbColumns.get(5).getType().equals(DATA_TYPE.LONG), "wrong column type");
		Assert.assertTrue(dbColumns.get(5).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(5).isNullable() == true, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(6).getName().equals("col266"), "wrong column name");
		Assert.assertTrue(dbColumns.get(6).getType().equals(DATA_TYPE.JSON), "wrong column type");
		Assert.assertTrue(dbColumns.get(6).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(6).isNullable() == true, "wrong is nullable");
		
		Assert.assertTrue(dbColumns.get(7).getName().equals("col277"), "wrong column name");
		Assert.assertTrue(dbColumns.get(7).getType().equals(DATA_TYPE.TIMESTAMP), "wrong column type");
		Assert.assertTrue(dbColumns.get(7).isArray() == true, "wrong is array");
		Assert.assertTrue(dbColumns.get(7).isNullable() == false, "wrong is nullable");
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
