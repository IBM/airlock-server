package tests.restapi.validations.purchaseOptions;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
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

public class InAppPurchasesUniqueName {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String purchaseOptions;
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID;
	
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
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID.contains("error"), "Can't add inAppPur: " + inAppPurID);
		
		purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "F1");
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "cannot create feature: " + featureID);
		
		jsonF.put("name", "F2");
		jsonF.put("namespace", "F2");
		featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "cannot create feature: " + featureID);
	}
	
	@Test (description = "Non unique feature/purchaseOptions name in create")
	public void addPurchaseOptions1() throws JSONException, IOException{
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "F1");
		jsonIP.put("namespace", "F1");
		String response = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Can create purchaseOptions with name identical to feature's");
	}
	
	@Test (dependsOnMethods = "addPurchaseOptions1", description = "Non unique feature/purchaseOptions name in update")
	public void updatePurchaseOptions1() throws JSONException, IOException{
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID, sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "Can't create purchaseOptions: " + inAppPurchaseID);
		
		jsonIP = new JSONObject(purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken));
		jsonIP.put("name", "F2");
		jsonIP.put("namespace", "F2");
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID, jsonIP.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can update purchaseOptions with name identical to feature's");
		
	}
	@Test (dependsOnMethods = "updatePurchaseOptions1", description = "Non unique purchaseOptions/purchaseOptions name in create")
	public void addPurchaseOptions2() throws JSONException, IOException{
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "F1");
		jsonIP.put("namespace", "F1");
		String response = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("An item with the specified namespace and name already exists"), "Can create purchaseOptions with non unique name");
	}
	
	@Test (dependsOnMethods = "addPurchaseOptions2", description = "Non unique purchaseOptions/purchaseOptions name in update")
	public void updatePurchaseOptions2() throws JSONException, IOException{
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", RandomStringUtils.randomAlphabetic(5));
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, jsonIP.toString(), inAppPurID, sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "Can't create purchaseOptions: " + inAppPurchaseID);
		
		jsonIP = new JSONObject(purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken));
		jsonIP.put("name", "F2");
		jsonIP.put("namespace", "F2");
		String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID, jsonIP.toString(), sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("An item with the specified namespace and name already exists"), "purchaseOptions updated");
		
	}
	
	@Test (dependsOnMethods = "updatePurchaseOptions2", description = "Non unique inAppPurchase/feature name in update. using reserved entitlements namespace")
	public void addFeatureWithPurchasesNamespace() throws JSONException, IOException{
		JSONObject jsonPO = new JSONObject(purchaseOptions);
		jsonPO.put("name", "name1");
		jsonPO.put("namespace", "airlockEntitlement");
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, jsonPO.toString(), inAppPurID, sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "Can't create purchaseOptions: " + inAppPurchaseID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "name1");
		jsonF.put("namespace", "airlockEntitlement");
		String response = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("An item with the specified namespace and name already exists") && response.contains("airlockEntitlement"), "feature create");	
	}

	@Test (dependsOnMethods = "addFeatureWithPurchasesNamespace", description = "Non unique inAppPurchase config/feature config name in craete")
	public void addNotUniqueConfigRules() throws JSONException, IOException{
		JSONObject jsonPO = new JSONObject(purchaseOptions);
		jsonPO.put("name", "name2");
		jsonPO.put("namespace", "airlockEntitlement");
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, jsonPO.toString(), inAppPurID, sessionToken);
		Assert.assertFalse(purchaseOptionsID.contains("error"), "Can't create purchaseOptions: " + purchaseOptionsID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "name1");
		jsonF.put("namespace", "ns");
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error") , "feature create: " + featureID);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		
		jsonCR.put("name", "cr1");
		jsonCR.put("namespace", "airlockEntitlement");
		
		String response = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), purchaseOptionsID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create config rule: " + response);
		
		response = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("An item with the specified namespace and name already exists"), "config rule create");	
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
