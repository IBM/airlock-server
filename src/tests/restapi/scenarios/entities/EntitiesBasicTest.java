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


public class EntitiesBasicTest {
	private String filePath;
	private EntitiesRestApi entitiesApi;
	private AirlockUtils baseUtils;
	private String productID;
	private String entityID;
	private String attributTypeID;
	private String dbColumn = null;
	private String attributeID;
	private String sessionToken = "";
	private DbHandler dbHandler; 
	private AthenaHandler athenaHandler; 
	private LinkedList<String> athenaColumnsToDelete; 
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "dburl", "dbuser", "dbpsw", "athenaRegion", "athenaOutputBucket", "athenaCatalog"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String dburl, String dbuser, String dbpsw, String athenaRegion,  String athenaOutputBucket, String athenaCatalog) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID); 
		
		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(url);
		
		dbHandler = new DbHandler(dburl, dbuser, dbpsw);
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);

		athenaColumnsToDelete = new LinkedList<>();
		athenaColumnsToDelete.add("acol1");
		
		try { 
			entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
			entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);

			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
			entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
			
		} catch (Exception e) {	
			System.out.println(e.getMessage());
		}
	}
	/*
	@Test (description="add db schema to product")
	public void addDBSchema() throws JSONException, IOException{
		String entities = entitiesApi.getProductEntities(productID, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", "airlock_test");
		
		entities = entitiesApi.updateEntities(productID, entitiesObj.toString(), sessionToken);
		entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}
	*/
	@Test (description="add entity to product")
	public void addEntity() throws JSONException, IOException{
		String entity = FileUtils.fileToString(filePath + "airlytics/entities/entity1.txt", "UTF-8", false);
		entityID = entitiesApi.createEntity(productID, entity, sessionToken);
		Assert.assertFalse(entityID.contains("error"), "Fail creating entity:" + entityID);
	}
	
	@Test (dependsOnMethods = "addEntity", description="add attribute type to product")
	public void addAttributeType() throws JSONException, IOException{
		String attributeType = FileUtils.fileToString(filePath + "airlytics/entities/attributeType1.txt", "UTF-8", false);
		JSONObject atObj = new JSONObject(attributeType);
		atObj.put("dbTable", "table1");
		attributTypeID = entitiesApi.createAttributeType(entityID, attributeType, sessionToken);
		Assert.assertFalse(attributTypeID.contains("error"), "Fail creating attributeType:" + attributTypeID);
	}
	
	@Test (dependsOnMethods = "addAttributeType", description="add attribute to product")
	public void addAttribute() throws JSONException, IOException{
		String attribute = FileUtils.fileToString(filePath + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attObj = new JSONObject(attribute);
		attObj.put("attributeTypeId", attributTypeID);
		attObj.put("dbColumn", "col1");
		dbColumn = attObj.getString("dbColumn");
		attributeID = entitiesApi.createAttribute(entityID, attObj.toString(), sessionToken);
		Assert.assertFalse(attributeID.contains("error"), "Fail creating attribute:" +attributeID);
	}

	@AfterTest
	public void reset() throws ClassNotFoundException, SQLException{
		try {
			if (dbColumn!=null) {
				entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
				entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_dev_test", "table1");
				entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_archive_test", "table1");
			}
		} catch (Exception e) {
			//ignore
		}
		
		try { 
			entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdb", "airlock_test1", athenaColumnsToDelete);
			entitiesApi.deleteColumnsFromAthena(athenaHandler, "airlocktestsdbdev", "airlock_test1", athenaColumnsToDelete);

		} catch (Exception e) {	
			System.out.println(e.getMessage());
		}
		baseUtils.reset(productID, sessionToken);
	}

	

}
