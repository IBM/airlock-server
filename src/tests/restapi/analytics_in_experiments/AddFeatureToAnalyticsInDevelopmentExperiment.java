package tests.restapi.analytics_in_experiments;

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
import tests.restapi.*;

public class AddFeatureToAnalyticsInDevelopmentExperiment {
	protected String seasonID1;
	protected String seasonID2;
	protected String experimentID;
	protected String branchID1;
	protected String branchID2;
	protected String variantID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String filePath;
	protected String m_branchType;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String m_analyticsUrl;
	private SeasonsRestApi s;
	
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(m_analyticsUrl);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);

		
	}
	
	
	@Test (description="Add season2")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"2.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Can't add second season: " + seasonID2);
	}
	
	@Test (dependsOnMethods="addSeason", description="Add components")
	public void addExperiment() throws Exception{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		expJson.put("stage","DEVELOPMENT");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

			
	}
	
	//Use api for a single feature

	@Test (dependsOnMethods="addExperiment", description="Add production features ")
	public void addFeatures() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add features to season1, checkout feature2
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "Feature1");
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season " + featureID1);
		//add Feature1 to analytics in master season1
		String response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		

		
		jsonF.put("name", "Feature2");
		featureID2 = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season " + featureID2);
		br.checkoutFeature(branchID1, featureID2, sessionToken);
		
		//add Feature2 to analytics in branch1
		response = an.addFeatureToAnalytics(featureID2, branchID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	
		
		
		jsonF.put("name", "Feature3");
		String featureID3 = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added to the season2 " + featureID3);
		
		//add Feature3 to analytics in master season2
		response = an.addFeatureToAnalytics(featureID3, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	


		jsonF.put("name", "Feature4");
		String featureID4 = f.addFeatureToBranch(seasonID2, branchID2, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to branch2 " + featureID4);
		
		//add Feature4 to analytics in branch2
		response = an.addFeatureToAnalytics(featureID4, branchID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season1");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was  found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season2");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");		
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was  found in the runtime production file in season2");

		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="addFeatures", description="Move  experiment and variant  to production")
	public void moveExperimentToProduction() throws Exception{
		String dateFormat = an.setDateFormat();

		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "PRODUCTION");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season1");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		

		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season2");

		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	
	}
	

	@Test (dependsOnMethods="moveExperimentToProduction", description="Move  experiment to development")
	public void moveExperimentToDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();

		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject jsonVar = new JSONObject(variant);
		jsonVar.put("stage", "DEVELOPMENT");
		String respVar = exp.updateVariant(variantID, jsonVar.toString(), sessionToken);
		Assert.assertFalse (respVar.contains("error"), "Variant was not updated: " + respVar);
		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "DEVELOPMENT");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse (response.contains("error"), "Experiment was not updated: " + response);
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season1");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was  found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
		

		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was not found in the runtime development file in season2");

		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature3"), "The feature \"ns1.Feature3\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature4"), "The feature \"ns1.Feature4\" was  found in the runtime production file in season2");

		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
	
	}

	private boolean noExperimentAnalytics(String input){

		try {
			JSONObject json = new JSONObject(input);
			if (json.getJSONObject("experiments").getJSONArray("experiments").size() == 0)
				return true;
			else
				return false;
		} catch (JSONException e) {
			return false;
		}
	}
	
	private boolean ifRuntimeContainsFeature(String input, String featureName){

		try{
			JSONObject json = new JSONObject(input);

			if (json.containsKey("experiments")){
				JSONArray inputFields = json.getJSONObject("experiments").getJSONArray("experiments").getJSONObject(0).getJSONObject("analytics").getJSONArray("featuresAndConfigurationsForAnalytics");
				for (Object s : inputFields) {
					if (s.equals(featureName)) 
						return true;
				}
				
				return false;
			} else {

				return false;
			}
		} catch (Exception e){
				return false;
		}
	}
	
	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "DEVELOPMENT");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String seasonId, String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonId, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
