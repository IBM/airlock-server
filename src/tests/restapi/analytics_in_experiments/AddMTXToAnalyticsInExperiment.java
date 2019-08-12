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

public class AddMTXToAnalyticsInExperiment {
	protected String seasonID1;
	protected String seasonID2;
	protected String experimentID;
	protected String branchID1;
	protected String branchID2;
	protected String productID;
	private String parentID;
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
		expJson.put("stage","PRODUCTION");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		String variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

			
	}
	

	@Test (dependsOnMethods="addExperiment", description="Add feature in development to season1")
	public void addFeatureToSeason1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "ParentFeature");
		parentID = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentID.contains("error"), "Parent feature was not added to the season " + parentID);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, featureMix, parentID, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);
		
		jsonF.put("name", "Feature1");
		featureID1 = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season " + featureID1);
		jsonF.put("name", "Feature2");
		featureID2 = f.addFeatureToBranch(seasonID1, BranchesRestApi.MASTER, jsonF.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added to the season " + featureID2);


		
		//add features to analytics featureOnOff
		String response = an.addFeatureToAnalytics(parentID, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");
		response = an.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");
		response = an.addFeatureToAnalytics(featureID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");


		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season2");		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="addFeatureToSeason1", description="Move feature to production in season1")
	public void moveParentFeatureToProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		
		String feature = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID1, parentID, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was found in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveParentFeatureToProduction", description="Move sub-features to production in season1")
	public void moveSubFeatureToProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		feature = f.getFeature(featureID2, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		response = f.updateFeature(seasonID1, featureID2, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season1");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveSubFeatureToProduction", description="Move sub-features to development in season1")
	public void moveFeatureToDevelopment() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		feature = f.getFeature(featureID2, sessionToken);
		json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		response = f.updateFeature(seasonID1, featureID2, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveFeatureToDevelopment", description="Move parent feature to development in season1")
	public void moveParentToDevelopment() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		
		String feature = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID1, parentID, json.toString(), sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
		
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not  found in the runtime developement file in season1");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not  found in the runtime developement file in season2");
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was  found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was  found in the runtime production file in season2");
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveParentToDevelopment", description="Move MTX tree to production in season1")
	public void moveTreeToProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = f.getFeature(parentID, sessionToken);
		feature = feature.replaceAll("DEVELOPMENT", "PRODUCTION");

		String response = f.updateFeature(seasonID1, parentID, feature, sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
			
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season1");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season2");

		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season2");

		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveTreeToProduction", description="Move MTX tree to development in season1")
	public void moveTreeToDevelopment() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = f.getFeature(parentID, sessionToken);
		feature = feature.replaceAll("PRODUCTION", "DEVELOPMENT");

		String response = f.updateFeature(seasonID1, parentID, feature, sessionToken);				
		Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response);
			
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season1");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime production file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime developement file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season2");

		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime production file in season2");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was found in the runtime production file in season2");

		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="moveTreeToDevelopment", description="Remove feature from analytics in season1")
	public void removeFeatureFromAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		
		String response = an.deleteFeatureFromAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season1");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime developement file in season1");
		Assert.assertTrue(!ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was found in the runtime developement file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime developement file in season1");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");	
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="removeFeatureFromAnalytics", description="Checkout Feature1 to branch and add to analytics")
	public void addFeatureToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();		
		
		br.checkoutFeature(branchID1, featureID1, sessionToken);
		String response = an.addFeatureToAnalytics(featureID1, branchID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");

		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");	
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	

	@Test (dependsOnMethods="addFeatureToAnalyticsInBranch", description="Add feature to season2 in in prod")
	public void addFeatureToBothSeasons() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("stage", "PRODUCTION");
		jsonF.put("name", "ParentFeature");
		String parentID2 = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(parentID2.contains("error"), "Parent feature was not added to the season " + parentID2);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID2 = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, featureMix, parentID2, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);
		
		jsonF.put("name", "Feature1");
		String featureID1b = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonF.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID1b.contains("error"), "Feature1 was not added to the season " + featureID1b);
		jsonF.put("name", "Feature2");
		String featureID2b = f.addFeatureToBranch(seasonID2, BranchesRestApi.MASTER, jsonF.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID2b.contains("error"), "Feature2 was not added to the season " + featureID2b);

		
		//add featureID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(parentID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	
		response = an.addFeatureToAnalytics(featureID1b, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	
		response = an.addFeatureToAnalytics(featureID2b, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");	
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");		
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season1");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season2");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");	
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime production file in season2");
		Assert.assertTrue(ifRuntimeContainsFeature(responseProd.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime production file in season2");
		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addFeatureToBothSeasons", description="Delete feature from season1, leave it in season2")
	public void deleteFeature() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
	
		int respCode = f.deleteFeature(parentID, sessionToken);
		Assert.assertTrue(respCode == 200, "Feature was not deleted");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID1, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.ParentFeature"), "The feature \"ns1.ParentFeature\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature1"), "The feature \"ns1.Feature1\" was not found in the runtime development file in season1");
		Assert.assertTrue(ifRuntimeContainsFeature(responseDev.message, "ns1.Feature2"), "The feature \"ns1.Feature2\" was not found in the runtime development file in season1");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
		prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID2, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

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
		variantJson.put("stage", "PRODUCTION");
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
