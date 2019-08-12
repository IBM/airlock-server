package tests.restapi.analytics;

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
import tests.restapi.*;

public class AddDeleteSingleFeatureToFeatureOnOff {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String filePath;
	protected String m_branchType;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
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
        m_branchType = branchType;
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
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	
	//Use api for a single feature

	@Test (description="Add feature in development to featuresAndConfigurationsForAnalytics")
	public void addFeatureToBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season " + featureID1);
		
		//add featureID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalytics(root, featureID1), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		}
	
	@Test (dependsOnMethods="addFeatureToBranch", description="feature in production featuresAndConfigurationsForAnalytics")
	public void addFeatureToBranchInProduction() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		//add feature
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season " + featureID2);
		
		//add featureID to analytics featureOnOff
		String response = an.addFeatureToAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==2, "The feature was not added to analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateSentToAnalytics(root, featureID2), "The field \"sendToAnalytics\" was not updated for feature");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime development feature file was not updated");
		JSONObject rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertTrue(validateSentToAnalytics(rootProd, featureID2), "The field \"sendToAnalytics\" was not updated for feature");

		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}

	@Test (dependsOnMethods="addFeatureToBranchInProduction", description="Delete single feature from featuresAndConfigurationsForAnalytics")
	public void deleteFeatureFromBranch() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
			
		//delete featureID to analytics featureOnOff
		String response = an.deleteFeatureFromAnalytics(featureID2, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect analytics response");		

		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not removed from analytics");
		
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateSentToAnalytics(root, featureID2), "The field \"sendToAnalytics\" was not updated for feature in dev file " + featureID2);
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime production feature file was not updated");
		JSONObject rootProd = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		Assert.assertFalse(validateSentToAnalytics(rootProd, featureID2), "The field \"sendToAnalytics\" was not updated for feature  in prod file " + featureID2);

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}

	private int numberOfFeature(String input){
		
		try{
			JSONObject json = new JSONObject(input);
			JSONObject analytics = json.getJSONObject("analyticsDataCollection");
			JSONArray inputFields = analytics.getJSONArray("featuresAndConfigurationsForAnalytics");
				return inputFields.size();
			
		} catch (Exception e){
				return -1;
		}
	}
	
	private boolean validateSentToAnalytics(JSONObject root, String featureId) throws JSONException{
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
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
