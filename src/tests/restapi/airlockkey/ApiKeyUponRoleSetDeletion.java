package tests.restapi.airlockkey;

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


public class ApiKeyUponRoleSetDeletion {
	
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

	private String productID3;
	private String productID4;

	
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

		removeUserFromExistingProducts(editorUser);
		removeUserFromExistingProducts("*@weather.com");
		
		productID1 = baseUtils.createProduct();
		productID2 = baseUtils.createProduct();
		//productID3 = baseUtils.createProduct();
		//productID4 = baseUtils.createProduct();
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
	}
	
	/*
	 * Can't delete key using jwt created from this key. 
	 * Only admin or owner using regular sessionToken can delete key
	 */
	
	@Test (description="create key with editor permissions")
	public void createEditorKey() throws Exception{
		doCreateEditorKey(productID1, productID2);
	}
	
	/*
	@Test (dependsOnMethods="createEditorKey", description="remove global roles from key")
	public void removeGlobalRoleSet() throws Exception{
		operApi.removeUserRoleSet(editorUser, adminToken, null);
		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 2, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID1), "wrong product id1");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(1).getString("productId").equals(productID2), "wrong product id2");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 1, "wrong number of roles in key");
 		
 		operApi.removeUserRoleSet("*@weather.com", adminToken, null);
 		response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 2, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID1), "wrong product id1");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(1).getString("productId").equals(productID2), "wrong product id2");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 0, "wrong number of roles in key");
	}*/
	
	@Test (dependsOnMethods="createEditorKey", description="remove product1 roles from key")
	public void removeProduct1RoleSet() throws Exception{
		operApi.removeUserRoleSet(editorUser, adminToken, productID1);
		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 2, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID1), "wrong product id1");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(1).getString("productId").equals(productID2), "wrong product id2");
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 1, "wrong product roles 1");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(1).getJSONArray("roles").size() == 2, "wrong product roles 2");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
 		
 		operApi.removeUserRoleSet("*@weather.com", adminToken, productID1);
 		response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID2), "wrong product id2");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 2, "wrong product roles 2");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
	}
	
	@Test (dependsOnMethods="removeProduct1RoleSet", description="remove product2 roles from key")
	public void removeProduct2RoleSet() throws Exception{
		operApi.removeUserRoleSet("*@weather.com", adminToken, productID2);
 		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 1, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getString("productId").equals(productID2), "wrong product id2");
 		Assert.assertTrue(keyObj.getJSONArray("products").getJSONObject(0).getJSONArray("roles").size() == 2, "wrong product roles 1");
 		
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
 		
 		operApi.removeUserRoleSet(editorUser, adminToken, productID2);
		
 		response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "key should be deleted when no roles left");
 		
 		keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 0, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
	}
	
	
	@Test (dependsOnMethods="removeProduct2RoleSet", description="remove global roles from key")
	public void removeGlobalRoleSet() throws Exception{
		operApi.removeUserRoleSet(editorUser, adminToken, null);
		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 0, "wrong number of products in key");
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 1, "wrong number of roles in key");
 		
 		operApi.removeUserRoleSet("*@weather.com", adminToken, null);
 		response = operApi.getKey(keyID, adminToken);
 		Assert.assertTrue(response.contains("error"), "key should be deleted when no roles left");
 		
	}
	
	@Test (dependsOnMethods="removeGlobalRoleSet", description="recreate key")
	public void craeteEditorKey2() throws Exception{
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		
		productID3 = baseUtils.createProduct();
		productID4 = baseUtils.createProduct();
		
		doCreateEditorKey(productID3, productID4);
	}
	
	@Test (dependsOnMethods="craeteEditorKey2", description="remove products roles from key")
	public void removeProductsRoles() throws Exception{
		operApi.removeUserRoleSet(editorUser, adminToken, productID3);
		operApi.removeUserRoleSet(editorUser, adminToken, productID4);
		
		operApi.removeUserRoleSet("*@weather.com", adminToken, productID3);
		operApi.removeUserRoleSet("*@weather.com", adminToken, productID4);
		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
 		
 		JSONObject keyObj = new JSONObject(response);
 		
 		Assert.assertTrue(keyObj.getJSONArray("products").size() == 0, "wrong number of products in key");	
 		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
	}
	
	
	@Test (dependsOnMethods="removeProductsRoles", description="remove global roles from key")
	public void removeGlobalRoles() throws Exception{
 		operApi.removeUserRoleSet(editorUser, adminToken, null);
		operApi.removeUserRoleSet("*@weather.com", adminToken, null);
 		
		String response = operApi.getKey(keyID, adminToken);
 		Assert.assertTrue(response.contains("error"), "key should be deleted when no roles left");
	}
	
		
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.reset(productID1, adminToken);
		baseUtils.reset(productID2, adminToken);
		baseUtils.reset(productID3, adminToken);
		baseUtils.reset(productID4, adminToken);
	}
	
	private void doCreateEditorKey(String prodId1, String prodId2) throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		roles.add("Viewer");
		
		json.put("roles", roles);
		JSONArray productsRolesArr = new JSONArray();
		JSONObject prodRolesObj1 = new JSONObject();
		prodRolesObj1.put("productId", prodId1);
		prodRolesObj1.put("roles", roles);
		productsRolesArr.add(prodRolesObj1);
		JSONObject prodRolesObj2 = new JSONObject();
		prodRolesObj2.put("productId", prodId2);
		prodRolesObj2.put("roles", roles);
		productsRolesArr.add(prodRolesObj2);
		
		json.put("products", productsRolesArr);
		
		
		String editorToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), editorToken);
		JSONObject keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Failed to get key: " + response);
		
		JSONObject keyObj = new JSONObject(response);
		Assert.assertTrue(keyObj.getJSONArray("products").size() == 2, "wrong number of products in key");
		Assert.assertTrue(keyObj.getJSONArray("roles").size() == 2, "wrong number of roles in key");
	}
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}
	private void removeUserFromExistingProducts(String user) throws Exception {
		//get all user's role sets
		String response = operApi.getUserRoleSets(adminToken, user);
		Assert.assertFalse(response.contains("error"), "Fail retrieving user role sets: " + response);	
		
		JSONArray userRoleSets = new JSONObject(response).getJSONArray("userRoleSets");
		 
		for (int i=0; i<userRoleSets.size(); i++) {
			JSONObject roleSet = userRoleSets.getJSONObject(i);
			if (roleSet.get("productId")!=null) {
				String roleSetId = roleSet.getString("uniqueId");
				int code = operApi.deleteAirlockUser(roleSetId, adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user roles");
			}
		}
	}

}
