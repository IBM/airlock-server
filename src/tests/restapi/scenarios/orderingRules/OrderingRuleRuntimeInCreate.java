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

public class OrderingRuleRuntimeInCreate {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
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
	

	
	@Test (description = "Add prod orderingRule")
	public void prodOrderingRule() throws JSONException, IOException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature); 
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));		
		jsonF.put("stage", "PRODUCTION");
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error") || featureID.contains("Invalid") , "Can't create feature: " + featureID	);

		
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonOR.put("stage", "PRODUCTION");
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(featureFound(responseProd.message)==1, "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	
	@Test (description = "Add prod MTX orderingRule")
	public void prodMtxOrderingRule() throws JSONException, IOException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature); 
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));		
		jsonF.put("stage", "PRODUCTION");
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error") || featureID.contains("Invalid") , "Can't create feature: " + featureID	);

		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, featureID, sessionToken);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonOR.put("stage", "PRODUCTION");
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureUnderMtxFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(featureUnderMtxFound(responseProd.message)==1, "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	private int featureFound(String content) throws JSONException{
		JSONObject root = RuntimeDateUtilities.getFeaturesList(content);
		JSONArray orderingRules = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules");
		return orderingRules.size();
	}
	
	private int featureUnderMtxFound(String content) throws JSONException{
		JSONObject root = RuntimeDateUtilities.getFeaturesList(content);
		JSONArray orderingRules = root.getJSONArray("features")
				.getJSONObject(0).getJSONArray("orderingRules")
				.getJSONObject(0).getJSONArray("orderingRules");
		return orderingRules.size();
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
