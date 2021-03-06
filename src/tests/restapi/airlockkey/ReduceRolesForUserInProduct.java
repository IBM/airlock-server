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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;


public class ReduceRolesForUserInProduct {
	
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
	private String productID2;

	
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

	}
	
	/*
	 * Can't delete key using jwt created from this key. 
	 * Only admin or owner using regular sessionToken can delete key
	 */
	
	@Test (description="create key with editor permissions")
	public void createEditorKey() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		json.put("roles", roles);
		
		String editorToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		JSONObject keyObj = new JSONObject(response);
		Assert.assertTrue(keyObj.getJSONArray("products").size() == 0, "wrong number of products in key");
		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
	}
	
	
	@Test (dependsOnMethods="createEditorKey", description="create product using key")
	public void createProductUsingEditorKey() throws Exception{
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

		 String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
         product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
         product = JSONUtils.generateUniqueString(product, 8, "name");
         productID1 = productApi.addProduct(product, keySessionToken);
         baseUtils.printProductToFile(productID1);
         Assert.assertNotNull(productID1);
         Assert.assertFalse(productID1.contains("error"), "Product was not created: " + productID1);

         String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 4, "wrong number of roles in product");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID1), "wrong  product id");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key"); 
 		
 		response = operApi.getUserRolesPerProduct(adminToken, productID1, editorUser);
 		Assert.assertFalse(response.contains("error"), "Failed to user roles in product: " + response);
 		
 		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
 		Assert.assertTrue(userRoles.size() == 4, "wrong key creator roles in product");
	}
	
	@Test (dependsOnMethods="createProductUsingEditorKey", description="redicu user roles in product")
	public void reduceUserRolesInProduct() throws Exception{
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

 		operApi.removeUserRoleFromProduct("Administrator", editorUser, keySessionToken, productID1);
 		
 		String response = operApi.getKey(keyID, adminToken);
  		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
  		
  		JSONObject keyObj = new JSONObject(response);
  		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
  		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 3, "wrong number of roles in product");
  		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID1), "wrong  product id");
  		
  		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key"); 
  		
  		//delete the product - impossible since admin role was removed from key
 		int code = productApi.deleteProduct(productID1, keySessionToken);
 		Assert.assertTrue(code != 200, "key can delete the product after admin role was removed");	
	}
	
	@Test (dependsOnMethods="reduceUserRolesInProduct", description="create key with product lead permissions")
	public void createProductLeadKey() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		roles.add("ProductLead");
		json.put("roles", roles);
		
		String prodLeadToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), prodLeadToken);
		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		JSONObject keyObj = new JSONObject(response);
		Assert.assertTrue(keyObj.getJSONArray("products").size() == 0, "wrong number of products in key");
		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 3, "wrong number of roles in key");
	}
	
	@Test (dependsOnMethods="createProductLeadKey", description="create product using product lead key")
	public void createProductUsingProdLeadKey() throws Exception{
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

		 String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
         product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
         product = JSONUtils.generateUniqueString(product, 8, "name");
         productID2 = productApi.addProduct(product, keySessionToken);
         baseUtils.printProductToFile(productID2);
         Assert.assertNotNull(productID2);
         Assert.assertFalse(productID2.contains("error"), "Product was not created: " + productID2);

         String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 4, "wrong number of roles in product");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID2), "wrong  product id");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 3, "wrong number of roles in key"); 
 		
 		response = operApi.getUserRolesPerProduct(adminToken, productID2, productLeadUser);
 		Assert.assertFalse(response.contains("error"), "Failed to user roles in product: " + response);
 		
 		JSONArray userRoles = new JSONObject(response).getJSONArray("roles");
 		Assert.assertTrue(userRoles.size() == 7, "wrong key creator roles in product");
	}
	
	@Test (dependsOnMethods="createProductUsingProdLeadKey", description="redicu user roles in product")
	public void reduceUserRolesInProduct2() throws Exception{
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);

 		operApi.removeUserRoleFromProduct("Administrator", productLeadUser, keySessionToken, productID2);
 		
 		String response = operApi.getKey(keyID, adminToken);
  		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
  		
  		JSONObject keyObj = new JSONObject(response);
  		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
  		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 3, "wrong number of roles in product");
  		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID2), "wrong  product id");
  		
  		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 3, "wrong number of roles in key"); 
  		
  		//delete the product - impossible since admin role was removed from key
 		int code = productApi.deleteProduct(productID2, keySessionToken);
 		Assert.assertTrue(code != 200, "key can delete the product after admin role was removed");	
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID1, adminToken);
		baseUtils.reset(productID2, adminToken);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
