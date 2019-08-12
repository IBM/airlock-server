package tests.restapi.scenarios.mutual_exclusion_configuration;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
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


public class AddMIConfigurationGroupToRegularMutuallyExclusive {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String featureMutualID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
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
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		featureMutualID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
	}
	
	/**
	 * Create mutually exclusive group
	 */
	@Test (description = "Create configuration mutual exclusion group with 1 configuration rule")
	public void createMutuallyExclusiveWithFeature() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			featureID = f.addFeature(seasonID, parent, "ROOT", sessionToken);

			
			String feature = FileUtils.fileToString(filePath+"configuration_feature-mutual.txt", "UTF-8", false);
			parentID = f.addFeature(seasonID, feature, featureID, sessionToken);
			
			String configuration = FileUtils.fileToString(filePath+"configuration_rule1.txt", "UTF-8", false);
			childID1 = f.addFeature(seasonID, configuration, parentID, sessionToken);
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test (dependsOnMethods="createMutuallyExclusiveWithFeature", description = "Move mutually exclusive configuration group to MI group of regular features")
	public void moveConfigurationToMI() throws JSONException{
		try {
			
			String configuration = f.getFeature(parentID, sessionToken);
			String featureMutual = f.getFeature(featureMutualID, sessionToken);
			JSONObject json = new JSONObject(featureMutual);
			json.put("features", configuration);
			String response = f.updateFeature(seasonID, featureMutualID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
