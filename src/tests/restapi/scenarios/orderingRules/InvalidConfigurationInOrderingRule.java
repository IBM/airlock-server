package tests.restapi.scenarios.orderingRules;

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
import tests.restapi.ProductsRestApi;

public class InvalidConfigurationInOrderingRule {
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
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
	}
	

	
	@Test (description = "Add feature that is not direct child to orderingRule configuration")
	public void featureUnderOrderingRule() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID2 = f.addFeature(seasonID, json.toString(), childID1, sessionToken);
		Assert.assertFalse(childID2.contains("error"), "Can't add feature: " + childID2);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		JSONObject configJson = new JSONObject();
		configJson.put(childID2, "1.5");
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added feature that is not a direct child to orderingRule configuration: " + response);

	}
	
	@Test (description = "Create orderingRule under MTX")
	public void orderingRuleUnderMTX() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		
		String featureMtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, featureMtx, childID1, sessionToken);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.5");
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added mtx that is not a direct child to orderingRule configuration: " + response);

	}
	
	@Test (description = "Create orderingRule under configuration rule")
	public void orderingRuleUnderCR() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);

		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID, configRule, childID1, sessionToken);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		JSONObject configJson = new JSONObject();
		configJson.put(configID, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added configuration rule to orderingRule configuration: " + response);

	}
	
	@Test (description = "Create orderingRule under MTX of configuration rules")
	public void orderingRuleUnderMTXCR() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);

		String featureMtx = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, featureMtx, childID1, sessionToken);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Added mtx configuration rule to orderingRule configuration: " + response);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
