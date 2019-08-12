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
import tests.restapi.SeasonsRestApi;

public class DuplicateSeasonWithOrderingRule {
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
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);

		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
	}
	

	
	@Test (description = "Add feature to orderingRule configuration")
	public void featureUnderOrderingRule() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID2 = f.addFeature(seasonID, json.toString(), featureID, sessionToken);
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, mtx, featureID, sessionToken);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID3 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);

		JSONObject jsonOR = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.5");
		configJson.put(childID2, "2.5");
		configJson.put(mtxID, "0.5");
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule  " + response);

	}
	
	@Test (dependsOnMethods="featureUnderOrderingRule", description = "Add season")
	public void orderingRuleUnderMTX() throws JSONException, IOException{
		String season = "{\"minVersion\":\"5.0\"}";
		String seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "can't add season2: "  +seasonID2);

		JSONArray parentFeaturesNew = f.getFeaturesBySeason(seasonID2, sessionToken);
		JSONObject orderingRules = parentFeaturesNew.getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);
		String configuration = orderingRules.getString("configuration");
		JSONArray featuresNew = parentFeaturesNew.getJSONObject(0).getJSONArray("features");
		for (int i=0; i<featuresNew.size(); i++ ){
			Assert.assertTrue(configuration.contains(featuresNew.getJSONObject(i).getString("uniqueId")), "Feature " + i + " not found in configuration");
		}
		
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
