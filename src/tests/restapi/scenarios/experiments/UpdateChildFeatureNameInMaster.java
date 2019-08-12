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

public class UpdateChildFeatureNameInMaster {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String configID1;
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
		String expID = baseUtils.addExperiment(m_analyticsUrl, false, false);
		
		JSONObject variant = new JSONObject( FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false));
		variant.put("branchName", "branch1");
		String variantID = exp.createVariant(expID, variant.toString(), sessionToken);
		
		//enable experiment
		String airlockExperiment = exp.getExperiment(expID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + expID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(expID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeature(seasonID, configuration1, featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		br.checkoutFeature(branchID, featureID1, sessionToken);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update name") 
	public void updateChildrenInMaster () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		JSONObject f2 = new JSONObject(f.getFeature(featureID2, sessionToken));
		JSONObject config = new JSONObject(f.getFeature(configID1, sessionToken));
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONObject feature = branch.getJSONArray("features").getJSONObject(0);
		
		Assert.assertTrue(feature.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Incorrect configuration name");
		Assert.assertTrue(feature.getJSONArray("branchFeaturesItems").getString(0).equals(f2.getString("namespace")+"."+f2.getString("name")), "Incorrect subfeature name");

		f2.put("name", "newF2");
		f2.put("namespace", "newF2namespace");
		f.updateFeature(seasonID, featureID2, f2.toString(), sessionToken);
		config.put("name", "newCR1");
		config.put("namespace", "newCR1namespace");
		f.updateFeature(seasonID, configID1, config.toString(), sessionToken);
		
		branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		feature = branch.getJSONArray("features").getJSONObject(0);
		f2 = new JSONObject(f.getFeature(featureID2, sessionToken));
		config = new JSONObject(f.getFeature(configID1, sessionToken));
		Assert.assertTrue(feature.getJSONArray("branchFeaturesItems").getString(0).equals(f2.getString("namespace")+"."+f2.getString("name")), "Subfeature name was not updated in branch");
		Assert.assertTrue(feature.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Incorrect configuration name");
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject feature1 = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Configuration name was not updated in branch");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		feature1 = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Feature name was not updated in branch");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	
	}
	
	@Test (dependsOnMethods="updateChildrenInMaster", description ="Update name") 
	public void updateParentInMaster () throws Exception {
		br.checkoutFeature(branchID, featureID2, sessionToken);
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot cancel checkout");
		
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		JSONObject f1 = new JSONObject(f.getFeature(featureID1, sessionToken));
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONObject feature = branch.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature.getString("branchFeatureParentName").equals(f1.getString("namespace")+"."+f1.getString("name")), "Incorrect parent name");

		f1.put("name", "newF1");
		f1.put("namespace", "newF1namespace");
		f.updateFeature(seasonID, featureID1, f1.toString(), sessionToken);
		
		f1 = new JSONObject(f.getFeature(featureID1, sessionToken));
		branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		feature = branch.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature.getString("branchFeatureParentName").equals(f1.getString("namespace")+"."+f1.getString("name")), "Parent name was not updated in branch");
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject feature1 = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1.getString("branchFeatureParentName").equals(f1.getString("namespace")+"."+f1.getString("name")), "Feature name was not updated in branch");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		feature1 = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(feature1.getString("branchFeatureParentName").equals(f1.getString("namespace")+"."+f1.getString("name")), "Feature name was not updated in branch");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
