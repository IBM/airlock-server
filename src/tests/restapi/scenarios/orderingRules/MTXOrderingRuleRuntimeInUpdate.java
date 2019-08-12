package tests.restapi.scenarios.orderingRules;

import java.io.IOException;









import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class MTXOrderingRuleRuntimeInUpdate {
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
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

	}
	

	
	@Test (description = "Add dev orderingRule")
	public void devOrderingRule() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, featureID, sessionToken);

		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="devOrderingRule", description = "Move orderingRule to prod")
	public void prodOrderingRule() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(featureFound(responseProd.message)==1, "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	
	@Test (dependsOnMethods="prodOrderingRule", description = "Move orderingRule to dev")
	public void updateOrderingRuleToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(featureFound(responseProd.message)==0, "Ordering rule was found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	private int featureFound(String content) throws JSONException{
		JSONObject root = RuntimeDateUtilities.getFeaturesList(content);
		JSONArray features = root.getJSONArray("features");
		if (features == null || features.size() == 0)
			return 0;
		
		JSONArray orderingRules1 = features
				.getJSONObject(0).getJSONArray("orderingRules");
		
		if (orderingRules1 == null || orderingRules1.size() == 0)
			return 0;
		
		JSONArray orderingRules2 = orderingRules1
				.getJSONObject(0).getJSONArray("orderingRules");
		
		if (orderingRules2 == null)
			return 0;
		
		return orderingRules2.size();
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
