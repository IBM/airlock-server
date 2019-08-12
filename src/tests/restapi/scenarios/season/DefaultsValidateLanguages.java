package tests.restapi.scenarios.season;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class DefaultsValidateLanguages {
	protected String seasonID;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;
		m_url = url;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		
		p.setURL(m_url);
		s.setURL(m_url);
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
	}
	
	
	@Test (description = "Add product, season and 1 feature")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		f.addFeature(seasonID, feature, "ROOT", sessionToken);	

	}
	
	/*
	@Test (description = "Check that translations field in defaults file is not empty")
	public void checkTranslations() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);

		Assert.assertTrue(json.containsKey("translations"), "translations not found in defaults file");
		JSONObject translations = json.getJSONObject("translations");
		Assert.assertTrue(!translations.isEmpty(), "Translations object exists but is empty");

	}
	*/
	
	@Test (description = "supportedLangauges includes defaultsLanguages")
	public void checkSupportedLanguages() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		Assert.assertTrue(json.containsKey("supportedLanguages"), "supportedLanguages not found in defaults file");
		JSONArray supportedLanguages = json.getJSONArray("supportedLanguages") ;
		
		String defaultsLang = json.getString("defaultLanguage");
		
		boolean contains = false;
		for (int i=0; i<supportedLanguages.length(); i++){
			if (supportedLanguages.get(i).equals(defaultsLang))
				contains = true;
		}
		
		Assert.assertTrue(contains, "Default language was not found in supported languages list");
		//Assert.assertTrue(Arrays.asList(supportedLanguages).contains(defaultsLang), "Default language was not found in supported languages list");

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
