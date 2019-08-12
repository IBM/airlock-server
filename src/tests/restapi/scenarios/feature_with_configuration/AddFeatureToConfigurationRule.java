package tests.restapi.scenarios.feature_with_configuration;

import java.io.IOException;






import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class AddFeatureToConfigurationRule {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String configRuleID;
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
		configRuleID = f.addFeature(seasonID, configuration, featureID, sessionToken );
		Assert.assertFalse(configRuleID.contains("error"), "Test should pass, but instead failed: " + configRuleID );
	}
	
	@Test (dependsOnMethods="createConfigurationRule", description = "Add a regular feature to configuration rule - not allowed")
	public void addFeatureToConfigurationRule() throws IOException, JSONException{
		
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, feature, configRuleID, sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
