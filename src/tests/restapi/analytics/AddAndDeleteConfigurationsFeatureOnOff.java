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

public class AddAndDeleteConfigurationsFeatureOnOff {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String filePath;
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
	
	/*Test flow:
	 * create feature with configs, add config to analytics, delete parent feature
	 * create feature with hierarchy of configs, add lower config to analytics, delete parent feature
	 */
	

	@Test (description="Add a feature and configuration rule. Add configuration to featuresAndConfigurationsForAnalytics")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		//add feature

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		JSONObject f1Json = new JSONObject(feature1);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, configuration, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Configuration rule was not added to the season" + featureID2);
		JSONObject c1Json = new JSONObject(configuration);
		
		//add configuration featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(anResponse)==1, "The feature was not added to analytics");
		
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		JSONObject json = new JSONObject(display);
		JSONObject displayFeature = json.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").getJSONObject(0);
		Assert.assertTrue(displayFeature.getString("name").equals(f1Json.getString("namespace")+"."+f1Json.getString("name")));		
	}
	

	@Test (dependsOnMethods="addComponents", description="Delete mix and validate analytics")
	public void deleteParentFeature() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(featureID1,branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "Feature was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		JSONObject json = new JSONObject(display);		
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").size()==0);		

		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "MIX group was not deleted from development runtime file");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");

	}
	
	@Test (dependsOnMethods="deleteParentFeature", description="Add feature and configuration rule. Add another configuration rule under the first config rule. Add the last configuration to featuresAndConfigurationsForAnalytics")
	public void addHierarchyComponents() throws IOException, JSONException, InterruptedException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		JSONObject f1Json = new JSONObject(feature1);
		
		String feature2 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Configuration rule was not added to the season" + featureID2);

		String feature3 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		featureID3 = f.addFeatureToBranch(seasonID, branchID, feature3, featureID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Configuration rule was not added to the season" + featureID3);
		
		//add featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, featureID3);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		JSONObject json = new JSONObject(display);
		JSONObject displayFeature = json.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").getJSONObject(0);
		Assert.assertTrue(displayFeature.getString("name").equals(f1Json.getString("namespace")+"."+f1Json.getString("name")));		


	}
	
	@Test (dependsOnMethods="addHierarchyComponents", description="Delete parent feature and validate analytics")
	public void deleteParentOfHierarchyFeature() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "MIX group was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");
		
		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, response).equals("true"));
		JSONObject json = new JSONObject(display);		
		Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("analyticsDataCollectionByFeatureNames").size()==0);		

		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(root.getJSONArray("features").size()==0, "Feature was not deleted from development runtime file");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");

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
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
