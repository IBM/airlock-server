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

//test all actions for attribute type that is all admin
//test all actions for attribute type that is all editor
//test all actions for attribute type that is all RW + RWDep = PowerUser, RWDel = admin


public class SpecificAttributesPermissionUpdate{
	
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
		
		//powerUser READ_WRITE+READ_WRITE_DEPRECATE 
		//admin READ_WRITE_DELETE
		//editor = READ_ONLY
		attributeTypeObj.put("name", "attributeType3");
		attributesPermissionObj = new JSONObject();
		attributesPermissionObj.put("READ_WRITE_DELETE", "Administrator");
		attributesPermissionObj.put("READ_WRITE", "AnalyticsPowerUser");
		attributesPermissionObj.put("READ_WRITE_DEPRECATE", "AnalyticsPowerUser");
		attributesPermissionObj.put("READ_ONLY", "AnalyticsEditor");
		attributeTypeObj.put("attributesPermission", attributesPermissionObj);
		
		attributeTypeID3 = entitiesApi.createAttributeType(entityID1, attributeTypeObj.toString(), adminToken);
		Assert.assertFalse(attributeTypeID3.contains("error"), "admin cannot create attributeType:" + attributeTypeID3);
	}
	
		
	@Test (dependsOnMethods="addAttributeTypes", description=" add attribute 1")
	public void addAttributes1() throws Exception {
		String attributeStr = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attribute = new JSONObject(attributeStr);

		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); 
		attribute.put("dbColumn", "col1"); 
		attribute.put("athenaColumn", "acol2"); 
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create attribute1 ");
		
		response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can create attribute1 ");
		
		response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsPowerUser can create attribute1 ");
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID1.contains("error"), "airlyticsAdmin cannot create attribute:" + attributeID1);
	}
	
	@Test (dependsOnMethods="addAttributes1", description=" add attribute 2")
	public void addAttributes2() throws Exception {
		String attributeStr = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attribute = new JSONObject(attributeStr);

		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID2); 
		attribute.put("dbColumn", "col11"); 
		attribute.put("athenaColumn", "acol22"); 
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create attribute1 ");
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertFalse(attributeID2.contains("error"), "airlyticsEditor cannot create attribute:" + attributeID2);
		
		deprecateAttributeByAdmin(attributeID2);
		int code = entitiesApi.deleteAttribute(attributeID2, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 2");	
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(attributeID2.contains("error"), "airlyticsPowerUser cannot create attribute:" + attributeID2);
		
		deprecateAttributeByAdmin(attributeID2);
		code = entitiesApi.deleteAttribute(attributeID2, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 2");	
		
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID2.contains("error"), "admin cannot create attribute:" + attributeID2);
	}
	
	@Test (dependsOnMethods="addAttributes2", description=" add attribute 3")
	public void addAttributes3() throws Exception {
		String attributeStr = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attribute = new JSONObject(attributeStr);

		attribute.put("name", "attribute3");
		attribute.put("attributeTypeId", attributeTypeID3); 
		attribute.put("dbColumn", "col111"); 
		attribute.put("athenaColumn", "acol222"); 
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create attribute1 ");
		
		response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can create attribute1 ");
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(attributeID3.contains("error"), "airlyticsPowerUser cannot create attribute:" + attributeID3);
		
		deprecateAttributeByAdmin(attributeID3);
		int code = entitiesApi.deleteAttribute(attributeID3, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 3");	
		
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID2.contains("error"), "admin cannot create attribute:" + attributeID3);
	}
	
	@Test (dependsOnMethods="addAttributes3", description=" get attribute 1")
	public void getAttributes1() throws Exception {
		String attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by admin:" + attribute);
		
		attribute = entitiesApi.getAttribute(attributeID1, airlyticsViewerToken);
		Assert.assertTrue(attribute.contains("error") && attribute.contains("READ_ONLY"), "airlyticsViewer can get attribute3 ");
		
		attribute = entitiesApi.getAttribute(attributeID1, airlyticsEditorToken);
		Assert.assertTrue(attribute.contains("error") && attribute.contains("READ_ONLY"), "airlyticsEditor can get attribute3 ");
		
		attribute = entitiesApi.getAttribute(attributeID1, airlyticsPowerUserToken);
		Assert.assertTrue(attribute.contains("error") && attribute.contains("READ_ONLY"), "airlyticsPowerUser can get attribute3 ");
	}
	
	@Test (dependsOnMethods="getAttributes1", description=" get attribute 2")
	public void getAttributes2() throws Exception {
		String attribute = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by admin:" + attribute);
		
		attribute = entitiesApi.getAttribute(attributeID2, airlyticsViewerToken);
		Assert.assertTrue(attribute.contains("error"), "airlyticsViewer can get attribute2");
		
		attribute = entitiesApi.getAttribute(attributeID2, airlyticsEditorToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by airlyticsEditor:" + attribute);
		
		attribute = entitiesApi.getAttribute(attributeID2, airlyticsPowerUserToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by airlyticsPowerUser:" + attribute);
		
	}
	
	@Test (dependsOnMethods="getAttributes2", description=" get attribute 3")
	public void getAttributes3() throws Exception {
		String attribute = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by admin:" + attribute);
		
		attribute = entitiesApi.getAttribute(attributeID3, airlyticsViewerToken);
		Assert.assertTrue(attribute.contains("error") && attribute.contains("READ_ONLY"), "airlyticsViewer can get attribute3 ");
		
		attribute = entitiesApi.getAttribute(attributeID3, airlyticsEditorToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by airlyticsEditor:" + attribute);
		
		attribute = entitiesApi.getAttribute(attributeID3, airlyticsPowerUserToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute by airlyticsPowerUser:" + attribute);
	}
	
	@Test (dependsOnMethods="getAttributes3", description=" get attributes")
	public void getAttributes() throws Exception {
		String attributes = entitiesApi.getAttributes(entityID1, adminToken);
		Assert.assertFalse(attributes.contains("error"), "Cannot get attributes by admin:" + attributes);
		JSONObject attributesObj = new JSONObject(attributes);
		
		JSONArray attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.size() == 3, "wrong number of attributes for admin");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("name").equals("attribute1") , "wrong attribute name");
		Assert.assertTrue(attributesArr.getJSONObject(1).getString("name").equals("attribute2") , "wrong attribute name");
		Assert.assertTrue(attributesArr.getJSONObject(2).getString("name").equals("attribute3") , "wrong attribute name");
		
		attributes = entitiesApi.getAttributes(entityID1, airlyticsViewerToken);
		Assert.assertFalse(attributes.contains("error"), "Cannot get attributes by airlyticsViewer:" + attributes);
		attributesObj = new JSONObject(attributes);
		
		attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.size() == 0, "wrong number of attributes for airlyticsViewer");
		
		attributes = entitiesApi.getAttributes(entityID1, airlyticsEditorToken);
		Assert.assertFalse(attributes.contains("error"), "Cannot get attributes by airlyticsEditor:" + attributes);
		attributesObj = new JSONObject(attributes);
		
		attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.size() == 2, "wrong number of attributes for airlyticsEditor");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("name").equals("attribute2") , "wrong attribute name");
		Assert.assertTrue(attributesArr.getJSONObject(1).getString("name").equals("attribute3") , "wrong attribute name");
		
		
		attributes = entitiesApi.getAttributes(entityID1, airlyticsPowerUserToken);
		Assert.assertFalse(attributes.contains("error"), "Cannot get attributes by airlyticsPowerUser:" + attributes);
		attributesObj = new JSONObject(attributes);
		
		attributesArr = attributesObj.getJSONArray("attributes");
		Assert.assertTrue(attributesArr.size() == 2, "wrong number of attributes for airlyticsEditor");
		Assert.assertTrue(attributesArr.getJSONObject(0).getString("name").equals("attribute2") , "wrong attribute name");
		Assert.assertTrue(attributesArr.getJSONObject(1).getString("name").equals("attribute3") , "wrong attribute name");
	}
	
	@Test (dependsOnMethods="getAttributes", description=" update attribute 1")
	public void updateAttributes1() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("description", "desc attribute1");
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can update attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsPowerUser can update attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot update attribute:" + response);
	}
	
	@Test (dependsOnMethods="updateAttributes1", description=" update attribute 2")
	public void updateAttributes2() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("description", "desc attribute2");
		String response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute2 ");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot update attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getString("description").equals("desc attribute2"), "wrong description");
		
		attribute.put("description", "desc attribute2 2");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot update attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getString("description").equals("desc attribute2 2"), "wrong description");
		
		attribute.put("description", "desc attribute2 3");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot update attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getString("description").equals("desc attribute2 3"), "wrong description");
	}
	
	@Test (dependsOnMethods="updateAttributes2", description=" update attribute 3")
	public void updateAttributes3() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute3 by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("description", "desc attribute3");
		String response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can update attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot update attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getString("description").equals("desc attribute3"), "wrong description");
		
		attribute.put("description", "desc attribute3 1");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot update attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getString("description").equals("desc attribute3 1"), "wrong description");
	}
	
	@Test (dependsOnMethods="updateAttributes3", description=" deprecate attribute 1")
	public void deprecateAttributes1() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsPowerUser can deprecate attribute1 ");
		
		response = entitiesApi.updateAttribute(attributeID1, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot deprecate attribute:" + response);
	}
	
	@Test (dependsOnMethods="deprecateAttributes1", description=" deprecate attribute 2")
	public void deprecateAttributes2() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute2 ");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot deprecate attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getBoolean("deprecated"), "wrong deprecated");
		
		undeprecateAttributeByAdmin(attributeID2);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot deprecate attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getBoolean("deprecated"), "wrong deprecated");
		
		undeprecateAttributeByAdmin(attributeID2);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		
		response = entitiesApi.updateAttribute(attributeID2, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot deprecate attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getBoolean("deprecated"), "wrong deprecated");
	}
	
	@Test (dependsOnMethods="deprecateAttributes2", description=" deprecate attribute 3")
	public void deprecateAttributes3() throws Exception {
		String attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute3 by admin:" + attributeStr);
		
		JSONObject attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE"), "airlyticsEditor can deprecate attribute3 ");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot deprectae attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getBoolean("deprecated"), "wrong deprecated");
		
		undeprecateAttributeByAdmin(attributeID3);
		
		attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		
		attribute.put("deprecated", "true");
		
		response = entitiesApi.updateAttribute(attributeID3, attribute.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "airlyticsAdmin cannot deprecate attribute:" + response);
		
		attributeStr = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attributeStr.contains("error"), "Cannot get attribute by admin:" + attributeStr);
		
		attribute = new JSONObject(attributeStr);
		Assert.assertTrue(attribute.getBoolean("deprecated"), "wrong deprecated");
	}
	
	@Test (dependsOnMethods="deprecateAttributes3", description=" delete attribute 1")
	public void deleteAttributes1() throws Exception {
		int code = entitiesApi.deleteAttribute(attributeID1, airlyticsViewerToken);
		Assert.assertFalse(code == 200, "airlyticsViewer can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID1, airlyticsEditorToken);
		Assert.assertFalse(code == 200, "airlyticsEditor can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID1, airlyticsPowerUserToken);
		Assert.assertFalse(code == 200, "airlyticsPowerUser can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID1, adminToken);
		Assert.assertTrue(code == 200, "admin cannot delete attribute1 ");
	}
	
	@Test (dependsOnMethods="deleteAttributes1", description=" delete attribute 2")
	public void deleteAttributes2() throws Exception {
		int code = entitiesApi.deleteAttribute(attributeID2, airlyticsViewerToken);
		Assert.assertFalse(code == 200, "airlyticsViewer can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID2, airlyticsEditorToken);
		Assert.assertFalse(code == 200, "airlyticsEditor can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID2, airlyticsPowerUserToken);
		Assert.assertFalse(code == 200, "airlyticsPowerUser can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID2, adminToken);
		Assert.assertTrue(code == 200, "admin cannot delete attribute1 ");
	}
	
	@Test (dependsOnMethods="deleteAttributes2", description=" delete attribute 3")
	public void deleteAttributes3() throws Exception {
		int code = entitiesApi.deleteAttribute(attributeID3, airlyticsViewerToken);
		Assert.assertFalse(code == 200, "airlyticsViewer can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID3, airlyticsEditorToken);
		Assert.assertFalse(code == 200, "airlyticsEditor can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID3, airlyticsPowerUserToken);
		Assert.assertFalse(code == 200, "airlyticsPowerUser can delete attribute1 ");
		
		code = entitiesApi.deleteAttribute(attributeID3, adminToken);
		Assert.assertTrue(code == 200, "admin cannot delete attribute1 ");
	}
	
	private void deprecateAttributeByAdmin(String attributeID) throws IOException, JSONException {
		String attribute = entitiesApi.getAttribute(attributeID, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute: " + response);
		
	}
	
	private void undeprecateAttributeByAdmin(String attributeID) throws IOException, JSONException {
		String attribute = entitiesApi.getAttribute(attributeID, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "false");
		String response = entitiesApi.updateAttribute(attributeID, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute: " + response);
		
	}
	
	
	private boolean jsonArrayContainsString(JSONArray arr, String val) throws JSONException {
		for (int i=0; i<arr.length(); i++) {
			if (val.equals(arr.getString(i))) {
				return true;
			}
		}
		return false;
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
