package tests.restapi.scenarios.experiments;

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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

//check out feature in dev, release in branch to production and verify that is new in runtime files
public class CheckoutFeatureScenario15 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String experimentID;
	private String variantID;
	private String mixID1;
	private JSONObject fJson;
	protected String filePath;
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
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl); 

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		fJson = new JSONObject(feature);		
	}
	
	
	@Test (description ="MIX -> (F1+ F2 + F3) checkout F1") 
	public void scenario15 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		fJson.put("name", "F1");
		featureID1 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		fJson.put("name", "F2");
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		fJson.put("name", "F3");
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
	}
	
	@Test (dependsOnMethods="scenario15", description="Checkout everything")
	public void checkout() throws JSONException, Exception{				
		String res = br.checkoutFeature(branchID, featureID1, sessionToken); //checks out MTX, F1, F2 and F3
		Assert.assertFalse(res.contains("error"), "Feature1 was not unchecked out: " + res);
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "mix incorrect status");
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		Assert.assertTrue(F1.getString("stage").equals("DEVELOPMENT"), "Feature1 incorrect stage");
		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		Assert.assertTrue(F2.getString("stage").equals("DEVELOPMENT"), "Feature2 incorrect stage");
		
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		Assert.assertTrue(F3.getString("stage").equals("DEVELOPMENT"), "Feature3 incorrect stage");
				
		//find subtree root in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		Assert.assertTrue(features.size() ==1, "More than one sub tree in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mixID1), "MTX is not the branch subTree root");
	}	

	
	@Test (dependsOnMethods="checkout", description="Move feature1 to prod in branch")
	public void moveF1ToProduction() throws JSONException, Exception{				
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		
		String feature1 = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject jsonF1 = new JSONObject(feature1);
		jsonF1.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, jsonF1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "mix incorrect status");
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		Assert.assertTrue(F1.getString("stage").equals("PRODUCTION"), "Feature1 incorrect stage");
		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		Assert.assertTrue(F2.getString("stage").equals("DEVELOPMENT"), "Feature2 incorrect stage");
		
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		Assert.assertTrue(F3.getString("stage").equals("DEVELOPMENT"), "Feature3 incorrect stage");
		
		//verify that feature1 is NEW in runtime files
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID, sessionToken);
		JSONArray features = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(features.size()==3, "wrong number of sub features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("PRODUCTION"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(features.getJSONObject(0).containsKey("minAppVersion"), "only delta is written for prod feature in branch");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong branchStatus for prod feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(2).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID, sessionToken);
		features = new JSONObject(branchesRuntimeProd.message).getJSONArray("features").getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(features.size()==1, "wrong number of sub features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("PRODUCTION"), "wrong stage in runtime file");
		Assert.assertTrue(features.getJSONObject(0).containsKey("minAppVersion"), "only delta is written for prod feature in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong branchStatus for prod feature in branch in runtime file");	
	}	
	
	@Test (dependsOnMethods="moveF1ToProduction", description="add branch to an experimant in the production stage")
	public void addBranchToProdExp() throws JSONException, Exception{	
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), true);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		variantID = addVariant("variant1", "branch1", true);
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		JSONArray features = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(features.size()==3, "wrong number of sub features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("PRODUCTION"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(features.getJSONObject(0).containsKey("minAppVersion"), "only delta is written for prod feature in branch");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong branchStatus for prod feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(2).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		features = new JSONObject(responseProd.message).getJSONArray("branches").getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(features.size()==1, "wrong number of sub features");
		Assert.assertTrue(features.getJSONObject(0).getString("stage").equals("PRODUCTION"), "wrong stage in runtime file");
		Assert.assertTrue(features.getJSONObject(0).containsKey("minAppVersion"), "only delta is written for prod feature in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("NEW"), "wrong branchStatus for prod feature in branch in runtime file");
	}
	
	@Test (dependsOnMethods="addBranchToProdExp", description="Move feature1 to dev in branch")
	public void revertF1ToDevelopment() throws JSONException, Exception{				
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		
		String feature1 = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject jsonF1 = new JSONObject(feature1);
		jsonF1.put("stage", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, jsonF1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "mix incorrect status");
		
		JSONObject F1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(F1.getString("branchStatus").equals("CHECKED_OUT"), "Feature1 incorrect status");
		Assert.assertTrue(F1.getString("stage").equals("DEVELOPMENT"), "Feature1 incorrect stage");
		
		JSONObject F2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(F2.getString("branchStatus").equals("CHECKED_OUT"), "Feature2 incorrect status");
		Assert.assertTrue(F2.getString("stage").equals("DEVELOPMENT"), "Feature2 incorrect stage");
		
		JSONObject F3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(F3.getString("branchStatus").equals("CHECKED_OUT"), "Feature3 incorrect status");
		Assert.assertTrue(F3.getString("stage").equals("DEVELOPMENT"), "Feature3 incorrect stage");
		
		//verify that feature1 is NEW in runtime files
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==200, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, seasonID, branchID, sessionToken);
		JSONArray features = new JSONObject(branchesRuntimeDev.message).getJSONArray("features").getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(features.size()==3, "wrong number of sub features");
		Assert.assertTrue(!features.getJSONObject(0).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("stage"), "wrong stage in runtime file");
		Assert.assertTrue(!features.getJSONObject(0).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(!features.getJSONObject(1).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(!features.getJSONObject(2).containsKey("minAppVersion"), "not only delta is written for dev feature in branch");
		Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(1).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		Assert.assertTrue(features.getJSONObject(2).getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus for dev feature in branch in runtime file");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, seasonID, branchID, sessionToken);
		features = new JSONObject(branchesRuntimeProd.message).getJSONArray("features");
		Assert.assertTrue(features.size()==0, "wrong number of features");
	}	

	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private String addExperiment(String experimentName, boolean inProd) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, inProd, false);
	}
	

	private String addVariant(String variantName, String branchName, boolean isProd) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", isProd?"PRODUCTION":"DEVELOPMENT");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
