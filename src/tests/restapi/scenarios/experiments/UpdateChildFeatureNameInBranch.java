package tests.restapi.scenarios.experiments;

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

public class UpdateChildFeatureNameInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private ExperimentsRestApi exp ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		m_analyticsUrl = analyticsUrl;
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
		- Checkout parent feature. A list of its children appear in branchChildrenList even if they are not checked out. 
		Update name/namespace of a child feature in master. It should be updated also in the list of branchChildrenList in runtime.

	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws Exception {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String experimentID = baseUtils.addExperiment(m_analyticsUrl, false, false);
				

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("branchName", "branch1");
		exp.createVariant(experimentID, variantJson.toString(), sessionToken);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		br.checkoutFeature(branchID, featureID1, sessionToken);
	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Update child feature name in branch") 
	public void updateChildInBranch () throws Exception {

		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		JSONObject f2 = new JSONObject(f.getFeature(featureID2, sessionToken));
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));

		f2.put("name", "newF1");
		f2.put("namespace", "newF1namespace");
		f.updateFeature(seasonID, featureID2, f2.toString(), sessionToken);
		
		f2 = new JSONObject(f.getFeature(featureID2,  sessionToken));
		branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONObject feature1 = branch.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1.getJSONArray("branchFeaturesItems").getString(0).equals(f2.getString("namespace")+"."+f2.getString("name")), "Feature name was not updated in branch");
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject feature = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature.getJSONArray("branchFeaturesItems").getString(0).equals(f2.getString("namespace")+"."+f2.getString("name")), "Feature name was not updated in branch");

		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		feature = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature.getJSONArray("branchFeaturesItems").getString(0).equals(f2.getString("namespace")+"."+f2.getString("name")), "Feature name was not updated in branch");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
