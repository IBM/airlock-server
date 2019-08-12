package tests.restapi.scenarios.strings;


import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class DeleteStringInUseByConfigurationInPurchaseItems {
	private String seasonID;
	private String stringID;
	private String filePath;
	private String str;
	private StringsRestApi t;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String productID;
	private String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private InAppPurchasesRestApi purchasesApi;
	private String configID;
	private String entitlementID;
	private String purchaseOptionsID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description = "Add string in production stage")
	public void addString() throws Exception{
		
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	@Test (dependsOnMethods="addString",  description = "Add entitlement with configuration")
	public void addEntitlement() throws Exception{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		configID = purchasesApi.addPurchaseItem(seasonID, crJson.toString(), entitlementID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}
	
	@Test (dependsOnMethods="addEntitlement",  description = "Delete string in use by configuration")
	public void deleteString() throws Exception{
		String response = t.getStringUsage(stringID, sessionToken);
		JSONObject usgaeObj = new JSONObject(response);
		
		JSONArray usedByConfig = usgaeObj.getJSONArray("UsedByConfigurations");
		JSONArray UsedByUtilities = usgaeObj.getJSONArray("UsedByUtilities");
		Assert.assertTrue(usedByConfig.size() == 1, "wrong ussage data");
		Assert.assertTrue(UsedByUtilities.size() == 0, "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("configName").equals("ns1.CR1"), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("configID").equals(configID), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("featureName").equals("ns1.inAppPurchase1"), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("featureID").equals(entitlementID), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("branchName").equals("MASTER"), "wrong ussage data");
		
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertNotEquals(responseCode, 200, "String in use by configuration was deleted");
	}
	
	@Test(dependsOnMethods = "deleteString", description = "Add purchaseOptions with config using string")
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
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID,purchaseOptions, purchaseOptionsMixID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "Test should pass, but instead failed: " + purchaseOptionsID );
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		configID = purchasesApi.addPurchaseItem(seasonID, crJson.toString(), purchaseOptionsID, sessionToken );
		Assert.assertFalse(configID.contains("error"), "Test should pass, but instead failed: " + configID );
	}

	@Test (dependsOnMethods="addPurchaseOptions",  description = "Delete string in use by configuration")
	public void deleteString2() throws Exception{
		String response = t.getStringUsage(stringID, sessionToken);
		JSONObject usgaeObj = new JSONObject(response);
		
		JSONArray usedByConfig = usgaeObj.getJSONArray("UsedByConfigurations");
		JSONArray UsedByUtilities = usgaeObj.getJSONArray("UsedByUtilities");
		Assert.assertTrue(usedByConfig.size() == 1, "wrong ussage data");
		Assert.assertTrue(UsedByUtilities.size() == 0, "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("configName").equals("ns1.CR1"), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("configID").equals(configID), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("featureName").equals("ns1.purchaseOptions1"), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("featureID").equals(purchaseOptionsID), "wrong ussage data");
		Assert.assertTrue(usedByConfig.getJSONObject(0).getString("branchName").equals("MASTER"), "wrong ussage data");
		
		int responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertNotEquals(responseCode, 200, "String in use by configuration was deleted");
		
		int res = purchasesApi.deletePurchaseItem(purchaseOptionsID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete purchaseOptions");
		
		responseCode = t.deleteString(stringID, sessionToken);
		Assert.assertEquals(responseCode, 200, "String not in use by configuration was not deleted");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
