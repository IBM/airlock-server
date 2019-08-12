package tests.restapi.validations.orderingRules;

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

public class OrderingRulesUniqueName {
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
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "F1");
		featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		

	}
	
	@Test (description = "Non unique feature/orderingRule name in create")
	public void addFeature() throws JSONException, IOException{
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "F1");
		jsonOR.put("namespace", "F1");
		String response = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Ordering rule created");
	}
	
	@Test (description = "Non unique feature/orderingRule name in update")
	public void updateFeature() throws JSONException, IOException{
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't create Ordering rule: " + orderingRuleID);
		
		jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		jsonOR.put("name", "F1");
		jsonOR.put("namespace", "F1");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Ordering rule updated");
		
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
