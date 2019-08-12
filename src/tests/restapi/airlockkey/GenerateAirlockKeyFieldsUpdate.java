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


public class GenerateAirlockKeyFieldsUpdate {
	
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
	protected String keyID;
	protected String productID;
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","adminPass","appName","productsToDeleteFile"})
	public void init(String url,String t_url,String a_url, String configPath, String c_operationsUrl,String admin,String adminPass, String appName,String productsToDeleteFile) throws IOException, JSONException{
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
		
		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		keyID = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(keyID.contains("error"), "Can't generate airlock key: " + keyID);
		
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
	public void emptKeyField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));		
		json.put("key", "");
		updateKey(keyID, json.toString(), "key", true);			
	}
	
	@Test
	public void missinKeyField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.remove("key");
		updateKey(keyID, json.toString(), "key", true);			
	}
	
	@Test
	public void nullKeyField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("key", JSONObject.NULL);
		updateKey(keyID, json.toString(), "key", true);
				
	}
	
	@Test
	public void changeKeyField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		updateKey(keyID, json.toString(), "key", true);				
	}
	
	@Test
	public void emptyPasswordField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("keyPassword", "");
		updateKey(keyID, json.toString(), "password", true);
	
	}
	@Test
	public void nullPasswordField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("keyPassword", JSONObject.NULL);
		updateKey(keyID, json.toString(), "password", false);
	}
	@Test
	public void changePsswordField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("keyPassword", "password1");
		updateKey(keyID, json.toString(), "password", true);
	
	}
	
	@Test
	public void missinOwnerField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.remove("owner");
		updateKey(keyID, json.toString(), "owner", true);
	}
	@Test
	public void emptyOwnerField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("owner", "");
		updateKey(keyID, json.toString(), "owner", true);
	}
	@Test
	public void nullOwnerField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("owner", JSONObject.NULL);
		updateKey(keyID, json.toString(), "owner", true);
	}
	@Test
	public void changeOwnerField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("owner", "owner1");
		updateKey(keyID, json.toString(), "owner", true);
	
	}
	
	@Test
	public void emptyLastModifiedField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("lastModified", "");
		updateKey(keyID, json.toString(), "lastModified", true);
	}
	@Test
	public void nullLastModifiedField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("lastModified", JSONObject.NULL);
		updateKey(keyID, json.toString(), "lastModified", true);
	}
	@Test
	public void changeLastModifiedField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("lastModified", System.currentTimeMillis()+10000);
		updateKey(keyID, json.toString(), "lastModified", false);
	
	}
	
	@Test
	public void emptyCreationDateField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("creationDate", "");
		updateKey(keyID, json.toString(), "creationDate", true);
	}
	
	@Test
	public void nullCreationDateField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));		
		json.put("creationDate", JSONObject.NULL);
		updateKey(keyID, json.toString(), "creationDate", true);
	
	}
	
	@Test
	public void changeCreationDateField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("creationDate", System.currentTimeMillis()-1000);
		updateKey(keyID, json.toString(), "creationDate", true);
	
	}
	

	@Test
	public void emptyUniqueIdField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("uniqueId", "");
		updateKey(keyID, json.toString(), "uniqueId", true);
	}
	
	@Test
	public void nullUniqueIdField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("uniqueId", JSONObject.NULL);
		updateKey(keyID, json.toString(), "uniqueId", false);
	}
	
	@Test
	public void changeUniqueIdField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));		
		json.put("uniqueId", UUID.randomUUID().toString());
		updateKey(keyID, json.toString(), "uniqueId", true);
	
	}

	
	@Test
	public void missingRolesField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.remove("roles");
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void emptyRolesField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("roles", "");
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void nullRolesField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("roles", JSONObject.NULL);
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void emptyArrayRolesField() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("roles", new JSONArray());
		json.put("products", new JSONArray());
		updateKey(keyID, json.toString(), "roles", true);
	
	}

	@Test
	public void nullProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		json.put("products", JSONObject.NULL);
		json.put("roles", roles);
		updateKey(keyID, json.toString(), "products", false);
	}
	
	@Test
	public void emptyProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("products", new JSONArray());
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		json.put("roles", roles);
		updateKey(keyID, json.toString(), "products", false);
	}
	
	@Test
	public void wrongProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("products", new JSONObject());
		updateKey(keyID, json.toString(), "products", true);
	}
	
	@Test
	public void emptyJsonInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "products", true);
	}
	
	@Test
	public void nullProductIdInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", JSONObject.NULL);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "productId", true);
	}
	
	@Test
	public void wrongRoleInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		JSONArray rolesArr = new JSONArray();
		rolesArr.add("xxx");
		prodRoles.put("roles", rolesArr);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void emptyProductIdInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", "");
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "productId", true);
	}
	
	@Test
	public void emptyRolesInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", new JSONArray());
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void nullRolesInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", JSONObject.NULL);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void missingRolesInProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "roles", true);
	}
	
	@Test
	public void bothGlobalAndProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		updateKey(keyID, json.toString(), "products", false);
	}
	
	
	@Test
	public void onlyProductsRoles() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray rolesPerProducts = new JSONArray();
		JSONObject prodRoles = new JSONObject();
		prodRoles.put("productId", productID);
		prodRoles.put("roles", json.getJSONArray("roles"));
		rolesPerProducts.add(prodRoles);
		json.put("products", rolesPerProducts);
		json.put("roles", new JSONArray());
		updateKey(keyID, json.toString(), "products", false);
	}
	
	@Test
	public void noRolesAndNoProducts() throws Exception{
		JSONObject json = new JSONObject(operApi.getKey(keyID, adminToken));
		JSONArray roles = new JSONArray();
		roles.add("Editor");
		
		JSONObject prodObj = new JSONObject();
		prodObj.put("roles", roles);
		prodObj.put("productId", productID);
		
		JSONArray productArr = new JSONArray();
		productArr.add(prodObj);
		json.put("products", productArr);
		json.put("roles", roles);
		updateKey(keyID, json.toString(), "products", false);
		
		json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("roles", new JSONArray());
		updateKey(keyID, json.toString(), "roles", false);
		
		json = new JSONObject(operApi.getKey(keyID, adminToken));
		json.put("products", new JSONArray());
		updateKey(keyID, json.toString(), "products", true);
	}
	
	@AfterTest(alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(adminUser);
		baseUtils.reset(productID, sessionToken);
	}
	
	private void updateKey(String keyID, String keyContent, String field, boolean expectedFailure) throws Exception{
		String response = operApi.updateKey(keyID, keyContent, adminToken);
		Assert.assertEquals(response.contains("error"), expectedFailure, "Invalid result for field: " + field + "  " + response);
	}

}
