package tests.restapi.validations.orderingRules;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class OrderingRulesUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String orderingRule;
	protected String orderingRuleID;
	protected String featureID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		orderingRuleID = f.addFeature(seasonID, orderingRule, featureID, sessionToken);
		Assert.assertFalse (orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		
	}

	
	@Test
	private void noCachedResultsField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("noCachedResults");
		updateFeature(json, "noCachedResults", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("noCachedResults", "");
		updateFeature(json, "noCachedResults", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("noCachedResults", JSON.NULL);
		updateFeature(json, "noCachedResults", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("uniqueId");
		updateFeature(json, "uniqueId", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("uniqueId", "");
		updateFeature(json, "uniqueId", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("uniqueId", JSON.NULL);
		updateFeature(json, "uniqueId", false);
	}

	@Test
	private void enabledField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("enabled");
		updateFeature(json, "enabled", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("enabled", "");
		updateFeature(json, "enabled", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("enabled", JSON.NULL);
		updateFeature(json, "enabled", true);
	}

	@Test
	private void typeField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("type");
		updateFeature(json, "type", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("type", "");
		updateFeature(json, "type", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("type", JSON.NULL);
		updateFeature(json, "type", true);
	}
	
	
	@Test
	private void stageField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("stage");
		updateFeature(json, "stage", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("stage", "");
		updateFeature(json, "stage", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("stage", JSON.NULL);
		updateFeature(json, "stage", true);
	}
	
	@Test
	private void namespaceField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("namespace");
		updateFeature(json, "namespace", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("namespace", "");
		updateFeature(json, "namespace", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("namespace", JSON.NULL);
		updateFeature(json, "namespace", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("creator");
		updateFeature(json, "creator", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("creator", "");
		updateFeature(json, "creator", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("creator", JSON.NULL);
		updateFeature(json, "creator", true);
	}
	
	
	@Test
	private void ownerField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("owner");
		updateFeature(json, "owner", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("owner", "");
		updateFeature(json, "owner", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("owner", JSON.NULL);
		updateFeature(json, "owner", false);
	}
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("description");
		updateFeature(json, "description", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("description", "");
		updateFeature(json, "description", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("description", JSON.NULL);
		updateFeature(json, "description", false);
	}
	
	@Test
	private void ruleField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("rule");
		updateFeature(json, "rule", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("rule", "");
		updateFeature(json, "rule", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("rule", JSON.NULL);
		updateFeature(json, "rule", true);
	}
	
	@Test
	private void ruleStringField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		ruleString.put("ruleString", JSON.NULL);
		json.put("rule", ruleString);
		updateFeature(json, "rule", false);
	}

	@Test
	private void minAppVersionField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("minAppVersion");
		updateFeature(json, "minAppVersion", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("minAppVersion", "");
		updateFeature(json, "minAppVersion", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("minAppVersion", JSON.NULL);
		updateFeature(json, "minAppVersion", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("name");
		updateFeature(json, "name", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("name", "");
		updateFeature(json, "name", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("name", JSON.NULL);
		updateFeature(json, "name", true);
	}

	
	@Test
	private void orderingRuleField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("orderingRule");
		updateFeature(json, "orderingRule", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("orderingRule", "");
		updateFeature(json, "orderingRule", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("orderingRule", JSON.NULL);
		updateFeature(json, "orderingRule", false);
	}
	
	@Test
	private void configurationField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("configuration");
		updateFeature(json, "configuration", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("configuration", "");
		updateFeature(json, "configuration", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("configuration", JSON.NULL);
		updateFeature(json, "configuration", true);
	}

	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("internalUserGroups");
		updateFeature(json, "internalUserGroups", false);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("internalUserGroups", "");
		updateFeature(json, "internalUserGroups", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("internalUserGroups", JSON.NULL);
		updateFeature(json, "internalUserGroups", false);
	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("rolloutPercentage");
		updateFeature(json, "rolloutPercentage", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("rolloutPercentage", "");
		updateFeature(json, "rolloutPercentage", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("rolloutPercentage", JSON.NULL);
		updateFeature(json, "rolloutPercentage", true);
	}	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		JSONObject json = new JSONObject(orderingRule);
		json.remove("lastModified");
		updateFeature(json, "lastModified", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("lastModified", "");
		updateFeature(json, "lastModified", true);
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		json = new JSONObject(orderingRule);
		json.put("lastModified", JSON.NULL);
		updateFeature(json, "lastModified", true);
	}	
	
	private void updateFeature(JSONObject json, String field, boolean expectedResult) throws JSONException{
		try {
			String response = f.updateFeature(seasonID, orderingRuleID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed " + response + " for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
