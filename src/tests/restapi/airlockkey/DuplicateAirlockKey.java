package tests.restapi.airlockkey;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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


public class DuplicateAirlockKey {
	
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
	private String keyID;
	
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

		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
		
		apikey = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		

	}
	
	@Test (description="use the same key name in create")
	public void generateKey() throws JSONException, IOException{
		JSONObject json = populateKeyName();
		keyID = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(keyID.contains("error"), "Can't generate key: " + keyID);
		
		String response = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Generate the same key name ");
	}
	
	@Test (dependsOnMethods = "generateKey", description="use the same key name in update")
	public void updateKey() throws Exception{
		JSONObject json = populateKeyName();
		String keyID2 = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(keyID2.contains("error"), "Can't generate key: " + keyID2);
		
		String key1 = operApi.getKey(keyID, adminToken);
		JSONObject json1 = new JSONObject(key1);
		
		String key2 = operApi.getKey(keyID2, adminToken);
		JSONObject json2 = new JSONObject(key2);
		json2.put("key", json1.getString("key"));
		
		String response = operApi.updateKey(keyID2, json2.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Update duplicate key name ");
	}
	
	@AfterTest (alwaysRun=true)
	private void reset() throws Exception{
		baseUtils.deleteKeys(adminUser);
	}
	
	
	
	private JSONObject populateKeyName() throws JSONException{
		JSONObject json = new JSONObject(apikey);
		json.put("key", RandomStringUtils.randomAlphabetic(3));
		return json;

	}

}
