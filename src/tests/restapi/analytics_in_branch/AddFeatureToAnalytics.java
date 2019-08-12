package tests.restapi.analytics_in_branch;

import java.io.IOException;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class AddFeatureToAnalytics {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	private String featureID3;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/*
	 * - add dev feature in master (seen also in branch)
	 * - add prod feature in master  (seen also in branch)
	 * - checkout feature
	 * - remove this feature in branch (fail)
	 * - add same feature in branch (fail)
	 * - remove feature from master
	 * - add feature in branch (not seen in master)
	 * - add same feature in master (should be ok, counter not updated)
	 * - remove feature from branch
	 * - add new feature to branch and add it to analytics
	 * - remove new feature from analytics in branch
	 * - feature reported in both master and branch can be removed from master and should remain in branch
	 * - report feature in master & branch and uncheck it from branch, it remains in analytics in branch
	 * - report feature in branch only, then cancel checkout. It should be removed from analytics in branch
	 */
	
	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		feature2.put("stage", "PRODUCTION");
		featureID2 = f.addFeature(seasonID, feature2.toString(), "ROOT", sessionToken);

		
	}
	
	
	@Test (dependsOnMethods="addBranch", description="Add features to analytics in master")
	public void addFeaturesToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add featureID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		response = an.addFeatureToAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in branch");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");


	}
	

	
	@Test (dependsOnMethods="addFeaturesToAnalyticsInMaster", description="Remove  feature reported to analytics in master from analytics in branch")
	public void removeReportedFeatureInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//delete featureID to analytics featureOnOff
		String response = an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect analytics response");		
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in branch");


		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	@Test (dependsOnMethods="removeReportedFeatureInBranch", description="Add feature reported in master to analytics in branch - not allowed")
	public void addReportedFeatureInBranchByAction() throws IOException, JSONException, InterruptedException{
		
		br.checkoutFeature(branchID, featureID1, sessionToken);
		
		String dateFormat = an.setDateFormat();

		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was updated. Expected update to fail");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	@Test (dependsOnMethods="addReportedFeatureInBranchByAction", description="Add feature reported in master to analytics in branch - not allowed")
	public void addReportedFieldInBranchByGlobal() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		
		//added by global action
		String input = an.addFeatureOnOff(response, featureID1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was updated. Expected update to fail");
	
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	@Test (dependsOnMethods="addReportedFieldInBranchByGlobal", description="Remove feature from analytics in master")
	public void removeFeatureFromAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		
		String dateFormat = an.setDateFormat();
		
		String response = an.deleteFeatureFromAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature1 not removed from analytics: " + response);
		response = an.deleteFeatureFromAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature2 not removed from analytics: " + response);
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Analytics was not updated and feature were not removed.");
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Analytics was not updated and feature were not removed in branch.");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");


	}
	
	@Test (dependsOnMethods="removeFeatureFromAnalyticsInMaster", description="Add unchecked features to analytics in branch")
	public void addUncheckedFeaturesToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//added by individual action
		
		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Unchecked Feature1 was added to analytics in branch: " + response);
		
		//add by global action
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Unchecked Feature1 was added to analytics in branch: " + response);
			
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");	
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was not changed");

	}
	
	@Test (dependsOnMethods="addUncheckedFeaturesToAnalyticsInBranch", description="Add features to analytics in branch")
	public void addFeaturesToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		br.checkoutFeature(branchID, featureID1, sessionToken);
		br.checkoutFeature(branchID, featureID2, sessionToken);
		
		String dateFormat = an.setDateFormat();
				
		//added by individual action
		
		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature1 not added to analytics in branch: " + response);
		
		//add by global action
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featuresOnOff in branch.");
			
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature added in branch is seen in master");
	
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, featureID1), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");	
		Assert.assertTrue(validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), featureID1), "Branch in runtime was not updated");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");
		Assert.assertTrue(validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeProd.message), featureID2), "Branch in runtime was not updated");

	}
	
	@Test (dependsOnMethods="addFeaturesToAnalyticsInBranch", description="Remove features from analytics in branch")
	public void removeFeaturesInBranch() throws IOException, JSONException, InterruptedException{

		String dateFormat = an.setDateFormat();

		String response = an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);
		response = an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults  responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, featureID1), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		Assert.assertFalse((validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), featureID1)), "Features were not removed from branch dev runtime");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertFalse((validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), featureID2)), "Features were not removed from branch prod runtime");
	
	}
	
	@Test (dependsOnMethods="removeFeaturesInBranch", description="Add new feature to analytics in branch")
	public void addNewFeatureInBranch() throws IOException, JSONException, InterruptedException{
		//add new feature to branch
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		feature.put("stage", "PRODUCTION");
		feature.put("name", "F3");
		featureID3 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);

		
		String dateFormat = an.setDateFormat();
		
		//added by individual action to branch
		String response = an.addFeatureToAnalytics(featureID3, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature from branch was added to analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, featureID3), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");

	}
	
	@Test (dependsOnMethods="addNewFeatureInBranch", description="Report to analytics in master  feature that exists only in branch")
	public void reportInMasterBranchFeature() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.addFeatureToAnalytics(featureID3, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was  updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was not changed");

	}
	
	@Test (dependsOnMethods="reportInMasterBranchFeature", description="Remove new feature from analytics in branch")
	public void removeNewFeatureInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//added by individual action to branch
		String response = an.deleteFeatureFromAnalytics(featureID3, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not removed from analytics in branch: " + response);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature was not removed analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, featureID3), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");

	}
	
	
	@Test (dependsOnMethods="removeNewFeatureInBranch", description="Add new feature to analytics in branch and then in master")
	public void addFeatureInBranchAndMaster() throws IOException, JSONException, InterruptedException{
		
		String dateFormat = an.setDateFormat();
		
		//added by individual action to branch
		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in branch " + response);
		//add to master
		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated in master" + response);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, featureID1), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was not changed");

	}
	
	
	@Test (dependsOnMethods="addFeatureInBranchAndMaster", description="Uncheck out reported feature in branch")
	public void uncheckedReportedFeatureInBranch() throws IOException, JSONException, InterruptedException{
		
		String dateFormat = an.setDateFormat();
		
		br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, featureID1), "Feature was deleted from report in runtime");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was not changed");

	}
	
	@Test (dependsOnMethods="uncheckedReportedFeatureInBranch", description="Delete feature from master")
	public void deleteFeature() throws Exception{
		br.checkoutFeature(branchID, featureID1, sessionToken);
		JSONObject masterFeature = new JSONObject(f.getFeature(featureID1, sessionToken));
		
		String dateFormat = an.setDateFormat();
		
		int code = f.deleteFeature(featureID1, sessionToken);
		Assert.assertTrue(code==200, "Can't delete feature");
		
		//find feature1 in branch
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONArray features = branch.getJSONArray("features");
		String newId = "";
		for (int i=0; i< features.size(); i++){
			if (features.getJSONObject(i).getString("name").equals(masterFeature.getString("name"))){
				newId = features.getJSONObject(i).getString("uniqueId");
			}
		}
		
		
		Assert.assertFalse(masterFeature.getString("uniqueId").equals(newId), "Deleted feature id was not updated in branch");
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was deleted from analytics in branch.");
		JSONObject jsonAnalytics = new JSONObject(response);
		boolean found = false;
		for (int i=0; i< jsonAnalytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics").size(); i++){
			if (jsonAnalytics.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics").getJSONObject(i).getString("id").equals(newId))
				found = true;
		}
		Assert.assertTrue(found, "Incorrect feature id in analytics");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature was not deleted analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, newId), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was not changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was not changed");

	}
	
	
	@Test (dependsOnMethods="deleteFeature", description="report feature in branch only, then cancel checkout. It should be removed from analytics in branch")
	public void uncheckedFeatureInBranch() throws IOException, JSONException, InterruptedException{
		//featureID2 is checked out to branch, but not reported
		
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
	
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Feature was not added to analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature was added to analytics in master");
		
		String dateFormat = an.setDateFormat();
				
		br.cancelCheckoutFeature(branchID, featureID2, sessionToken);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not removed from analytics in branch.");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject input = new JSONObject(responseDev.message);
		Assert.assertTrue(input.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAndConfigurationsForAnalytics").size()==1, "Incorrect number of features reported to analytics in runtime");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		

	}
	
	private boolean validateSentToAnalyticsInExperiment(String input, String featureId) throws JSONException{
		//runtime dev/prod experiments section
		JSONObject feature = new JSONObject(f.getFeatureFromBranch(featureId, branchID, sessionToken));
		String name = feature.getString("namespace") + "." + feature.getString("name");
		
		JSONArray features = new JSONObject(input).getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAndConfigurationsForAnalytics");
		for (int i=0; i< features.size(); i++){
			if (features.getString(i).equals(name)){
				return true;
			}
		}	
		return false; //if feature not found
	}
	
	private boolean validateSentToAnalyticsInBranch(JSONObject root, String featureId) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features) {
			JSONObject feature = new JSONObject(f);
			if (feature.getString("uniqueId").equals(featureId)) {
				if (feature.has("sendToAnalytics"))
					return feature.getBoolean("sendToAnalytics");
			}	
		}
		return false;
	}
	
	private String addExperiment(String experimentName) throws IOException, JSONException{

		return baseUtils.addExperiment(m_analyticsUrl, false, false);

	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private JSONArray featureOnOff(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics");
	}
	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
