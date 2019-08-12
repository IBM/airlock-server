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


public class BranchAddDeleteFeatureInRuntime {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
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
		Add new dev feature to branch
		Add new prod feature to branch
		Move to prod
		Move to dev
		Delete new dev feature from branch
*/

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
		
	}
	
	
	@Test(dependsOnMethods="addComponents", description="add development feature to branch")
	public void addDevFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeDev.message, featureID1).equals("NEW"), "Incorrect feature status in runtime development branch file");

	}
	
	@Test(dependsOnMethods="addDevFeature", description="add production feature to branch")
	public void addProdFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String feature1 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature1);
		json.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==2, "Incorrect number of features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeProd.message, featureID2).equals("NEW"), "Incorrect feature status in runtime production branch file");

	}
	
	@Test(dependsOnMethods="addProdFeature", description="Move production feature to development")
	public void moveProdFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String feature2 = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production feature was not moved to development");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==2, "Incorrect number of features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of features in runtime production branch file");

	}
	
	@Test(dependsOnMethods="moveProdFeature", description="Delete development feature from branch")
	public void deleteDevFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		int code = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(code == 200, "Development feature was not deleted from branch");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="deleteDevFeature", description="Move development feature to production")
	public void moveDevFeatureToProd() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();
		
		String feature2 = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production feature was not moved to development");
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of features in branches1 development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		
		Assert.assertTrue(getFeatureStatus(branchesRuntimeProd.message, featureID1).equals("NEW"), "Incorrect feature status in runtime production branch file");

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
