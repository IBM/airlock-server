package tests.restapi.analytics;

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


public class AddOrderingRuleToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String branchType = null;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInEnabledExperiment(analyticsUrl, true);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdEnabledExperiment(analyticsUrl, true).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
		
		this.branchType = branchType;
	}

	
	@Test (description="Add feature and ordering rule to the season - use seaprate api")
	public void addORToAnalytics1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		String response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(validateFeatureInAnalytics(anResponse, jsonOR.getString("namespace")+"."+jsonOR.getString("name")), "Ordering rule not found in analytics");

		response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature2 was not added to analytics" + response);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID3 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "feature3 was not added to the season" + featureID2);

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		if (branchType.equals("ProdExp")  || branchType.equals("DevExp")) {
			responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
			JSONObject rootInBranch = RuntimeDateUtilities.getBranchFeaturesList(responseDev.message);
			Assert.assertTrue(validateFeatureInRuntime(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
					
			responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
				
		}

	}
	

	@Test (description="Add feature and ordering rule to the season - use update global analytics object")
	public void addORToAnalytics2() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false))  ;
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		
		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, ORId1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(validateFeatureInAnalytics(anResponse, jsonOR.getString("namespace")+"."+jsonOR.getString("name")), "Ordering rule not found in analytics");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		if (branchType.equals("ProdExp")  || branchType.equals("DevExp")) {
			responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
			JSONObject rootInBranch = RuntimeDateUtilities.getBranchFeaturesList(responseDev.message);
			Assert.assertTrue(validateFeatureInRuntime(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
					
			responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
				
		}

	}
	
	
	@Test (description="Add MTX and ordering rule to the season - use seaprate api")
	public void addORToAnalytics3() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false))  ;
		
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		String response = an.addFeatureToAnalytics(ORId1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(validateFeatureInAnalytics(anResponse, jsonOR.getString("namespace")+"."+jsonOR.getString("name")), "Ordering rule not found in analytics");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		if (branchType.equals("ProdExp")  || branchType.equals("DevExp")) {
			responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
			JSONObject rootInBranch = RuntimeDateUtilities.getBranchFeaturesList(responseDev.message);
			Assert.assertTrue(validateFeatureInRuntime(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
					
			responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
				
		}
		

	}
	
	@Test (description="Add mtx and ordering rule to the season - use update global analytics object")
	public void addORToAnalytics4() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONObject feature =  new JSONObject(FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false))  ;
		
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		feature.put("name", RandomStringUtils.randomAlphabetic(5));
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, feature.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature2 was not added to the season" + featureID2);

		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		String configuration = "{\"" + featureID2 + "\": 1.5 }";
		jsonOR.put("configuration", configuration);
		String ORId1 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(ORId1.contains("error"), "Configuration1 was not added to the season" + ORId1);

		
		//add featureID to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, ORId1);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "OrderingRule was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(validateFeatureInAnalytics(anResponse, jsonOR.getString("namespace")+"."+jsonOR.getString("name")), "Ordering rule not found in analytics");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		if (branchType.equals("ProdExp")  || branchType.equals("DevExp")) {
			responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
			JSONObject rootInBranch = RuntimeDateUtilities.getBranchFeaturesList(responseDev.message);
			Assert.assertTrue(validateFeatureInRuntime(rootInBranch, featureID1), "ordering rule sendToAnalytics=false in runtime development file ");
					
			responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, "MASTER", dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
			Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
				
		}

	}
	
	private boolean validateFeatureInAnalytics(String analytics, String featureName) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresInAnalytics = analyticsDataCollection.getJSONArray("featuresAndConfigurationsForAnalytics");
		
		for (int i=0; i<featuresInAnalytics.size(); i++){
			JSONObject singleFeature = featuresInAnalytics.getJSONObject(i);
			if (singleFeature.getString("name").equals(featureName))
				return true;
		}

		return false;

	}
	
	private boolean validateFeatureInRuntime(JSONObject root, String parentID) throws JSONException{
		JSONArray allFeatures = root.getJSONArray("features");
		for (int i=0; i<allFeatures.size(); i++){
			JSONObject singleFeature = allFeatures.getJSONObject(i);
			if (singleFeature.getString("uniqueId").equals(parentID))
				return singleFeature.getJSONArray("orderingRules").getJSONObject(0).getBoolean("sendToAnalytics");
		}
		return false;
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
