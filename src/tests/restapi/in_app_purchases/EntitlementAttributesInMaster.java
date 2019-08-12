package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;

public class EntitlementAttributesInMaster {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String branchID;
	private String filePath;
	private FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
    
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		br = new BranchesRestApi();
		br.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
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
	
	@Test (dependsOnMethods="addBranch", description="Create master entitlement with default attributes and 2 configurations. Checkout entitlement")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		JSONObject defaultConfiguration = new JSONObject();
		defaultConfiguration.put("title1", "test");
		defaultConfiguration.put("title2", "test");
		json.put("defaultConfiguration", defaultConfiguration);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added to the season" + entitlementID);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color1", "red");
		newConfiguration.put("color2", "white");
		jsonConfig.put("configuration", newConfiguration);
		String configID1 = purchasesApi.addPurchaseItem(seasonID, jsonConfig.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
	

		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config2);
		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("size1", "large");
		newConfiguration2.put("size2", "small");
		jsonConfig2.put("configuration", newConfiguration2);
		String configID2 = purchasesApi.addPurchaseItem(seasonID, jsonConfig2.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);

		//checkout entitlement
		String response = br.checkoutFeature(branchID, entitlementID, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
	}
	
	@Test (dependsOnMethods="addComponents", description="Check the number of  attributes")
	public void	getAttributes() throws JSONException{
		String response = f.getFeatureAttributesFromBranch(entitlementID, branchID, sessionToken);
		JSONObject attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 6, "Incorrect number of attributes");
	}

	
	@Test (dependsOnMethods="getAttributes", description="Add configuration to branch to checked out entitlement")
	public void addConfigurationToBranch() throws JSONException, IOException, InterruptedException{

		//add configuration to branch
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "CR3");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("attr1", "value1");
		newConfiguration.put("attr2", "value2");
		jsonConfig.put("configuration", newConfiguration);
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration3 was not added to branch" + configID3);
	
		//check that attributes are not added in master
		String response = f.getFeatureAttributes(entitlementID,  sessionToken);
		JSONObject attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 6, "Incorrect number of attributes in master");

		//check that attributes are added in branch
		response = f.getFeatureAttributesFromBranch(entitlementID, branchID, sessionToken);
		attrs = new JSONObject(response);
		Assert.assertTrue(attrs.getJSONArray("attributes").size() == 8, "Incorrect number of attributes in branch");
	}
	
	//configuration can't be added to a entitlement that is not checked out
	@Test (dependsOnMethods="addConfigurationToBranch", description="Add configuration in branch to master entitlement")
	public void addConfigurationToMaster() throws JSONException, IOException, InterruptedException{
		//cannot un-checkout feature with new sub items
		String response = br.cancelCheckoutFeature(branchID, entitlementID, sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement was unchecked from branch  eventhough it has new sub feature");
	
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "tempName");
		String tmpEntitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(tmpEntitlementID.contains("error"), "entitlement was not added to the season" + entitlementID);
		
		//add configuration to branch
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		jsonConfig.put("name", "CR4");
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("attr3", "value3");
		newConfiguration.put("attr4", "value4");
		jsonConfig.put("configuration", newConfiguration);
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonConfig.toString(), tmpEntitlementID, sessionToken);
		Assert.assertTrue(configID4.contains("error"), "Configuration4 was added to branch" + configID4);
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
