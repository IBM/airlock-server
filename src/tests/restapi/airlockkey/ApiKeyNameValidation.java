package tests.restapi.airlockkey;

import java.io.IOException;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class ApiKeyNameValidation {
	protected String seasonID;
	protected String m_url;
	protected JSONObject json;
	protected OperationRestApi operApi;
	protected String key;
	protected List<String> illegalCharacters;
	protected List<String> legalCharacters;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String adminToken;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	protected String adminUser;
	protected String adminPassword;
	protected String m_appName;
	protected String config;
	protected String apikey;

	
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
		operApi.setURL(operationsUrl);
		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		key = FileUtils.fileToString(config + "airlockkey/key_template1.txt", "UTF-8", false);
		illegalCharacters= new ArrayList<String>(Arrays.asList("(", ")", "[", "]", "{", "}", "|", "/", "\\", "\"", ">", "<", 
				",", "!", "?", "@", "#", "$", "%", "^", "&", "*", "~", ";", "'", "-","_"));
		legalCharacters= new ArrayList<String>(Arrays.asList(" ", "."));

	}
	
	//in name we allow literals, numbers and spaces
	//in namespace we allow literals and numbers
	
	@Test (description = "Validate legal special characters in name")
	public void legalCharactersInName() throws IOException, JSONException{
		JSONObject json = new JSONObject(key);
		json.put("key", "key 123");
		String response = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't create apikey: " + response );
		json.put("key", "key.123a");
		response = operApi.generateAirlockKey(json.toString(), adminToken);
		Assert.assertFalse(response.contains("error"), "Can't create apikey: " + response );

	}
	
		
	@Test (description = "Validate illegal special characters in name")
	public void illegalCharactersInName() throws JSONException, IOException{
		JSONObject json = new JSONObject(key);
		for (String character : illegalCharacters) {
			json.put("key", "key" + character + "123");
			String response = operApi.generateAirlockKey(json.toString(), adminToken);
			Assert.assertTrue(response.contains("error"), "Test failed: " + response );
		}		
	}
	

	@AfterTest (alwaysRun=true)
	public void reset() throws Exception{
		baseUtils.deleteKeys(null);
		baseUtils.reset(productID, sessionToken);
	}
	


}
