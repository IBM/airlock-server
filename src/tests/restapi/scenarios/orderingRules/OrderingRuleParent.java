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

public class OrderingRuleParent {
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
	}
	
	@Test (description = "Create orderingRule under ROOT")
	public void orderingRuleUnderRoot() throws IOException, JSONException{
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Created Ordering rule under ROOT ");

	}
	
	@Test (description = "Create orderingRule under feature")
	public void orderingRuleUnderFeature() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't create Ordering rule under another feature: " + response);

	}
	
	@Test (description = "Create orderingRule under MTX")
	public void orderingRuleUnderMTX() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		
		Assert.assertFalse(response.contains("error") || response.contains("Invalid") , "Can't create Ordering rule under another feature mtx: " + response);

	}
	
	@Test (description = "Create orderingRule under configuration rule")
	public void orderingRuleUnderCR() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = f.addFeature(seasonID, configRule, featureID2, sessionToken);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), configID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't create Ordering rule under configuration rule: " + response);

	}
	
	@Test (description = "Create orderingRule under MTX of configuration rules")
	public void orderingRuleUnderMTXCR() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, feature, featureID, sessionToken);

		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't create Ordering rule under mtx of configuration rules: " + response);

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
