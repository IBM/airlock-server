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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class AirlockKeyUponProductDeletion {
	

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
	protected ProductsRestApi prodApi;
	private SeasonsRestApi s;
	private String KeyId;
	private String productID1;
	private String productID2;
	private String productID3;
	private String productID4;
	private FeaturesRestApi f;
	private String productLeadUser;
	private String productLeadPassword;
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
		
		prodApi = new ProductsRestApi();
		prodApi.setURL(url);
		

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		baseUtils.deleteKeys(null); //delete previous keys since some tests are counting keys so need to start clean
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		
		productID1 = baseUtils.createProduct();
		productID2 = baseUtils.createProduct();
		productID3 = baseUtils.createProduct();
		
	}
	
	@Test (description="Create Api Key with roles for 3 products")
	public void createApiKeyForThreeProducts() throws Exception{
		String sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("ProductLead");
		roles.add("Editor");
		roles.add("Viewer");
		json.put("roles", roles);
		
		JSONArray productsRolesArr = new JSONArray();
		JSONObject prodRolesObj = new JSONObject();
		prodRolesObj.put("productId", productID1);
		prodRolesObj.put("roles", roles);
		productsRolesArr.add(prodRolesObj);
		prodRolesObj = new JSONObject();
		prodRolesObj.put("productId", productID2);
		prodRolesObj.put("roles", roles);
		productsRolesArr.add(prodRolesObj);
		prodRolesObj = new JSONObject();
		
		prodRolesObj.put("productId", productID3);
		JSONArray roles2 = new JSONArray();
		roles2.add("Editor");
		roles2.add("Viewer");	
		prodRolesObj.put("roles", roles2);
		productsRolesArr.add(prodRolesObj);
		json.put("products", productsRolesArr);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(completeResponse.contains("error"), "Can't create key: " + completeResponse);
	
		JSONObject keyJson = new JSONObject(completeResponse);
		
		KeyId = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(KeyId, sessionToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 3, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 3, "wrong number of products");
		
		Assert.assertTrue(products.getJSONObject(0).getString("productId").equals(productID1), "wrong id of first product id");
		Assert.assertTrue(products.getJSONObject(0).getJSONArray("roles").size() == 3, "wrong number of first product roles");
		
		Assert.assertTrue(products.getJSONObject(1).getString("productId").equals(productID2), "wrong id of second product id");
		Assert.assertTrue(products.getJSONObject(1).getJSONArray("roles").size() == 3, "wrong number of second product roles");
		
		Assert.assertTrue(products.getJSONObject(2).getString("productId").equals(productID3), "wrong id of third product id");
		Assert.assertTrue(products.getJSONObject(2).getJSONArray("roles").size() == 2, "wrong number of third product roles");
		
	}
	
	
	@Test (dependsOnMethods="createApiKeyForThreeProducts", description="remove second product")
	public void removeFirstProduct() throws Exception{
		prodApi.deleteProduct(productID1, adminToken);
		
		String response = operApi.getKey(KeyId, adminToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 3, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 2, "wrong number of products");
		
		Assert.assertTrue(products.getJSONObject(0).getString("productId").equals(productID2), "wrong id of second product id");
		Assert.assertTrue(products.getJSONObject(0).getJSONArray("roles").size() == 3, "wrong number of second product roles");
		
		Assert.assertTrue(products.getJSONObject(1).getString("productId").equals(productID3), "wrong id of third product id");
		Assert.assertTrue(products.getJSONObject(1).getJSONArray("roles").size() == 2, "wrong number of third product roles");
		
	}
	
	@Test (dependsOnMethods="removeFirstProduct", description="remove second product")
	public void removeSecondProduct() throws Exception{
		prodApi.deleteProduct(productID2, adminToken);
		
		String response = operApi.getKey(KeyId, adminToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 3, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 1, "wrong number of products");
		
		Assert.assertTrue(products.getJSONObject(0).getString("productId").equals(productID3), "wrong id of third product id");
		Assert.assertTrue(products.getJSONObject(0).getJSONArray("roles").size() == 2, "wrong number of third product roles");
		
	}
	
	@Test (dependsOnMethods="removeSecondProduct", description="remove third product")
	public void removeThirdProduct() throws Exception{
		prodApi.deleteProduct(productID3, adminToken);
		
		String response = operApi.getKey(KeyId, adminToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 3, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 0, "wrong number of products");	
	}
	

	@Test (dependsOnMethods="removeThirdProduct", description="Add product to key ")
	public void addProductToKey() throws Exception{
		productID4 = baseUtils.createProduct();
		
		String sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		String response = operApi.getKey(KeyId, sessionToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray productsRolesArr = new JSONArray();
		JSONObject prodRolesObj = new JSONObject();
		
		prodRolesObj.put("productId", productID4);
		JSONArray roles2 = new JSONArray();
		roles2.add("Editor");
		roles2.add("Viewer");	
		prodRolesObj.put("roles", roles2);
		productsRolesArr.add(prodRolesObj);
		jsonKey.put("products", productsRolesArr);
		
		response = operApi.updateKey(KeyId, jsonKey.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update key: " + response);
		
		response = operApi.getKey(KeyId, sessionToken);
		jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 3, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 1, "wrong number of products");
		
		Assert.assertTrue(products.getJSONObject(0).getString("productId").equals(productID4), "wrong id of first product id");
		Assert.assertTrue(products.getJSONObject(0).getJSONArray("roles").size() == 2, "wrong number of first product roles");
	}
	
	@Test (dependsOnMethods="addProductToKey", description="remove global roles")
	public void removeGlobalRoles() throws Exception{
		String sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		String response = operApi.getKey(KeyId, sessionToken);
		JSONObject jsonKey = new JSONObject(response);
		
		jsonKey.put("roles", new JSONArray());
		
		response = operApi.updateKey(KeyId, jsonKey.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update key: " + response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(globalRoles.size() == 0, "wrong number of global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 1, "wrong number of products");
		
		Assert.assertTrue(products.getJSONObject(0).getString("productId").equals(productID4), "wrong id of first product id");
		Assert.assertTrue(products.getJSONObject(0).getJSONArray("roles").size() == 2, "wrong number of first product roles");
	}
	
	@Test (dependsOnMethods="removeGlobalRoles", description="remove forth product")
	public void removeForthProduct() throws Exception{
		prodApi.deleteProduct(productID4, adminToken);
		
		//the should be deleted since no product and no roles exists
		String response = operApi.getKey(KeyId, adminToken); 
		Assert.assertTrue(response.contains("error"), "Can't update key: " + response);
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID1, adminToken);
		baseUtils.reset(productID2, adminToken);
		baseUtils.reset(productID3, adminToken);
	}
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
