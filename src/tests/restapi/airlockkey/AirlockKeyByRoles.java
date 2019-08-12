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
import tests.restapi.SeasonsRestApi;


public class AirlockKeyByRoles {
	
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
	private SeasonsRestApi s;
	private String keyID;
	private String seasonID;
	private String productID;
	private FeaturesRestApi f;
	private String editorUser;
	private String editorPassword;
	private String productLeadUser;
	private String productLeadPassword;
	
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
		editorUser = editor;
		editorPassword = editorPass;
		productLeadUser = productLead;
		productLeadPassword = productLeadPass;

		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		sessionToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		
		productID = baseUtils.createProduct();
		seasonID = baseUtils.createSeason(productID);
		

	}
	
	/*
	 * 
	 */
	
	@Test (description="Can't create key for roles that have higher permissions than the user. Editor ask for Admin permission")
	public void editorRoleForAdmin() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Administrator");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal key permission for role Editor in create");
		
		//Create legal key for role Editor
		roles.remove("Administrator");
		roles.add("Editor");
		roles.add("Viewer");
		json.put("roles", roles);
		keyID = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertFalse(keyID.contains("error"), "Can't create key for Editor role: " + keyID);
		
		//add Admin role to Editor in update
		
		String key = operApi.getKey(keyID, sessionToken);
		json = new JSONObject(key) ;
		json.getJSONArray("roles").add("Administrator");
		response = operApi.updateKey(keyID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal key permission for role Editor in update");
		
	}
	
	@Test (description="Can't create key for roles that have higher permissions than the user. Editor ask for TranslationSpecialist permission")
	public void editorRoleForTranslator() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("TranslationSpecialist");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal key permission for role Editor in create");
		
		//Create legal key for role Editor
		roles.remove("TranslationSpecialist");
		roles.add("Editor");
		json.put("roles", roles);
		keyID = operApi.generateAirlockKey(json.toString(), sessionToken);
		Assert.assertFalse(keyID.contains("error"), "Can't sessionToken  key " + keyID);
		
		//add TranslationSpecialist role to Editor in update
		
		String key = operApi.getKey(keyID, sessionToken);
		json = new JSONObject(key) ;
		json.getJSONArray("roles").add("TranslationSpecialist");
		response = operApi.updateKey(keyID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Illegal key permission for role Editor in update");
		
	}
	
	@Test (description="Editor ask for Viewer permission")
	public void editorRoleForViewer() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create key");
		
		JSONObject keyJson = new JSONObject(response)	;
		String startSessionResp = operApi.startSessionFromKey(keyJson.getString("key"), keyJson.getString("keyPassword"));
		Assert.assertFalse(startSessionResp.contains("error"), "Can't start session with generated key");
		
		//key was generated for role Viewer, so Editor action should fail
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		response = f.addFeature(seasonID, feature, "ROOT", startSessionResp);
		Assert.assertTrue(response.contains("error"), "Feature created for Viewer permission");
	}
	
	@Test (description="Administrator can generate api key")
	public void administratorKey() throws Exception{
		//remove Admin from Editor role
		operApi.removeUserRole("Editor", adminUser, adminToken);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Administrator");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKeyCompleteResponse(json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't create key");
		
		JSONObject keyJson = new JSONObject(response)	;
		String startSessionResp = operApi.startSessionFromKey(keyJson.getString("key"), keyJson.getString("keyPassword"));
		Assert.assertFalse(startSessionResp.contains("error"), "Can't start session with generated key");
		
		//user with admin key can perform admin operation, but not editor operations
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		response = f.addFeature(seasonID, feature, "ROOT", startSessionResp);
		Assert.assertTrue(response.contains("error"), "Feature created for Admin permissions without Editor premissions");
		
		//reset roles and adminToken
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
	}
	
	@Test (description="ProductLead can generate api key")
	public void productLeadKey() throws Exception{
		//remove productLead from Editor role
		operApi.removeUserRole("Editor", productLeadUser, adminToken);
		
		sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("ProductLead");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create key");
		
		JSONObject keyJson = new JSONObject(response)	;
		String startSessionResp = operApi.startSessionFromKey(keyJson.getString("key"), keyJson.getString("keyPassword"));
		Assert.assertFalse(startSessionResp.contains("error"), "Can't start session with generated key");
		
		//user with productlead key can perform productlead operation, but not editor operations
		String minVersion = "{\"minVersion\":\"9.0\"}";
		s.addSeason(productID, minVersion, startSessionResp);
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		response = f.addFeature(seasonID, feature, "ROOT", startSessionResp);
		Assert.assertTrue(response.contains("error"), "Feature created for Admin permissions without Editor premissions");
		
		//reset roles
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
	}
	
	@Test (description="ProductLead that is not Editor can't request editor api key")
	public void productLeadNoEditorKey() throws Exception{
		sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		//remove editor from ProductLead role
		operApi.removeUserRole("Editor", productLeadUser, adminToken);
				
		JSONObject json = populateKeyName();
		JSONArray keyRoles = new JSONArray();
		keyRoles.add("Editor");
		json.put("roles", keyRoles);
		
		String response = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Create key for incorrect role since is added automatically");
		
		
		//return productLeadUser to Editor role for further tests
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
	}
		
	@Test (description="ProductLead that is not ProductLead can't request ProductLead api key")
	public void productLeadNoProductLeadKey() throws Exception{
		sessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		//remove productLeadUser from ProductLead role
		operApi.removeUserRole("ProductLead", productLeadUser, adminToken);
				
		JSONObject json = populateKeyName();
		JSONArray keyRoles = new JSONArray();
		keyRoles.add("ProductLead");
		json.put("roles", keyRoles);
		
		String response = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Create key for incorrect role");
		
		//return productLeadUser to Editor role for further tests
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
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
