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

public class OrderingRuleChangeParent {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	
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
		
	}
	

	//F1 -> OR, F3; F2
	@Test (description = "Move OR from F1 to F2; Move F3 from F1 to F2")
	public void moveORtoDifferentParent() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String childID1 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5));

		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.0");
		
		jsonOR.put("configuration", configJson.toString());

		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule  " + orderingRuleID);
		
		//move child1 from F1 to F2
		
		JSONObject parent2 = new JSONObject(f.getFeature(featureID2, sessionToken));
		JSONObject child1 = new JSONObject(f.getFeature(childID1, sessionToken));
		JSONArray children = new JSONArray();
		children.add(child1);
		parent2.put("features", children);
		
		String response = f.updateFeature(seasonID, featureID2, parent2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Feature3 was moved to another parent");
		
		//move ordering rule from F1 to F2
		JSONObject or1 = new JSONObject(f.getFeature(orderingRuleID, sessionToken));
		children = new JSONArray();
		children.add(or1);
		parent2.put("features",  new JSONArray());	//remove child1 from F2
		parent2.put("orderingRules", children);		//add OR to F2
		response = f.updateFeature(seasonID, featureID2, parent2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "OR was moved to another parent");

	}
	
	
	//F1-> F2, OR1->OR2 point to F2. Move OR2 under F1
	@Test (description = "Add features")
	public void moveORtoUpperLevelSameParent() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);
		
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5));

		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
		
		//move OR2 from OR1 to F1
		
		JSONObject parent1 = new JSONObject(f.getFeature(featureID1, sessionToken));
		JSONObject or2 = new JSONObject(f.getFeature(orderingRuleID2, sessionToken));
		JSONObject or1 = new JSONObject(f.getFeature(orderingRuleID1, sessionToken));
		or1.put("orderingRules", new JSONArray());
		JSONArray children = new JSONArray();
		children.add(or2);
		children.add(or1);
		parent1.put("orderingRules", children);
		
		String response = f.updateFeature(seasonID, featureID1, parent1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to an upper level of the same parent: " + response);
		
	}
	

	//F1-> F2, ORMTX->OR2 point to F2. Move OR2 under F1
	@Test (description = "Add features")
	public void moveORFromMtxToUpperLevelSameParent() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleID1 = f.addFeature(seasonID, orderingRuleMtx, featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5));

		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
		
		//move OR2 from OR1 to F1
		
		JSONObject parent1 = new JSONObject(f.getFeature(featureID1, sessionToken));
		JSONObject or2 = new JSONObject(f.getFeature(orderingRuleID2, sessionToken));
		JSONObject ormtx = new JSONObject(f.getFeature(orderingRuleID1, sessionToken));
		ormtx.put("orderingRules", new JSONArray());
		JSONArray children = new JSONArray();
		
		children.add(or2);
		children.add(ormtx);
		parent1.put("orderingRules", children);
	
		String response = f.updateFeature(seasonID, featureID1, parent1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to an upper level of the same parent: " + response);
		
	}	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
