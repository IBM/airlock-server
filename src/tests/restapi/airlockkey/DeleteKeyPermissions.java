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


public class DeleteKeyPermissions {
	
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
		if(appName != null) {
			m_appName = appName;
		}

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
	
	@Test (description="create and update key using different permissions")
	public void updateKey() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("ProductLead");
		roles.add("Editor");
		json.put("roles", roles);
		
		String prodLeadsessionToken = baseUtils.setNewJWTToken(productLeadUser, productLeadPassword, m_appName);
		
		//delete using jwt created from the key
		String completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), prodLeadsessionToken);
		JSONObject keyJson = new JSONObject(completeResponse);
		String key= keyJson.getString("key");
		String keyPassword = keyJson.getString("keyPassword");
		String keyID = keyJson.getString("uniqueId");
		
		String keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		Assert.assertFalse(keySessionToken.contains("error"), "Failed to generate session token from key: " + keySessionToken);
		int respCode = operApi.deleteKey(keyID, keySessionToken);
		Assert.assertFalse(respCode==200, "Deleted key using jwt created from this key");
		
		//delete using admin token	
		respCode = operApi.deleteKey(keyID, adminToken);
		Assert.assertTrue(respCode==200, "Can't delete key using adminToken");
		
		//generate deleted key
		completeResponse = operApi.generateAirlockKeyCompleteResponse(json.toString(), prodLeadsessionToken);
		keyJson = new JSONObject(completeResponse);
		key= keyJson.getString("key");
		keyPassword = keyJson.getString("keyPassword");
		keyID = keyJson.getString("uniqueId");
		keySessionToken = operApi.startSessionFromKey(key, keyPassword);
		
		//delete using owner regular sessionToken
		respCode = operApi.deleteKey(keyID, prodLeadsessionToken);
		Assert.assertTrue(respCode==200, "Can't delete key using owner sessionToken");


	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(productLeadUser);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
