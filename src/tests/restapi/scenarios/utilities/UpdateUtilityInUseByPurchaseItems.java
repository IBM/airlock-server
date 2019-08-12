package tests.restapi.scenarios.utilities;

import java.io.File;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
import tests.restapi.UtilitiesRestApi;

public class UpdateUtilityInUseByPurchaseItems {
	private String seasonID;
	private String productID;
	private String utilityID;
	private String entitlementID;
	private String purchaseOptionsID;
	private String filePath;
	private FeaturesRestApi f;
	private ProductsRestApi p;
	private UtilitiesRestApi u;
	private InAppPurchasesRestApi purchasesApi;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	@Test(description = "Add valid utility")
	public void addUtility() throws IOException, JSONException{
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isTrue(){return true;}");
		utilProps.setProperty("minAppVersion", "1.0");
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	@Test(dependsOnMethods = "addUtility", description = "Add entitlement with rule using utility")
	public void addEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		eJson.put("rule", rule);
		eJson.put("minAppVersion", "1.0");
		entitlementID = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Test should pass, but instead failed: " + entitlementID );
	}
	
	@Test(dependsOnMethods = "addEntitlement", description = "Change utility name when it is in use")
	public void updateUtility() throws JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue1(){return false;}");
		String response = u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "updateUtility", description = "Add purchaseOptions with rule using utility")
	public void addPurchaseOptions() throws IOException, JSONException{
		int res = purchasesApi.deletePurchaseItem(entitlementID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete entitlement");
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Test should pass, but instead failed: " + entitlementID );
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject poJson = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		poJson.put("rule", rule);
		poJson.put("minAppVersion", "1.0");
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, poJson.toString(), entitlementID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "Test should pass, but instead failed: " + purchaseOptionsID );
	}
	
	@Test(dependsOnMethods = "addPurchaseOptions", description = "Change utility name when it is in use")
	public void updateUtility2() throws JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue1(){return false;}");
		String response = u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "updateUtility2", description = "delete purchaseOptions and update utility")
	public void deletePurchaseOptions() throws IOException, JSONException{
		int res = purchasesApi.deletePurchaseItem(purchaseOptionsID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete purchaseOptions");
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("utility", "function isTrue1(){return false;}");
		String response = u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
