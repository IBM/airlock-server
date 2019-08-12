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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class OrderingRuleChangeParentInBranch {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private BranchesRestApi br ;
	private String branchID;
	
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
	
	
	//F1-> F2, OR1->OR2 point to F2. Move OR2 under F1 in branch
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
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout feature: " + response);
		//move OR2 from OR1 to F1 in branch
		
		JSONObject parent1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject or2 = new JSONObject(f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		JSONObject or1 = new JSONObject(f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		or1.put("orderingRules", new JSONArray());
		JSONArray children = new JSONArray();
		children.add(or2);
		children.add(or1);
		parent1.put("orderingRules", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, parent1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to an upper level of the same parent in branch");
		
	}
	

	//F1-> F2, ORMTX->OR2 point to F2. Move OR2 under F1 in branch
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
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout feature: " + response);

		//move OR2 from OR1 to F1
		
		JSONObject parent1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject or2 = new JSONObject(f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		JSONObject ormtx = new JSONObject(f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		ormtx.put("orderingRules", new JSONArray());
		JSONArray children = new JSONArray();
		
		children.add(or2);
		children.add(ormtx);
		parent1.put("orderingRules", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, parent1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to an upper level of the same parent in branch: " + response);
		
	}
	
	
	//F1-> F2, OR1 point to F2. Add ORMTX in branch and move OR2 under ORMTX in branch
	@Test (description = "Add features")
	public void moveORFromFeatureToMtx() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, json.toString(), featureID1, sessionToken);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR."+RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout feature: " + response);

		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String mtxorderingRuleID1 = f.addFeatureToBranch(seasonID, branchID, orderingRuleMtx, featureID1, sessionToken);
		Assert.assertFalse(mtxorderingRuleID1.contains("error"), "Can't add orderingRule  " + mtxorderingRuleID1);
		
		
		JSONObject parent1 = new JSONObject(f.getFeatureFromBranch(mtxorderingRuleID1, branchID, sessionToken));
		JSONObject or2 = new JSONObject(f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		JSONArray children = new JSONArray();
		children.add(or2);
		parent1.put("orderingRules", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, mtxorderingRuleID1, parent1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move ordering rule to mtxor in branch: " + response);
		
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
