package tests.restapi.validations.purchaseOptions;

import java.io.IOException;

import org.apache.wink.json4j.JSON;
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

public class InAppPurchaseUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String purchaseOptions;
	protected String purchaseOptionsID;
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
		Assert.assertFalse (inAppPurID.contains("error"), "Can't add orderingRule: " + inAppPurID);
		
		purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, inAppPurID, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add orderingRule: " + purchaseOptionsID);	
	}

	
	@Test
	private void noCachedResultsField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("noCachedResults");
		updateFeature(json, "noCachedResults", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("noCachedResults", "");
		updateFeature(json, "noCachedResults", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("noCachedResults", JSON.NULL);
		updateFeature(json, "noCachedResults", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("uniqueId");
		updateFeature(json, "uniqueId", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("uniqueId", "");
		updateFeature(json, "uniqueId", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("uniqueId", JSON.NULL);
		updateFeature(json, "uniqueId", false);
	}

	@Test
	private void enabledField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("enabled");
		updateFeature(json, "enabled", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("enabled", "");
		updateFeature(json, "enabled", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("enabled", JSON.NULL);
		updateFeature(json, "enabled", true);
	}

	@Test
	private void typeField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("type");
		updateFeature(json, "type", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("type", "");
		updateFeature(json, "type", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("type", JSON.NULL);
		updateFeature(json, "type", true);
	}
	
	
	@Test
	private void stageField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("stage");
		updateFeature(json, "stage", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("stage", "");
		updateFeature(json, "stage", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("stage", JSON.NULL);
		updateFeature(json, "stage", true);
	}
	
	@Test
	private void namespaceField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("namespace");
		updateFeature(json, "namespace", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("namespace", "");
		updateFeature(json, "namespace", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("namespace", JSON.NULL);
		updateFeature(json, "namespace", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("creator");
		updateFeature(json, "creator", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("creator", "");
		updateFeature(json, "creator", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("creator", JSON.NULL);
		updateFeature(json, "creator", true);
	}
	
	
	@Test
	private void ownerField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("owner");
		updateFeature(json, "owner", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("owner", "");
		updateFeature(json, "owner", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("owner", JSON.NULL);
		updateFeature(json, "owner", false);
	}
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("description");
		updateFeature(json, "description", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("description", "");
		updateFeature(json, "description", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("description", JSON.NULL);
		updateFeature(json, "description", false);
	}
	
	@Test
	private void ruleField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("rule");
		updateFeature(json, "rule", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("rule", "");
		updateFeature(json, "rule", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("rule", JSON.NULL);
		updateFeature(json, "rule", true);
	}
	
	@Test
	private void ruleStringField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		ruleString.put("ruleString", JSON.NULL);
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
	}

	@Test
	private void minAppVersionField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("minAppVersion");
		updateFeature(json, "minAppVersion", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("minAppVersion", "");
		updateFeature(json, "minAppVersion", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("minAppVersion", JSON.NULL);
		updateFeature(json, "minAppVersion", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("name");
		updateFeature(json, "name", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("name", "");
		updateFeature(json, "name", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("name", JSON.NULL);
		updateFeature(json, "name", true);
	}

	
	@Test
	private void orderingRuleField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("orderingRule");
		updateFeature(json, "orderingRule", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("orderingRule", "");
		updateFeature(json, "orderingRule", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("orderingRule", JSON.NULL);
		updateFeature(json, "orderingRule", false);
	}
	
	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("internalUserGroups");
		updateFeature(json, "internalUserGroups", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("internalUserGroups", "");
		updateFeature(json, "internalUserGroups", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("internalUserGroups", JSON.NULL);
		updateFeature(json, "internalUserGroups", false);
	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("rolloutPercentage");
		updateFeature(json, "rolloutPercentage", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("rolloutPercentage", "");
		updateFeature(json, "rolloutPercentage", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("rolloutPercentage", JSON.NULL);
		updateFeature(json, "rolloutPercentage", true);
	}	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("lastModified");
		updateFeature(json, "lastModified", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("lastModified", "");
		updateFeature(json, "lastModified", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("lastModified", JSON.NULL);
		updateFeature(json, "lastModified", true);
	}	
	
	private void updateFeature(JSONObject json, String field, boolean expectedFailure) throws JSONException{
		try {
			String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedFailure,  "Test failed " + response + " for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	@Test
	private void defaultIfAirlockSystemIsDownField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("defaultIfAirlockSystemIsDown");
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("defaultIfAirlockSystemIsDown", "");
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("defaultIfAirlockSystemIsDown", JSON.NULL);
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
	}
	
	@Test
	private void additionalInfoField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("additionalInfo");
		updateFeature(json, "additionalInfo", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("additionalInfo", new JSONObject());
		updateFeature(json, "additionalInfo", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("additionalInfo", JSON.NULL);
		updateFeature(json, "additionalInfo", false);
	}
	
	@Test
	private void configurationSchemaField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("configurationSchema");
		updateFeature(json, "configurationSchema", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("configurationSchema", JSON.NULL);
		updateFeature(json, "configurationSchema", false);
	}
	
	@Test
	private void defaultConfigurationField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("defaultConfiguration");
		updateFeature(json, "defaultConfiguration", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("defaultConfiguration", new JSONObject());
		updateFeature(json, "defaultConfiguration", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("defaultConfiguration", JSON.NULL);
		updateFeature(json, "defaultConfiguration", false);
	}
	
	@Test
	private void displayNameField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("displayName");
		updateFeature(json, "displayName", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("displayName", "");
		updateFeature(json, "displayName", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("displayName", JSON.NULL);
		updateFeature(json, "displayName", false);
	}
	
	@Test
	private void storeProductIdsField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("storeProductIds");
		updateFeature(json, "storeProductIds", true);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("storeProductIds", new JSONArray());
		updateFeature(json, "storeProductIds", false);
		
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		json = new JSONObject(purchaseOptions);
		json.put("storeProductIds", JSON.NULL);
		updateFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "store1");
		storeProdId.put("productId", "123");
		storeProductIdsArr.add(storeProdId);
		json.put("storeProductIds", storeProductIdsArr);
		updateFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField2() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "Apple App Store");
		storeProdId.put("productId", "");
		storeProductIdsArr.add(storeProdId);
		json.put("storeProductIds", storeProductIdsArr);
		updateFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField3() throws IOException, JSONException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId1 = new JSONObject();
		storeProdId1.put("storeType", "Apple App Store");
		storeProdId1.put("productId", "1");
		storeProductIdsArr.add(storeProdId1);
		JSONObject storeProdId2 = new JSONObject();
		storeProdId2.put("storeType", "Apple App Store");
		storeProdId2.put("productId", "2");
		storeProductIdsArr.add(storeProdId2);
		json.put("storeProductIds", storeProductIdsArr);
		updateFeature(json, "storeProductIds", true);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
