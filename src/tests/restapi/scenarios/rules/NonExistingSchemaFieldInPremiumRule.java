package tests.restapi.scenarios.rules;

import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;
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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class NonExistingSchemaFieldInPremiumRule {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	private AirlockUtils baseUtils;
	protected String filePath;
	protected FeaturesRestApi f;
	protected String feature;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
	}
	
	@Test (description = "Add components")
	public void addComponents() throws JSONException, IOException{
		//add entitlements to season1
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "Entitlement1");
		jsonE.put("stage", "PRODUCTION");
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement1 was not added to the season " + entitlementID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonIP.toString(), entitlementID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject premiumFeatureObj = new JSONObject(feature1);
		premiumFeatureObj.put("entitlement", entitlementID1);
		premiumFeatureObj.put("premium", true);
		JSONObject premiumRule = new JSONObject();
		premiumRule.put("ruleString", "");
		premiumFeatureObj.put("premiumRule", premiumRule);
		featureID = f.addFeature(seasonID, premiumFeatureObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
	}
	
	
	@Test (dependsOnMethods = "addComponents", description = "Create a feature with a rule that uses a field that doesn't exist in inputSchema")
	public void nonExistingFieldInCreate() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
				
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("premiumRule", rule);
		
		String response = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (dependsOnMethods = "nonExistingFieldInCreate", description = "Update a feature with a rule that uses a field that doesn't exist in inputSchema")
	public void nonExistingFieldInUpdate() throws JSONException, IOException{
		feature = f.getFeature(featureID, sessionToken);
		JSONObject json = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("premiumRule", rule);
		
		String response = f.updateFeature(seasonID, featureID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
