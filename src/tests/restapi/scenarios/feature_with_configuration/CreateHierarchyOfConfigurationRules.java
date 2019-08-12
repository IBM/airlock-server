package tests.restapi.scenarios.feature_with_configuration;

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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class CreateHierarchyOfConfigurationRules {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String configRuleID1;
	protected String configRuleID2;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );

	}
	

	
	@Test (description = "Add configuration rule and assign an existing feature as its parent")
	public void createConfigurationRule() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configRuleID1 = f.addFeature(seasonID, configuration, featureID, sessionToken );
		Assert.assertFalse(configRuleID1.contains("error"), "Test should pass, but instead failed: " + configRuleID1 );
	}
	
	@Test (dependsOnMethods="createConfigurationRule", description = "Add configuration rule under the previous configuration rule")
	public void addHierarchyConfigurationRule() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configRuleID2 = f.addFeature(seasonID, configuration, configRuleID1, sessionToken );
		Assert.assertFalse(configRuleID2.contains("error"), "Test should pass, but instead failed: " + configRuleID2 );
	}
	
	@Test (dependsOnMethods="addHierarchyConfigurationRule", description = "Get the top configuration rule and check that it has a child")
	public void validateConfigurationRule() throws IOException, JSONException{
		
		String feature = f.getFeature(configRuleID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		JSONArray  children = json.getJSONArray("configurationRules");
		Assert.assertTrue(children.length()==1, "Configuration rule was not added to parent");
		

	}
	
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
