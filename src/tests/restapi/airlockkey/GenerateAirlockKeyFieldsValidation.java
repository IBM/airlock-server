package tests.restapi.airlockkey;

import java.io.IOException;
import java.util.UUID;

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
import tests.restapi.OperationRestApi;


public class GenerateAirlockKeyFieldsValidation {
	
	protected String sessionToken;
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
	protected String productID;
    
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPass","appName","productsToDeleteFile"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName,String productsToDeleteFile) throws IOException{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;

		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		baseUtils.sessionToken = adminToken;

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(configPath + "airlockkey/key_template1.txt", "UTF-8", false);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationUrl, configPath, "", admin, adminPass, appName, productsToDeleteFile);
		
		productID = baseUtils.createProduct();

	}
	
	/*
		private UUID uniqueId;
	private LinkedList<RoleType> roles = new LinkedList<RoleType>();//c+u
		private String owner; //nc+u (not changed)
		
		private Date creationDate; 
		private String password; //nc+nu
		private String key; //c+u (not changed)
		protected Date lastModified = null; // nc + u
	 */
	
	@Test
	public void missingKeyField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.remove("key");
		generateKey(json.toString(), "key", true);		
	}
	
	@Test
	public void emptyKeyField() throws JSONException, IOException{
		JSONObject json = populateKeyName();		
		json.put("key", "");
		generateKey(json.toString(), "key", true);
	}
	
	@Test
	public void nullKeyField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("key", JSONObject.NULL);
		generateKey(json.toString(), "key", true);
				
	}
	
	@Test
	public void emptyPasswordField() throws JSONException, IOException{
 		JSONObject json = populateKeyName();
		json.put("keyPassword", "");
		generateKey(json.toString(), "password", true);
	}
	
	@Test
	public void nullPasswordField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("keyPassword", JSONObject.NULL);
		generateKey(json.toString(), "password", false);
	}
	
	@Test
	public void nonEmptyPasswordField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("keyPassword", "password1");
		generateKey(json.toString(), "password", true);
	
	}
	
	@Test
	public void emptyOwnerField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("owner", "");
		generateKey(json.toString(), "owner", true);
	}
	
	@Test
	public void nullOwnerField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("owner", JSONObject.NULL);
		generateKey(json.toString(), "owner", false);
	}
	
	@Test
	public void nonEmptyOwnerField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("owner", "owner1");
		generateKey(json.toString(), "owner", true);
	
	}
	
	@Test
	public void emptyLastModifiedField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("lastModified", "");
		generateKey(json.toString(), "lastModified", true);
	}
	
	@Test
	public void nullLastModifiedField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("lastModified", JSONObject.NULL);
		generateKey(json.toString(), "lastModified", false);
	}
	
	@Test
	public void nonEmptyLastModifiedField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("lastModified", System.currentTimeMillis());
		generateKey(json.toString(), "lastModified", true);
	
	}
	
	@Test
	public void emptyCreationDateField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("creationDate", "");
		generateKey(json.toString(), "creationDate", true);
	}
	
	@Test
	public void nullCreationDateField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("creationDate", JSONObject.NULL);
		generateKey(json.toString(), "creationDate", false);
	}
	
	@Test
	public void nonEmptyCreationDateField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("creationDate", System.currentTimeMillis());
		generateKey(json.toString(), "creationDate", true);
	
	}
	
	@Test
	public void emptyUniqueIdField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("uniqueId", "");
		generateKey(json.toString(), "uniqueId", true);

	}
	
	@Test
	public void nullUniqueIdField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("uniqueId", JSONObject.NULL);
		generateKey(json.toString(), "uniqueId", false);
	}
	
	@Test
	public void nonEmptyUniqueIdField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("uniqueId", UUID.randomUUID().toString());
		generateKey(json.toString(), "uniqueId", true);
	
	}
	
	@Test
	public void missingRolesField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.remove("roles");
		generateKey(json.toString(), "roles", true);
	}
	@Test
	public void emptyRolesField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("roles", "");
		generateKey(json.toString(), "roles", true);
	}
	@Test
	public void nullRolesField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("roles", JSONObject.NULL);
		generateKey(json.toString(), "roles", true);
	}
	
	@Test
	public void emptyArrayRolesField() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("roles", new JSONArray());
		generateKey(json.toString(), "roles", true);
	
	}
	
	@Test
	public void nullProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("products", JSONObject.NULL);
		generateKey(json.toString(), "products", false);
	}
	
	@Test
	public void emptyProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("products", new JSONArray());
		generateKey(json.toString(), "products", false);
	}
	
	@Test
	public void wrongProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		json.put("products", new JSONObject());
		generateKey(json.toString(), "products", true);
	}
	
	@Test
	public void emptyJsonInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "products", true);
	}
	
	@Test
	public void nullProductIdInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", JSONObject.NULL);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "productId", true);
	}
	
	@Test
	public void wrongRoleInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		JSONArray rolesArr = new JSONArray();
		rolesArr.add("xxx");
		prodRoles.put("roles", rolesArr);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "roles", true);
	}
	
	@Test
	public void emptyProductIdInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", "");
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "productId", true);
	}
	
	@Test
	public void emptyRolesInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", new JSONArray());
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "roles", true);
	}
	
	@Test
	public void nullRolesInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", JSONObject.NULL);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "roles", true);
	}
	
	@Test
	public void missingRolesInProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		generateKey(json.toString(), "roles", true);
	}
	
	@Test
	public void bothGlobalAndProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		//json.put("roles", new JSONArray());
		generateKey(json.toString(), "products", false);
	}
	
	
	@Test
	public void onlyProductsRoles() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		json.put("roles", new JSONArray());
		generateKey(json.toString(), "products", false);
	}
	
	@AfterTest(alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(adminUser);
		baseUtils.reset(productID, sessionToken);
	}
	
	private void generateKey(String content, String field, boolean expectedFailure) throws IOException{
		String response = operApi.generateAirlockKey(content, adminToken);
		Assert.assertEquals(response.contains("error") && !response.contains("DOCTYPE html"), expectedFailure, "Invalid result for field: " + field);
	}
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
