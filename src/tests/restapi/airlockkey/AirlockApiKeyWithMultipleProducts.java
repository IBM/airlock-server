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
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class AirlockApiKeyWithMultipleProducts {
	
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
	private String productID1;
	private String seasonID1;
	private String productID2;
	private String seasonID2;
	private String productID3;
	private String seasonID3;
	
	private JSONArray plRoles;
	private JSONArray adminRoles;
	private JSONArray editorRoles;
	private JSONArray viewerRoles;
	
	private String m_configPath;
	private String keySessionToken;
	
	private int seasonCounter;
	
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
		
		productID1 = baseUtils.createProduct();
		productID2 = baseUtils.createProduct();
		productID3 = baseUtils.createProduct();

		plRoles = new JSONArray();
		plRoles.add("Editor");
		plRoles.add("Viewer");
		plRoles.add("ProductLead");
		
		adminRoles = new JSONArray();
		adminRoles.add("Editor");
		adminRoles.add("Viewer");
		adminRoles.add("ProductLead");
		adminRoles.add("Administrator");

		editorRoles = new JSONArray();
		editorRoles.add("Editor");
		editorRoles.add("Viewer");
		
		viewerRoles = new JSONArray();
		viewerRoles.add("Viewer");
				
	}
		
	@Test (description="create key with diffrent roles to different products")
	public void createApiKey() throws Exception{
		JSONObject json = populateKeyName();
		
		json.put("roles", plRoles);
		
		JSONArray productsArray = new JSONArray();
		JSONObject prod1Obj = new JSONObject();
		prod1Obj.put("productId", productID1);
		prod1Obj.put("roles", adminRoles);
		JSONObject prod2Obj = new JSONObject();
		prod2Obj.put("productId", productID2);
		prod2Obj.put("roles", editorRoles);
		JSONObject prod3Obj = new JSONObject();
		prod3Obj.put("productId", productID3);
		prod3Obj.put("roles", viewerRoles);
		
		productsArray.add(prod1Obj);
		productsArray.add(prod2Obj);
		productsArray.add(prod1Obj);
		
		json.put("products", productsArray);
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), adminToken);
		Assert.assertTrue(completeResponse.contains("error"), "can generate key with duplicate products. ");

		productsArray.clear();
		productsArray.add(prod1Obj);
		productsArray.add(prod2Obj);
		productsArray.add(prod3Obj);
		
		json.put("products", productsArray);
		completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), adminToken);
		Assert.assertFalse(completeResponse.contains("error"), "can't generate key with several products. ");

		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		Assert.assertTrue(keyJson.getJSONArray("products").size() == 3, "wrong number of products in key");
	}
	
	@Test (dependsOnMethods="createApiKey",description="test proper permissions in different products using the key")
	public void testPermissionsInProducts() throws Exception{
		keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

		testProductsRolesInKey(keySessionToken, productID1, productID2, productID3);
	}

	@Test (dependsOnMethods="testPermissionsInProducts",description="test proper permissions in different products using the key after extend sessions")
	public void testPermissionsInProductsAfterExtendSession() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/authentication/extend", keySessionToken);
		Assert.assertTrue(res.code==200, "Failed extending sessions");
		String extendedToken = res.message;
			
		testProductsRolesInKey(extendedToken, productID1, productID2, productID3);
	}
	
	@Test (dependsOnMethods="testPermissionsInProductsAfterExtendSession",description="update products roles in api key")
	public void updateApiKey() throws Exception{
			
		String response = operApi.getKey(keyID, adminToken);
	 	Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
	 		
	 	JSONObject keyObj = new JSONObject(response);
	 	JSONArray productsArray = new JSONArray();
		JSONObject prod1Obj = new JSONObject();
		prod1Obj.put("productId", productID3);
		prod1Obj.put("roles", adminRoles);
		JSONObject prod2Obj = new JSONObject();
		prod2Obj.put("productId", productID1);
		prod2Obj.put("roles", editorRoles);
		JSONObject prod3Obj = new JSONObject();
		prod3Obj.put("productId", productID2);
		prod3Obj.put("roles", viewerRoles);
		
		productsArray.add(prod1Obj);
		productsArray.add(prod2Obj);
		productsArray.add(prod1Obj);
		
		keyObj.put("products", productsArray);
		String completeResponse = operApi.updateKey(keyID, keyObj.toString(), adminToken);
		Assert.assertTrue(completeResponse.contains("error"), "can update key with duplicate products. ");

		productsArray.clear();
		productsArray.add(prod1Obj);
		productsArray.add(prod2Obj);
		productsArray.add(prod3Obj);
		
		keyObj.put("products", productsArray);
		completeResponse = operApi.updateKey(keyID, keyObj.toString(), adminToken);
		Assert.assertFalse(completeResponse.contains("error"), "can't update key with several products. ");

		testProductsRolesInKey(keySessionToken, productID3, productID1, productID2);
	}

	@Test (dependsOnMethods="updateApiKey",description="test proper permissions in different products using the key after extend sessions")
	public void testPermissionsInProductsAfterExtendSession2() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url+"/authentication/extend", keySessionToken);
		Assert.assertTrue(res.code==200, "Failed extending sessions");
		String extendedToken = res.message;
			
		testProductsRolesInKey(extendedToken, productID3, productID1, productID2);
	}
	
	@Test (dependsOnMethods="testPermissionsInProductsAfterExtendSession2",description="test proper permissions in different products new jwt from key")
	public void testPermissionsInProductsAfterJwtCreation() throws Exception{
		keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);
	
		testProductsRolesInKey(keySessionToken, productID3, productID1, productID2);
	}
	
	private void testProductsRolesInKey(String token, String adminProdId, String editorProdId, String viewerProdId) throws Exception{
		String seasonId = createSeason(adminProdId, token);
		Assert.assertFalse(seasonId.contains("error"), "create season with proper permissions");
		seasonCounter++;
		
		seasonId = createSeason(editorProdId, token);
		Assert.assertTrue(seasonId.contains("error"), "create season with proper permissions");
		String editorSeasonId = createSeason(editorProdId, adminToken);
		Assert.assertFalse(editorSeasonId.contains("error"), "cannotcreate season with admin role");
		seasonCounter++;
		
		String feature = FileUtils.fileToString(m_configPath + "feature1.txt", "UTF-8", false);
		feature = JSONUtils.generateUniqueString(feature, 8, "name");
		String featureID = f.addFeature(editorSeasonId, feature, "ROOT", token);
		Assert.assertFalse(featureID.contains("error"), "cannot create feature with editor role");
		seasonCounter++;
		
		seasonId = createSeason(viewerProdId, token);
		Assert.assertTrue(seasonId.contains("error"), "create product with proper permissions");
		String viewerSeasonId = createSeason(viewerProdId, adminToken);
		Assert.assertFalse(viewerSeasonId.contains("error"), "create season with admin role");
		seasonCounter++;
		
		featureID = f.addFeature(viewerSeasonId, feature, "ROOT", token);
		Assert.assertTrue(featureID.contains("error"), "can create feature with viewer role");
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
		seasonObj.put("minVersion", seasonCounter);
		String seasonID = s.addSeason(productId, seasonObj.toString(), token);
		return seasonID;
	}


	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID1, adminToken);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
