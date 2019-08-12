package tests.restapi.scenarios.experiments;

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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class BranchUpdateFeatureFieldsInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String mixID;
	private String mixConfigID;
	private String configID;
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
		
	}
	
	/*
		Updated checked out feature fields: check deltas in runtime
			- updated feature fields
			- update configuration rule fields
			- update MIX fields (maxFeaturesOn)
			- update MIXCR fields (maxFeaturesOn)
	
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeature(seasonID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");

		//checkout feature
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

	}
	
	@Test (dependsOnMethods="addComponents", description ="Checkout feature and updated its fields in branch") 
	public void updateFeatureInBranch() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("description", "new description");
		//String configuration = FileUtils.fileToString(filePath + "configurationSchema1.txt", "UTF-8", false);
		//jsonF.put("configurationSchema", new JSONObject(configuration));
		jsonF.put("defaultConfiguration", "{\"color\":\"red\"}");
		jsonF.put("defaultIfAirlockSystemIsDown", true);
		jsonF.put("enabled", false);

		JSONArray groups = new JSONArray();
		groups.add("QA");
		jsonF.put("internalUserGroups", groups);
		jsonF.put("minAppVersion", "1.0");
		jsonF.put("noCachedResults", true);
		jsonF.put("rolloutPercentage", 100);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("displayName", "F1");
		JSONObject ruleString = new JSONObject();
		ruleString.put("ruleString", "false;");
		jsonF.put("rule", ruleString);

		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated");
		
		JSONArray afterUpdate = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		JSONObject featureFromBranch = afterUpdate.getJSONObject(0);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in production file
		JSONObject featureProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		validateFields(featureProd, featureFromBranch, "production");
		
		//validate field values in development file
		JSONObject featureDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		validateFields(featureDev, featureFromBranch, "development");
		
		
		//check there are no changes in master feature
		JSONObject featureInBranch = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject featureInMaster = new JSONObject(f.getFeature(featureID1, sessionToken));

		Assert.assertTrue(!featureInBranch.getString("description").equals(featureInMaster.getString("description")), "Description was changed in master feature");
		Assert.assertTrue(featureInMaster.isNull("defaultConfiguration"), "defaultConfiguration was changed in master feature");
		Assert.assertTrue(!featureInBranch.getString("minAppVersion").equals(featureInMaster.getString("minAppVersion")), "minAppVersion was changed in master feature");
		Assert.assertTrue(!featureInBranch.getString("stage").equals(featureInMaster.getString("stage")), "stage was changed in master feature");
		Assert.assertTrue(featureInMaster.isNull("displayName"), "displayName was changed in master feature");
		Assert.assertTrue(featureInBranch.getBoolean("defaultIfAirlockSystemIsDown")!=featureInMaster.getBoolean("defaultIfAirlockSystemIsDown"), "Description was changed in master feature");
		Assert.assertTrue(featureInBranch.getBoolean("enabled")!=featureInMaster.getBoolean("enabled"), "enabled was changed in master feature");
		Assert.assertTrue(featureInBranch.getBoolean("noCachedResults")!=featureInMaster.getBoolean("noCachedResults"), "noCachedResults was changed in master feature");
		Assert.assertNotEquals(featureInBranch.getJSONArray("internalUserGroups"), featureInMaster.getJSONArray("internalUserGroups"), "internalUserGroups was changed in master feature");
		Assert.assertNotEquals(featureInBranch.getJSONObject("rule"), featureInMaster.getJSONObject("rule"), "internalUserGroups was changed in master feature");
	}

	@Test (dependsOnMethods="updateFeatureInBranch", description ="Checkout feature and updated its fields in master and check delta") 
	public void updateFeatureInMaster() throws Exception{
		//recreate feature
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);


		//move feature to production so all runtime files should be updated
		String feature1 = f.getFeature(featureID1, sessionToken);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION"); 
		String response = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated to production");
		
		//checkout feature
		response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		

		String dateFormat = f.setDateFormat();
		
		feature1 = f.getFeature(featureID1, sessionToken);
		jsonF = new JSONObject(feature1);
		jsonF.put("enabled", false); //changing field in the master and verifying that the branch runtime includes the delta

	    response = f.updateFeature(seasonID, featureID1, jsonF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated");
		
		//JSONArray afterUpdate = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);
		//JSONObject featureFromBranch = afterUpdate.getJSONObject(0);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in branch production file
		JSONObject featureProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureProd.getBoolean("enabled") == true, "enabled not updated in runtime " + "production" + " branch file");
		
		//validate field values in branch development file
		JSONObject featureDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureDev.getBoolean("enabled") == true, "enabled not updated in runtime " + "development" + " branch file");
	}


	@Test (dependsOnMethods="updateFeatureInMaster", description ="Checkout ordering rule and updated its fields in master and check delta") 
	public void updateOrderingInMaster() throws Exception{
		//recreate feature
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		String orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);
		
		//checkout feature
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		

		String dateFormat = f.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID1, sessionToken);
		JSONObject orJson = new JSONObject(orderingRule);
		orJson.put("rolloutPercentage", 33); //changing field in the master and verifying that the branch runtime includes the delta

	    response = f.updateFeature(seasonID, orderingRuleID1, orJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Ordering rule was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

		//validate field values in branch development file
		JSONObject featureDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);
		Assert.assertTrue(featureDev.getInt("rolloutPercentage") == 100, "rolloutPercentage not updated in runtime " + "development" + " branch file");
	}

	@Test (dependsOnMethods="updateOrderingInMaster", description ="Checkout Config rule and updated its description field in master and check delta - nothing should be changed since desc is not in runtime file") 
	public void updateConfigRuleInMaster() throws Exception{
		//recreate feature
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//checkout feature
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		

		String dateFormat = f.setDateFormat();
		
		String configRule = f.getFeature(configID, sessionToken);
		JSONObject crJson = new JSONObject(configRule);
		crJson.put("description", "ttt"); //changing field in the master and verifying that the branch runtime includes the delta

	    response = f.updateFeature(seasonID, configID, crJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration rule was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		//nothing should be changed since desc is not in runtime file
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file changed");
	}

	@Test (dependsOnMethods="updateConfigRuleInMaster", description ="Checkout mtx and updated its fields in master and check delta") 
	public void updateMTXInMaster() throws Exception{
		//recreate feature
		int delBranchCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue (delBranchCode==200, "branch was  not deleted");
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		//checkout feature
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		

		String dateFormat = f.setDateFormat();
		
		String mtx = f.getFeature(mixID, sessionToken);
		JSONObject mtxJson = new JSONObject(mtx);
		mtxJson.put("maxFeaturesOn", 33); //changing field in the master and verifying that the branch runtime includes the delta

	    response = f.updateFeature(seasonID, mixID, mtxJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "MTX was not updated");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		//validate field values in branch development file
		JSONObject mtxDev = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(mtxDev.getInt("maxFeaturesOn") == 1, "maxFeaturesOn not updated in runtime " + "development" + " branch file");
		
		//validate field values in branch production file
		JSONObject mtxProd = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(mtxProd.getInt("maxFeaturesOn") == 1, "maxFeaturesOn not updated in runtime " + "production" + " branch file");

	}

	private void validateFields(JSONObject feature, JSONObject featureFromBranch, String stage) throws JSONException{
		Assert.assertTrue(feature.getBoolean("defaultIfAirlockSystemIsDown") == featureFromBranch.getBoolean("defaultIfAirlockSystemIsDown"), "defaultIfAirlockSystemIsDown not updated in runtime " + stage + " branch file");
		Assert.assertTrue(feature.getBoolean("enabled") == featureFromBranch.getBoolean("enabled"), "enabled not updated in runtime " + stage + " branch file");
		Assert.assertTrue(feature.getBoolean("noCachedResults") == featureFromBranch.getBoolean("noCachedResults"), "noCachedResults not updated in runtime " + stage + " branch file");
		Assert.assertTrue(feature.getDouble("rolloutPercentage") == featureFromBranch.getDouble("rolloutPercentage"), "rolloutPercentage not updated in runtime " + stage + " branch file");
		Assert.assertTrue(feature.getString("defaultConfiguration").equals(featureFromBranch.getString("defaultConfiguration")), "defaultConfiguration not updated in runtime " + stage + " branch file");		
		Assert.assertTrue(feature.getString("minAppVersion").equals(featureFromBranch.getString("minAppVersion")), "minAppVersion not updated in runtime " + stage + " branch file");
		Assert.assertTrue(feature.getString("stage").equals(featureFromBranch.getString("stage")), "stage not updated in runtime " + stage + " branch file");
		Assert.assertEquals(feature.getJSONObject("rule"), featureFromBranch.getJSONObject("rule"), "rule not updated in runtime " + stage + " branch file");
		Assert.assertEquals(feature.getJSONArray("internalUserGroups"), featureFromBranch.getJSONArray("internalUserGroups"), "internalUserGroups not updated in runtime " + stage + " branch file");
		/*
		 // Vicky: these fields do not exist in runtime file
			Assert.assertTrue(feature.getString("description").equals(featureFromBranch.getString("description")), "description not updated in runtime " + stage + " branch file");
			Assert.assertTrue(feature.getString("displayName").equals(featureFromBranch.getString("displayName")), "displayName not updated in runtime " + stage + " branch file");
			
		*/
		

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
