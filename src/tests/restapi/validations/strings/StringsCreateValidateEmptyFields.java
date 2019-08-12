package tests.restapi.validations.strings;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class StringsCreateValidateEmptyFields {
	protected String seasonID;
	protected String filePath;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String str;
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		
	}

	@Test 
	public void emptyLastModified() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("lastModified", "");
		addString(json.toString());
	}
	
	@Test
	public void emptyUniqueId() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("uniqueId", "");
		addString(json.toString());
	}
	
	//minAppVersion is removed from strings
	/*
	@Test 
	public void emptyMinAppVersion() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("minAppVersion", "");
		addString(json.toString());
	}*/
	
	@Test 
	public void emptyKey() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("key", "");
		addString(json.toString());
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("creator", "");
		addString(json.toString());
	}
	
	@Test 
	public void emptyValue() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("value", "");
		addString(json.toString());
	}
	
	@Test
	public void emptyStage() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("stage", "");
		addString(json.toString());
	}

	
	@Test
	public void emptySeasonId() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("seasonId", "");
		addString(json.toString());
	}
	
	@Test 
	public void emptyOwner() throws Exception{
		
		JSONObject json = new JSONObject(str);
		json.put("owner", "");
		String newKey = RandomStringUtils.randomAlphabetic(5);
		json.put("key", newKey);
		String response = t.addString(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	}
	
	@Test 
	public void emptyDescription() throws Exception{
		
		JSONObject json = new JSONObject(str);
		json.put("description", "");
		json.put("key", RandomStringUtils.randomAlphabetic(5));
		String response = t.addString(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	}
	
	
	@Test 
	public void emptyInternationalFallback() throws Exception{
		JSONObject json = new JSONObject(str);
		json.put("internationalFallback", "");
		String newKey = RandomStringUtils.randomAlphabetic(5);
		json.put("key", newKey);
		String response = t.addString(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	}
	
	private void addString(String input) throws Exception{
		String response = t.addString(seasonID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
