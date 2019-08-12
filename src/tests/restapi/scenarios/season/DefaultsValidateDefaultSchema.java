package tests.restapi.scenarios.season;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
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

public class DefaultsValidateDefaultSchema {
	protected String seasonID;
	protected String productID;
	protected String config;
	protected String featureID;
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
		
		//prepare defaultConfiguration object
		JSONObject configSchema = new JSONObject();
		configSchema.put("color", "red");
		configSchema.put("text", "some text");
		JSONObject json = new JSONObject(feature);
		json.put("defaultConfiguration", configSchema.toString());
		
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);	
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	@Test (dependsOnMethods = "addComponents", description = "Compare defaultSchema field from defaults file to defaultSchema from feature")
	public void checkDefaultConfiguration() throws JSONException{
		String feature = f.getFeature(featureID, sessionToken);
		JSONObject featureJson = new JSONObject(feature);
		
		String defaultConfigFromFeature = featureJson.getString("defaultConfiguration");
		
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject jsonDefaults = new JSONObject(defaults);
		JSONArray defaultFeatures = jsonDefaults.getJSONObject("root").getJSONArray("features");
		
		Assert.assertTrue(defaultFeatures.size()>0, "Missing features in defaults file");
		
		Assert.assertTrue(defaultFeatures.getJSONObject(0).containsKey("defaultConfiguration"), "defaultConfiguration not found in defaults file");
		String defaultConfigFromDefaults = defaultFeatures.getJSONObject(0).getString("defaultConfiguration");
		
		Assert.assertTrue(defaultConfigFromFeature.equals(defaultConfigFromDefaults), "defaultConfiguration object in defaults file is incorrect");
	}
	

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
