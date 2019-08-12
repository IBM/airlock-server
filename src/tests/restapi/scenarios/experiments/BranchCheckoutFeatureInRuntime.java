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

public class BranchCheckoutFeatureInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String branchID2;
	private String featureID;
	private String configID;
	private String prodFeatureID;
	private String prodConfigID;
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
		Add 2 branches
		Update branch name
		Checkout feature in development
		Checkout feature in production
		Move feature from production to development
		Uncheckout feature in development
		Uncheckout feature in production
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		String dateFormat = f.setDateFormat();
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID2);

		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID, sessionToken);
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out features in dev branches1 runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID, sessionToken);
		Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out features in prod branches1 runtime file");

		branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID2, sessionToken);
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out features in dev branches1 runtime file");
		branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID2, sessionToken);
		Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out features in prod branches1 runtime file");

	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Update branch name ") 
	public void updateBranchName () throws Exception {
		String dateFormat = f.setDateFormat();
		
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "branch1a");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Branch1 was not updated: " + response);
				
		f.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branched runtime development file was not changed");
		JSONObject result = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(result.getString("name").equals("branch1a"), "Branch name was not updated in branches1 runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branched runtime production file was not changed");
		result = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(result.getString("name").equals("branch1a"), "Branch name was not updated in branches1 runtime file");
				
	}
	
	@Test (dependsOnMethods="updateBranchName", description ="Add development feature with configuration rules and check it out ") 
	public void checkoutDevFeature () throws Exception {
		
		
		//add feature with configuration
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeature(seasonID, configuration, featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");
		
		f.setSleep();
		String dateFormat = f.setDateFormat();
		
		String response = br.checkoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeDev.message, featureID).equals("CHECKED_OUT"), "Incorrect feature status in runtime development branch file");
		

	}
	
	@Test (dependsOnMethods="checkoutDevFeature", description ="Add production feature with configuration rules and check it out ") 
	public void checkoutProdFeature () throws Exception {
				
		//add feature with configuration
		String feature1 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1) ;
		json.put("stage", "PRODUCTION");
		prodFeatureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Feature was not added to the season");
				
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration) ;
		jsonCR.put("stage", "PRODUCTION");
		prodConfigID = f.addFeature(seasonID, jsonCR.toString(), prodFeatureID, sessionToken);
		Assert.assertFalse(prodConfigID.contains("error"), "Configuration was not added to the season");
		
		f.setSleep();
		String dateFormat = f.setDateFormat();
		
		String response = br.checkoutFeature(branchID, prodFeatureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==2, "Incorrect number of checked out features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==1, "Incorrect number of checked out features in branches1 production runtime file");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeDev.message, prodFeatureID).equals("CHECKED_OUT"), "Incorrect feature status in runtime development branch file");
		Assert.assertTrue(getFeatureStatus(branchesRuntimeProd.message, prodFeatureID).equals("CHECKED_OUT"), "Incorrect feature status in runtime production branch file");
	}
	
	@Test (dependsOnMethods="checkoutProdFeature", description ="Move feature from prod to dev in master - doesn't influence branch ") 
	public void moveProdFeatureToDev () throws Exception {
		String dateFormat = f.setDateFormat();
		
		//update feature with configuration
		String configuration = f.getFeature(prodConfigID, sessionToken);
		JSONObject jsonCR = new JSONObject(configuration) ;
		jsonCR.put("stage", "DEVELOPMENT");
		prodConfigID = f.updateFeature(seasonID, prodConfigID,  jsonCR.toString(), sessionToken);
		Assert.assertFalse(prodConfigID.contains("error"), "Configuration was not updated: " + prodConfigID);

		
		String feature1 = f.getFeature(prodFeatureID, sessionToken);
		JSONObject json = new JSONObject(feature1) ;
		json.put("stage", "DEVELOPMENT");
		prodFeatureID = f.updateFeature(seasonID, prodFeatureID, json.toString(), sessionToken);
		Assert.assertFalse(prodFeatureID.contains("error"), "Feature was not updated: "+ prodFeatureID);
				
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		
		//branch runtime files were updated because the delta from the master was changed
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="moveProdFeatureToDev", description ="Uncheck dev feature ") 
	public void uncheckDevFeature () throws Exception {
		String dateFormat = f.setDateFormat();
		
		String response = br.cancelCheckoutFeature(branchID, featureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of checked out features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
	}
	
	@Test (dependsOnMethods="uncheckDevFeature", description ="Uncheck prod feature ") 
	public void uncheckProdFeature () throws Exception {
		String dateFormat = f.setDateFormat();
		
		String response = br.cancelCheckoutFeature(branchID, prodFeatureID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==0, "Incorrect number of checked out features in branches1 production runtime file");
		
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
