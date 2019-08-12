package tests.restapi.validations.inAppPurchase;

import java.io.IOException;

import org.apache.wink.json4j.JSON;
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

public class InAppPurchaseUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String inAppPurchase;
	protected String inAppPurchaseID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchase, "ROOT", sessionToken);
		Assert.assertFalse (inAppPurchaseID.contains("error"), "Can't add orderingRule: " + inAppPurchaseID);
		
		
	}

	
	@Test
	private void noCachedResultsField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("noCachedResults");
		updateFeature(json, "noCachedResults", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("noCachedResults", "");
		updateFeature(json, "noCachedResults", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("noCachedResults", JSON.NULL);
		updateFeature(json, "noCachedResults", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("uniqueId");
		updateFeature(json, "uniqueId", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("uniqueId", "");
		updateFeature(json, "uniqueId", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("uniqueId", JSON.NULL);
		updateFeature(json, "uniqueId", false);
	}

	@Test
	private void enabledField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("enabled");
		updateFeature(json, "enabled", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("enabled", "");
		updateFeature(json, "enabled", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("enabled", JSON.NULL);
		updateFeature(json, "enabled", true);
	}

	@Test
	private void typeField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("type");
		updateFeature(json, "type", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("type", "");
		updateFeature(json, "type", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("type", JSON.NULL);
		updateFeature(json, "type", true);
	}
	
	
	@Test
	private void stageField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("stage");
		updateFeature(json, "stage", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("stage", "");
		updateFeature(json, "stage", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("stage", JSON.NULL);
		updateFeature(json, "stage", true);
	}
	
	@Test
	private void namespaceField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("namespace");
		updateFeature(json, "namespace", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("namespace", "");
		updateFeature(json, "namespace", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("namespace", JSON.NULL);
		updateFeature(json, "namespace", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("creator");
		updateFeature(json, "creator", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("creator", "");
		updateFeature(json, "creator", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("creator", JSON.NULL);
		updateFeature(json, "creator", true);
	}
	
	
	@Test
	private void ownerField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("owner");
		updateFeature(json, "owner", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("owner", "");
		updateFeature(json, "owner", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("owner", JSON.NULL);
		updateFeature(json, "owner", false);
	}
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("description");
		updateFeature(json, "description", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("description", "");
		updateFeature(json, "description", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("description", JSON.NULL);
		updateFeature(json, "description", false);
	}
	
	@Test
	private void ruleField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("rule");
		updateFeature(json, "rule", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("rule", "");
		updateFeature(json, "rule", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("rule", JSON.NULL);
		updateFeature(json, "rule", true);
	}
	
	@Test
	private void ruleStringField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		ruleString.put("ruleString", JSON.NULL);
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
	}

	@Test
	private void minAppVersionField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("minAppVersion");
		updateFeature(json, "minAppVersion", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("minAppVersion", "");
		updateFeature(json, "minAppVersion", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("minAppVersion", JSON.NULL);
		updateFeature(json, "minAppVersion", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("name");
		updateFeature(json, "name", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("name", "");
		updateFeature(json, "name", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("name", JSON.NULL);
		updateFeature(json, "name", true);
	}

	
	@Test
	private void orderingRuleField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("orderingRule");
		updateFeature(json, "orderingRule", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("orderingRule", "");
		updateFeature(json, "orderingRule", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("orderingRule", JSON.NULL);
		updateFeature(json, "orderingRule", false);
	}
	
	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("internalUserGroups");
		updateFeature(json, "internalUserGroups", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("internalUserGroups", "");
		updateFeature(json, "internalUserGroups", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("internalUserGroups", JSON.NULL);
		updateFeature(json, "internalUserGroups", false);
	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("rolloutPercentage");
		updateFeature(json, "rolloutPercentage", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("rolloutPercentage", "");
		updateFeature(json, "rolloutPercentage", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("rolloutPercentage", JSON.NULL);
		updateFeature(json, "rolloutPercentage", true);
	}	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("lastModified");
		updateFeature(json, "lastModified", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("lastModified", "");
		updateFeature(json, "lastModified", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("lastModified", JSON.NULL);
		updateFeature(json, "lastModified", true);
	}	
	
	private void updateFeature(JSONObject json, String field, boolean expectedFailure) throws JSONException{
		try {
			String response = purchasesApi.updatePurchaseItem(seasonID, inAppPurchaseID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedFailure,  "Test failed " + response + " for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	@Test
	private void defaultIfAirlockSystemIsDownField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("defaultIfAirlockSystemIsDown");
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("defaultIfAirlockSystemIsDown", "");
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("defaultIfAirlockSystemIsDown", JSON.NULL);
		updateFeature(json, "defaultIfAirlockSystemIsDown", true);
	}
	
	@Test
	private void additionalInfoField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("additionalInfo");
		updateFeature(json, "additionalInfo", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("additionalInfo", new JSONObject());
		updateFeature(json, "additionalInfo", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("additionalInfo", JSON.NULL);
		updateFeature(json, "additionalInfo", false);
	}
	
	@Test
	private void configurationSchemaField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("configurationSchema");
		updateFeature(json, "configurationSchema", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("configurationSchema", JSON.NULL);
		updateFeature(json, "configurationSchema", false);
	}
	
	@Test
	private void defaultConfigurationField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("defaultConfiguration");
		updateFeature(json, "defaultConfiguration", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("defaultConfiguration", new JSONObject());
		updateFeature(json, "defaultConfiguration", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("defaultConfiguration", JSON.NULL);
		updateFeature(json, "defaultConfiguration", false);
	}
	
	@Test
	private void displayNameField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("displayName");
		updateFeature(json, "displayName", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("displayName", "");
		updateFeature(json, "displayName", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("displayName", JSON.NULL);
		updateFeature(json, "displayName", false);
	}
	
	@Test
	private void premiumField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("premium");
		updateFeature(json, "premium", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("premium", false);
		updateFeature(json, "premium", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("premium", JSON.NULL);
		updateFeature(json, "premium", false);
	}
	
	@Test
	private void premiumRuleField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("premiumRule");
		updateFeature(json, "premiumRule", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("premiumRule", new JSONObject());
		updateFeature(json, "premiumRule", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("premiumRule", JSON.NULL);
		updateFeature(json, "premiumRule", false);
	}
	
	@Test
	private void inAppPurchaseField() throws IOException, JSONException{
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		JSONObject json = new JSONObject(inAppPurchase);
		json.remove("entitlement");
		updateFeature(json, "entitlement", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("entitlement", "");
		updateFeature(json, "entitlement", false);
		
		inAppPurchase = purchasesApi.getPurchaseItem(inAppPurchaseID, sessionToken);
		json = new JSONObject(inAppPurchase);
		json.put("entitlement", JSON.NULL);
		updateFeature(json, "entitlement", false);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
