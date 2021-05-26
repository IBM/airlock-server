package tests.restapi.airlytics_attributes_permission;


import static org.testng.Assert.assertTrue;

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


public class AttributesPermissionsBVT{
	
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
	
	/*@Test (dependsOnMethods="setUsersPermission", description="Test set db schema")
	public void setDBSchema() throws Exception {
		String entities = entitiesApi.getProductEntities(productID, adminToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", "airlock_test");
		
		String response = entitiesApi.updateEntities(productID, entitiesObj.toString(), airlyticsViewerToken);
		assertTrue(response.contains("error") && response.contains(permissionError), "airlytics viewer can set db schema");
		
		response = entitiesApi.updateEntities(productID, entitiesObj.toString(), airlyticsEditorToken);
		assertTrue(response.contains("error") && response.contains(permissionError), "airlytics viewer can set db schema");
		
		response = entitiesApi.updateEntities(productID, entitiesObj.toString(), airlyticsPowerUserToken);
		assertTrue(response.contains("error") && response.contains(permissionError), "airlytics viewer can set db schema");
		
		response = entitiesApi.updateEntities(productID, entitiesObj.toString(), adminToken);
		assertFalse(response.contains("error"), "admin cannot set db schema");
		entitiesObj = new JSONObject(response);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
	}*/
	
	@Test (dependsOnMethods="setUsersPermission", description="Test add entity")
	public void addEntity() throws Exception {
		String entityStr = FileUtils.fileToString(config + "airlytics/entities/entity1.txt", "UTF-8", false);
		
		JSONObject entity = new JSONObject(entityStr);
		entity.put("name", "entity1");
		String response  = entitiesApi.createEntity(productID, entity.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create entity");
		
		response  = entitiesApi.createEntity(productID, entity.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsEditor can create entity");
		
		response  = entitiesApi.createEntity(productID, entity.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsPowerUser can create entity");
		
		entityID1 = entitiesApi.createEntity(productID, entity.toString(), adminToken);
		Assert.assertFalse(entityID1.contains("error"), "admin cannot create entity:" + entityID1);
	}
	
	@Test (dependsOnMethods="addEntity", description="Test add entity")
	public void updateEntity() throws Exception {
		String entity = entitiesApi.getEntity(entityID1, adminToken);
		Assert.assertFalse(entity.contains("error"), "Cannot get entity:" + entity);
		
		JSONObject entityObj = new JSONObject(entity);
		entityObj.put("description", "desc123456789");
		entityObj.remove("attributes");
		entityObj.remove("attributeTypes");
		String response = entitiesApi.updateEntity(entityID1, entityObj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update entity");
		
		response = entitiesApi.updateEntity(entityID1, entityObj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsEditor can update entity");
		
		response = entitiesApi.updateEntity(entityID1, entityObj.toString(), airlyticsPowerUserToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsPowerUser can update entity");
		
		response = entitiesApi.updateEntity(entityID1, entityObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot can update entity: " +response);
	}
	
	@Test (dependsOnMethods="updateEntity", description="Test get entity")
	public void getEntity() throws Exception {
		String response = entitiesApi.getEntity(entityID1, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get entity:" + response);
		
		response = entitiesApi.getEntity(entityID1, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get entity:" + response);
		
		response = entitiesApi.getEntity(entityID1, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get entity:" + response);
		
		response = entitiesApi.getEntity(entityID1, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get entity:" + response);
	}
	
	@Test (dependsOnMethods="getEntity", description="Test get entities")
	public void getEntities() throws Exception {
		String response = entitiesApi.getProductEntities(productID, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get entities:" + response);
		
		response = entitiesApi.getProductEntities(productID, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get entities:" + response);
		
		response = entitiesApi.getProductEntities(productID, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get entities:" + response);
		
		response = entitiesApi.getProductEntities(productID, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get entities:" + response);
	}
	
	@Test (dependsOnMethods="getEntities", description="Test add attribute type")
	public void addAttributeType() throws Exception {
		String attributeTypeStr = FileUtils.fileToString(config + "airlytics/entities/attributeType1.txt", "UTF-8", false);
		JSONObject attributeType = new JSONObject(attributeTypeStr);
		
		attributeType.put("name", "attributeType1");
		attributeType.put("dbTable", "table1");
		String response = entitiesApi.createAttributeType(entityID1, attributeType.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create attribute type");
		
		attributeTypeID1 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), airlyticsEditorToken);
		Assert.assertFalse(attributeTypeID1.contains("error"), "airlyticsEditor cannot create attributeType:" + attributeTypeID1);
		
		attributeType.put("name", "attributeType2");
		attributeType.put("dbTable", "table2");
		attributeType.put("athenaTable", "airlock_test2");
		
		attributeTypeID2 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(attributeTypeID2.contains("error"), "airlyticsPowerUser cannot create attributeType:" + attributeTypeID2);
		
		attributeType.put("name", "attributeType3");
		
		attributeTypeID3 = entitiesApi.createAttributeType(entityID1, attributeType.toString(), adminToken);
		Assert.assertFalse(attributeTypeID3.contains("error"), "admin cannot create attributeType:" + attributeTypeID3);
		
	}
	
	@Test (dependsOnMethods="addAttributeType", description="Test update attribute type")
	public void updateAttributeType() throws Exception {
		String attributeType = entitiesApi.getAttributeType(attributeTypeID1, adminToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType:" + attributeType);
		
		JSONObject attributeTypeObj = new JSONObject(attributeType);
		attributeTypeObj.put("description", "desc 123456789");
		String response = entitiesApi.updateAttributeType(attributeTypeID1, attributeTypeObj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute type");
		
		response = entitiesApi.updateAttributeType(attributeTypeID1, attributeTypeObj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error") , "airlyticseditor cannot update attribute type: " + response);
		
		attributeType = entitiesApi.getAttributeType(attributeTypeID1, adminToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType:" + attributeType);
		
		attributeTypeObj = new JSONObject(attributeType);
		attributeTypeObj.put("description", "desc 123456789123456789123456789");
		response = entitiesApi.updateAttributeType(attributeTypeID1, attributeTypeObj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error") , "airlyticsPowerUser cannot update attribute type: " + response);
		
		attributeType = entitiesApi.getAttributeType(attributeTypeID1, adminToken);
		Assert.assertFalse(attributeType.contains("error"), "Cannot get attributeType:" + attributeType);
		
		attributeTypeObj = new JSONObject(attributeType);
		attributeTypeObj.put("description", "desc 123456789123456789");
		response = entitiesApi.updateAttributeType(attributeTypeID1, attributeTypeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute type: " + response);		
	}
	
	@Test (dependsOnMethods="updateAttributeType", description="Test get attribute type")
	public void getAttributeType() throws Exception {
		String response = entitiesApi.getAttributeType(attributeTypeID1, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get attribute type:" + response);
		
		response = entitiesApi.getAttributeType(attributeTypeID1, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get attribute type:" + response);
		
		response = entitiesApi.getAttributeType(attributeTypeID1, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get attribute type:" + response);
		
		response = entitiesApi.getAttributeType(attributeTypeID1, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get attribute type:" + response);
	}
	
	@Test (dependsOnMethods="getAttributeType", description="Test get attribute types")
	public void getAttributeTypes() throws Exception {
		String response = entitiesApi.getAttributeTypes(entityID1, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get attribute types:" + response);
		
		response = entitiesApi.getAttributeTypes(entityID1, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get attribute types:" + response);
		
		response = entitiesApi.getAttributeTypes(entityID1, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get attribute types:" + response);
		
		response = entitiesApi.getAttributeTypes(entityID1, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get attribute types:" + response);
	}
	
	@Test (dependsOnMethods="getAttributeTypes", description="Test add attribute ")
	public void addAttribute() throws Exception {
		String attributeStr = FileUtils.fileToString(config + "airlytics/entities/attribute1.txt", "UTF-8", false);
		JSONObject attribute = new JSONObject(attributeStr);

		attribute.put("name", "attribute1");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col1"); 
		attribute.put("athenaColumn", "acol2"); 
		String response = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can create attribute");
		
		attributeID1 = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsEditorToken);
		Assert.assertFalse(attributeID1.contains("error"), "airlyticsEditor cannot create attribute:" + attributeID1);
		
		
		attribute.put("name", "attribute2");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col11"); 
		attribute.put("athenaColumn", "acol22"); 
		attributeID2 = entitiesApi.createAttribute(entityID1, attribute.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(attributeID2.contains("error"), "airlyticsPowerUser cannot create attribute:" + attributeID2);
		
		attribute.put("name", "attribute3");
		attribute.put("attributeTypeId", attributeTypeID1); //table1
		attribute.put("dbColumn", "col111"); 
		attribute.put("athenaColumn", "acol222"); 
		attributeID3 = entitiesApi.createAttribute(entityID1, attribute.toString(), adminToken);
		Assert.assertFalse(attributeID3.contains("error"), "admin cannot create attribute:" + attributeID3);
	}
	
	
	@Test (dependsOnMethods="addAttribute", description="Test update attribute")
	public void updateAttribute() throws Exception {
		String attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("description", "desc 123456789");
		String response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can update attribute ");
		
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsEditorToken);
		Assert.assertFalse(response.contains("error") , "airlyticseditor cannot update attribute: " + response);
		
		attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("description", "desc 123456789123456789123456789");
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error") , "airlyticsPowerUser cannot update attribute : " + response);
		
		attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("description", "desc 123456789123456789");
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute : " + response);		
	}
	
	@Test (dependsOnMethods="updateAttribute", description="Test get attribute")
	public void getAttribute() throws Exception {
		String response = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get attribute :" + response);
		
		response = entitiesApi.getAttribute(attributeID1, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get attribute :" + response);
		
		response = entitiesApi.getAttribute(attributeID1, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get attribute :" + response);
		
		response = entitiesApi.getAttribute(attributeID1, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get attribute :" + response);
	}
	
	@Test (dependsOnMethods="getAttribute", description="Test get attributes")
	public void getAttributes() throws Exception {
		String response = entitiesApi.getAttributes(entityID1, adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get attributes:" + response);
		
		response = entitiesApi.getAttributes(entityID1, airlyticsViewerToken);
		Assert.assertFalse(response.contains("error"), "airlyticsViewer cannot get attributes:" + response);
		
		response = entitiesApi.getAttributes(entityID1, airlyticsEditorToken);
		Assert.assertFalse(response.contains("error"), "airlyticsEditor cannot get attributes:" + response);
		
		response = entitiesApi.getAttributes(entityID1, airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser cannot get attributes:" + response);
	}
	
	@Test (dependsOnMethods="getAttributes", description="Test get db schemas")
	public void getDBSchemas() throws Exception {
		String response = entitiesApi.getDbSchemas(adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get db schemas:" + response);
		
		response = entitiesApi.getDbSchemas(airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer in product (but not global) can get db schemas");
		
		response = entitiesApi.getDbSchemas(airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsEditor in product (but not global) can get db schemas");
		
		//user3@weather.com is analyticsViewer in global
		response = entitiesApi.getDbSchemas(airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser (analytics viewer in global) cannot get db schemas:" + response);
	}
	
	@Test (dependsOnMethods="getDBSchemas", description="Test get db tables")
	public void getDBTables() throws Exception {
		String response = entitiesApi.getDbTablesInSchema("airlock_test", adminToken);
		Assert.assertFalse(response.contains("error"), "admin cannot get db tables:" + response);
		
		response = entitiesApi.getDbTablesInSchema("airlock_test", airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer in product (but not global) can get db tables");
		
		response = entitiesApi.getDbTablesInSchema("airlock_test", airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsEditor in product (but not global) can get db tables");
		
		response = entitiesApi.getDbTablesInSchema("airlock_test", airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error"), "airlyticsPowerUser (analytics viewer in global) cannot get db tables:" + response);
	}
	
	@Test (dependsOnMethods="getDBTables", description="Test deprecate attribute")
	public void deprecateAttribute() throws Exception {
		String attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		JSONObject attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "true");
		String response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsViewerToken);
		Assert.assertTrue(response.contains("error") && response.contains(permissionError), "airlyticsViewer can deprecate attribute ");
		
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsEditorToken);
		Assert.assertTrue(response.contains("error") && response.contains("READ_WRITE_DEPRECATE"), "airlyticsEditor can deprecate attribute ");
		
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), airlyticsPowerUserToken);
		Assert.assertFalse(response.contains("error") , "airlyticsPowerUser cannot deprecate attribute : " + response);
		
		attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "false");
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot update attribute : " + response);
		
		//attribute 1
		attribute = entitiesApi.getAttribute(attributeID1, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute 1:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID1, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute 1: " + response);
		
		//attribute 2
		attribute = entitiesApi.getAttribute(attributeID2, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute 2:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID2, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute 2: " + response);
		
		//attribute 3
		attribute = entitiesApi.getAttribute(attributeID3, adminToken);
		Assert.assertFalse(attribute.contains("error"), "Cannot get attribute 3:" + attribute);
		
		attributeObj = new JSONObject(attribute);
		attributeObj.put("deprecated", "true");
		response = entitiesApi.updateAttribute(attributeID3, attributeObj.toString(), adminToken);
		Assert.assertFalse(response.contains("error") , "admin cannot deprecate attribute 3: " + response);
	}
	
	@Test (dependsOnMethods="deprecateAttribute", description="Test delete attribute")
	public void deleteAttribute() throws Exception {
		int  code = entitiesApi.deleteAttribute(attributeID1, airlyticsViewerToken);
		Assert.assertTrue(code!=200, "airlyticsViewer can delete attribute ");
		
		code = entitiesApi.deleteAttribute(attributeID1, airlyticsEditorToken);
		Assert.assertTrue(code!=200, "airlyticsEditor can delete attribute ");
		
		code = entitiesApi.deleteAttribute(attributeID1, airlyticsPowerUserToken);
		Assert.assertTrue(code!=200, "airlyticsPowerUser can delete attribute ");
		
		code = entitiesApi.deleteAttribute(attributeID1, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 1");		
		
		code = entitiesApi.deleteAttribute(attributeID2, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 2");		
		
		code = entitiesApi.deleteAttribute(attributeID3, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute 3");		
	}
	
	@Test (dependsOnMethods="deleteAttribute", description="Test delete attribute type")
	public void deleteAttributeType() throws Exception {
		int  code = entitiesApi.deleteAttributeType(attributeTypeID1, airlyticsViewerToken);
		Assert.assertTrue(code!=200, "airlyticsViewer can delete attribute type");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID1, airlyticsEditorToken);
		Assert.assertTrue(code==200, "airlyticsEditor cannot delete attribute type");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID2, airlyticsPowerUserToken);
		Assert.assertTrue(code==200, "airlyticsPowerUser cannot delete attribute type ");
		
		code = entitiesApi.deleteAttributeType(attributeTypeID3, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete attribute type");		
	}
	
	@Test (dependsOnMethods="deleteAttributeType", description="Test delete entity")
	public void deleteEntity() throws Exception {
		int  code = entitiesApi.deleteEntity(entityID1, airlyticsViewerToken);
		Assert.assertTrue(code!=200, "airlyticsViewer can delete entity ");
		
		code = entitiesApi.deleteEntity(entityID1, airlyticsEditorToken);
		Assert.assertTrue(code!=200, "airlyticsEditor can delete entity ");
		
		code = entitiesApi.deleteEntity(entityID1, airlyticsPowerUserToken);
		Assert.assertTrue(code!=200, "airlyticsPowerUser can delete entity ");
		
		code = entitiesApi.deleteEntity(entityID1, adminToken);
		Assert.assertTrue(code==200, "admin cannot delete entity");		
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
