package tests.restapi.scenarios.capabilities;


import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;
import tests.restapi.SeasonsRestApi;

public class TestEntitiesCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private SeasonsRestApi seasonApi;
	private TestAllApi allApis;
	private String productID;
	private String entityID;
	private String attributeID;
	private String attributeTypeID;
	
	private JSONObject entityObj;
	private JSONObject attributeObj;
	private JSONObject attributeTypeObj;
	
	private ArrayList<String> results = new ArrayList<String>();
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{
		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		seasonApi = new SeasonsRestApi();
		seasonApi.setURL(url);;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);
		
		if (isAuthenticated.equals("true"))
			allApis.isAuthenticated = true;

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}
	

	
	@Test (description = "Set  capabilities in product without entities")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("ENTITIES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create string without translation capability")
	public void testCreateEntity() throws Exception{
		String response = allApis.addEntity(productID, "/airlytics/entities/entity1.txt", sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("capability"), "Added entity without ENTITIES capabilities");
	}
	
	@Test (dependsOnMethods="testCreateEntity", description = "Set ENTITIES capability and add entity, remove ENTITIES capability")
	public void addEntity() throws Exception{
		//add ENTITIES capability
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("ENTITIES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		/*
		//set db schema
		String entities = allApis.getProductEntities(productID, sessionToken);
		JSONObject entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.get("dbSchema")==null, "db schema is not null when product is created");
		
		entitiesObj.remove("entities");
		entitiesObj.put("dbSchema", "airlock_test");
		
		entities = allApis.updateEntities(productID, entitiesObj.toString(), sessionToken);
		entitiesObj = new JSONObject(entities);
		Assert.assertTrue(entitiesObj.getString("dbSchema").equals("airlock_test"), "db schema is not 'airlock_test' after update");
		*/
		//add entity
		entityID = allApis.addEntity(productID, "/airlytics/entities/entity1.txt", sessionToken);
		Assert.assertFalse(entityID.contains("error"), "Can't create entity: " + entityID);
		
		response = allApis.getEntity(entityID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get entity: " + response);
		entityObj = new JSONObject(response);
		
		//add attribute type
		attributeTypeID = allApis.addAttributeType(entityID, "/airlytics/entities/attributeType1.txt", sessionToken);
		Assert.assertFalse(attributeTypeID.contains("error"), "Can't create attribute type: " + attributeTypeID);
		
		response = allApis.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attributeType: " + response);
		attributeTypeObj = new JSONObject(response);
		
		//add attribute
		attributeID = allApis.addAttribute(entityID, "/airlytics/entities/attribute1.txt", attributeTypeID, sessionToken);
		Assert.assertFalse(attributeID.contains("error"), "Can't create attribute: " + attributeID);
		
		response = allApis.getAttribute(attributeID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attribute: " + response);
		attributeObj = new JSONObject(response);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("ENTITIES"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="addEntity", description = "test entities api without capability")
	public void testNegativeCapabilities() throws Exception{
		String response = allApis.addEntity(productID, "/airlytics/entities/entity2.txt", sessionToken);
		validateNegativeTestResult(response, "create entity");
		response = allApis.getProductEntities(productID, sessionToken);
		validateNegativeTestResult(response, "get all entities");
		response = allApis.getEntity(entityID, sessionToken);
		validateNegativeTestResult(response, "get entity");
		response = allApis.updateEntity(entityID, entityObj.toString(), sessionToken);
		validateNegativeTestResult(response, "update entity");
		
		response = allApis.addAttributeType(entityID, "/airlytics/entities/attributeType1.txt", sessionToken);
		validateNegativeTestResult(response, "create AttributeType");
		response = allApis.getAttributeTypes(entityID, sessionToken);
		validateNegativeTestResult(response, "get all AttributeType");
		response = allApis.getAttributeType(attributeTypeID, sessionToken);
		validateNegativeTestResult(response, "get AttributeType");
		response = allApis.updateAttributeType(attributeTypeID, attributeTypeObj.toString(), sessionToken);
		validateNegativeTestResult(response, "update AttributeType");
		
		response = allApis.addAttribute(entityID, "/airlytics/entities/attribute1.txt", attributeTypeID, sessionToken);
		validateNegativeTestResult(response, "create Attribute");
		response = allApis.getAttributes(entityID, sessionToken);
		validateNegativeTestResult(response, "get all Attribute");
		response = allApis.getAttribute(attributeID, sessionToken);
		validateNegativeTestResult(response, "get Attribute");
		response = allApis.updateAttribute(attributeID, attributeObj.toString(), sessionToken);
		validateNegativeTestResult(response, "update Attribute");
		
		//depends on the capability in the server and no in the product
		//response = allApis.getDbSchemas(sessionToken);
		//validateNegativeTestResult(response, "getDbSchemas");
		//response = allApis.getDbTablesInSchema("airlock_test", sessionToken);
		//validateNegativeTestResult(response, "getDbTablesInSchema");
		
		int responseCode = allApis.deleteEntity(entityID, sessionToken);
		if (responseCode == 200)
			results.add("delete entity");	
		
		responseCode = allApis.deleteAttributeType(attributeTypeID, sessionToken);
		if (responseCode == 200)
			results.add("delete attribute type");
		
		responseCode = allApis.deleteAttribute(attributeID, sessionToken);
		if (responseCode == 200)
			results.add("delete attribute");
		
		if (results.size() > 0)
			Assert.fail("negative string capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set ENTITIES capability")
	public void addEntitiesCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("ENTITIES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addEntitiesCapabilities", description = "test ENTITIES api with capability")
	public void testPositiveCapabilities() throws Exception{
		attributeObj.put("deprecated", true);
		
		String response = allApis.updateAttribute(attributeID, attributeObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update attributes: " + response);
		
		int responseCode = allApis.deleteAttribute(attributeID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete attribute");
		
		responseCode = allApis.deleteAttributeType(attributeTypeID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete attribute type");
		
		responseCode = allApis.deleteEntity(entityID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete entity");
		
		entityID = allApis.addEntity(productID, "/airlytics/entities/entity1.txt", sessionToken);
		Assert.assertFalse(entityID.contains("error"), "Can't create entity: " + entityID);
		
		response = allApis.getProductEntities(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get entities: " + response);
		
		response = allApis.getEntity(entityID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get entity: " + response);
		
		JSONObject entityObj  = new JSONObject(response);
		entityObj.remove("attributes");
		entityObj.remove("attributeTypes");
		response = allApis.updateEntity(entityID, entityObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update entity: " + response);
		
		attributeTypeID = allApis.addAttributeType(entityID, "/airlytics/entities/attributeType1.txt", sessionToken);
		Assert.assertFalse(attributeTypeID.contains("error"), "Can't create attributetype: " + attributeTypeID);
		
		response = allApis.getAttributeTypes(entityID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attributeTypes: " + response);
		
		response = allApis.getAttributeType(attributeTypeID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attributeType: " + response);
		
		JSONObject atObj  = new JSONObject(response);
		response = allApis.updateAttributeType(attributeTypeID, atObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update attributeType: " + response);
		
		attributeID = allApis.addAttribute(entityID, "/airlytics/entities/attribute1.txt", attributeTypeID, sessionToken);
		Assert.assertFalse(attributeID.contains("error"), "Can't create attribute: " + attributeID);
		
		response = allApis.getAttributes(entityID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attributes: " + response);
		
		response = allApis.getAttribute(attributeID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get attribute: " + response);
		
		JSONObject aObj  = new JSONObject(response);
		aObj.put("deprecated", true);
		response = allApis.updateAttribute(attributeID, aObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update attributes: " + response);
		
		//clean resources - delete DB and Athena columns
		responseCode = allApis.deleteAttribute(attributeID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Can't delete attribute");
	}
	
	@AfterTest(alwaysRun = true)
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
	private void validateNegativeTestResult(String response, String error) {
		if (!response.contains("error") || !response.contains("capability") || !response.contains("ENTITIES") )
			results.add(error);
	}


}
