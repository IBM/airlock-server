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

public class AddAndDeleteMIXFeatureOnOff {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String mixId;
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
	 * add mix of features, add one mix child to analytics and delete parent mix
	 * add mix of features, add mix child to analytics and delete parent mix
 	* create mix of features under a parent feature , add one mix feature to analytics, delete parent feature
	 * -create mix of features under a parent feature , add mix to analytics, delete parent feature
	 * 
	 */
	

	@Test (description="Add mix with 2 features. Add one of the features to featuresAndConfigurationsForAnalytics")
	public void addComponents1() throws IOException, JSONException, InterruptedException{
		//add feature
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season");
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, mixId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, mixId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);

		
		//add featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");

	}
	

	@Test (dependsOnMethods="addComponents1", description="Delete mix and validate analytics")
	public void deleteMIX1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(mixId, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "MIX group was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");
		
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
	
	@Test (dependsOnMethods="deleteMIX1", description="Add mix with 2 features. Add mix to featuresAndConfigurationsForAnalytics")
	public void addComponents2() throws IOException, JSONException, InterruptedException{
		//add feature
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season");
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, mixId, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, mixId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);

		
		//add featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, mixId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");

	}
	

	@Test (dependsOnMethods="addComponents2", description="Delete mix and validate analytics")
	public void deleteMIX2() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(mixId, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "MIX group was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");

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
	
	@Test (dependsOnMethods="deleteMIX2", description="Add feature. Add mix with 2 features under the parent feature. Add one of the mix features to featuresAndConfigurationsForAnalytics")
	public void addHierarchyComponents1() throws IOException, JSONException, InterruptedException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season");
		
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, mixId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeatureToBranch(seasonID, branchID, feature3, mixId, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season" + featureID1);

		
		//add featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, featureID2);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");

	}
	
	@Test (dependsOnMethods="addHierarchyComponents1", description="Delete parent feature and validate analytics")
	public void deleteFeatureFromBranch1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "MIX group was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");
		

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
	

	@Test (dependsOnMethods="deleteFeatureFromBranch1", description="Add feature. Add mix with 2 features under the parent feature. Add mix to featuresAndConfigurationsForAnalytics")
	public void addHierarchyComponents2() throws IOException, JSONException, InterruptedException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season" + featureID1);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season");
		
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, mixId, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season" + featureID2);
		
		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeatureToBranch(seasonID, branchID, feature3, mixId, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season" + featureID1);

		
		//add featureID2 to analytics featureOnOff
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");		
		String input = an.addFeatureOnOff(response, mixId);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==1, "The feature was not added to analytics");

	}
	
	@Test (dependsOnMethods="addHierarchyComponents2", description="Delete parent feature and validate analytics")
	public void deleteFeatureFromBranch2() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		int responseCode = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "MIX group was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(numberOfFeature(response)==0, "The feature was not removed from analytics");

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
