package tests.restapi.scenarios.experiments;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateFeatureInBranchReorderSubItems {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private ExperimentsRestApi exp ;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*	
		- Checkout parent feature. 
		- change production children at branch
		- verify that production branch file is written
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws Exception {
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		
		jsonF.put("name", "F1");
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		jsonF.put("name", "F3");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID2);
		
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature1 was not checked out: " + response);
		
		//check if files were changed
		Thread.sleep(3000);
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		JSONObject contentDev = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(contentDev.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(0).equals("ns1.F2"), "features are not in the right order");
		Assert.assertTrue(contentDev.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(1).equals("ns1.F3"), "features are not in the right order");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		JSONObject contentProd = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(contentProd.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(0).equals("ns1.F2"), "features are not in the right order");
		Assert.assertTrue(contentProd.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(1).equals("ns1.F3"), "features are not in the right order");
	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Update subfeature order in branch") 
	public void updateSubFeatureOrderInBranch () throws Exception {

		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));

		JSONArray newSubFeatures = new JSONArray();
		newSubFeatures.add(feature1.getJSONArray("features").getJSONObject(1));
		newSubFeatures.add(feature1.getJSONArray("features").getJSONObject(0));
		feature1.put("features", newSubFeatures);
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, feature1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated in branch: " + response);
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		JSONObject contentDev = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(contentDev.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(0).equals("ns1.F3"), "features are not in the right order");
		Assert.assertTrue(contentDev.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(1).equals("ns1.F2"), "features are not in the right order");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		JSONObject contentProd = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(contentProd.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(0).equals("ns1.F3"), "features are not in the right order");
		Assert.assertTrue(contentProd.getJSONArray("features").getJSONObject(0).getJSONArray("branchFeaturesItems").getString(1).equals("ns1.F2"), "features are not in the right order");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
