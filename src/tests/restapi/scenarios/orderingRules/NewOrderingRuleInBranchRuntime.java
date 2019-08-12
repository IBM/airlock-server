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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class NewOrderingRuleInBranchRuntime {
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
	private String featureID1;
	private String featureID0;
	private String orderingRuleID1;

	
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
	
	/*
		Checkout feature in development
		Add OR in branch to checked out feature
		Move OR from dev to prod
		Uncheckout feature in development

	 */

	//F0-> F1, OR1
	@Test (description ="Add development feature with configuration rules and check it out ") 
	public void checkoutParentFeature () throws Exception {
		
		
		//add feature with configuration
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID0 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID0.contains("error"), "Feature0 was not added to the season");
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), featureID0, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		
		String response = br.checkoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
	}
	
	@Test(dependsOnMethods="checkoutParentFeature", description="add dev ordering rule in branch")
	public void addDevOrderingRule() throws IOException, JSONException, InterruptedException{

		String dateFormat = f.setDateFormat();

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);

		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		JSONObject configJson = new JSONObject();
		configJson.put(featureID1, "1.5");
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID0, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(branchesRuntimeDev.message.contains(orderingRuleID1), "Ordering rule not found in branch development runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test(dependsOnMethods="addDevOrderingRule", description="move ordering rule to production")
	public void moveOrderingRuleToProd() throws JSONException, InterruptedException, IOException{

		String dateFormat = f.setDateFormat();
		
		//move development ordering rule to production
		String parent = f.getFeatureFromBranch(featureID0, branchID, sessionToken);
		JSONObject jsonParent = new JSONObject(parent) ;
		jsonParent.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID0,  jsonParent.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Parent feature was not updated: " + response);
	
		String orderingRule = f.getFeatureFromBranch(orderingRuleID1, branchID, sessionToken);
		JSONObject jsonOR = new JSONObject(orderingRule) ;
		jsonOR.put("stage", "PRODUCTION");
		orderingRuleID1 = f.updateFeatureInBranch(seasonID, branchID, orderingRuleID1,  jsonOR.toString(), sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Ordering rule was not updated: " + orderingRuleID1);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(branchesRuntimeDev.message.contains(orderingRuleID1), "Ordering rule not found in branch1 development runtime file");
		JSONObject json = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0); 
		Assert.assertTrue(json.getString("stage").equals("PRODUCTION"), "Incorrect ordering rule status in branch development file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");

		Assert.assertTrue(branchesRuntimeProd.message.contains(orderingRuleID1), "Ordering rule was not found in branch1 production runtime file");

		json = new JSONObject(branchesRuntimeProd.message).getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0); 
		Assert.assertTrue(json.getString("stage").equals("PRODUCTION"), "Incorrect ordering rule status in branch production file");
	}
	
/*
	//cannot un-chcekout feature that has new sub features 
	@Test(dependsOnMethods="moveOrderingRuleToProd", description="uncheck parent feature from branch")
	public void uncheckParent() throws IOException, InterruptedException, JSONException{

		//uncheck parent feature F0
		String dateFormat = f.setDateFormat();
		
		String response = br.cancelCheckoutFeature(branchID, featureID0, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertFalse(branchesRuntimeDev.message.contains(orderingRuleID1), "Ordering rule found in branch1 development runtime file");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");
		Assert.assertFalse(branchesRuntimeProd.message.contains(orderingRuleID1), "Ordering rule found in branch1 development runtime file");
	}*/
	

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	private JSONArray getBranchFeatures(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		return json.getJSONArray("features");
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
