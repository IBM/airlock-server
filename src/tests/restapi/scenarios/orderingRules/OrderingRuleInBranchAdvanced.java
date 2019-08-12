package tests.restapi.scenarios.orderingRules;

import java.io.IOException;

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

public class OrderingRuleInBranchAdvanced {
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
	protected String featureID0;
	protected String featureID1;
	protected String featureID2;
	protected String newFeatureID2;
	private String branchID;
	private BranchesRestApi br;
	
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
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
	}
	
	/*
	  In master:
	  add F0
	  		F1
	  		F2
	  		OR1 (refering to F1)
	  		
	  In Branch
	   	  F0 (checked_out)
	   	  	F1
	   	  	F2
	   	  	OR1 (checked_out as part of F1 check out)
	   	  	
	   	  In branch update OR to point to F2
	   	  In master remove F2 - should be refused.
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject feature0Obj = new JSONObject(feature);
		feature0Obj.put("name", "F0");
		featureID0 = f.addFeature(seasonID, feature0Obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Can't create feature: " + featureID0);
		
		JSONObject feature1Obj = new JSONObject(feature);
		feature1Obj.put("name", "F1");
		featureID1 = f.addFeature(seasonID, feature1Obj.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Can't create feature: " + featureID1);
		
		JSONObject feature2Obj = new JSONObject(feature);
		feature2Obj.put("name", "F2");
		featureID2 = f.addFeature(seasonID, feature2Obj.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Can't create feature: " + featureID2);
	
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR1");
		jsonOR.put("namespace", "or");
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.0");
		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Can't create ordering rule: " + orderingRuleID);
	}
	
	@Test (dependsOnMethods="addComponents", description = "chceking out F0")
	public void orderingRuleUnderFeature() throws JSONException, IOException{
		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		//F0
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect feature0 status in get feature from branch");

		//F0 ordering rules
        if(!featureFromBranch.has("branchOrderingRuleItems")){
            Assert.assertTrue(false, "branchOrderingRuleItems is missing in feature");
        }
        else {
            JSONArray branchOrderingRuleItems = featureFromBranch.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchOrderingRuleItems.size() == 1, "wrong number of branchOrderingRuleItems is feature.");
            Assert.assertTrue(branchOrderingRuleItems.getString(0).equals("or.OR1"));            
        }
		
		//OR1
		JSONObject orderingRuleFromBranch = new JSONObject( f.getFeatureFromBranch(orderingRuleID, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect feature0 status in get feature from branch");
	}
	
	@Test (dependsOnMethods="orderingRuleUnderFeature", description = "update ordering rule in branch")
	public void updateOrderingRuleInBranch() throws JSONException, IOException{		
		//OR1
		JSONObject orderingRuleFromBranch = new JSONObject( f.getFeatureFromBranch(orderingRuleID, branchID, sessionToken));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.0");
		
		orderingRuleFromBranch.put("configuration", configJson.toString());
		String response = f.updateFeatureInBranch(seasonID, branchID, orderingRuleID, orderingRuleFromBranch.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update ordering rule in branch");
		
		orderingRuleFromBranch = new JSONObject( f.getFeatureFromBranch(orderingRuleID, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch.getString("configuration").contains(featureID2), "ordering rule was not updated in branch");
		Assert.assertFalse(orderingRuleFromBranch.getString("configuration").contains(featureID1), "ordering rule was not updated in branch");
		
		JSONObject orderingRuleFromMaster = new JSONObject( f.getFeature(orderingRuleID, sessionToken));
		Assert.assertTrue(orderingRuleFromMaster.getString("configuration").contains(featureID1), "ordering rule was updated in master");
		Assert.assertFalse(orderingRuleFromMaster.getString("configuration").contains(featureID2), "ordering rule was updated in master");
	}
	
	@Test (dependsOnMethods="updateOrderingRuleInBranch", description = "delete F1 and F2 from master")
	public void deleteFeatureFromMaster() throws JSONException, IOException{		
		
		int code = f.deleteFeature(featureID1, sessionToken);
		Assert.assertFalse(code==200, "feature was deleted from master even though it is referenced in an oredering rule in master");
		
		code = f.deleteFeature(featureID2, sessionToken);
		Assert.assertFalse(code==200, "feature was deleted from master even though it is referenced in an oredering rule in branch");
	}
	
	@Test (dependsOnMethods="deleteFeatureFromMaster", description = "checkout F2 from master")
	public void checkoutFeature() throws Exception{		
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		//after checkout i can delete F2 from master
		//it will be created in branch with status NEW and newId
		int code = f.deleteFeature(featureID2, sessionToken);
		Assert.assertTrue(code==200, "feature was not deleted from master even though it is checked out in branch");
		
		response = f.getFeatureFromBranch(featureID0, branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray features = brJson.getJSONArray("features");
		Assert.assertTrue(features.size()==2, "Incorrect number of checked out features");
		String newFeatureID2 = features.getJSONObject(1).getString("uniqueId");
		
		Assert.assertFalse(newFeatureID2.equals(featureID2), "Deleted from master ordering rule uniqueId was not changed" );
		
		JSONArray orderingRules = brJson.getJSONArray("orderingRules");
		Assert.assertTrue(orderingRules.size()==1, "Incorrect number of ordering rules");
		
		String newORConfig = orderingRules.getJSONObject(0).getString("configuration");
		
		Assert.assertFalse(newORConfig.contains(featureID2), "new id was not updated in ordering rule" );
		Assert.assertTrue(newORConfig.contains(newFeatureID2), "new id was not updated in ordering rule" );
		
	}
		
	
	@Test (dependsOnMethods="deleteFeatureFromMaster", description = "delete branch and then delete F1 from master and F2 from branch")
	public void deleteBranchAnfFeaturefromMaster() throws Exception{		
		int code = f.deleteFeatureFromBranch(newFeatureID2, branchID, sessionToken);
		Assert.assertTrue(code==400, "feature was not deleted from branch since in use by ordering rule in branch");
		
		code = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue(code==200, "branch was not deleted");
		
		code = f.deleteFeature(featureID1, sessionToken);
		Assert.assertFalse(code==200, "feature was not deleted from master even though it is referenced in an oredering rule in master");		
	}
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
}
