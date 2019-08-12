package tests.restapi.in_app_purchases;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class DeletePurchaseIncludedInPurchase {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String bundleID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description = "add 2 in-app-purchase one purchase including the 2")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String puOptID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle");
		iapObj.put("includedEntitlements", includedPurchases);
		bundleID = purchasesApi.addPurchaseItem(seasonID, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID.contains("error"), "Can't add inAppPurchase: " + bundleID);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==2, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");		
	}
	
	@Test (dependsOnMethods = "addComponents", description = "delete a purchase that is part of a bundle")
	public void deletePurcahse() throws JSONException, IOException, InterruptedException{
		int res = purchasesApi.deletePurchaseItem(inAppPurID1, sessionToken);
		Assert.assertTrue(res != 200, "can delete inAppPurID1 even though part of bundle");
		
		res = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(res != 200, "can delete inAppPurID2 even though part of bundle");
	}
	
	@Test (dependsOnMethods = "deletePurcahse", description = "update bundle - remove purchase 1")
	public void updateBundle() throws JSONException, IOException, InterruptedException{
		String bundle = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleJson = new JSONObject(bundle);
		
		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID2);	
		bundleJson.put("includedEntitlements", includedPurchases);
		
		String response = purchasesApi.updatePurchaseItem(seasonID, bundleID, bundleJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update bundle: " + response);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==1, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID2), "wrong includedPurchase");
	}
	
	@Test (dependsOnMethods = "updateBundle", description = "delete a purchase that is part of a bundle")
	public void deletePurcahse2() throws JSONException, IOException, InterruptedException{
		int res = purchasesApi.deletePurchaseItem(inAppPurID1, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete inAppPurID1 even though it is not a part of bundle anymore ");
		
		res = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(res != 200, "can delete inAppPurID2 even though part of bundle");
	}
	
	@Test (dependsOnMethods = "deletePurcahse2", description = "delete a purchase bundle")
	public void deleteBundle() throws JSONException, IOException, InterruptedException{
		int res = purchasesApi.deletePurchaseItem(bundleID, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete bundle ");
	}
	
	@Test (dependsOnMethods = "deleteBundle", description = "delete a purchase that is part of a bundle")
	public void deletePurcahse3() throws JSONException, IOException, InterruptedException{
		int res = purchasesApi.deletePurchaseItem(inAppPurID2, sessionToken);
		Assert.assertTrue(res == 200, "cannot delete inAppPurID2 even though it is not a part of bundle anymore ");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
