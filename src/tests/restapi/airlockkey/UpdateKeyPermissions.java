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


public class UpdateKeyPermissions {
	
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
	private String productLeadUser;
	private String productLeadPassword;
	private String editorUser;
	private String editorPassword;
	
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
		productLeadPassword = productLeadPass;
		editorUser = editor;
		editorPassword = editorPass;

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

		try {
			operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Only owner or admin can update key
	 */
	
	@Test (description="create and update key using different permissions")
	public void updateKey() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("ProductLead");
		roles.add("Editor");
		json.put("roles", roles);
		
		//update using owner token
		String prodLeadsessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);		
		String keyID = operApi.generateAirlockKey(json.toString(), prodLeadsessionToken);
		Assert.assertFalse(keyID.contains("error"), "Can't create api key: " + keyID);		
		String response = operApi.getKey(keyID, prodLeadsessionToken);
		Assert.assertFalse(response.contains("error"), "Can't get api key: " + response);		
		JSONObject jsonKey = new JSONObject(response);
		roles = new JSONArray();
		roles.add("ProductLead");
		jsonKey.put("roles", roles);
		response = operApi.updateKey(keyID, jsonKey.toString(), prodLeadsessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update api key: " + response);
		
		//update using admin token	
		response = operApi.getKey(keyID, adminToken);
		Assert.assertFalse(response.contains("error"), "Can't get api key: " + response);		
		jsonKey = new JSONObject(response);
		roles = new JSONArray();
		roles.add("ProductLead");
		roles.add("Editor");
		jsonKey.put("roles", roles);
		response = operApi.updateKey(keyID, jsonKey.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't update api key: " + response);
		
		//update using editor token
		String editorToken = baseUtils.setNewJWTToken(editorUser, editorPassword, m_appName);
		response = operApi.getKey(keyID, editorToken);
		Assert.assertFalse(response.contains("error"), "Can't get api key: " + response);		
		jsonKey = new JSONObject(response);
		roles = new JSONArray();
		roles.add("Editor");
		jsonKey.put("roles", roles);
		response = operApi.updateKey(keyID, jsonKey.toString(), editorToken);
		Assert.assertTrue(response.contains("error"), "Can't update api key: " + response);
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		operApi.resetUsersFromList(config + "airlockkey/original_users.txt", adminToken);
		baseUtils.deleteKeys(productLeadUser);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
