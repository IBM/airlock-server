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

public class BranchAddDeleteMasterFeatureInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID;
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
Branches in runtime:

	Delete dev feature from master - it remains in branch and gets a new status
	Delete dev configuration rule from master - it remains in branch and gets a new status
*/

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeature(seasonID, configuration, featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");

		
	}

	@Test (dependsOnMethods="addComponents", description ="Checkout feature with configuration in branch") 
	public void checkoutDevFeature () throws Exception {
		
	
		String dateFormat = f.setDateFormat();
		
		String response = br.checkoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		f.setSleep();
		
		JSONObject configRule = new JSONObject(f.getFeature(configID, sessionToken));
		
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out features in branches1 development runtime file");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("branchConfigurationRuleItems").get(0).equals(configRule.getString("namespace") + "." + configRule.getString("name")), "Incorrect configuraton name listed in branches development runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeDev.message, featureID).equals("CHECKED_OUT"), "Incorrect feature status in runtime development branch file");
				
	}	
	
	@Test(dependsOnMethods="checkoutDevFeature", description="Delete configuration rule from master")
	public void deleteMasterConfiguration() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		int code = f.deleteFeature(configID,sessionToken);
		Assert.assertTrue(code == 200, "Configuraiton feature was not deleted from master");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		//configuration status changes to NEW
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master configuration rule status in branch is not NEW in runtime" );
		//new uniqueId is assigned
		Assert.assertFalse(getBranchFeatures(branchesRuntimeDev.message).getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master configuration rule uniqueId was not changed in runtime" );

		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="deleteMasterConfiguration", description="Delete feature from master")
	public void deleteMasterFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		int code = f.deleteFeature(featureID,sessionToken);
		Assert.assertTrue(code == 200, "Feature was not deleted from master");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was not changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		//configuration status changes to NEW
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).getJSONObject(0).getString("branchStatus").equals("NEW"), "Deleted from master feature in branch is not NEW in runtime" );
		//new uniqueId is assigned
		Assert.assertFalse(getBranchFeatures(branchesRuntimeDev.message).getJSONObject(0).getString("uniqueId").equals(configID), "Deleted from master feature uniqueId was not changed in runtime" );

		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
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
	
	private String getFeatureStatus(String result, String id) throws JSONException{
		JSONArray features = new JSONObject(result).getJSONArray("features");
		String status="";
		for(int i =0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(id)){
				status = features.getJSONObject(i).getString("branchStatus");
			}
		}
		
		return status;
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
