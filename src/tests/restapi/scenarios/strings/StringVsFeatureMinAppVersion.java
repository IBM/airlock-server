package tests.restapi.scenarios.strings;


import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class StringVsFeatureMinAppVersion {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected String featureID;
	protected String configID;
	protected String str;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	/*	
	@BeforeClass
	@Parameters({"url", "translationsUrl", "configPath", "sessionToken", "productsToDeleteFile"})
	public void init(String url, String translationsUrl, String configPath, @Optional String sToken, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(m_url, configPath, sessionToken, productsToDeleteFile);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		//baseUtils.createUtility(seasonID);
	}

	//minAppVersion is removed from strings

	
	@Test (description = "Add string value")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	//configuration
	@Test (dependsOnMethods="addString",  description = "Add feature with configuration minAppVersion=1.0 that uses the strings")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		String response = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

	@Test (dependsOnMethods="addFeature",  description = "Increase string minAppVersion - should fail")
	public void updateString() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("minAppVersion", "4.0");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (dependsOnMethods="updateString",  description = "Add another configuration with higher minAppVersion - it works with lower version strings")
	public void addFeature2() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		configID = f.addFeature(seasonID, crJson.toString(), featureID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test (dependsOnMethods="addFeature2",  description = "Update configuration to lower minAppVersion - should fail")
	public void decreaseConfigurationVersion() throws Exception{
		
		String configuration =  f.getFeature(configID, sessionToken);
		JSONObject crJson = new JSONObject(configuration);
		crJson.put("minAppVersion", "0.0");
		configID = f.updateFeature(seasonID, configID, crJson.toString(), sessionToken);
		Assert.assertTrue(configID.contains("error"), "Test should fail, but instead passed: " + configID );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(p, productID, sessionToken);
	}
	*/
}
