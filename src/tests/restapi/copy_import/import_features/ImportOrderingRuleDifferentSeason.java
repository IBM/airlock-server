package tests.restapi.copy_import.import_features;

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
import tests.restapi.SeasonsRestApi;

public class ImportOrderingRuleDifferentSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private BranchesRestApi br ;
	private AirlockUtils baseUtils;
	private SeasonsRestApi s;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String srcBranchID;
	private String destBranchID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
		
		String season2 = "{\"minVersion\":\"5.0\"}";
		seasonID2 = s.addSeason(productID, season2, sessionToken);

		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {
			String allBranches = br.getAllBranches(seasonID2,sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
		
	}
	

	// F1 -> OR, MTX->(F2, F3), F4, orderingRule config has MTX & F4
	@Test (description = "F1 -> OR, MTX->(F2, F3), F4, orderingRule config has MTX & F4")
	public void copy1() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "F1" + RandomStringUtils.randomAlphabetic(3));
		String parentID = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), "ROOT", sessionToken);
			
		String featureMtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeatureToBranch(seasonID, srcBranchID, featureMtx, parentID, sessionToken);
		
		json.put("name", "F2" + RandomStringUtils.randomAlphabetic(3));
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);
		
		json.put("name", "F3" + RandomStringUtils.randomAlphabetic(3));
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);

		json.put("name", "F4" + RandomStringUtils.randomAlphabetic(3));
		String featureID4 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), parentID, sessionToken);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		String configuration = "{\"" + featureID4 + "\": 1.0, \"" + mtxID + "\":5.0}";
		jsonOR.put("configuration", configuration);

		String orderingRuleID = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), parentID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule  " + orderingRuleID);
		
		//copy F1 to root
		
		String rootId = f.getRootId(seasonID2, sessionToken);
		String featureToImport = f.getFeatureFromBranch(parentID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error"), "Can't copy feature: " + response);
		
		int resCode = f.deleteFeatureFromBranch(parentID, srcBranchID, sessionToken);
		Assert.assertTrue(resCode==200, "Original feature was not deleted");
		
		JSONObject jsonResp = new JSONObject(response);
		String newParentID = jsonResp.getString("newSubTreeId");
		JSONObject newTree = new JSONObject(f.getFeatureFromBranch(newParentID, destBranchID, sessionToken));

		String newMtxId = newTree.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String newF4Id = newTree.getJSONArray("features").getJSONObject(1).getString("uniqueId");
			
		JSONObject newOR = newTree.getJSONArray("orderingRules").getJSONObject(0);
		Assert.assertTrue(newOR.getString("configuration").contains(newMtxId), "New copied MTX uniqueId not found in ordering rule configuration");
		Assert.assertTrue(newOR.getString("configuration").contains(newF4Id), "New copied F4 uniqueId not found in ordering rule configuration");

	}
	

	// F1 -> MTX->(OR, F1, F2)
	@Test (description = "F1 -> MTX->(OR, F1, F2)")
	public void copy2() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "F1"+RandomStringUtils.randomAlphabetic(3));
		String parentID = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), "ROOT", sessionToken);
			
		String featureMtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeatureToBranch(seasonID, srcBranchID, featureMtx, parentID, sessionToken);
		
		json.put("name", "F2"+RandomStringUtils.randomAlphabetic(3));
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);		

		json.put("name", "F3"+RandomStringUtils.randomAlphabetic(3));
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		String configuration = "{\"" + featureID2 + "\": 1.0, \"" + featureID3 + "\":5.0}";
		jsonOR.put("configuration", configuration);

		String orderingRuleID = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule  " + orderingRuleID);
		
		//copy F1 to root
		
		String rootId = f.getRootId(seasonID2, sessionToken);
		String featureToImport = f.getFeatureFromBranch(parentID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error"), "Can't copy feature: " + response);
		
		JSONObject jsonResp = new JSONObject(response);
		String newParentID = jsonResp.getString("newSubTreeId");
		JSONObject newTree = new JSONObject(f.getFeatureFromBranch(newParentID, destBranchID, sessionToken));

		String newMtxId = newTree.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String mtx = f.getFeatureFromBranch(newMtxId, destBranchID, sessionToken);
		JSONObject newMtxJson = new JSONObject(mtx);
		String newF2Id = newMtxJson.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String newF3Id = newMtxJson.getJSONArray("features").getJSONObject(1).getString("uniqueId");
			
		JSONObject newOR = newMtxJson.getJSONArray("orderingRules").getJSONObject(0);
		Assert.assertTrue(newOR.getString("configuration").contains(newF2Id), "New copied F2 uniqueId not found in ordering rule configuration");
		Assert.assertTrue(newOR.getString("configuration").contains(newF3Id), "New copied F3s uniqueId not found in ordering rule configuration");


	}
	
	//MTX->(OR, F1, F2)
	@Test (description = "MTX->(OR, F1, F2)")
	public void copy3() throws JSONException, IOException{
			
		String featureMtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeatureToBranch(seasonID, srcBranchID, featureMtx, "ROOT", sessionToken);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "F2"+RandomStringUtils.randomAlphabetic(3));
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);		

		json.put("name", "F3"+RandomStringUtils.randomAlphabetic(3));
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		String configuration = "{\"" + featureID2 + "\": 1.0, \"" + featureID3 + "\":5.0}";
		jsonOR.put("configuration", configuration);

		String orderingRuleID = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), mtxID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule  " + orderingRuleID);
		
		//copy F1 to root
		
		String rootId = f.getRootId(seasonID2, sessionToken);
		String featureToImport = f.getFeatureFromBranch(mtxID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error"), "Can't copy feature: " + response);
		
		JSONObject jsonResp = new JSONObject(response);
		String newParentID = jsonResp.getString("newSubTreeId");
		JSONObject newTree = new JSONObject(f.getFeatureFromBranch(newParentID, destBranchID, sessionToken));

		String newF1Id = newTree.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String newF2Id = newTree.getJSONArray("features").getJSONObject(1).getString("uniqueId");
			
		JSONObject newOR = newTree.getJSONArray("orderingRules").getJSONObject(0);
		Assert.assertTrue(newOR.getString("configuration").contains(newF1Id), "New copied F1 uniqueId not found in ordering rule configuration");
		Assert.assertTrue(newOR.getString("configuration").contains(newF2Id), "New copied F2 uniqueId not found in ordering rule configuration");


	}
	
	// F1 -> MTX->F2, F3, F4, MTXOR -> ((OR1->F2, F3)+(OR2->F3, F4))	
	@Test (description = "F1 -> MTX->(F2, F3, F4, MTXOR1")
	public void copy4() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "F1" + RandomStringUtils.randomAlphabetic(3));
		String parentID = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), "ROOT", sessionToken);

		String featureMtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mtxID = f.addFeatureToBranch(seasonID, srcBranchID, featureMtx, parentID, sessionToken);

		json.put("name", "F2"+RandomStringUtils.randomAlphabetic(3));
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);		

		json.put("name", "F3"+RandomStringUtils.randomAlphabetic(3));
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);

		json.put("name", "F4"+RandomStringUtils.randomAlphabetic(3));
		String featureID4 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), mtxID, sessionToken);

		String orderingMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String ORMTXID = f.addFeatureToBranch(seasonID, srcBranchID, orderingMtx, mtxID, sessionToken);
				
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		String configuration = "{\"" + featureID2 + "\": 1.0, \"" + featureID3 + "\":5.0}";
		jsonOR.put("configuration", configuration);
		String orderingRuleID1 = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), ORMTXID, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);
		
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		configuration = "{\"" + featureID3 + "\": 1.0, \"" + featureID4 + "\":5.0}";
		jsonOR.put("configuration", configuration);
		String orderingRuleID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), ORMTXID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
		
		//copy MTX to root
		
		String rootId = f.getRootId(seasonID2, sessionToken);		
		String featureToImport = f.getFeatureFromBranch(mtxID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error"), "Can't copy feature: " + response);
		
		JSONObject jsonResp = new JSONObject(response);
		String newParentID = jsonResp.getString("newSubTreeId");
		JSONObject newTree = new JSONObject(f.getFeatureFromBranch(newParentID, destBranchID, sessionToken));

		String newF2Id = newTree.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String newF3Id = newTree.getJSONArray("features").getJSONObject(1).getString("uniqueId");
		String newF4Id = newTree.getJSONArray("features").getJSONObject(2).getString("uniqueId");
		
		JSONObject newMtxOR = newTree.getJSONArray("orderingRules").getJSONObject(0);
		JSONObject newOR1 = newMtxOR.getJSONArray("orderingRules").getJSONObject(0);
		JSONObject newOR2 = newMtxOR.getJSONArray("orderingRules").getJSONObject(1);
			
		Assert.assertTrue(newOR1.getString("configuration").contains(newF2Id), "New copied F2 uniqueId not found in ordering rule configuration1");
		Assert.assertTrue(newOR1.getString("configuration").contains(newF3Id), "New copied F3 uniqueId not found in ordering rule configuration1");
		Assert.assertTrue(newOR2.getString("configuration").contains(newF3Id), "New copied F3 uniqueId not found in ordering rule configuration2");
		Assert.assertTrue(newOR2.getString("configuration").contains(newF4Id), "New copied F4 uniqueId not found in ordering rule configuration2");


	}	
	
	//F1 - > F2, F3, F4, MTXOR (OR1->F2, F3)+(OR2->F3, F4)
	@Test (description = "F1 - > F2, F3, F4, MTXOR1 (OR1->F2, F3)+(OR2->F3, F4)")
	public void copy5() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("name", "F1" + RandomStringUtils.randomAlphabetic(3));
		String parentID = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), "ROOT", sessionToken);

		json.put("name", "F2"+RandomStringUtils.randomAlphabetic(3));
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), parentID, sessionToken);		

		json.put("name", "F3"+RandomStringUtils.randomAlphabetic(3));
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), parentID, sessionToken);

		json.put("name", "F4"+RandomStringUtils.randomAlphabetic(3));
		String featureID4 = f.addFeatureToBranch(seasonID, srcBranchID, json.toString(), parentID, sessionToken);

		String orderingMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String ORMTXID = f.addFeatureToBranch(seasonID, srcBranchID, orderingMtx, parentID, sessionToken);
				
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		String configuration = "{\"" + featureID2 + "\": 1.0, \"" + featureID3 + "\":5.0}";
		jsonOR.put("configuration", configuration);

		String orderingRuleID1 = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), ORMTXID, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't add orderingRule  " + orderingRuleID1);
		
		jsonOR.put("name", "OR"+RandomStringUtils.randomAlphabetic(3));
		configuration = "{\"" + featureID3 + "\": 1.0, \"" + featureID4 + "\":5.0}";
		jsonOR.put("configuration", configuration);

		String orderingRuleID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), ORMTXID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't add orderingRule  " + orderingRuleID2);
		
		//copy F1 to root
		
		String rootId = f.getRootId(seasonID2, sessionToken);		
		String featureToImport = f.getFeatureFromBranch(parentID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertFalse(response.contains("error"), "Can't copy feature: " + response);

		JSONObject jsonResp = new JSONObject(response);
		String newParentID = jsonResp.getString("newSubTreeId");
		JSONObject newTree = new JSONObject(f.getFeatureFromBranch(newParentID, destBranchID, sessionToken));

		String newF2Id = newTree.getJSONArray("features").getJSONObject(0).getString("uniqueId");
		String newF3Id = newTree.getJSONArray("features").getJSONObject(1).getString("uniqueId");
		String newF4Id = newTree.getJSONArray("features").getJSONObject(2).getString("uniqueId");

		JSONObject newMtxOR = newTree.getJSONArray("orderingRules").getJSONObject(0);
		JSONObject newOR1 = newMtxOR.getJSONArray("orderingRules").getJSONObject(0);
		JSONObject newOR2 = newMtxOR.getJSONArray("orderingRules").getJSONObject(1);
			
		Assert.assertTrue(newOR1.getString("configuration").contains(newF2Id), "New copied F2 uniqueId not found in ordering rule configuration1");
		Assert.assertTrue(newOR1.getString("configuration").contains(newF3Id), "New copied F3 uniqueId not found in ordering rule configuration1");
		Assert.assertTrue(newOR2.getString("configuration").contains(newF3Id), "New copied F3 uniqueId not found in ordering rule configuration2");
		Assert.assertTrue(newOR2.getString("configuration").contains(newF4Id), "New copied F4 uniqueId not found in ordering rule configuration2");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
