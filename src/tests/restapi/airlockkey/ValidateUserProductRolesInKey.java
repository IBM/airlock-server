package tests.restapi.airlockkey;

import java.io.IOException;






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
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class ValidateUserProductRolesInKey {
	
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
	private FeaturesRestApi f;
	private ProductsRestApi productApi;   
	private String productLeadUser;
	private String productLeadPassword;
	private String editorUser;
	private String editorPassword;
	private String key;
	private String keyID;
	private String keyPassword;
	private String productID;
	private String m_configPath;

	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPass","appName","editor","editorPass","productLead", "productLeadPass","productsToDeleteFile"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName, String editor, String editorPass, String productLead, String productLeadPass, String productsToDeleteFile) throws IOException{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		productLeadUser = productLead;
		editorUser = editor;
		editorPassword = editorPass;
		productLeadPassword = productLeadPass;
		m_configPath = configPath;
		if(appName != null) {
			m_appName = appName;
		}

		productApi = new ProductsRestApi();
		productApi.setURL(m_url);
        
		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		
		f = new FeaturesRestApi();
		f.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		
		productID = baseUtils.createProduct();
		String seasonID = baseUtils.createSeason(productID);

	}
	
	@Test (description="create key with global prodLead permissions by editor user")
	public void createGlobalProdLeadKey() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		roles.add("ProductLead");
		json.put("roles", roles);
		
		String editorToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		Assert.assertTrue(completeResponse.contains("error"), "can generate key with higer permissions. ");

		roles.remove("ProductLead");
		
		completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		Assert.assertFalse(completeResponse.contains("error"), "can't generate key with proper permissions. ");
		
		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		JSONObject keyObj = new JSONObject(response);
		roles.add("Administrator");
		keyObj.put("roles", roles);
		response = operApi.updateKey(keyID, keyObj.toString(), editorToken);
		Assert.assertTrue(response.contains("error"), "can update key with higer permissions. ");

		
		roles.remove("Administrator");
		keyObj.put("roles", roles);
		response = operApi.updateKey(keyID, keyObj.toString(), editorToken);
		Assert.assertFalse(response.contains("error"), "can't update key with proper permissions. ");

	}

	@Test (dependsOnMethods="createGlobalProdLeadKey",description="create key with product prodLead permissions by editor user")
	public void createProductProdLeadKey() throws Exception{
		JSONObject json = populateKeyName();
		
		JSONObject prodRoles = new JSONObject();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		roles.add("ProductLead");
		prodRoles.put("roles", roles);
		prodRoles.put("productId", productID);
		
		JSONArray products = new JSONArray();
		products.add(prodRoles);
		
		json.put("products", products);
		String editorToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		Assert.assertTrue(completeResponse.contains("error"), "can generate key with higer permissions. ");

		roles.remove("ProductLead");
		
		completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		Assert.assertFalse(completeResponse.contains("error"), "can't generate key with proper permissions. ");
		
		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		JSONObject keyObj = new JSONObject(response);
		JSONArray productsArr = keyObj.getJSONArray("products");
		roles.add("ProductLead");
		productsArr.getJSONObject(0).put("roles", roles);
		response = operApi.updateKey(keyID, keyObj.toString(), editorToken);
		Assert.assertTrue(response.contains("error"), "can update key with higer permissions. ");

		
		roles.remove("ProductLead");
		keyObj.put("roles", roles);
		response = operApi.updateKey(keyID, keyObj.toString(), editorToken);
		Assert.assertFalse(response.contains("error"), "can't update key with proper permissions. ");
		
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

		//add product lead role to user in product and update again
		operApi.addUserProductRole("ProductLead", editorUser, adminToken, productID);

		response = createSeason(productID, keySessionToken); 
		Assert.assertTrue(response.contains("error"), "can to create season from key: " + keySessionToken);

		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		keyObj = new JSONObject(response);
		productsArr = keyObj.getJSONArray("products");
		roles.add("ProductLead");
		productsArr.getJSONObject(0).put("roles", roles);
		response = operApi.updateKey(keyID, keyObj.toString(), editorToken);
		Assert.assertFalse(response.contains("error"), "can update key with proper permissions. ");
		
		String seasonID = createSeason(productID, keySessionToken); 
		Assert.assertFalse(response.contains("error"), "Failed to create season from key: " + keySessionToken);

		//remove prod lead from user
		operApi.removeUserRoleFromProduct("ProductLead", editorUser, adminToken, productID);

		int code = deleteSeason(seasonID, keySessionToken); 
		Assert.assertTrue(code!=200, "can to delete season from key without permmisions: " + keySessionToken);
	}

	public int deleteSeason(String seasonID, String token) throws Exception{
		SeasonsRestApi s = new SeasonsRestApi();
		s.setURL(m_url);
		
		return s.deleteSeason(seasonID, token);
	}
	
	public String createSeason(String productId, String token) throws Exception{
		SeasonsRestApi s = new SeasonsRestApi();
		s.setURL(m_url);		
		String season = FileUtils.fileToString(m_configPath + "season1.txt", "UTF-8", false);
		JSONObject seasonObj = new JSONObject(season);
		seasonObj.put("minVersion", "10.0");
		String seasonID = s.addSeason(productId, seasonObj.toString(), token);
		return seasonID;
	}

	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID, adminToken);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
