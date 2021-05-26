package tests.restapi.airlytics_attributes_permission;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
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
import tests.restapi.OperationRestApi;


public class DeprecatePermissionTest{
	
	public static final String permissionError = "User does not have permission to call this method";
	
	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String analyticsUrl;
	protected String adminUser;
	protected String adminPassword;
	protected String m_appName;
	protected String config;
	protected AirlockUtils baseUtils; 
	protected String apikey;
	protected OperationRestApi operApi;
	private EntitiesRestApi entitiesApi;
	private String entityID1;
	private String attributeTypeID1;
	private String attributeTypeID2;
	private String attributeTypeID3;
	private String attributeID1;
	private String attributeID2;
	private String attributeID3;
	private String productID;
	private String airlyticsViewerUser;
	private String airlyticsViewerPassword;
	private String airlyticsEditorUser;
	private String airlyticsEditorPassword;
	private String airlyticsPowerUser;
	private String airlyticsPowerUserPassword;
	private String airlyticsViewerToken;
	private String airlyticsEditorToken;
	private String airlyticsPowerUserToken;
	private DbHandler dbHandler;
	private AthenaHandler athenaHandler;
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPassword","appName","airlyticsViewerUser","airlyticsViewerPassword","airlyticsEditorUser", "airlyticsEditorPassword","airlyticsPowerUser", "airlyticsPowerUserPassword","productsToDeleteFile", "dburl", "dbuser", "dbpsw", "athenaRegion", "athenaOutputBucket", "athenaCatalog"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName, String airlyticsViewerUser, String airlyticsViewerPassword, String airlyticsEditorUser, String airlyticsEditorPassword, String airlyticsPowerUser, String airlyticsPowerUserPassword, String productsToDeleteFile, String dburl, String dbuser, String dbpsw, String athenaRegion,  String athenaOutputBucket, String athenaCatalog) throws Exception{
		m_url = url;
		operationsUrl = c_operationsUrl;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		this.airlyticsViewerUser = airlyticsViewerUser;
		this.airlyticsViewerPassword = airlyticsViewerPassword;
		this.airlyticsEditorUser = airlyticsEditorUser;
		this.airlyticsEditorPassword = airlyticsEditorPassword;
		this.airlyticsPowerUser = airlyticsPowerUser;
		this.airlyticsPowerUserPassword = airlyticsPowerUserPassword;
		
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		
		entitiesApi = new EntitiesRestApi();
		entitiesApi.setURL(m_url);
		
		m_appName = appName;
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		productID = baseUtils.createProduct();
		
		dbHandler = new DbHandler(dburl, dbuser, dbpsw);
		entitiesApi.deleteAllColumnsFromTable(dbHandler, "airlock_test", "table1");
		
		athenaHandler = new AthenaHandler(athenaRegion, athenaOutputBucket, athenaCatalog);
		athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol2");
		athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol22");
		athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol222");
	}
	
	@Test (description="set users permission")
	public void setUsersPermission() throws Exception {
		operApi.updateUserProductRole("AnalyticsViewer", airlyticsViewerUser, adminToken, productID, config);
		String userProductRoles = operApi.getUserRolesPerProduct(adminToken, productID, airlyticsViewerUser);
		JSONObject uprObj = new JSONObject(userProductRoles);
		assertTrue(uprObj.getJSONArray("roles").size() == 2, "wrong number of roles");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"Viewer"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsViewer"), "wrong role");
		airlyticsViewerToken = baseUtils.setNewJWTToken(airlyticsViewerUser, airlyticsViewerPassword, m_appName);
		
		operApi.updateUserProductRole("AnalyticsEditor", airlyticsEditorUser, adminToken, productID, config);
		userProductRoles = operApi.getUserRolesPerProduct(adminToken, productID, airlyticsEditorUser);
		uprObj = new JSONObject(userProductRoles);
		assertTrue(uprObj.getJSONArray("roles").size() == 3, "wrong number of roles");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"Viewer"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsViewer"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsEditor"), "wrong role");
		airlyticsEditorToken = baseUtils.setNewJWTToken(airlyticsEditorUser, airlyticsEditorPassword, m_appName);
		
		operApi.updateUserProductRole("AnalyticsPowerUser", airlyticsPowerUser, adminToken, productID, config);
		userProductRoles = operApi.getUserRolesPerProduct(adminToken, productID, airlyticsPowerUser);
		uprObj = new JSONObject(userProductRoles);
		assertTrue(uprObj.getJSONArray("roles").size() == 4, "wrong number of roles");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"Viewer"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsViewer"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsEditor"), "wrong role");
		assertTrue(jsonArrayContainsString(uprObj.getJSONArray("roles"),"AnalyticsPowerUser"), "wrong role");
		airlyticsPowerUserToken = baseUtils.setNewJWTToken(airlyticsPowerUser, airlyticsPowerUserPassword, m_appName);
	}
	/*
	@Test (dependsOnMethods="setUsersPermission", description=" set db schema")
	public void setDBSchema() throws Exception {		
		String entities = entitiesApi.getProductEntities(productID, adminToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", "airlock_test");
		
		String response = entitiesApi.updateEntities(productID, entitiesObj.toString(), adminToken);
		assertFalse(response.contains("error"), "admin cannot set db schema");
		entitiesObj = new JSONObject(response);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}
	*/
	@Test (dependsOnMethods="setUsersPermission", description=" add entity")
	public void addEntity() throws Exception {
		String entityStr = FileUtils.fileToString(config + "airlytics/entities/entity1.txt", "UTF-8", false);
		
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		
		entityID1 = entitiesApi.createEntity(productID, entity.toString(), adminToken);
		Assert.assertFalse(entityID1.contains("error"), "admin cannot create entity:" + entityID1);
	}
	
	@Test (dependsOnMethods="addEntity", description=" add attribute types")
	public void addAttributeTypes() throws Exception {
		String attributeTypeStr = FileUtils.fileToString(config + "airlytics/entities/attributeType1.txt", "UTF-8", false);
		JSONObject attributeTypeObj = new JSONObject(attributeTypeStr);
		
		attributeTypeObj.put("dbTable", "table1");
		
		//all admin
		attributeTypeObj.put("name", "attributeType1");
		JSONObject attributesPermissionObj = new JSONObject();
		attributesPermissionObj.put("READ_WRITE_DELETE", "Administrator");
		attributesPermissionObj.put("READ_WRITE", "Administrator");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "Administrator");
		attributesPermissionObj.put("READ_ONLY", "Administrator");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeTypeObj.toString(), adminToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "admin cannot create attributeType:" + attributeTypeID1);
		
		//all editor
		attributeTypeObj.put("name", "attributeType2");
		attributesPermissionObj = new JSONObject();
		attributesPermissionObj.put("READ_WRITE_DELETE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_WRITE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_ONLY", "AnalyticsEditor");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeTypeObj.toString(), adminToken);
		Assert.assertFalse(attributeTypeID2.contains("error"), "admin cannot create attributeType:" + attributeTypeID2);
		
		//admin READ_WRITE_DEPRECATE+READ_WRITE_DELETE
		attributeTypeObj.put("name", "attributeType3");
		attributesPermissionObj = new JSONObject();
		attributesPermissionObj.put("READ_WRITE_DELETE", "Administrator");
		attributesPermissionObj.put("READ_WRITE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "Administrator");
		attributesPermissionObj.put("READ_ONLY", "AnalyticsViewer");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		
		attributeTypeID3 = entitiesApi.createAttributeType(entityID1, attributeTypeObj.toString(), adminToken);
		Assert.assertFalse(attributeTypeID3.contains("error"), "admin cannot create attributeType:" + attributeTypeID3);
	}
	
		
	@Test (dependsOnMethods="addAttributeTypes", description=" add attributes")
	public void addAttributes() throws Exception {
		String attributeStr = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attribute = new JSONObject(attributeStr);

		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col1"); 
		attribute.put("athenaColumn", "acol2"); 
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID1.contains("error"), "airlyticsEditor cannot create attribute:" + attributeID1);
		
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID2); //table1
		attribute.put("dbColumn", "col11"); 
		attribute.put("athenaColumn", "acol22"); 
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID2.contains("error"), "airlyticsPowerUser cannot create attribute:" + attributeID2);
		
		attribute.put("name", "attribute3");
		attribute.put("attributeTypeId", attributeTypeID3); //table1
		attribute.put("dbColumn", "col111"); 
		attribute.put("athenaColumn", "acol222"); 
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID3.contains("error"), "admin cannot create attribute:" + attributeID3);
	}
	
	@Test (dependsOnMethods="addAttributes", description="Test deprecate attribute")
	public void deprecateAttributes() throws Exception {
		//attribute 1
		String attribute1 = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute:" + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsPowerUser can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute1: " + response);
		
		//attribute 2
		String attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute:" + attribute2);
		
		JSONObject attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute2 ");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error") , "airlyticsEditor cannot deprecate attribute2 ");
		
		undeprecateAttributeByAdmin(attributeID2);
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error") , "airlyticsPowerUser cannot deprecate attribute 2: " + response);
		
		undeprecateAttributeByAdmin(attributeID2);
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute 2: " + response);
		
		//attribute 3
		String attribute3 = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute3:" + attribute3);
		
		JSONObject attribute3Obj = new JSONObject(attribute3);
		attribute3Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE_DEPRECATE"), "airlyticsEditor can deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE_DEPRECATE"), "airlyticsPowerUser can deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute3: " + response);	
	}
	
	@Test (dependsOnMethods="deprecateAttributes", description="Test undeprecate attribute")
	public void undeprecateAttributes() throws Exception {
		//attribute 1
		String attribute1 = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute:" + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		Assert.assertTrue(attribute1Obj.getBoolean("deprecated"), "attribute1 is not deprectaed");
		
		attribute1Obj.put("deprecated", "false");
		String response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can un-deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can un-deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsPowerUser can un-deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute1: " + response);
		
		//attribute 2
		String attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute:" + attribute2);
		
		JSONObject attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated"), "attribute2 is not deprectaed");
		
		attribute2Obj.put("deprecated", "false");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute2 ");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error") , "airlyticsEditor cannot un-deprecate attribute2 ");
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute 2: " + response);
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated"), "attribute2 is not deprectaed");
		
		attribute2Obj.put("deprecated", "false");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error") , "airlyticsPowerUser cannot un-deprecate attribute 2: " + response);
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		attribute2Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute 2: " + response);
		
		attribute2 = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute2.contains("error"), "Cannot get attribute 2:" + attribute2);
		
		attribute2Obj = new JSONObject(attribute2);
		Assert.assertTrue(attribute2Obj.getBoolean("deprecated"), "attribute2 is not deprectaed");
		
		attribute2Obj.put("deprecated", "false");
		response = entitiesApi.updateAttribute(attributeID2, attribute2Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute 2: " + response);
		
		//attribute 3
		String attribute3 = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute3:" + attribute3);
		
		JSONObject attribute3Obj = new JSONObject(attribute3);
		Assert.assertTrue(attribute3Obj.getBoolean("deprecated"), "attribute3 is not deprectaed");
		
		attribute3Obj.put("deprecated", "false");
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can un-deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE_DEPRECATE"), "airlyticsEditor can un-deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE_DEPRECATE"), "airlyticsPowerUser can un-deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute3Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot un-deprecate attribute3: " + response);	
	}
	
	@Test (dependsOnMethods="undeprecateAttributes", description=" update attribute type")
	public void updateAttributeType() throws Exception {
		String attributeType = entitiesApi.getAttributeType(attributeTypeID1, adminToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType:" + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		JSONObject attributesPermissionObj = new JSONObject();
		attributesPermissionObj.put("READ_WRITE_DELETE", "Administrator");
		attributesPermissionObj.put("READ_WRITE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "AnalyticsEditor");
		attributesPermissionObj.put("READ_ONLY", "AnalyticsViewer");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		
		String response = entitiesApi.updateAttributeType(attributeTypeID1, attributeTypeObj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error") , "airlyticseditor cannot update attribute type: " + response);		
	}
	
	@Test (dependsOnMethods="updateAttributeType", description="Test deprecate attribute after attribute type update")
	public void deprecateAttributesAfterAttTypeUpdate() throws Exception {
		String attribute1 = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute:" + attribute1);
		
		JSONObject attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot deprecate attribute1 ");
		
		undeprecateAttributeByAdmin(attributeID1);
		
		attribute1 = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1:" + attribute1);
		
		attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot deprecate attribute1 ");
		
		undeprecateAttributeByAdmin(attributeID1);
		
		attribute1 = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute1.contains("error"), "Cannot get attribute1:" + attribute1);
		
		attribute1Obj = new JSONObject(attribute1);
		attribute1Obj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID1, attribute1Obj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute1: " + response);		
	}
	
	private boolean jsonArrayContainsString(JSONArray arr, String val) throws JSONException {
		for (int i=0; i<arr.length(); i++) {
			if (val.equals(arr.getString(i))) {
				return true;
			}
		}
		return false;
	}
	private void undeprecateAttributeByAdmin(String attributeID) throws IOException, JSONException {
		String attribute = entitiesApi.getAttribute(attributeID, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "false");
		String response = entitiesApi.updateAttribute(attributeID, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute: " + response);
		
	}
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.deleteKeys(null);
		try { 
			if (dbHandler!=null) {
				dbHandler.removeColumnFromTable("airlock_test", "table1", "col1");
				dbHandler.removeColumnFromTable("airlock_test", "table1", "col11");
				dbHandler.removeColumnFromTable("airlock_test", "table1", "col111");
			}
			
			if(athenaHandler!=null) {
				athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol2");
				athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol22");
				athenaHandler.deleteColumn("airlocktestsdb", "airlock_test1", "acol222");
			}
		} catch (Exception e) {
			//ignore
		}
	}
}
