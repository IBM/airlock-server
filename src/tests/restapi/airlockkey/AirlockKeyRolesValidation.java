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
import tests.restapi.OperationRestApi;


public class AirlockKeyRolesValidation {
	
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
		
		apikey = FileUtils.fileToString(config+ "airlockkey/key_template1.txt", "UTF-8", false);
		

	}
	
	@Test (description="Illegal role in create/update")
	public void illegalRole() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("NoRole");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Generated  key with invalid role ");
		
		json.getJSONArray("roles").remove("NoRole");
		keyID = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(keyID.contains("error"), "Can't generate  key " + keyID);
		
		//add invalid role in update
		
		String key = operApi.getKey(keyID, adminToken);
		json = new JSONObject(key) ;
		roles.add("NoRole");
		json.put("roles", roles);
		response = operApi.updateKey(keyID, json.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Updated  key with invalid role ");
		
	}
	
	
	@Test (description="Duplicate role in create/update")
	public void duplicateRole() throws Exception{
		JSONObject json = populateKeyName();
		JSONArray roles = new JSONArray();
		roles.add("Viewer");
		roles.add("Viewer");
		json.put("roles", roles);
		
		String response = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Generated  key with duplicate role ");
		
		json.getJSONArray("roles").remove(0);
		keyID = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(keyID.contains("error"), "Can't generate key: " + keyID);
		
		//add invalid role in update
		
		String key = operApi.getKey(keyID, adminToken);
		json = new JSONObject(key) ;
		roles.add("SomeRole");
		json.put("roles", roles);
		response = operApi.updateKey(keyID, json.toString(), adminToken);
		Assert.assertTrue(response.contains("error"), "Updated  key with duplicate role");
		
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
