package tests.restapi.airlockkey;

import java.io.IOException;







import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;


public class ChangeUserRolesScenarios {

	protected String sessionToken;
	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	private String seasonID;
	private String productID;

	protected String config;
	protected String adminUser;
	protected String productLeadUser;
	protected String editorUser;
	protected String translatorUser;
	protected String viewerUser;
	protected String adminPassword;
	protected String productLeadPassword;
	protected String password;
	protected String translatorPassword;
	protected String viewerPassword;
	protected String m_appName;
	private JSONArray originalUsers;

	private FeaturesRestApi f;
	protected OperationRestApi operApi;
	protected AirlockUtils baseUtils;
	protected ProductsRestApi p;
	private StringsRestApi stringApi;
	private TranslationsRestApi translationsApi;

	private ArrayList<String> users = new ArrayList<String>();

	protected List<String> rolesToRun = new ArrayList<String>();
	//private SoftAssert softAssert = new SoftAssert();

	private String key;
	private String keyPassword;
	private String keyID;

	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","configPath", "operationsUrl","admin","productLead","editor","translator","viewer","adminPass","productLeadPass","editorPass","translatorPass","viewerPass","appName","productsToDeleteFile", "runRoles"})
	public void init(String url,String t_url,String a_url,String configPath, String c_operationsUrl,String admin,String productLead, String editor,String translator,String viewer,String adminPass,String productleadPass,String editorPass,String translatorPass,String viewerPass, String appName,String productsToDeleteFile, String runRoles) throws Exception{
		m_url = url;
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;

		password = editorPass;

		operApi = new OperationRestApi();
		operApi.setURL(c_operationsUrl);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		stringApi = new StringsRestApi();
		stringApi.setURL(translationUrl);
		translationsApi = new TranslationsRestApi();
		translationsApi.setURL(translationUrl);

		if(appName != null) {
			m_appName = appName;
		}
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);


		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);


		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}

		//remove users from previous products so they can be deleted from global
		removeUserFromExistingProducts("user3@weather.com");
		removeUserFromExistingProducts("user2@weather.com");
		removeUserFromExistingProducts("user5@weather.com");
		removeUserFromExistingProducts("user1@weather.com");
		
		
		removeUserFromExistingProducts("*@weather.com");
		

		productID = baseUtils.createProduct();
		seasonID = baseUtils.createSeason(productID);

		try {
			String response = translationsApi.addSupportedLocales(seasonID,"fr",adminToken);
			Assert.assertFalse(response.contains("error") && response.contains("SecurityPolicyException"), "Can't add locale fr to season: " + response);
		} catch (Exception e1) {
			System.out.println("Can't add locale fr ");
			e1.printStackTrace();			
		}

		//initialize a list of possible users, password is the same for all users
		users.add("user3@weather.com");	// prodLead
		users.add("user2@weather.com");	// editor
		users.add("user5@weather.com");	//translator
		users.add("user1@weather.com");	//viewer	

		rolesToRun = Arrays.asList(runRoles.split(","));

		//save original list of users
		try {
			String response = operApi.getAirlockUsers(adminToken);
			originalUsers = new JSONObject(response).getJSONArray("roles");
		} catch (Exception e){
			System.out.println("Error when saving original list of users: " + e.getLocalizedMessage());
		}
		
		
	}

	@Test
	public void run() throws Exception{

		if (rolesToRun.contains("all")) {
			runAdmin();
			runProductLead();
			runEditor();
		}

		if (rolesToRun.contains("admin"))
			runAdmin();

		if (rolesToRun.contains("productLead"))
			runProductLead();

		if (rolesToRun.contains("editor"))
			runEditor();

		if (rolesToRun.contains("translator"))
			runTranslator();

	}

	@Test
	private void zzreset() throws Exception{				//must run as the last test
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.reset(productID, adminToken);
		baseUtils.deleteKeys(null);
	}

	/*
	 * add User to Role
	 * create api key for all roles and session jwt
	 *
	 * check that an action allowed only for this role succeeds
	 * remove User from Role
	 * validate that this role was removed from user's key
	 * check that an action allowed only for this role fails with old jwt - currently known issue, as jwt is not recalculated
	 * create new session jwt
	 * validate that Role was deleted from user key
	 * check that an action allowed only for this role fails with new jwt
	 * validate viewer action
	 * add User to Role again
	 * check that an action allowed only for this role fails with existing jwt as jwt is not recalculated
	 * remove user from all roles, leave him only in Viewer as "*weather.com"
	 * validate viewer action
	 * delete "*weather.com" from Viewer - all User's keys must be deleted
	 * validate that user's keys were deleted, validate that getKey doesn't return key
	 * reset original user roles
	 */


	private void runAdmin() throws Exception{
		String testUser;

		for (int i=0; i< users.size(); i++) {
			testUser = users.get(i);
			System.out.println("running addUser for role admin for user " + testUser);
			addAllrolesToUser( testUser);
			addAllrolesToUserInProduct(testUser, productID);
			sessionToken = baseUtils.setNewJWTToken(testUser, password, m_appName);
			String[] possibleRoles = {"Administrator", "ProductLead", "Editor", "TranslationSpecialist", "Viewer"};
			String sessionJwt = createApiKey("Administrator", possibleRoles);
			testAdminRole(sessionJwt, false);
			reduceUserRoles("Administrator", testUser);
			validateRoleRemoved("Administrator", testUser);	
			validateRoleInKeyDeleted("Administrator", sessionJwt);							
			//sessionJwt = operApi.startSessionFromKey(key, keyPassword);

			testAdminRole(sessionJwt, true);			
			testViewer(sessionJwt, false); 	
			addAllrolesToUser(testUser);	
			addAllrolesToUserInProduct(testUser, productID);
			testAdminRole(sessionJwt, true);
			deleteProductRoleSetForUser(testUser, productID);
			
			leaveUserAsViewer(testUser);
			testViewer(sessionJwt, false);
			deleteUser(testUser);
			getUserKey(testUser);
			testViewer(sessionJwt, true);
			resetUsers();
		}	
	}

	private void runProductLead() throws Exception{
		String testUser;		
		
		for (int i=0; i< users.size(); i++) {
			testUser = users.get(i);
			System.out.println("running addUser for role productLead for user " + testUser);
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			reduceUserRoles("Administrator", testUser);
			sessionToken = baseUtils.setNewJWTToken(testUser, password, m_appName);
			String[] possibleRoles = {"ProductLead", "Editor", "TranslationSpecialist", "Viewer"};
			String sessionJwt = createApiKey("ProductLead", possibleRoles);			
			testAdminRole(sessionJwt, true);
			testProductLeadRole(sessionJwt, false);
			reduceUserRoles("ProductLead", testUser);
			validateRoleRemoved("ProductLead", testUser);			
			validateRoleInKeyDeleted("ProductLead", sessionJwt);
			//sessionJwt = operApi.startSessionFromKey(key, keyPassword);
			testProductLeadRole(sessionJwt, true);
			testViewer(sessionJwt, false); 	
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			//when role is added, api key permissions are not automatically updated, user must generate a new api key
			testProductLeadRole(sessionJwt, true);
			deleteProductRoleSetForUser(testUser, productID);
			leaveUserAsViewer(testUser);
			testViewer(sessionJwt, false);
			deleteUser(testUser);
			getUserKey(testUser);
			testViewer(sessionJwt, true);
			resetUsers();
		}		
	}

	private void runEditor() throws Exception{
		String testUser;
		
		for (int i=0; i< users.size(); i++) {
			testUser = users.get(i);
			System.out.println("running addUser for role editor for user " + testUser);
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			reduceUserRoles("Administrator", testUser);
			reduceUserRoles("ProductLead", testUser);
			sessionToken = baseUtils.setNewJWTToken(testUser, password, m_appName);
			String[] possibleRoles = {"Editor", "TranslationSpecialist", "Viewer"};
			String sessionJwt = createApiKey("Editor", possibleRoles);
			testAdminRole(sessionJwt, true);
			testProductLeadRole(sessionJwt, true);
			testEditorRole(sessionJwt, false);
			reduceUserRoles("Editor", testUser);
			validateRoleRemoved("Editor", testUser);			
			validateRoleInKeyDeleted("Editor", sessionJwt);
			//sessionJwt = operApi.startSessionFromKey(key, keyPassword);
			testEditorRole(sessionJwt, true);
			testViewer(sessionJwt, false); 	
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			//when role is added, api key permissions are not automatically updated, user must generate a new api key
			deleteProductRoleSetForUser(testUser, productID);
			testEditorRole(sessionJwt, true);
			leaveUserAsViewer(testUser);
			testViewer(sessionJwt, false);
			deleteUser(testUser);
			getUserKey(testUser);
			testViewer(sessionJwt, true);
			resetUsers();
		}	
	}

	private void runTranslator() throws Exception{
		String testUser;
		
		for (int i=0; i< users.size(); i++) {
			testUser = users.get(i);
			System.out.println("running addUser for role translator for user " + testUser);
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			sessionToken = baseUtils.setNewJWTToken(testUser, password, m_appName);
			String[] possibleRoles = {"TranslationSpecialist", "Viewer"};
			String sessionJwt = createApiKey("TranslationSpecialist", possibleRoles);
			testAdminRole(sessionJwt, true);
			testProductLeadRole(sessionJwt, true);
			testEditorRole(sessionJwt, true);
			testTranslationSpecialist(sessionJwt, false);
			reduceUserRoles("TranslationSpecialist", testUser);
			validateRoleRemoved("TranslationSpecialist", testUser);			
			validateRoleInKeyDeleted("TranslationSpecialist", sessionJwt);
			//sessionJwt = operApi.startSessionFromKey(key, keyPassword);
			testTranslationSpecialist(sessionJwt, true);
			testViewer(sessionJwt, false); 	
			addAllrolesToUser(testUser);
			addAllrolesToUserInProduct(testUser, productID);
			deleteProductRoleSetForUser(testUser, productID);
			leaveUserAsViewer(testUser);
			testViewer(sessionJwt, false);
			deleteUser(testUser);			
			getUserKey(testUser);
			testViewer(sessionJwt, true);
			resetUsers();
		}	
	}




	private void addAllrolesToUser(String userIdentifier) throws Exception{

		String response = operApi.getAirlockUsers(adminToken);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");

		JSONArray allRoles  = new JSONArray();
		allRoles.add("Viewer");
		allRoles.add("Editor");
		allRoles.add("AnalyticsEditor");
		allRoles.add("AnalyticsViewer");
		allRoles.add("TranslationSpecialist");
		allRoles.add("Administrator");
		allRoles.add("ProductLead");

		boolean found = false;
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				user.put("roles", allRoles);
				String resp = operApi.updateAirlockUser(user.getString("uniqueId"), user.toString(), adminToken);
				Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);

				found = true;
			}
		} 

		if (!found) {
			//add user with all roles
			String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);
			JSONObject userObj = new JSONObject(user);
			userObj.put("identifier", userIdentifier);
			userObj.put("roles", allRoles);
			
			String globalUserID = operApi.addGlobalAirlockUser(userObj.toString(), adminToken);
			Assert.assertNotNull(globalUserID);
			Assert.assertFalse(globalUserID.contains("error"), "Global was not created: " + productID);

		}

	}

	private void addAllrolesToUserInProduct(String userIdentifier, String productID) throws Exception{

		String response = operApi.getProductAirlockUsers(adminToken, productID);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");

		JSONArray allRoles  = new JSONArray();
		allRoles.add("Viewer");
		allRoles.add("Editor");
		allRoles.add("AnalyticsEditor");
		allRoles.add("AnalyticsViewer");
		allRoles.add("TranslationSpecialist");
		allRoles.add("Administrator");
		allRoles.add("ProductLead");

		boolean found = false;
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				user.put("roles", allRoles);
				String resp = operApi.updateAirlockUser(user.getString("uniqueId"), user.toString(), adminToken);
				Assert.assertFalse(resp.contains("error"), "Can't update user roles: " + resp);

				found = true;
			}
		} 

		if (!found) {
			//add user with all roles
			String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);
			JSONObject userObj = new JSONObject(user);
			userObj.put("identifier", userIdentifier);
			userObj.put("roles", allRoles);
			
			String globalUserID = operApi.addProductAirlockUser(userObj.toString(), productID, adminToken);
			Assert.assertNotNull(globalUserID);
			Assert.assertFalse(globalUserID.contains("error"), "Product was not created: " + productID);

		}

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

	public String createApiKey(String role, String[] possibleRoles) throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = populateRoles(possibleRoles);
		setGlobalAndProductRoles(json, roles, productID);

		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), sessionToken);
		Assert.assertFalse(completeResponse.contains("error"), "Can't create key: " + completeResponse);

		JSONObject keyJson = new JSONObject(completeResponse);

		//key and password saved for further tests
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");

		String sessionJwt = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(sessionJwt.contains("error"), "Can't start session with generated key");
		return sessionJwt;

	}

	private void testAdminRole(String sessionJwt, boolean expectedFailure) throws Exception {
		String response = operApi.getWebhooks(sessionToken);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "testAdminRole failed: " + response);
	}

	private void testProductLeadRole(String sessionJwt, boolean expectedFailure) throws IOException, JSONException {
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionJwt);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "testProductLeadRole failed: " + response);
	}

	private void testEditorRole(String sessionJwt, boolean expectedFailure) throws IOException, JSONException {
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionJwt);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "testProductLeadRole failed: " + response);
	}


	private void reduceUserRoles(String role, String user) throws Exception{
		operApi.removeUserRole(role, user, adminToken);
		operApi.removeUserRoleFromProduct(role, user, adminToken, productID);
	}

	//actually removes the user role set and the the *@weather.com group
	private void deleteUser(String userIdentifier) throws Exception {
		String response = operApi.getAirlockUsers(adminToken);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");

		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").contains("*@weather.com")){
				int code = operApi.deleteAirlockUser(user.getString("uniqueId"), adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user");
				break;
			}
		} 

		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)) {
				int code = operApi.deleteAirlockUser(user.getString("uniqueId"), adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user");
				break;
			}
		} 
	}

	//actually removes the user role set and the the *@weather.com group
	private void deleteProductRoleSetForUser(String userIdentifier, String productId) throws Exception {
		String response = operApi.getProductAirlockUsers(adminToken, productId);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");

		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").contains("*@weather.com")){
				int code = operApi.deleteAirlockUser(user.getString("uniqueId"), adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user");
				break;
			}
		} 
		
		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)) {
				int code = operApi.deleteAirlockUser(user.getString("uniqueId"), adminToken);
				Assert.assertEquals(code, 200, "Cannot delete user");
				break;
			}
		} 

	}

	//actually  removes the user (remains viewer from group)
	private void leaveUserAsViewer(String userIdentifier) throws Exception {
		String response = operApi.getAirlockUsers(adminToken);
		JSONObject allUsers = new JSONObject(response);
		JSONArray users = allUsers.getJSONArray("users");

		for (int i=0; i< users.size(); i++){
			JSONObject user = users.getJSONObject(i);
			if (user.getString("identifier").equals(userIdentifier)){
				JSONArray roles = new JSONArray();
				roles.add("Viewer");
				user.put("roles", roles);
				String res = operApi.updateAirlockUser(user.getString("uniqueId"), user.toString(), adminToken);
				Assert.assertFalse(res.contains("error"), "Cannot update user");
				return;
			}
		} 
	}
	/*	private void leaveUserAsViewer(String user) throws Exception{
		//delete user from all roles, it remains only as * 
		String response = operApi.getAirlockUsers(adminToken);
		JSONObject allRoles = new JSONObject(response);
		JSONArray roles = allRoles.getJSONArray("roles");

		for (int i=0; i< roles.size(); i++){
			if (roles.getJSONObject(i).getJSONArray("users").contains(user)){
				roles.getJSONObject(i).getJSONArray("users").remove(user);

			}
		}

		allRoles.put("roles", roles);
		String resp = operApi.setAirlockUsers(allRoles.toString(), adminToken);
		Assert.assertFalse(resp.contains("error"), "Can't update users list: " + resp);
	}

	private void deleteUser(String user) throws Exception{
		//delete user * in Viewer 
		String response = operApi.getAirlockUsers(adminToken);
		JSONObject allRoles = new JSONObject(response);
		JSONArray roles = allRoles.getJSONArray("roles");

		for (int i=0; i< roles.size(); i++){

			if (roles.getJSONObject(i).getString("role").equals("Viewer") && roles.getJSONObject(i).getJSONArray("users").contains("*@weather.com")){
				roles.getJSONObject(i).getJSONArray("users").remove("*@weather.com");
			}
		}

		allRoles.put("roles", roles);
		String resp = operApi.setAirlockUsers(allRoles.toString(), adminToken);
		Assert.assertFalse(resp.contains("error"), "Can't update users list: " + resp);
	}*/

	private void testViewer(String sessionJwt, boolean expectedFailure) throws Exception{
		String response = p.getAllProducts(sessionJwt);		
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "testViewer failed: " + response);
	}



	private JSONObject populateKeyName() throws JSONException, IOException{
		String apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

	private JSONArray populateRoles(String[] possibleRoles){
		JSONArray roles = new JSONArray();
		for (int i=0; i<possibleRoles.length; i++ ){
			roles.add(possibleRoles[i]);
		}
		return roles;
	}


	private void resetUsers() throws Exception{
		//reset from file:
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		operApi.resetUsersFromListForProduct(productID, config + "airlockkey/original_users.txt", adminToken);

	}

	private void validateRoleRemoved(String role, String user) throws Exception{
		String response = operApi.getRolesPerUser(user, adminToken);
		JSONArray rolesPerUser = new JSONObject(response).getJSONArray("roles");
		Assert.assertFalse(rolesPerUser.contains(role), "A list of roles still contain deleted role: " + role);

	}

	private void getUserKey(String user) throws Exception{
		String response = operApi.getKey(keyID, adminToken);
		Assert.assertTrue(response.contains("error"), "Key was not deleted");
		String rolesPerUser = operApi.getAllKeys(user, adminToken);
		JSONArray keys = new JSONObject(rolesPerUser).getJSONArray("airlockAPIKeys");
		Assert.assertTrue(keys.size()==0, "User keys were not deleted");
	}

	private void validateRoleInKeyDeleted(String role, String sessionJwt) throws Exception{
		String response = operApi.getKey(keyID, sessionJwt);
		JSONArray rolesInKey = new JSONObject(response).getJSONArray("roles");
		Assert.assertFalse(rolesInKey.contains(role), "Role was not deleted from user key");
	}

	private void testTranslationSpecialist(String sessionJwt, boolean expectedFailure) throws Exception{
		//only TranslationSpecialist can update translation
		String str = FileUtils.fileToString(config + "/strings/string1.txt", "UTF-8", false);
		String stringID;
		stringID = stringApi.addString(seasonID, str, sessionJwt);
		Assert.assertEquals(stringID.contains("error") || stringID.contains("SecurityPolicyException"), expectedFailure, "Added string by not TranslationSpecialist");

		if (expectedFailure)
			stringID = stringApi.addString(seasonID, str, adminToken);

		String response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionJwt);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "Marked for translation by not TranslationSpecialist");

		response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionJwt);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "Review for translation by not TranslationSpecialist");

		response = translationsApi.sendToTranslation(seasonID,new String[]{stringID},sessionJwt);
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "Send for translation by not TranslationSpecialist");

		String translationFr = FileUtils.fileToString(config + "strings/translationFR5.txt", "UTF-8", false);
		response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionJwt);	     
		Assert.assertEquals(response.contains("error") || response.contains("SecurityPolicyException"), expectedFailure, "Updated translation by not TranslationSpecialist");

		int respCode = stringApi.deleteString(stringID, sessionJwt);
		Assert.assertEquals(respCode!=200, expectedFailure, "Deleted string by not TranslationSpecialist");

		if (expectedFailure)
			stringApi.deleteString(stringID, adminToken);
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
