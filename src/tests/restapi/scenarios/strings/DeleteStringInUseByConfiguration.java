package tests.restapi.scenarios.strings;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class DeleteStringInUseByConfiguration {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}


	
	@Test (description = "Add string in production stage")
	public void addString1() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	@Test (dependsOnMethods="addString1",  description = "Add feature with configuration")
	public void addFeature1() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		String response = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="addFeature1",  description = "Delete string in use by configuration")
	public void deleteString1() throws Exception{
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertNotEquals(responseCode, 200, "String in use by configuration was deleted");
	}
	
	@Test (dependsOnMethods="deleteString1", description = "Add string in production stage")
	public void addString2() throws Exception{
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	@Test (dependsOnMethods="addString2",  description = "Add feature with configuration")
	public void addFeature2() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String ruleString =  "  translate(\"app.hi\", \"testing string1\", \"testing string2\");  true;	" ;
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		crJson.put("rule", ruleObj);
		String response = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods="addFeature2",  description = "Delete string in use by configuration rule")
	public void deleteString2() throws Exception{
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertNotEquals(responseCode, 200, "String in use by configuration was deleted");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
