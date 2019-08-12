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

public class FeatureNamesInOrderingRuleRuntime {
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
	private String childID1;
	private String childID2;
	
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

	}
	
	@Test (description = "Add orderingRule")
	public void addOrderingRule() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		//F1 -> F2, MIX, F3, OR1
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		feature.put("namespace", "childOfF1");
		
		feature.put("name", "F2");
		childID1 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		feature.put("name", "F3");
		childID2 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeature(seasonID, mtx, featureID, sessionToken);
		
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.0");
		configJson.put(childID2, "2.0");
		configJson.put(mtxID, "3");
		jsonOR.put("configuration", configJson.toString());
		
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONObject orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").contains("childOfF1.F2"), "F2 name was not found in orderingRule configuration");	
		Assert.assertTrue(orderingRule.getString("configuration").contains("childOfF1.F3"), "F3 name was not found in orderingRule configuration");
		Assert.assertTrue(orderingRule.getString("configuration").contains("mx."+mtxID), "mtx name was not found in orderingRule configuration");
	}
	
	@Test (dependsOnMethods="addOrderingRule", description = "Update feature names")
	public void updateFeatureNames() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();


		String childID1Feature = f.getFeature(childID1, sessionToken);
		JSONObject child1Json = new JSONObject(childID1Feature);
		child1Json.put("namespace", "newChildOfF1");
		child1Json.put("name", "newF2");
		String response = f.updateFeature(seasonID, childID1, child1Json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Can't update child1 " + response);
		
		String childID2Feature = f.getFeature(childID2, sessionToken);
		JSONObject child2Json = new JSONObject(childID2Feature);
		child2Json.put("namespace", "newChildOfF1");
		child2Json.put("name", "newF3");
		response = f.updateFeature(seasonID, childID2, child2Json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Can't update child2 " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONObject orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").contains("newChildOfF1.newF2"), "newF2 name was not found in orderingRule configuration");	
		Assert.assertTrue(orderingRule.getString("configuration").contains("newChildOfF1.newF3"), "newF3 name was not found in orderingRule configuration");

	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
