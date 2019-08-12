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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;

public class CheckoutAndCancelCheckoutMTXWithOrderingRule {
	protected String productID;
	protected String seasonID;
	private String branchID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private JSONObject fJson;
	private String parentMTXId;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		fJson = new JSONObject(feature);	

		branchID = addBranch("branch1");
	}


	//ROOT->MTX->(OR1, OR2), MTX(F0, F1), MTX(F3, F4)
	@Test (description ="Add mtx, features, ordering rule and check one feature out") 
	public void test1 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);

		String parentMTXId1 = f.addFeature(seasonID, parent, parentMTXId, sessionToken);
		
		//add features to first mtx group
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), parentMTXId1, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), parentMTXId1, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		
		String parentMTXId2 = f.addFeature(seasonID, parent, parentMTXId, sessionToken);
		
		//add features to second mtx group
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID3 = f.addFeature(seasonID, fJson.toString(), parentMTXId2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID4 = f.addFeature(seasonID, fJson.toString(), parentMTXId2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to the season");

		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);

		//validate that all features were checked out
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule1 is not checked out");
		
		orderingRule = f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule2 is not checked out");
		
		//feature1
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out");
		//feature3
		feature = f.getFeatureFromBranch(featureID3, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out");
		//feature 4
		feature = f.getFeatureFromBranch(featureID3, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out");

		
		//cancel checkout features
		br.cancelCheckoutFeature(branchID, featureID0, sessionToken);
		br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		br.cancelCheckoutFeature(branchID, featureID3, sessionToken);
		br.cancelCheckoutFeature(branchID, featureID4, sessionToken);
		br.cancelCheckoutFeature(branchID, parentMTXId, sessionToken);
		br.cancelCheckoutFeature(branchID, parentMTXId1, sessionToken);
		br.cancelCheckoutFeature(branchID, parentMTXId2, sessionToken);
		
		//checkout feature again
		br.checkoutFeature(branchID, featureID0, sessionToken);
		
		
		//feature1
		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out 2");
		//feature3
		feature = f.getFeatureFromBranch(featureID3, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out 2");
		//feature 4
		feature = f.getFeatureFromBranch(featureID3, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "Feature in mtx is not checked out 2");

		
	}
	
	
	@Test (description ="Add mtx, features, ordering rule and check out mtx") 
	public void test2 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);
	
		//add features to first mtx group
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		

		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		//checkout mtx
		String response = br.checkoutFeature(branchID, parentMTXId, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);

		//validate that all ordering rules were checked out
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule1 is not checked out");
		
		orderingRule = f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule2 is not checked out");

		
		//cancel checkout features
		br.cancelCheckoutFeature(branchID, parentMTXId, sessionToken);
	
		//validate that all features checkout is cancelled
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("NONE"), "Ordering rule1 is not cancelled");
		
		orderingRule = f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("NONE"), "Ordering rule2 is not cancelled");
		
	}

	
	@Test (description ="Add mtx, features, ordering rule and check one mtx, then delete OR from master, then delete F1 from master ") 
	public void test3 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);
	
		//add features to first mtx group
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		

		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		//String configuration = "{\"" + featureID1 + "\": 1.5 }";
		jsonOR.put("configuration", configJson.toString());
		
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		//checkout mtx
		String response = br.checkoutFeature(branchID, parentMTXId, sessionToken);
		Assert.assertFalse(response.contains("error"), "mtx was not checked out to branch: " + response);

		//validate that ordering rules is checked out
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule1 is not checked out");

		//delete ordering rule from master
		int code = f.deleteFeature(orderingRuleID1, sessionToken);
		Assert.assertTrue(code==200, "ordering rule was not deleted from master");
						
		//validate that ordering rules turned to new
		String mtxStr = f.getFeatureFromBranch(parentMTXId, branchID, sessionToken);
		JSONObject mtxObj = new JSONObject(mtxStr);		
		Assert.assertTrue(mtxObj.getJSONArray("orderingRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW" );
		
		//new uniqueId is assigned
		Assert.assertFalse(mtxObj.getJSONArray("orderingRules").getJSONObject(0).getString("uniqueId").equals(orderingRuleID1), "Deleted from master configuration rule uniqueId was not changed" );

		//try deleting f1 from master
		code = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(code==400, "feature was deleted from master even though an ordering rule in branch is reffering to it.");
		
	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}




	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
