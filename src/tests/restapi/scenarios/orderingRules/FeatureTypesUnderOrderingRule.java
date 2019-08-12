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

public class FeatureTypesUnderOrderingRule {
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
	private String orderingRuleMtxID;
	
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
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, featureID, sessionToken);

	}
	

	
	@Test (description = "Add feature under orderingRule")
	public void featureUnderOrderingRule() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		
		String response = f.addFeature(seasonID, feature, orderingRuleID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added feature to orderingRule: " + response);

		response = f.addFeature(seasonID, feature, orderingRuleMtxID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added feature to orderingRule mtx: " + response);

	}
	
	@Test (description = "Add MTX to orderingRule configuration")
	public void orderingRuleUnderMTX() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, feature, orderingRuleID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added mtx to orderingRule: " + response);
		
		response = f.addFeature(seasonID, feature, orderingRuleMtxID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added mtx to orderingRule mtx: " + response);

	}
	
	@Test (description = "Add config rule to orderingRule configuration")
	public void orderingRuleUnderCR() throws JSONException, IOException{
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, configRule, orderingRuleID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added configuration rule to orderingRule: " + response);
		
		response = f.addFeature(seasonID, configRule, orderingRuleMtxID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added configuration rle to orderingRule mtx: " + response);

	}
	
	@Test (description = "Add mtx config rule to orderingRule configuration")
	public void orderingRuleUnderMTXCR() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String response = f.addFeature(seasonID, feature, orderingRuleID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added configuration rule to orderingRule: " + response);

		response = f.addFeature(seasonID, feature, orderingRuleMtxID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added configuration rle to orderingRule mtx: " + response);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
