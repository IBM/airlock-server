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

public class AddFeatureToAnalyticsInGeneralRuntime {
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
	 * - add prod feature in master (seen also in branch)
	 * - add dev feature in master under the prod feature (seen also in branch)
	 * - add features to analytics in master
	 * - remove features from analytics in master (stayed in branch analytics)
	 * - remove features from analytics in branch
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

		JSONObject feature2 = new JSONObject(FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false));
		feature2.put("stage", "PRODUCTION");
		featureID2 = f.addFeature(seasonID, feature2.toString(), "ROOT", sessionToken);

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		featureID1 = f.addFeature(seasonID, feature, featureID2, sessionToken);
		
	}
	
	
	@Test (dependsOnMethods="addBranch", description="Add features to analytics in master")
	public void addFeaturesToAnalyticsInMaster() throws Exception{
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
		
		//since no feature was checked out - verify that no features exist in branch in general runtime files
		Assert.assertTrue (numberOfFeaturesInBrancheInGenaralRuntime(responseDev.message) == 0, "several features exist in branche in general dev runtime file even though no feature was checked-out");
		Assert.assertTrue (numberOfFeaturesInBrancheInGenaralRuntime(responseProd.message) == 0, "several features exist in branche in general prod runtime file even though no feature was checked-out");
		
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}
	

	
	private int numberOfFeaturesInBrancheInGenaralRuntime(String result) throws Exception{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("branches")){
				org.apache.wink.json4j.JSONArray branchesArray = json.getJSONArray("branches");
				if (branchesArray == null || branchesArray.size() == 0) {
					throw new Exception("Response doesn't contain branches array");
				}
					
				JSONObject branch = branchesArray.getJSONObject(0);
				if (branch.containsKey("features")){
					org.apache.wink.json4j.JSONArray featuresArray = branch.getJSONArray("features");
					if (featuresArray == null || featuresArray.size() == 0) {
						return 0;
						
					}
					return featuresArray.size();
				}
				else {
					return 0;
				}
			} else {
				throw new Exception( "Response doesn't contain branches array");
			}
		} catch (Exception e){
				throw new Exception( "Response is not a valid json");
		}
	}
	
	@Test (dependsOnMethods="addFeaturesToAnalyticsInMaster", description="checkout feature")
	public void checkoutFeaure() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed checkout");
		response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "failed checkout");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		JSONObject rootInBranch = getBranchFeaturesRoot(responseDev.message);
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
	    rootInBranch = getBranchFeaturesRoot(responseProd.message);
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
	}

	public static JSONObject getBranchFeaturesRoot(String result) throws JSONException{
		JSONObject json = new JSONObject();
		try{
			json = new JSONObject(result);
			if (json.containsKey("branches")){
				org.apache.wink.json4j.JSONArray branchesArray = json.getJSONArray("branches");
				if (branchesArray == null || branchesArray.size() == 0) {
					json.put("error", "Response doesn't contain branches array");
					return json;
				}
					
				JSONObject branch =  branchesArray.getJSONObject(0);
				if (branch.containsKey("features")){
					org.apache.wink.json4j.JSONArray featuresArray = branch.getJSONArray("features");
					if (featuresArray == null || featuresArray.size() == 0) {
						json.put("error", "Response doesn't contain features for first branch");
						return json;
					}
					return featuresArray.getJSONObject(0);
				}
				else {
					json.put("error", "Response doesn't contain features for first branche");
					return json;
				}
			} else {
				json.put("error", "Response doesn't contain branches array");
				return json;
			}
		} catch (Exception e){
				json.put("error", "Response is not a valid json");
				return json;
		}
		

	}

	//look for feature in the given root or one of its sub features.
	private boolean validateFeatureSentToAnalyticsInGeneralRuntimeBranches(JSONObject root, String featureId) throws JSONException{
		if (root.getString("uniqueId").equals(featureId) && root.containsKey("sendToAnalytics"))
			return root.getBoolean("sendToAnalytics");
		
		JSONArray allFeatures = root.getJSONArray("features");
		for (int i=0; i<allFeatures.size(); i++){
			JSONObject singleFeature = allFeatures.getJSONObject(i);
			if (singleFeature.getString("uniqueId").equals(featureId) && singleFeature.containsKey("sendToAnalytics"))
				return singleFeature.getBoolean("sendToAnalytics");
		}
		return false;
	}


	@Test (dependsOnMethods="checkoutFeaure", description="Remove feature from analytics in master")
	public void removeFeatureFromAnalyticsFromMaster() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//delete featureID to analytics featureOnOff
		String response = an.deleteFeatureFromAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		
		
		
		response = an.deleteFeatureFromAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		JSONObject rootInBranch = getBranchFeaturesRoot(responseDev.message);
		//feature is  not reported to analytics in master but is still reported to analytics in branch
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
	    rootInBranch = getBranchFeaturesRoot(responseProd.message);
		Assert.assertTrue(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==304, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}
	
	@Test (dependsOnMethods="removeFeatureFromAnalyticsFromMaster", description="Remove feature from analytics in branch")
	public void removeFeatureFromAnalyticsFromBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
				
		//delete featureID to analytics featureOnOff
		String response = an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		
		
		
		response = an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		JSONObject rootInBranch = getBranchFeaturesRoot(responseDev.message);
		//feature is  not reported to analytics in branch
		Assert.assertFalse(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		Assert.assertFalse(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
	    rootInBranch = getBranchFeaturesRoot(responseProd.message);
		Assert.assertFalse(validateFeatureSentToAnalyticsInGeneralRuntimeBranches(rootInBranch, featureID2), "ordering rule sendToAnalytics=false in runtime development file ");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");		
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
	}
	
	/*
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
	*/
	private String addExperiment(String experimentName) throws IOException, JSONException{

		return baseUtils.addExperiment(m_analyticsUrl, true, false);

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
	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
