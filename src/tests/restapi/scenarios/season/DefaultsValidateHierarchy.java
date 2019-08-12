package tests.restapi.scenarios.season;

import java.io.IOException;


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

public class DefaultsValidateHierarchy {
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
	

	}
	
	@Test (dependsOnMethods = "addComponents", description = "Create 2 features, create MI under the first, add 1 child to MI")
	public void createHierarchy() throws JSONException, IOException{
		String feature1 = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);	
		
		String feature2 = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
		f.addFeature(seasonID, feature2, "ROOT", sessionToken);
		//add configuration under feature2
		String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		f.addFeature(seasonID, configuration, featureID1, sessionToken);
		
		String miFeature = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
		String miFeatureID = f.addFeature(seasonID, miFeature, featureID1, sessionToken);
		
		String feature3 = FileUtils.fileToString(config + "feature3.txt", "UTF-8", false);
		f.addFeature(seasonID, feature3, miFeatureID, sessionToken);
		



	}
	
	@Test (dependsOnMethods = "createHierarchy", description = "Check hierarchy in Defaults file")
	public void checkHierarchyInDefaults() throws JSONException, IOException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject jsonDefaults = new JSONObject(defaults);
		JSONArray defaultFeatures = jsonDefaults.getJSONObject("root").getJSONArray("features");
		
		Assert.assertTrue(defaultFeatures.size()==2, "Missing features in defaults file");

		Assert.assertTrue(defaultFeatures.getJSONObject(0).containsKey("uniqueId"), "Mutual exclusion group uniqueId not found in defaults file");
		JSONObject mi = defaultFeatures.getJSONObject(0).getJSONArray("features").getJSONObject(0);
		
		JSONArray featuresInMI = mi.getJSONArray("features");
		Assert.assertTrue(featuresInMI.size()==1, "Missing feature in hierarchy in defaults file");
		
		JSONObject feature2 = defaultFeatures.getJSONObject(1);
		Assert.assertTrue(feature2.containsKey("uniqueId"), "Feature uniqueId not found in defaults file");
		Assert.assertTrue(!feature2.containsKey("configurationRules"), "configurationRules found in default file");
	}
	

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
