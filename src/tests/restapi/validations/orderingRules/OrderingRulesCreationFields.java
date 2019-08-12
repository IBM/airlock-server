package tests.restapi.validations.orderingRules;

import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
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

public class OrderingRulesCreationFields {
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

	}

	
	@Test
	private void noCachedResultsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("noCachedResults");
		addFeature(json, "noCachedResults", false);
		
		json = new JSONObject(orderingRule);
		json.put("noCachedResults", "");
		addFeature(json, "noCachedResults", true);
		
		json = new JSONObject(orderingRule);
		json.put("noCachedResults", JSON.NULL);
		addFeature(json, "noCachedResults", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("uniqueId");
		addFeature(json, "uniqueId", false);
		
		json = new JSONObject(orderingRule);
		json.put("uniqueId", "");
		addFeature(json, "uniqueId", true);
		
		json = new JSONObject(orderingRule);
		json.put("uniqueId", JSON.NULL);
		addFeature(json, "uniqueId", false);
	}

	@Test
	private void enabledField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("enabled");
		addFeature(json, "enabled", true);
		
		json = new JSONObject(orderingRule);
		json.put("enabled", "");
		addFeature(json, "enabled", true);
		
		json = new JSONObject(orderingRule);
		json.put("enabled", JSON.NULL);
		addFeature(json, "enabled", true);
	}

	@Test
	private void typeField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("type");
		addFeature(json, "type", true);
		
		json = new JSONObject(orderingRule);
		json.put("type", "");
		addFeature(json, "type", true);
		
		json = new JSONObject(orderingRule);
		json.put("type", JSON.NULL);
		addFeature(json, "type", true);
	}
	
	
	@Test
	private void stageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("stage");
		addFeature(json, "stage", true);
		
		json = new JSONObject(orderingRule);
		json.put("stage", "");
		addFeature(json, "stage", true);
		
		json = new JSONObject(orderingRule);
		json.put("stage", JSON.NULL);
		addFeature(json, "stage", true);
	}
	
	@Test
	private void namespaceField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("namespace");
		addFeature(json, "namespace", true);
		
		json = new JSONObject(orderingRule);
		json.put("namespace", "");
		addFeature(json, "namespace", true);
		
		json = new JSONObject(orderingRule);
		json.put("namespace", JSON.NULL);
		addFeature(json, "namespace", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("creator");
		addFeature(json, "creator", true);
		
		json = new JSONObject(orderingRule);
		json.put("creator", "");
		addFeature(json, "creator", true);
		
		json = new JSONObject(orderingRule);
		json.put("creator", JSON.NULL);
		addFeature(json, "creator", true);
	}
	
	
	@Test
	private void ownerField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("owner");
		addFeature(json, "owner", false);
		
		json = new JSONObject(orderingRule);
		json.put("owner", "");
		addFeature(json, "owner", false);
		
		json = new JSONObject(orderingRule);
		json.put("owner", JSON.NULL);
		addFeature(json, "owner", false);
	}
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("description");
		addFeature(json, "description", false);
		
		json = new JSONObject(orderingRule);
		json.put("description", "");
		addFeature(json, "description", false);
		
		json = new JSONObject(orderingRule);
		json.put("description", JSON.NULL);
		addFeature(json, "description", false);
	}
	
	@Test
	private void ruleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("rule");
		addFeature(json, "rule", true);
		
		json = new JSONObject(orderingRule);
		json.put("rule", "");
		addFeature(json, "rule", true);
		
		json = new JSONObject(orderingRule);
		json.put("rule", JSON.NULL);
		addFeature(json, "rule", true);
	}
	
	@Test
	private void ruleStringField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "");
		json.put("rule", ruleString);
		addFeature(json, "rule", false);
		
		json = new JSONObject(orderingRule);
		ruleString.put("ruleString", JSON.NULL);
		json.put("rule", ruleString);
		addFeature(json, "rule", false);
	}

	@Test
	private void minAppVersionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("minAppVersion");
		addFeature(json, "minAppVersion", true);
		
		json = new JSONObject(orderingRule);
		json.put("minAppVersion", "");
		addFeature(json, "minAppVersion", true);
		
		json = new JSONObject(orderingRule);
		json.put("minAppVersion", JSON.NULL);
		addFeature(json, "minAppVersion", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("name");
		addFeature(json, "name", true);
		
		json = new JSONObject(orderingRule);
		json.put("name", "");
		addFeature(json, "name", true);
		
		json = new JSONObject(orderingRule);
		json.put("name", JSON.NULL);
		addFeature(json, "name", true);
	}

	
	@Test
	private void orderingRuleField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("orderingRule");
		addFeature(json, "orderingRule", false);
		
		json = new JSONObject(orderingRule);
		json.put("orderingRule", "");
		addFeature(json, "orderingRule", false);
		
		json = new JSONObject(orderingRule);
		json.put("orderingRule", JSON.NULL);
		addFeature(json, "orderingRule", false);
	}
	
	@Test
	private void configurationField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("configuration");
		addFeature(json, "configuration", true);
		
		json = new JSONObject(orderingRule);
		json.put("configuration", "");
		addFeature(json, "configuration", true);
		
		json = new JSONObject(orderingRule);
		json.put("configuration", JSON.NULL);
		addFeature(json, "configuration", true);
	}

	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("internalUserGroups");
		addFeature(json, "internalUserGroups", false);
		
		json = new JSONObject(orderingRule);
		json.put("internalUserGroups", "");
		addFeature(json, "internalUserGroups", true);
		
		json = new JSONObject(orderingRule);
		json.put("internalUserGroups", JSON.NULL);
		addFeature(json, "internalUserGroups", false);
	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("rolloutPercentage");
		addFeature(json, "rolloutPercentage", true);
		
		json = new JSONObject(orderingRule);
		json.put("rolloutPercentage", "");
		addFeature(json, "rolloutPercentage", true);
		
		json = new JSONObject(orderingRule);
		json.put("rolloutPercentage", JSON.NULL);
		addFeature(json, "rolloutPercentage", true);
	}	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		JSONObject json = new JSONObject(orderingRule);
		json.remove("lastModified");
		addFeature(json, "lastModified", false);
		
		json = new JSONObject(orderingRule);
		json.put("lastModified", "");
		addFeature(json, "lastModified", true);
		
		json = new JSONObject(orderingRule);
		json.put("lastModified", JSON.NULL);
		addFeature(json, "lastModified", false);
	}	
	
	private void addFeature(JSONObject json, String field, boolean expectedResult) throws JSONException{
		if (!field.equals("name"))
			json.put("name", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
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
