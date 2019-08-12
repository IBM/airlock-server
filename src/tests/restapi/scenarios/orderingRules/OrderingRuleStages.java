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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class OrderingRuleStages {
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
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	

	//F1 -> F2, F3, OR
	@Test (description = "Add feature to orderingRule configuration")
	public void orderingRuleUnderFeature() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		//all features in production
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		
		json.put("name", "parent"+RandomStringUtils.randomAlphabetic(3));		
		String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
		
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID2 = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
		
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.5");
		configJson.put(childID2, "2.5");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), parentID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't create orderingRule  " + orderingRuleID);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(responseProd.message.contains(orderingRuleID), "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		//move ordering rule to development
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertFalse(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//move ordering rule to production
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//move parent feature to development
		
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String parentFeature = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parentFeature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertFalse(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");


	}
	

	@Test (description = "Add feature to orderingRule configuration")
	public void orderingRuleUnderMtx() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		//all features in production
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		
		json.put("name", "parent"+RandomStringUtils.randomAlphabetic(3));		
		String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, mtx, parentID, sessionToken);

		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		
		
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.5");
		configJson.put(childID2, "2.5");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't create orderingRule  " + orderingRuleID);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(responseProd.message.contains(orderingRuleID), "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		//move ordering rule to development
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertFalse(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		//move ordering rule to production
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);
		jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		//move parent feature to development
		
		dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String parentFeature = f.getFeature(parentID, sessionToken);
		json = new JSONObject(parentFeature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);
		
		f.setSleep();
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(responseDev.message.contains(orderingRuleID), "Ordering rule was not found in development runtime file");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertFalse(responseProd.message.contains(orderingRuleID), "Ordering rule was found in production runtime file");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
