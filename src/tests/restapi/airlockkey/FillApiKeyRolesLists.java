package tests.restapi.airlockkey;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.ibm.airlock.admin.Utilities;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;


public class FillApiKeyRolesLists {
	

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
	private String keyID;
	private String productID1;
	private String productID2;
	private String productID3;
	private String productID4;
	private String productID5;
	private String productID6;
	
	private FeaturesRestApi f;
	private InputSchemaRestApi schema;
	String[] AdminRoles = new String[] {"Administrator", "ProductLead", "Editor", "Viewer"};
	String[] ProductLeadRoles = new String[] {"ProductLead", "Editor", "Viewer"};
	String[] EditorRoles = new String[] {"Editor", "Viewer"};
	String[] AnalyticsEditorRoles = new String[] {"AnalyticsEditor", "Viewer"};
	String[] AnalyticsViewerRoles = new String[] {"AnalyticsViewer", "Viewer"};
	String[] TranslationSpecialistRoles = new String[] {"TranslationSpecialist", "Viewer"};
	String[] ViewerRoles = new String[] {"Viewer"};

	
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
		productID4 = baseUtils.createProduct();
		productID5 = baseUtils.createProduct();
		productID6 = baseUtils.createProduct();
		
	}
	
	
	@Test (description="Create Api Key with roles for 6 products")
	public void createApiKeyForSixProducts() throws Exception{
	
		JSONObject json = populateKeyName();
		JSONArray rolesAdmin = new JSONArray();
		rolesAdmin.add("Administrator");
		json.put("roles", rolesAdmin);
		
		JSONArray productsRolesArr = new JSONArray();
		
		JSONObject prodRolesObj1 = new JSONObject();
		prodRolesObj1.put("productId", productID1);
		JSONArray rolesPL = new JSONArray();
		rolesPL.add("ProductLead");
		prodRolesObj1.put("roles", rolesPL);
		productsRolesArr.add(prodRolesObj1);
		
		JSONObject prodRolesObj2 = new JSONObject();
		prodRolesObj2.put("productId", productID2);
		JSONArray rolesEditor = new JSONArray();
		rolesEditor.add("Editor");
		prodRolesObj2.put("roles", rolesEditor);
		productsRolesArr.add(prodRolesObj2);
		
		JSONObject prodRolesObj3 = new JSONObject();
		prodRolesObj3.put("productId", productID3);
		JSONArray rolesViewer = new JSONArray();
		rolesViewer.add("Viewer");
		prodRolesObj3.put("roles", rolesViewer);
		productsRolesArr.add(prodRolesObj3);
		
		JSONObject prodRolesObj4 = new JSONObject();
		prodRolesObj4.put("productId", productID4);
		JSONArray rolesAnalyticsEditor = new JSONArray();
		rolesAnalyticsEditor.add("AnalyticsEditor");
		prodRolesObj4.put("roles", rolesAnalyticsEditor);
		productsRolesArr.add(prodRolesObj4);
		
		JSONObject prodRolesObj5 = new JSONObject();
		prodRolesObj5.put("productId", productID5);
		JSONArray rolesAnalyticsViewer = new JSONArray();
		rolesAnalyticsViewer.add("AnalyticsViewer");
		prodRolesObj5.put("roles", rolesAnalyticsViewer);
		productsRolesArr.add(prodRolesObj5);
		
		JSONObject prodRolesObj6 = new JSONObject();
		prodRolesObj6.put("productId", productID6);
		JSONArray rolesTranslationSpecialist = new JSONArray();
		rolesTranslationSpecialist.add("TranslationSpecialist");
		prodRolesObj6.put("roles", rolesTranslationSpecialist);
		productsRolesArr.add(prodRolesObj6);
		
		json.put("products", productsRolesArr);
		
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), adminToken);
		Assert.assertFalse(completeResponse.contains("error"), "Can't create key: " + completeResponse);
	
		JSONObject keyJson = new JSONObject(completeResponse);
		
		keyID = keyJson.getString("uniqueId");
		
		String response = operApi.getKey(keyID, adminToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(globalRoles, AdminRoles), "wrong admin global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 6, "wrong number of products");
		
		JSONArray prod1Roles = jsonKey.getJSONArray("products").getJSONObject(0).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod1Roles, ProductLeadRoles), "wrong prod1 roles");
		
		JSONArray prod2Roles = jsonKey.getJSONArray("products").getJSONObject(1).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod2Roles, EditorRoles), "wrong prod2 roles");
		
		JSONArray prod3Roles = jsonKey.getJSONArray("products").getJSONObject(2).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod3Roles, ViewerRoles), "wrong prod3 roles");
		
		JSONArray prod4Roles = jsonKey.getJSONArray("products").getJSONObject(3).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod4Roles, AnalyticsEditorRoles), "wrong prod4 roles");
		
		JSONArray prod5Roles = jsonKey.getJSONArray("products").getJSONObject(4).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod5Roles, AnalyticsViewerRoles), "wrong prod5 roles");
		
		JSONArray prod6Roles = jsonKey.getJSONArray("products").getJSONObject(5).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod6Roles, TranslationSpecialistRoles), "wrong prod6 roles");
	}

	@Test (dependsOnMethods = "createApiKeyForSixProducts", description="update Api Key with roles for 6 products")
	public void updateApiKeyForSixProducts() throws Exception{
		
		String response = operApi.getKey(keyID, adminToken);
		JSONObject jsonKey = new JSONObject(response);
		
		JSONArray rolesAdmin = new JSONArray();
		rolesAdmin.add("Viewer");
		jsonKey.put("roles", rolesAdmin);
		
		JSONArray productsRolesArr = new JSONArray();
		
		JSONObject prodRolesObj1 = new JSONObject();
		prodRolesObj1.put("productId", productID1);
		JSONArray rolesPL = new JSONArray();
		rolesPL.add("Editor");
		prodRolesObj1.put("roles", rolesPL);
		productsRolesArr.add(prodRolesObj1);
		
		JSONObject prodRolesObj2 = new JSONObject();
		prodRolesObj2.put("productId", productID2);
		JSONArray rolesEditor = new JSONArray();
		rolesEditor.add("ProductLead");
		prodRolesObj2.put("roles", rolesEditor);
		productsRolesArr.add(prodRolesObj2);
		
		JSONObject prodRolesObj3 = new JSONObject();
		prodRolesObj3.put("productId", productID3);
		JSONArray rolesViewer = new JSONArray();
		rolesViewer.add("AnalyticsEditor");
		prodRolesObj3.put("roles", rolesViewer);
		productsRolesArr.add(prodRolesObj3);
		
		JSONObject prodRolesObj4 = new JSONObject();
		prodRolesObj4.put("productId", productID4);
		JSONArray rolesAnalyticsEditor = new JSONArray();
		rolesAnalyticsEditor.add("Administrator");
		prodRolesObj4.put("roles", rolesAnalyticsEditor);
		productsRolesArr.add(prodRolesObj4);
		
		JSONObject prodRolesObj5 = new JSONObject();
		prodRolesObj5.put("productId", productID5);
		JSONArray rolesAnalyticsViewer = new JSONArray();
		rolesAnalyticsViewer.add("TranslationSpecialist");
		prodRolesObj5.put("roles", rolesAnalyticsViewer);
		productsRolesArr.add(prodRolesObj5);
		
		JSONObject prodRolesObj6 = new JSONObject();
		prodRolesObj6.put("productId", productID6);
		JSONArray rolesTranslationSpecialist = new JSONArray();
		rolesTranslationSpecialist.add("AnalyticsViewer");
		prodRolesObj6.put("roles", rolesTranslationSpecialist);
		productsRolesArr.add(prodRolesObj6);
		
		jsonKey.put("products", productsRolesArr);
		
		response = operApi.updateKey(keyID, jsonKey.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't update key: " + response);
	
		response = operApi.getKey(keyID, adminToken);
		jsonKey = new JSONObject(response);
		
		JSONArray globalRoles = jsonKey.getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(globalRoles, ViewerRoles), "wrong admin global roles");
		
		JSONArray products = jsonKey.getJSONArray("products");
		Assert.assertTrue(products.size() == 6, "wrong number of products");
		
		JSONArray prod1Roles = jsonKey.getJSONArray("products").getJSONObject(0).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod1Roles, EditorRoles), "wrong prod1 roles");
		
		JSONArray prod2Roles = jsonKey.getJSONArray("products").getJSONObject(1).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod2Roles, ProductLeadRoles), "wrong prod2 roles");
		
		JSONArray prod3Roles = jsonKey.getJSONArray("products").getJSONObject(2).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod3Roles, AnalyticsEditorRoles), "wrong prod3 roles");
		
		JSONArray prod4Roles = jsonKey.getJSONArray("products").getJSONObject(3).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod4Roles, AdminRoles), "wrong prod4 roles");
		
		JSONArray prod5Roles = jsonKey.getJSONArray("products").getJSONObject(4).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod5Roles, TranslationSpecialistRoles), "wrong prod5 roles");
		
		JSONArray prod6Roles = jsonKey.getJSONArray("products").getJSONObject(5).getJSONArray("roles");
		Assert.assertTrue(areRolesListEqual(prod6Roles, AnalyticsViewerRoles), "wrong prod6 roles");
	}

	private boolean areRolesListEqual(JSONArray existingRoles, String[] expectedRoles) throws JSONException {
		if ((existingRoles == null || existingRoles.isEmpty()) && ((expectedRoles == null) || expectedRoles.length == 0))
			return true; //both null or empty

		if (existingRoles == null || expectedRoles == null)
			return false; //only one is null and the other is not empty

		HashSet<String> h1 = new HashSet<String>( Arrays.asList( Utilities.jsonArrToStringArr(existingRoles) ));
		HashSet<String> h2 = new HashSet<String>(Arrays.asList(expectedRoles));
		return h1.equals(h2);
	}
	/*
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
	*/
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID1, adminToken);
		baseUtils.reset(productID2, adminToken);
		baseUtils.reset(productID3, adminToken);
		baseUtils.reset(productID4, adminToken);
		baseUtils.reset(productID5, adminToken);
		baseUtils.reset(productID6, adminToken);
	}
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
