package tests.restapi.validations.purchaseOptions;

import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class PurchaseOptionsCreationFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String purchaseOptions;
	protected String inAppPurID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurID = purchasesApi.addPurchaseItem(seasonID, inAppPur, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID.contains("error"), "Can't add orderingRule: " + inAppPurID);
	}

	
	@Test
	private void noCachedResultsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("noCachedResults");
		addFeature(json, "noCachedResults", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("noCachedResults", "");
		addFeature(json, "noCachedResults", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("noCachedResults", JSON.NULL);
		addFeature(json, "noCachedResults", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("uniqueId");
		addFeature(json, "uniqueId", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("uniqueId", "");
		addFeature(json, "uniqueId", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("uniqueId", JSON.NULL);
		addFeature(json, "uniqueId", false);
	}

	@Test
	private void enabledField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("enabled");
		addFeature(json, "enabled", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("enabled", "");
		addFeature(json, "enabled", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("enabled", JSON.NULL);
		addFeature(json, "enabled", true);
	}

	@Test
	private void typeField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("type");
		addFeature(json, "type", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("type", "");
		addFeature(json, "type", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("type", JSON.NULL);
		addFeature(json, "type", true);
	}
	
	
	@Test
	private void stageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("stage");
		addFeature(json, "stage", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("stage", "");
		addFeature(json, "stage", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("stage", JSON.NULL);
		addFeature(json, "stage", true);
	}
	
	@Test
	private void namespaceField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("namespace");
		addFeature(json, "namespace", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("namespace", "");
		addFeature(json, "namespace", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("namespace", JSON.NULL);
		addFeature(json, "namespace", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("creator");
		addFeature(json, "creator", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("creator", "");
		addFeature(json, "creator", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("creator", JSON.NULL);
		addFeature(json, "creator", true);
	}
	
	
	@Test
	private void ownerField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("owner");
		addFeature(json, "owner", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("owner", "");
		addFeature(json, "owner", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("owner", JSON.NULL);
		addFeature(json, "owner", false);
	}
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("description");
		addFeature(json, "description", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("description", "");
		addFeature(json, "description", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("description", JSON.NULL);
		addFeature(json, "description", false);
	}
	
	@Test
	private void ruleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("rule");
		addFeature(json, "rule", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("rule", "");
		addFeature(json, "rule", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("rule", JSON.NULL);
		addFeature(json, "rule", true);
	}
	
	@Test
	private void ruleStringField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		addFeature(json, "rule", false);
		
		json = new JSONObject(purchaseOptions);
		ruleString.put("ruleString", JSON.NULL);
		json.put("rule", ruleString);
		addFeature(json, "rule", false);
	}

	@Test
	private void minAppVersionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("minAppVersion");
		addFeature(json, "minAppVersion", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("minAppVersion", "");
		addFeature(json, "minAppVersion", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("minAppVersion", JSON.NULL);
		addFeature(json, "minAppVersion", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("name");
		addFeature(json, "name", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("name", "");
		addFeature(json, "name", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("name", JSON.NULL);
		addFeature(json, "name", true);
	}

	
	@Test
	private void orderingRuleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("orderingRule");
		addFeature(json, "orderingRule", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("orderingRule", "");
		addFeature(json, "orderingRule", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("orderingRule", JSON.NULL);
		addFeature(json, "orderingRule", false);
	}
	/*
	@Test
	private void configurationField() throws IOException, JSONException{
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("configuration");
		addFeature(json, "configuration", true);
		
		json = new JSONObject(inAppPurchase);
		json.put("configuration", "");
		addFeature(json, "configuration", true);
		
		json = new JSONObject(inAppPurchase);
		json.put("configuration", JSON.NULL);
		addFeature(json, "configuration", true);
	}
*/
	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("internalUserGroups");
		addFeature(json, "internalUserGroups", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("internalUserGroups", "");
		addFeature(json, "internalUserGroups", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("internalUserGroups", JSON.NULL);
		addFeature(json, "internalUserGroups", false);
	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("rolloutPercentage");
		addFeature(json, "rolloutPercentage", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("rolloutPercentage", "");
		addFeature(json, "rolloutPercentage", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("rolloutPercentage", JSON.NULL);
		addFeature(json, "rolloutPercentage", true);
	}	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("lastModified");
		addFeature(json, "lastModified", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("lastModified", "");
		addFeature(json, "lastModified", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("lastModified", JSON.NULL);
		addFeature(json, "lastModified", false);
	}	
	
	@Test
	private void defaultIfAirlockSystemIsDownField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("defaultIfAirlockSystemIsDown");
		addFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("defaultIfAirlockSystemIsDown", "");
		addFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("defaultIfAirlockSystemIsDown", JSON.NULL);
		addFeature(json, "defaultIfAirlockSystemIsDown", true);
	}
	
	private void addFeature(JSONObject json, String field, boolean expectedResult) throws JSONException{
		if (!field.equals("name"))
			json.put("name", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = purchasesApi.addPurchaseItem(seasonID, json.toString(), inAppPurID, sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed " + response + " for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	@Test
	private void additionalInfoField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("additionalInfo");
		addFeature(json, "additionalInfo", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("additionalInfo", new JSONObject());
		addFeature(json, "additionalInfo", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("additionalInfo", JSON.NULL);
		addFeature(json, "additionalInfo", false);
	}
	
	@Test
	private void configurationSchemaField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("configurationSchema");
		addFeature(json, "configurationSchema", false);
		
		//json = new JSONObject(inAppPurchase);
		//json.put("configurationSchema", new JSONObject());
		//addFeature(json, "configurationSchema", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("configurationSchema", JSON.NULL);
		addFeature(json, "configurationSchema", false);
	}
	
	@Test
	private void defaultConfigurationField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("defaultConfiguration");
		addFeature(json, "defaultConfiguration", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("defaultConfiguration", new JSONObject());
		addFeature(json, "defaultConfiguration", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("defaultConfiguration", JSON.NULL);
		addFeature(json, "defaultConfiguration", false);
	}
	
	@Test
	private void displayNameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("displayName");
		addFeature(json, "displayName", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("displayName", "");
		addFeature(json, "displayName", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("displayName", JSON.NULL);
		addFeature(json, "displayName", false);
	}
	
	@Test
	private void storeProductIdsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		json.remove("storeProductIds");
		addFeature(json, "storeProductIds", true);
		
		json = new JSONObject(purchaseOptions);
		json.put("storeProductIds", new JSONArray());
		addFeature(json, "storeProductIds", false);
		
		json = new JSONObject(purchaseOptions);
		json.put("storeProductIds", JSON.NULL);
		addFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "store1");
		storeProdId.put("productId", "123");
		storeProductIdsArr.add(storeProdId);
		json.put("storeProductIds", storeProductIdsArr);
		addFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField2() throws IOException, JSONException{
		JSONObject json = new JSONObject(purchaseOptions);
		JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId = new JSONObject();
		storeProdId.put("storeType", "Apple App Store");
		storeProdId.put("productId", "");
		storeProductIdsArr.add(storeProdId);
		json.put("storeProductIds", storeProductIdsArr);
		addFeature(json, "storeProductIds", true);
	}
	
	@Test
	private void illegalStoreProductIdsField3() throws IOException, JSONException{
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
		addFeature(json, "storeProductIds", true);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
