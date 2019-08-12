package tests.restapi.scenarios.experiments;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;



public class FeatureAttributesInMaster {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	private String branchID;
	protected String filePath;
	protected JSONObject json;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{

		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
			
	}
	
	@Test (dependsOnMethods="addBranch", description="Create master feature with default attributes and 2 configurations. Checkout feature")
	public void addComponents() throws JSONException, IOException, InterruptedException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("title1", "test");
		defaultConfiguration.put("title2", "test");
		json.put("defaultConfiguration", defaultConfiguration);
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "feature was not added to the season" + featureID);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color1", "red");
		newConfiguration.put("color2", "white");
		jsonConfig.put("configuration", newConfiguration);
		String configID1 = f.addFeature(seasonID, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
	

		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config2);
		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("size1", "large");
		newConfiguration2.put("size2", "small");
		jsonConfig2.put("configuration", newConfiguration2);
		String configID2 = f.addFeature(seasonID, jsonConfig2.toString(), featureID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);

		//checkout feature
		String response = br.checkoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

	}
	
	@Test (dependsOnMethods="addComponents", description="Check the number of  attributes")
	public void	getAttributes() throws JSONException{
		
		String response = f.getFeatureAttributesFromBranch(featureID, branchID, sessionToken);
		JSONObject attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 6, "Incorrect number of attributes");
	
	}

	
	@Test (dependsOnMethods="getAttributes", description="Add configuration to branch to checked out feature")
	public void addConfigurationToBranch() throws JSONException, IOException, InterruptedException{

		//add configuration to branch
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "CR3");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("attr1", "value1");
		newConfiguration.put("attr2", "value2");
		jsonConfig.put("configuration", newConfiguration);
		String configID3 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration3 was not added to branch" + configID3);
	
		//check that attributes are not added in master
		String response = f.getFeatureAttributes(featureID,  sessionToken);
		JSONObject attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 6, "Incorrect number of attributes in master");

		//check that attributes are added in branch
		response = f.getFeatureAttributesFromBranch(featureID, branchID, sessionToken);
		attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 8, "Incorrect number of attributes in branch");

	}
	
	//configuration can't be added to a feature that is not checked out
	
	@Test (dependsOnMethods="addConfigurationToBranch", description="Add configuration in branch to master feature")
	public void addConfigurationToMaster() throws JSONException, IOException, InterruptedException{
		//cannot un-checkout feature with new sub items
		String response = br.cancelCheckoutFeature(branchID, featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "feature was unchecked from branch  eventhough it has new sub feature");
	
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "tempName");
		String tmpFeatureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(tmpFeatureID.contains("error"), "feature was not added to the season" + featureID);
		
		//add configuration to branch
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "CR4");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("attr3", "value3");
		newConfiguration.put("attr4", "value4");
		jsonConfig.put("configuration", newConfiguration);
		String configID4 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), tmpFeatureID, sessionToken);
		Assert.assertTrue(configID4.contains("error"), "Configuration4 was added to branch" + configID4);

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
