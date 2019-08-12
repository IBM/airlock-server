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

public class CheckoutOrderingRuleScenarios {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private JSONObject fJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;

	
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
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

	}
	
	
	@Test (description ="Checkout ordering rule") 
	public void scenario1 () throws Exception {
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String orderingRuleID1 = f.addFeature(seasonID, orderingRule, featureID0, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		String response = br.checkoutFeature(branchID, orderingRuleID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Checked out ordering rule to branch directly");

	}
	

	@Test (description ="F0 -> F1,F2,OR1,OR2; checkout F0") 
	public void scenario2 () throws Exception {
		

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeature(seasonID, fJson.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season");
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID0, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
	    configJson = new JSONObject();
		configJson.put(featureID2, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), featureID0, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID2);
		
		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//F0
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect feature0 status in get feature from branch");

		//OR1
		JSONObject orderingRuleFromBranch1 = new JSONObject( f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//OR2
		JSONObject orderingRuleFromBranch2 = new JSONObject( f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch2.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//F0 ordering rules
        if(!featureFromBranch.has("branchOrderingRuleItems")){
            Assert.assertTrue(false, "branchOrderingRuleItems is missing in feature");
        }
        else {
            JSONArray branchOrderingRuleItems = featureFromBranch.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchOrderingRuleItems.size() == 2, "wrong number of branchOrderingRuleItems is feature.");
            Assert.assertTrue(branchOrderingRuleItems.getString(0).equals(orderingRuleFromBranch1.getString("namespace")+"."+orderingRuleFromBranch1.getString("name")));            
            Assert.assertTrue(branchOrderingRuleItems.getString(1).equals(orderingRuleFromBranch2.getString("namespace")+"."+orderingRuleFromBranch2.getString("name")));
        }
		
		String res = br.cancelCheckoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
			
		featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Incorrect status of OR1 after cancel checkout");
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(1).getString("branchStatus").equals("NONE"), "Incorrect status of OR2 after cancel checkout");
	}
	

	
	@Test (description ="F0 -> F1, OR1->OR2; checkout F0") 
	public void scenario3 () throws Exception {
		

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID0, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		//configuration = "{\"" + featureID1 + "\": 1.5 }";
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID2);
		
		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//F0
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect feature0 status in get feature from branch");

		//OR1
		JSONObject orderingRuleFromBranch1 = new JSONObject( f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//OR2
		JSONObject orderingRuleFromBranch2 = new JSONObject( f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch2.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//F0 ordering rules
        if(!featureFromBranch.has("branchOrderingRuleItems")){
            Assert.assertTrue(false, "branchOrderingRuleItems is missing in feature");
        }
        else {
            JSONArray branchOrderingRuleItems = featureFromBranch.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchOrderingRuleItems.size() == 1, "wrong number of branchOrderingRuleItems is feature.");
            Assert.assertTrue(branchOrderingRuleItems.getString(0).equals(orderingRuleFromBranch1.getString("namespace")+"."+orderingRuleFromBranch1.getString("name")));            
            branchOrderingRuleItems = orderingRuleFromBranch1.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchOrderingRuleItems.getString(0).equals(orderingRuleFromBranch2.getString("namespace")+"."+orderingRuleFromBranch2.getString("name")));
        }
		
		String res = br.cancelCheckoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
			
		featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Incorrect status of OR1 after cancel checkout");
		featureFromBranch = new JSONObject( f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Incorrect status of OR2 after cancel checkout");
	}

	@Test (description ="F0 -> F1, MTXOR ->(OR1+ OR2); checkout F0") 
	public void scenario4 () throws Exception {
		

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID0 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeature(seasonID, fJson.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");

		String orderingRuleMtx = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String orderingRuleMtxID = f.addFeature(seasonID, orderingRuleMtx, featureID0, sessionToken);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.0");
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
	    configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		jsonOR.put("configuration", configJson.toString());
		String orderingRuleID2 = f.addFeature(seasonID, jsonOR.toString(), orderingRuleMtxID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID2);
		
		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		//F0
		JSONObject featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect feature0 status in get feature from branch");

		//MTXOR
		JSONObject mtxOrderingRuleFromBranch = new JSONObject( f.getFeatureFromBranch(orderingRuleMtxID, branchID, sessionToken));
		Assert.assertTrue(mtxOrderingRuleFromBranch.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect mtxOrderingRuleFromBranch status in get feature from branch");

		//OR1
		JSONObject orderingRuleFromBranch1 = new JSONObject( f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch1.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//OR2
		JSONObject orderingRuleFromBranch2 = new JSONObject( f.getFeatureFromBranch(orderingRuleID2, branchID, sessionToken));
		Assert.assertTrue(orderingRuleFromBranch2.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect orderingRuleID1 status in get feature from branch");

		//F0 ordering rules
        if(!featureFromBranch.has("branchOrderingRuleItems")){
            Assert.assertTrue(false, "branchOrderingRuleItems is missing in feature");
        }
        else {
            JSONArray branchOrderingRuleItems = featureFromBranch.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchOrderingRuleItems.size() == 1, "wrong number of branchOrderingRuleItems is feature.");
            Assert.assertTrue(branchOrderingRuleItems.getString(0).equals("mx."+orderingRuleMtxID), "Incorrect MTXOR id");            
            //Assert.assertTrue(branchOrderingRuleItems.getString(1).equals(orderingRuleFromBranch2.getString("namespace")+"."+orderingRuleFromBranch2.getString("name")));
        }
		
		String res = br.cancelCheckoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
			
		featureFromBranch = new JSONObject( f.getFeatureFromBranch(featureID0, branchID, sessionToken));
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0).getString("branchStatus").equals("NONE"), "Incorrect status of OR1 after cancel checkout");
		Assert.assertTrue(featureFromBranch.getJSONArray("orderingRules").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(1).getString("branchStatus").equals("NONE"), "Incorrect status of OR2 after cancel checkout");
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
