package tests.restapi.copy_import.copyBetweenMasterAndBranch;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

//when running with branches this is a copy with in the same branch
public class CopyFromMasterToBranch {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String targetFeatureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private BranchesRestApi br ;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String destBranchID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		try {			
			destBranchID = baseUtils.createBranchInExperiment(analyticsUrl);			
		}catch (Exception e){
			destBranchID = null;
		}		
	}
	
	/*
		F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
					->F3 -> CR3 -> CR4
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
			
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID3 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, feature3, "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		String configID2 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Copy a feature from master under root in branch")
	public void copyFeatureUnderRoot() throws IOException, JSONException{
		//copy F2 under ROOT
		String rootId = f.getBranchRootId(seasonID, destBranchID, sessionToken);		
		
		String response = f.copyItemBetweenBranches(featureID2, rootId, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID2, featureID1, "ACT", null, "suffix1", sessionToken, destBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID2, destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
				
	}
	
	@Test (dependsOnMethods="copyFeatureUnderRoot", description="Copy a feature from master under new feature in branch")
	public void copyFeatureUnderNew() throws IOException, JSONException{
		//create F4 in branch
		String rootId = f.getBranchRootId(seasonID, destBranchID, sessionToken);		
		
		String feature4 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		String featureID4 = f.addFeatureToBranch(seasonID, destBranchID, feature4, rootId, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season");
		
		//copy F2 under F4		
		String response = f.copyItemBetweenBranches(featureID2, featureID4, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID2, featureID4, "ACT", null, "suffix2", sessionToken, BranchesRestApi.MASTER, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
				
	}
	
	@Test (dependsOnMethods="copyFeatureUnderNew", description="Copy a feature from master under none feature in branch")
	public void copyFeatureUnderNone() throws IOException, JSONException{
		
		//copy F2 under F4		
		String response = f.copyItemBetweenBranches(featureID2, featureID1, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("Cannot paste an item under an item that is not checked out"), "Feature was copied under none feature");		
				
	}
	
	
	
	
	@Test (dependsOnMethods="copyFeatureUnderNone", description="Copy a production feature from master to branch")
	public void copyProductionFeatureToBranch() throws Exception{
		//create new production feature in master
		String feature10 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature10);
		jsonF.put("description", "feature 10 desc");
		jsonF.put("name", "feature10");
		jsonF.put("stage", "PRODUCTION");
		
		String featureID10 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID10.contains("error"), "Feature was not added to the season");
					
		an.setSleep();
		
		//copy F10 under ROOT
		String rootId = f.getBranchRootId(seasonID, destBranchID, sessionToken);
		
		String response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		String dateFormat = an.setDateFormat();
		
		response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, "suffix1", sessionToken, BranchesRestApi.MASTER, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		
		JSONObject newFeatureJson = new JSONObject(newFeature);
		Assert.assertTrue(newFeatureJson.getString("stage").equals("DEVELOPMENT"), "copied faeture is in production");
		Assert.assertTrue(newFeatureJson.getString("branchStatus").equals("NEW"), "copied string is not in status new");
		
		//validate that only master development runtime was changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, destBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was updated");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, destBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");
		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}