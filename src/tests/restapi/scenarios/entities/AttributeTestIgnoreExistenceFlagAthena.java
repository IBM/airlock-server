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


public class AttributeTestIgnoreExistenceFlagAthena {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String productID2;
	private String entityID1;
	private String entityID2;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String attributeID1;
	private String attributeID2;
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
		
		athenaColumnsToDelete = new LinkedList<>();
		athenaColumnsToDelete.add("acol20");
		athenaColumnsToDelete.add("acol21");
		athenaColumnsToDelete.add("acol22");
		
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);

		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
		entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);
	}
	
	@Test (description="add entity")
	public void addEntity() throws JSONException, IOException{
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		entityID1 = entitiesApi.createEntity(productID1, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity1: " + entityID1);
		
		entity = new JSONObject(entityStr);
		entity.put("name", "entity2");
		entityID2 = entitiesApi.createEntity(productID2, entity.toString(), sessionToken);
		Assert.assertFalse(entityID1.contains("error"), "Cannot create entity2: " + entityID2);
	}
		
	@Test (dependsOnMethods = "addEntity", description="add attribute type")
	public void addAttributeType() throws JSONException, IOException{
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType1:" + attributeTypeID1);
		
		attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType2");
		attributeType.put("dbTable", "table1");
		attributeTypeID2 = entitiesApi.createAttributeType(entityID2, attributeType.toString(), sessionToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "Cannot create attributeType2:" + attributeTypeID2);
	}
	
	@Test (dependsOnMethods = "addAttributeType", description="add attribute ")
	public void addAttribute() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col20"); 
		attribute.put("dataType", "DOUBLE"); 
		attribute.put("defaultValue", 0.99); 
		attribute.put("nullable", true); 
		attribute.put("athenaColumn", "acol20"); 
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), sessionToken);
		Assert.assertFalse(attributeID1.contains("error"), "Cannot create attribute1:" + attributeTypeID1);
	}
	
	@Test (dependsOnMethods = "addAttribute", description="add attribute using already existing athena column")
	public void addAttributeWithExistingAthenaColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
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
		
		//create the attribute in another entity but the column already exists. 
		response = entitiesApi.createAttributeIgnoreExistence(entityID2, attribute.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot create attribute using an already existing athena column with ignoreExistence flag:" + response);
	}
	
	@Test (dependsOnMethods = "addAttributeWithExistingAthenaColumn", description="update attribute to already existing athena column")
	public void updateAttributeToExistingAthenaColumn() throws JSONException, IOException, ClassNotFoundException, SQLException{
		JSONObject attribute = new JSONObject(attributeStr);
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col22"); 
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
		
		response = entitiesApi.updateAttributeIgnoreExistence(attributeID2, attribute2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update attribute to an already existing athena column with ignoreExistence flag:" + response);
	}
	
	
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
