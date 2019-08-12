package tests.restapi.airlockkey;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.SeasonsRestApi;


public class AirlockKeyBVT {
	

	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	protected String adminUser;
	protected String adminPassword;
	protected String m_appName;
	protected String config;
	protected AirlockUtils baseUtils; 
	protected String apikey;
	protected OperationRestApi operApi;
	private SeasonsRestApi s;
	private String  editorKeyID;
	private String productLeadKeyID;
	private String seasonID;
	private String productID;
	private FeaturesRestApi f;
	private String editorUser;
	private String editorPassword;
	private String productLeadUser;
	private String productLeadPassword;
	private String editorSessionJwt;
	private String productLeadSessionJwt;
	private InputSchemaRestApi schema;
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPass","appName","editor","editorPass","productLead", "productLeadPass","productsToDeleteFile"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName, String editor, String editorPass, String productLead, String productLeadPass, String productsToDeleteFile) throws Exception{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		editorUser = editor;
		editorPassword = editorPass;
		productLeadUser = productLead;
		productLeadPassword = productLeadPass;

		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);

        m_appName = appName;
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		
		

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		baseUtils.deleteKeys(null); //delete previous keys since some tests are counting keys so need to start clean
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		
		productID = baseUtils.createProduct();
		seasonID = baseUtils.createSeason(productID);
	}
	
	/* create key for product lead and add season
	 * create key for editor and add feature
	 * use editor key to move feature to production - fail
	 * update productlead key and remove editor role
	 * using productlead key add feature - fail (no editor permissions)
	 * getKeyByUser 
	 * getAllKeys
	 * delete all keys
	 */
	

	@Test (description="Create productLead jwt from api key")
	public void createProductLeadSessionToken() throws Exception{
		String sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("ProductLead");
		roles.add("Editor");
		roles.add("Viewer");
		setGlobalAndProductRoles(json, roles, productID);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(completeResponse.contains("error"), "Can't create key: " + completeResponse);
		
		JSONObject keyJson = new JSONObject(completeResponse);
		
		String key= keyJson.getString("key");
		String keyPassword = keyJson.getString("keyPassword");
		productLeadKeyID = keyJson.getString("uniqueId");
		
		productLeadSessionJwt = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(productLeadSessionJwt.contains("error"), "Can't start session with generated key");

		String sch = schema.getInputSchema(seasonID, productLeadSessionJwt);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(config + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), productLeadSessionJwt);
        Assert.assertFalse(response.contains("error"), "product lead can't update input schema: " + response);

	}
	
	private void setGlobalAndProductRoles(JSONObject apiKeyJson, JSONArray roles, String productID) throws JSONException{
		apiKeyJson.put("roles", roles);
		JSONArray productsRolesArr = new JSONArray();
		JSONObject prodRolesObj = new JSONObject();
		prodRolesObj.put("productId", productID);
		prodRolesObj.put("roles", roles);
		productsRolesArr.add(prodRolesObj);
		apiKeyJson.put("products", productsRolesArr);
	}

	@Test (dependsOnMethods="createProductLeadSessionToken", description="Create editor jwt from api key")
	public void editorSessionToken() throws Exception{
		String sessionToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		setGlobalAndProductRoles(json, roles, productID);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(completeResponse.contains("error"), "Can't create key: " + completeResponse);
		
		JSONObject keyJson = new JSONObject(completeResponse);
		
		String key= keyJson.getString("key");
		String keyPassword = keyJson.getString("keyPassword");
		editorKeyID = keyJson.getString("uniqueId");
		
		editorSessionJwt = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(editorSessionJwt.contains("error"), "Can't start session with generated key");
		
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonF.toString(), "ROOT", editorSessionJwt);
		Assert.assertFalse(response.contains("error") || response.contains("SecurityPolicyException"), "Editor can't add feature: " + response);
	}
	
	
	@SuppressWarnings("rawtypes")
	@Test (dependsOnMethods="editorSessionToken", description="Validate product lead user in roles")
	public void validateRolesPerUser() throws Exception{
		String response = operApi.getRolesPerUser(productLeadUser, productLeadSessionJwt);
		JSONArray rolesPerUser = new JSONObject(response).getJSONArray("roles");
		ArrayList possibleRoles = new ArrayList<>(Arrays.asList("AnalyticsEditor", "AnalyticsViewer", "ProductLead", "Editor", "TranslationSpecialist", "Viewer"));
		Assert.assertEqualsNoOrder(rolesPerUser.toArray(), possibleRoles.toArray(), "A list of roles is different");
	}
	
	@Test (dependsOnMethods="validateRolesPerUser", description="Use editor jwt to move feature to PROD")
	public void editorSessionTokenInvalidPermission() throws Exception{
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonF.toString(), "ROOT", editorSessionJwt);
		Assert.assertTrue(response.contains("error") || response.contains("SecurityPolicyException"), "Editor created feature in production: " + response);

		
	}
	
	@Test (dependsOnMethods="editorSessionTokenInvalidPermission", description="Update productLead key")
	public void updateProductLeadKey() throws Exception{
		String response = operApi.getKey(productLeadKeyID, productLeadSessionJwt);
		JSONObject jsonKey = new JSONObject(response);
		jsonKey.getJSONArray("roles").remove("Editor");
		jsonKey.getJSONArray("roles").remove("ProductLead");
		jsonKey.getJSONArray("products").getJSONObject(0).getJSONArray("roles").remove("Editor");
		jsonKey.getJSONArray("products").getJSONObject(0).getJSONArray("roles").remove("ProductLead");
		
		response = operApi.updateKey(productLeadKeyID, jsonKey.toString(), productLeadSessionJwt);
		Assert.assertFalse(response.contains("error"), "Failed to update productLead key: " + response);
		
		//test editor role
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		response = f.addFeature(seasonID, jsonF.toString(), "ROOT", productLeadSessionJwt);
		Assert.assertTrue(response.contains("error") || response.contains("SecurityPolicyException"), "Used Editor action without Editor role in key: " + response);

	}
	
	@Test (dependsOnMethods="updateProductLeadKey", description="Get key by user")
	public void getProductLeadKey() throws Exception{
		String response = operApi.getAllKeys("user3@weather.com", productLeadSessionJwt);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("airlockAPIKeys").size()==1, "Incorrect number of api keys");
		
		//check case independence
		response = operApi.getAllKeys("USER3@weather.com", productLeadSessionJwt);
		json = new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("airlockAPIKeys").size()==1, "Incorrect number of api keys");
	}
	
	@Test (dependsOnMethods="getProductLeadKey", description="Get all keys")
	public void getAllKeys() throws Exception{
		String response = operApi.getAllKeys(null, editorSessionJwt);
		JSONObject json = new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("airlockAPIKeys").size()==2, "Incorrect number of api keys");
	}
	
	@Test (dependsOnMethods="getAllKeys", description="Delete all keys")
	public void deleteAllKeys() throws Exception{
		String allKeys = operApi.getAllKeys(null, adminToken);
		JSONArray airlockAPIKeys = new JSONObject(allKeys).getJSONArray("airlockAPIKeys");
		int response;
		for (int i=0; i<airlockAPIKeys.size(); i++ ){
			JSONObject apikey = airlockAPIKeys.getJSONObject(i);
			response = operApi.deleteKey(apikey.getString("uniqueId"), adminToken);
			Assert.assertTrue(response ==200, "Can't delete key: " + apikey.getString("key") + " id: " + apikey.getString("uniqueId"));
		}
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.deleteKeys(null);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
