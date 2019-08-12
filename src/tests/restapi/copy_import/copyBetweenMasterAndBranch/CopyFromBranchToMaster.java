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
public class CopyFromBranchToMaster {
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
	private String srcBranchID;
	
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
			srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);			
		}catch (Exception e){
			srcBranchID = null;
		}		
	}
	
	/*
		F1
		F2 -> MIXCR ->CR1, CR2, MIXOR -> OR1
		F3 -> CR3 -> CR4
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

		String orderingRuleMix = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		String mixOrderingRuleID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, orderingRuleMix, featureID2, sessionToken);
		Assert.assertFalse(mixOrderingRuleID.contains("error"), "ordering rule mix was not added to the season");

		String or1 = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(or1);
		jsonCR.put("name", "OR1");
		String orID1 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonOR.toString(), mixOrderingRuleID, sessionToken);
		Assert.assertFalse(orID1.contains("error"), "ordering rule was not added to the mtx");

		
		jsonCR.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Copy a none checked out feature from barnch to its master")
	public void copyNonCheckedOutFeatureToMaster() throws IOException, JSONException{
		//copy F2 under F1
		String response = f.copyItemBetweenBranches(featureID2, featureID1, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID2, featureID1, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID2, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
		
		int responseCode = f.deleteFeature(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New feature was not deleted");
	}
	
	@Test (dependsOnMethods="copyNonCheckedOutFeatureToMaster", description="Copy a checked out feature from barnch to its master")
	public void copyCheckedOutFeatureToMaster() throws IOException, JSONException{
		String response = br.checkoutFeature(srcBranchID, featureID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature was not checked out: " + response);
		
		//copy F2 under F3		
		response = f.copyItemBetweenBranches(featureID2, featureID3, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID2, featureID3, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID2, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
		
		int responseCode = f.deleteFeature(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New feature was not deleted");
	}
	
	@Test (dependsOnMethods="copyCheckedOutFeatureToMaster", description="Copy a new feature from barnch to its master")
	public void copyNewFeatureToMaster() throws IOException, JSONException{
		//checkout F3
		String response = br.checkoutFeature(srcBranchID, featureID3, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature was not checked out: " + response);
		
		//create F4->F5 in branch under F3
		String feature4 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		String featureID4 = f.addFeatureToBranch(seasonID, srcBranchID, feature4, featureID3, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season");
			
		String feature5 = FileUtils.fileToString(filePath + "feature5.txt", "UTF-8", false);
		String featureID5 = f.addFeatureToBranch(seasonID, srcBranchID, feature5, featureID4, sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature was not added to the season");

		
		//copy F4 under F1		
		response = f.copyItemBetweenBranches(featureID4, featureID1, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID4, featureID1, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID4, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
		
		int responseCode = f.deleteFeature(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New feature was not deleted");
	}
	
	@Test (dependsOnMethods="copyNewFeatureToMaster", description="Copy a new+checkedOut+none features from barnch to its master")
	public void copyMixBranchStatusesToMaster() throws IOException, JSONException{
		//createInMaster F6,CR6 under F1
		//checkout F1 (F6 remains un-checked out) 
		//add F7 and under CR9, ConfigMtx -> CR7, CR8 under F1 in branch
		
		String feature6 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature6);
		jsonF.put("description", "feature6 desc");
		jsonF.put("name", "feature6");
				
		String featureID6 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID6.contains("error"), "Feature was not added to the season");
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR6");
		String configID6 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),featureID1, sessionToken);
		Assert.assertFalse(configID6.contains("error"), "Feature was not added to the season");

		String response = br.checkoutFeature(srcBranchID, featureID1, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature was not checked out: " + response);
		
		String feature7 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		jsonF = new JSONObject(feature7);
		jsonF.put("description", "feature7 desc");
		jsonF.put("name", "feature7");
				
		String featureID7 = f.addFeatureToBranch(seasonID, srcBranchID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID7.contains("error"), "Feature was not added to the season");
		
		jsonCR.put("name", "CR9");
		String configID9 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(),featureID7, sessionToken);
		Assert.assertFalse(configID9.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID7, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		jsonCR.put("name", "CR7");
		String configID7 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID7.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR8");
		String configID8 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID8.contains("error"), "Feature was not added to the season");
		
		//create F8 in master
		String feature8 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		jsonF = new JSONObject(feature8);
		jsonF.put("description", "feature8 desc");
		jsonF.put("name", "feature8");
		
		String featureID8 = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID8.contains("error"), "Feature was not added to the season");
		
		//copy F1 from branch to master under F8		
		response = f.copyItemBetweenBranches(featureID1, featureID8, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		response = f.copyItemBetweenBranches(featureID1, featureID8, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));		
	}
	
	@Test (dependsOnMethods="copyMixBranchStatusesToMaster", description="Copy a new production feature from barnch to its master")
	public void copyNewProductionFeatureToMaster() throws Exception{
		//create new production feature in branch
		String feature10 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature10);
		jsonF.put("description", "feature 10 desc");
		jsonF.put("name", "feature10");
		jsonF.put("stage", "PRODUCTION");
		
		String featureID10 = f.addFeatureToBranch(seasonID, srcBranchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID10.contains("error"), "Feature was not added to the season");
					
		
		//copy F10 under ROOT
		String rootId = f.getBranchRootId(seasonID, BranchesRestApi.MASTER, sessionToken);
		
		String response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		String dateFormat = an.setDateFormat();
		
		response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		
		JSONObject newFeatureJson = new JSONObject(newFeature);
		Assert.assertTrue(newFeatureJson.getString("stage").equals("DEVELOPMENT"), "copied faeture is in production");
		
		
		//validate that only master development runtime was changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, srcBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, srcBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");
		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}