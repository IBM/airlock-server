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

public class DeleteUtilityInUseByPurchaseItems {
	private String seasonID;
	private String productID;
	private String utilityID;
	private String filePath;
	private FeaturesRestApi f;
	private ProductsRestApi p;
	private UtilitiesRestApi u;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private InAppPurchasesRestApi purchasesApi;
	private String entitlementID;
	private String purchaseOptionsID;
	
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
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	@Test(dependsOnMethods = "addUtility", description = "Add entitlement with rule using utility")
	public void addEntitlement() throws IOException, JSONException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String entitlementMixID = purchasesApi.addPurchaseItem(seasonID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMixID.contains("error"), "Test should pass, but instead failed: " + entitlementMixID );
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		eJson.put("rule", rule);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementMixID, sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Test should pass, but instead failed: " + entitlementID );
	}
	
	@Test(dependsOnMethods = "addEntitlement", description = "Delete utility")
	public void deleteUtility(){
		int responseCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertFalse(responseCode == 200, "Should not delete utility if it is in use by entitlement");
	}
	//E_MTX->E->PO_MTX->PO
	@Test(dependsOnMethods = "deleteUtility", description = "Add purchaseOptions with rule using utility")
	public void addPurchaseOptions() throws IOException, JSONException{
		int res = purchasesApi.deletePurchaseItem(entitlementID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete entitlement");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String entitlementMixID = purchasesApi.addPurchaseItem(seasonID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(entitlementMixID.contains("error"), "Test should pass, but instead failed: " + entitlementMixID );
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, entitlementMixID, sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Test should pass, but instead failed: " + entitlementID );
		
		
		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String purchaseOptionsMixID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsMix, entitlementID, sessionToken);
		Assert.assertFalse(purchaseOptionsMixID.contains("error"), "Test should pass, but instead failed: " + purchaseOptionsMixID );
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject poJson = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		poJson.put("rule", rule);
		poJson.put("minAppVersion", "1.0");
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, poJson.toString(), purchaseOptionsMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "Test should pass, but instead failed: " + purchaseOptionsID );
	}
	
	@Test(dependsOnMethods = "addPurchaseOptions", description = "Delete utility")
	public void deleteUtility2(){
		int responseCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertFalse(responseCode == 200, "Should not delete utility if it is in use by purchaseOptions");
	}
	
	@Test(dependsOnMethods = "deleteUtility2", description = "delete purchaseOptions and update utility")
	public void deletePurchaseOptions() throws IOException, JSONException{
		int res = purchasesApi.deletePurchaseItem(purchaseOptionsID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete purchaseOptions");
		int responseCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Should delete utility if it is not in use by purchaseOptions");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
