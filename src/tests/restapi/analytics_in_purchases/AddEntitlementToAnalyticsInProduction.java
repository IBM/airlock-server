package tests.restapi.analytics_in_purchases;

import java.io.IOException;

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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class AddEntitlementToAnalyticsInProduction {
	protected String seasonID;
	protected String productID;
	protected String entitlementID1;
	protected String entitlementID2;
	private String entitlementID3;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
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
	 * - add dev entitlement in master (seen also in branch)
	 * - add prod entitlement in master  (seen also in branch)
	 * - checkout entitlement
	 * - remove this entitlement in branch (fail)
	 * - add same entitlement in branch (fail)
	 * - remove entitlement from master
	 * - add entitlement in branch (not seen in master)
	 * - add same entitlement in master (should be ok, counter not updated)
	 * - remove entitlement from branch
	 * - add new entitlement to branch and add it to analytics
	 * - remove new entitlement from analytics in branch
	 * - entitlement reported in both master and branch can be removed from master and should remain in branch
	 * - report entitlement in master & branch and uncheck it from branch, it remains in analytics in branch
	 */
	
	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);	//in production
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");	//in production
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject entitlementObj = new JSONObject(entitlement);
		entitlementObj.put("name", "Entitlement1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlementObj.toString(), "ROOT", sessionToken);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		JSONObject entitlement2 = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false));
		entitlement2.put("stage", "PRODUCTION");
		entitlement2.put("name", "Entitlement2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2.toString(), "ROOT", sessionToken);
	}
	
	@Test (dependsOnMethods="addBranch", description="Add entitlementד to analytics in master")
	public void addEntitlementsToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add entitlementID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(entitlementID1, BranchesRestApi.MASTER, sessionToken);
		response = an.addFeatureToAnalytics(entitlementID2, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in master");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in branch");
		
		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement1"), "Entitlement1 not sent to analytics in experiment");
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement2"), "Entitlement2 not sent to analytics in experiment");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}
	
	@Test (dependsOnMethods="addEntitlementsToAnalyticsInMaster", description="Remove  entitlement reported to analytics in master from analytics in branch")
	public void removeReportedEntitlementInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//delete entitlementID to analytics featureOnOff
		String response = an.deleteFeatureFromAnalytics(entitlementID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect analytics response");		
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff in branch");
		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement1"), "Entitlement1 not sent to analytics in experiment");
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement2"), "Entitlement2 not sent to analytics in experiment");

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
	
	@Test (dependsOnMethods="removeReportedEntitlementInBranch", description="Add entitlement reported in master to analytics in branch - not allowed")
	public void addReportedEntitlementInBranchByAction() throws IOException, JSONException, InterruptedException{
		
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		
		String dateFormat = an.setDateFormat();

		String response = an.addFeatureToAnalytics(entitlementID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was updated. Expected update to fail");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featureOnOff");

		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement1"), "Entitlement1 not sent to analytics in experiment");
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement2"), "Entitlement2 not sent to analytics in experiment");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "Runtime production feature file was changed");
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	@Test (dependsOnMethods="addReportedEntitlementInBranchByAction", description="Add entitlement reported in master to analytics in branch - not allowed")
	public void addReportedFieldInBranchByGlobal() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		
		//added by global action
		String input = an.addFeatureOnOff(response, entitlementID1);
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
		Assert.assertTrue(prodChanged.code ==304, "Runtime production feature file was changed");
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}
	
	@Test (dependsOnMethods="addReportedFieldInBranchByGlobal", description="Remove feature from analytics in master")
	public void removeEntitlementFromAnalyticsInMaster() throws IOException, JSONException, InterruptedException{
		br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		
		String dateFormat = an.setDateFormat();
		
		String response = an.deleteFeatureFromAnalytics(entitlementID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement1 not removed from analytics: " + response);
		response = an.deleteFeatureFromAnalytics(entitlementID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement2 not removed from analytics: " + response);
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Analytics was not updated and feature were not removed.");
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Analytics was not updated and feature were not removed in branch.");

		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		JSONObject featuresInAnalytics = new JSONObject(analyticsReponse);
		Assert.assertTrue(featuresInAnalytics.getJSONArray("analyticsDataCollectionByFeatureNames").size()==0, "Incorrect number of features sent to analytics in experiment");
		
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
	
	@Test (dependsOnMethods="removeEntitlementFromAnalyticsInMaster", description="Add unchecked entitlements to analytics in branch")
	public void addUncheckedEntitlementsToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//added by individual action
		
		String response = an.addFeatureToAnalytics(entitlementID1, branchID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Unchecked Entitlement1 was added to analytics in branch: " + response);
		
		//add by global action
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeatureOnOff(response, entitlementID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Unchecked Entitlement1 was added to analytics in branch: " + response);
			
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
	
	@Test (dependsOnMethods="addUncheckedEntitlementsToAnalyticsInBranch", description="Add entitlements to analytics in branch")
	public void addEntitlementsToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		
		String dateFormat = an.setDateFormat();
				
		//added by individual action
		
		String response = an.addFeatureToAnalytics(entitlementID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement1 not added to analytics in branch: " + response);
		
		//add by global action
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeatureOnOff(response, entitlementID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==2, "Incorrect number of featuresOnOff in branch.");
			
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature added in branch is seen in master");
	
		String analyticsReponse = an.getExperimentGlobalDataCollection(experimentID, sessionToken);
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement1"), "Entitlement1 not sent to analytics in experiment");
		Assert.assertTrue(analyticsFeaturesInExperiment(analyticsReponse, "Entitlement2"), "Entitlement2 not sent to analytics in experiment");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID1), "Branch in runtime development was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID2), "Branch in runtime production was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");	
		Assert.assertTrue(validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), entitlementID1), "Branch in runtime was not updated");
		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");
		Assert.assertTrue(validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeProd.message), entitlementID2), "Branch in runtime was not updated");

	}
	
	@Test (dependsOnMethods="addEntitlementsToAnalyticsInBranch", description="Remove entitlements from analytics in branch")
	public void removeEntitlementsInBranch() throws IOException, JSONException, InterruptedException{

		String dateFormat = an.setDateFormat();

		String response = an.deleteFeatureFromAnalytics(entitlementID1, branchID, sessionToken);
		response = an.deleteFeatureFromAnalytics(entitlementID2, branchID, sessionToken);

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in branch");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults  responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID1), "Branch in runtime development was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID2), "Branch in runtime production was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		Assert.assertFalse((validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), entitlementID1)), "Features were not removed from branch dev runtime");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertFalse((validateSentToAnalyticsInBranch(new JSONObject(branchesRuntimeDev.message), entitlementID2)), "Features were not removed from branch prod runtime");
	
	}
	
	@Test (dependsOnMethods="removeEntitlementsInBranch", description="Add new entitlement to analytics in branch")
	public void addNewEntitlementInBranch() throws IOException, JSONException, InterruptedException{
		//add new entitlement to branch
		JSONObject entitlement = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlement.put("stage", "PRODUCTION");
		entitlement.put("name", "Entitlement3");
		
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement.toString(), "ROOT", sessionToken);
		
		String dateFormat = an.setDateFormat();
		
		//added by individual action to branch
		String response = an.addFeatureToAnalytics(entitlementID3, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Feature was not added to analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Feature from branch was added to analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID3), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID3), "Branch in runtime production was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");

	}
	
	
	@Test (dependsOnMethods="addNewEntitlementInBranch", description="Remove new entitlement from analytics in branch")
	public void removeNewEntitlementInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//added by individual action to branch
		String response = an.deleteFeatureFromAnalytics(entitlementID3, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement was not removed from analytics in branch: " + response);

		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Entitlement was not removed analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==0, "Incorrect number of featureOnOff in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID3), "Branch in runtime was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertFalse(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID3), "Branch in runtime production was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");
	}
	

	
	@Test (dependsOnMethods="removeNewEntitlementInBranch", description="Uncheck out reported entitlement in branch")
	public void uncheckedReportedEntitlementInBranch() throws IOException, JSONException, InterruptedException{
		String response = an.addFeatureToAnalytics(entitlementID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement2 was not reported to analytics in master");
		
		String dateFormat = an.setDateFormat();
		
		response = br.cancelCheckoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement2 was not unchecked in branch");
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Entitlement doesn't appears in analytics in branch.");
		
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "VERBOSE", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Entitlement was not added to analytics in master");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID2), "Entitlement was deleted from report in runtime");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was not changed");
		Assert.assertTrue(validateSentToAnalyticsInExperiment(responseDev.message, entitlementID2), "Entitlement was deleted from report in runtime production");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		//Vicky: i would expect the branch runtime file to be written here... Please take a look. 
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was not changed");
	}
	

	private boolean validateSentToAnalyticsInExperiment(String input, String entitlementID) throws JSONException{
		//runtime dev/prod experiments section
		JSONObject entitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID, branchID, sessionToken));
		String name = entitlement.getString("namespace") + "." + entitlement.getString("name");
		
		JSONArray entitlements = new JSONObject(input).getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAndConfigurationsForAnalytics");
		for (int i=0; i< entitlements.size(); i++){
			if (entitlements.getString(i).equals(name)){
				return true;
			}
		}	
		return false; //if entitlement not found
	}
	
	private boolean validateSentToAnalyticsInBranch(JSONObject root, String entitlementID) throws JSONException{
		JSONArray entitlements = root.getJSONArray("entitlements");
		for (Object f : entitlements) {
			JSONObject entitlement = new JSONObject(f);
			if (entitlement.getString("uniqueId").equals(entitlementID)) {
				if (entitlement.has("sendToAnalytics"))
					return entitlement.getBoolean("sendToAnalytics");
			}	
		}
		return false;
	}
	

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
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
	
	private boolean analyticsFeaturesInExperiment(String analytics, String entitlementName) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONArray featuresToAnalytics = json.getJSONArray("analyticsDataCollectionByFeatureNames");
		for(int i=0; i<featuresToAnalytics.size(); i++){
			if (featuresToAnalytics.getJSONObject(i).getString("name").contains(entitlementName))
				return featuresToAnalytics.getJSONObject(i).getBoolean("sendToAnalytics") ;
		}	
		return false;
	}
	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
