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

public class CheckoutMTXWithOrderingRuleSibling {
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

	//ROOT->MTX->F1, F2, OR1
	@Test (description ="Add mtx, features, ordering rule and check one feature out") 
	public void test1 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);

		//add features 
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		configJson.put(featureID0, "1.4");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);

		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);

		JSONObject orderingRuleObj = new JSONObject(orderingRule); 

		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule is not checked out");
	}

	//ROOT->MTX->OR1, OR2, ORMTX->OR3, OR4
	@Test (description ="Add mtx, ordering rules and check the mtx out") 
	public void test2 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);

		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleMtxID.contains("error"), "Can't create orderingRule mtx " + orderingRuleID1);


		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID3 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID3.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID4 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID4.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String response = br.checkoutFeature(branchID, parentMTXId, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);


		//verify that they are all checked out
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 1 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 2 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID3, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 3 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID4, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 4 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleMtxID, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule mtx is not checked out");

		String mtx = f.getFeatureFromBranch(parentMTXId, branchID, sessionToken);
		JSONObject mtxObj = new JSONObject(mtx); 
		Assert.assertTrue(mtxObj.getString("branchStatus").equals("CHECKED_OUT"), "MTX is not checked out");
	}

	//ROOT->MTX->OR1->OR2, MTX->OR3, F1, F2->OR4
	@Test (description ="Add feature structure to master and then checkout F1") 
	public void test3 () throws Exception {
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		parentMTXId = f.addFeature(seasonID, parent, "ROOT", sessionToken);

		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxId = f.addFeature(seasonID, parent, parentMTXId, sessionToken);
		Assert.assertFalse(mtxId.contains("error"), "Can't create mtx  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID3 = f.addFeature(seasonID, jsonOR.toString(), mtxId, sessionToken);
		Assert.assertFalse(orderingRuleID3.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, fJson.toString(), parentMTXId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season");

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID4 = f.addFeature(seasonID, jsonOR.toString(), featureID2, sessionToken);
		Assert.assertFalse(orderingRuleID4.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		//check out F1
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);


		//verify that they are all checked out
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 1 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 2 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID3, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 3 is not checked out");

		orderingRule = f.getFeatureFromBranch(orderingRuleID4, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule); 
		Assert.assertTrue(orderingRuleObj.getString("branchStatus").equals("CHECKED_OUT"), "Ordering rule 4 is not checked out");

		String mtxStr = f.getFeatureFromBranch(mtxId, branchID, sessionToken);
		JSONObject mtxObj = new JSONObject(mtxStr); 
		Assert.assertTrue(mtxObj.getString("branchStatus").equals("CHECKED_OUT"), "mtx is not checked out");

		String parentMtx = f.getFeatureFromBranch(parentMTXId, branchID, sessionToken);
		mtxObj = new JSONObject(parentMtx); 
		Assert.assertTrue(mtxObj.getString("branchStatus").equals("CHECKED_OUT"), "MTX is not checked out");
		
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "feature is not checked out");

		feature = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		featureObj = new JSONObject(feature); 
		Assert.assertTrue(featureObj.getString("branchStatus").equals("CHECKED_OUT"), "feature is not checked out");

		//verify that changing ordering rule master does not change it in branch
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule);
		String descInBranch = orderingRuleObj.getString("description");
		
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, "MASTER", sessionToken);
		orderingRuleObj = new JSONObject(orderingRule);
		String descInMaster = orderingRuleObj.getString("description");
		
		Assert.assertTrue(descInMaster.equals(descInBranch), "descriptions are not the same in master and in branch is not checked out");
		
		String newDescInMaster = descInMaster + "XXX";
		
		orderingRuleObj.put("description", newDescInMaster);

		response = f.updateFeatureInBranch(seasonID, "MASTER", orderingRuleID1, orderingRuleObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Ordering rule was not updated " + response);

		orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		orderingRuleObj = new JSONObject(orderingRule);
		String descInBranchAfterChange = orderingRuleObj.getString("description");
		
		orderingRule = f.getFeatureFromBranch(orderingRuleID1, "MASTER", sessionToken);
		orderingRuleObj = new JSONObject(orderingRule);
		String descInMasterAfterChange = orderingRuleObj.getString("description");
		
		Assert.assertFalse(descInMasterAfterChange.equals(descInBranchAfterChange), "descriptions are equal after change in the master");
		
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
