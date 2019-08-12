package tests.restapi.scenarios.strings;


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

public class UpdateStringsAndCompareParameters {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	
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
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
		
	}

//TODO validate all fields

	
	@Test
	public void updateInternationalFallback() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("internationalFallback", "new fallback");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("internationalFallback").equals("new fallback"), "internationalFallback was not updated");
		
	}
	
	@Test
	public void updateValue() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("value", "new value");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("value").equals("new value"), "Value was not updated");
		
	}
	
	@Test 
	public void updateKey() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("key", "new.key");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "key was not updated: " + response );
	}
	
	//minAppVersion is removed from strings
	/*
	@Test
	public void updateMinAppVersion() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("minAppVersion", "1.1");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("minAppVersion").equals("1.1"), "minAppVersion was not updated");		
	}*/
	
	@Test
	public void updateStage() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("stage", "PRODUCTION");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("stage").equals("PRODUCTION"), "stage was not updated");
		
		//move back to dev to remove product
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		json.put("stage", "DEVELOPMENT");
		String response = t.updateString(stringID, json.toString(), sessionToken);

		Assert.assertFalse(response.contains("error"), "stage was not updated: " + response );
	}
	
	@Test
	public void updateOwner() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("owner", "Jon");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("owner").equals("Jon"), "owner was not updated");
		
	}
	
	@Test
	public void updateDescription() throws Exception{
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("description", "new description");
		t.updateString(stringID, json.toString(), sessionToken);
		str = t.getString(stringID, sessionToken);
		json = new JSONObject(str);
		Assert.assertTrue(json.getString("description").equals("new description"), "description was not updated");
		
	}
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
