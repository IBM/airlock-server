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
import tests.restapi.ProductsRestApi;

public class DeleteFeatureReportedInOrderingRuleBranchAndMaster {
	protected String seasonID;
	protected String branchID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID5;
	private String orderingRuleID1;
	private String orderingRuleID2;
	private String mtxID;
	private String orderingRuleMtxID1;
	private String orderingRuleMtxID2;
	
	private BranchesRestApi br ;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		branchID = addBranch("branch1");
	}


	//F1 -> OR{F2, F3}, F3; F2 -> F4;F5;OR{F4}
	@Test (description = "Add features and ordering rules to master")
	public void addComponentsToMaster() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF1");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF2");
		featureID2 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add feature  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF3");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF4");
		featureID4 = f.addFeature(seasonID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF5");
		featureID5 = f.addFeature(seasonID, json.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR1");

		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR2");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), featureID2, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
	}

	@Test(dependsOnMethods = "addComponentsToMaster", description = "checkout feature with ordering rule")
	public void checkoutFeatureWithOrderingRule () throws Exception {
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);
	}

	@Test(dependsOnMethods = "checkoutFeatureWithOrderingRule", description = "try to delete checked out feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertNotEquals(respCode, 200, "checked out feature with ordering rule from master was deleted from master");
	}

	@Test(dependsOnMethods = "deleteFeatureWithOrderingRuleFromMaster", description = "checkout feature with ordering rule")
	public void cancelCheckoutFeatureWithOrderingRule () throws Exception {
		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not canceled checked out from branch: " + response);
	}

	@Test(dependsOnMethods = "cancelCheckoutFeatureWithOrderingRule", description = "delete feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster2 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "not checked out feature with ordering rule from master was not deleted from master");
	}

	//F1 -> OR{MTX, F3}, F3; MTX -> MTX,F4;F5;OR{F4, MTX}
	@Test (dependsOnMethods ="deleteFeatureWithOrderingRuleFromMaster2",description = "Add features, mtx and  ordering rules")
	public void reAddComponentsToMaster() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF11");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		String mtxStr = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mtxID = f.addFeature(seasonID, mtxStr, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add mtx  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF33");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF44");
		featureID4 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF55");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
				
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR11");

		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

				
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR22");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);

	}

	@Test(dependsOnMethods = "reAddComponentsToMaster", description = "checkout feature with ordering rule")
	public void checkoutFeatureWithOrderingRule2 () throws Exception {
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);
	}

	@Test(dependsOnMethods = "checkoutFeatureWithOrderingRule2", description = "try to delete checked out feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster3 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertNotEquals(respCode, 200, "checked out feature with ordering rule from master was deleted from master");
	}

	@Test(dependsOnMethods = "deleteFeatureWithOrderingRuleFromMaster3", description = "checkout feature with ordering rule")
	public void cancelCheckoutFeatureWithOrderingRule2 () throws Exception {
		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not canceled checked out from branch: " + response);
	}

	@Test(dependsOnMethods = "cancelCheckoutFeatureWithOrderingRule2", description = "delete feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster4 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "not checked out feature with ordering rule from master was not deleted from master");
	}

	////
	//F1 -> OR_MTX->OR{MTX, F3}, F3; MTX -> MTX,F4;F5;MTX_OR->OR{F4, MTX}
	@Test (dependsOnMethods ="deleteFeatureWithOrderingRuleFromMaster4",description = "Add features, mtx, ordering rules and  mtx ordering rules")
	public void reAddComponentsToMaster2() throws JSONException, IOException{
		//add features
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF11");
		featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Can't add feature  " + featureID1);

		String mtxStr = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mtxID = f.addFeature(seasonID, mtxStr, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Can't add mtx  " + featureID2);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF33");
		featureID3 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Can't add feature  " + featureID3);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF44");
		featureID4 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Can't add feature  " + featureID4);

		json.put("name", RandomStringUtils.randomAlphabetic(5)+"XXXF55");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Can't add feature  " + featureID5);


		//add ordering rules
		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		orderingRuleMtxID1 = f.addFeature(seasonID, orderingRuleMtx, featureID1, sessionToken);
		Assert.assertFalse(orderingRuleMtxID1.contains("error"), "Can't add ordering rule mtx  " + orderingRuleMtxID1);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR11");

		JSONObject configJson = new JSONObject();
		configJson.put(mtxID, "1.0");
		configJson.put(featureID3, "2.0");

		jsonOR.put("configuration", configJson.toString());

		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);

		orderingRuleMtxID2 = f.addFeature(seasonID, orderingRuleMtx, mtxID, sessionToken);
		Assert.assertFalse(orderingRuleMtxID2.contains("error"), "Can't add ordering rule mtx  " + orderingRuleMtxID2);

		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5)+"XXXOR22");
		configJson = new JSONObject();
		configJson.put(featureID4, "1.0");		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID2, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);

	}

	@Test(dependsOnMethods = "reAddComponentsToMaster2", description = "checkout feature with ordering rule")
	public void checkoutFeatureWithOrderingRule3 () throws Exception {
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch: " + response);
	}

	@Test(dependsOnMethods = "checkoutFeatureWithOrderingRule3", description = "try to delete checked out feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster5 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertNotEquals(respCode, 200, "checked out feature with ordering rule from master was deleted from master");
	}

	@Test(dependsOnMethods = "deleteFeatureWithOrderingRuleFromMaster5", description = "checkout feature with ordering rule")
	public void cancelCheckoutFeatureWithOrderingRule3 () throws Exception {
		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not canceled checked out from branch: " + response);
	}

	@Test(dependsOnMethods = "cancelCheckoutFeatureWithOrderingRule3", description = "delete feature with ordering rule from master")
	public void deleteFeatureWithOrderingRuleFromMaster6 () throws Exception {
		int respCode = f.deleteFeature(featureID1, sessionToken);
		Assert.assertEquals(respCode, 200, "not checked out feature with ordering rule from master was not deleted from master");
	}

	////

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
