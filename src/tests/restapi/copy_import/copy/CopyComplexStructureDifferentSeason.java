package tests.restapi.copy_import.copy;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyComplexStructureDifferentSeason {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String featureID1;
	protected String targetFeatureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private boolean runOnMaster;
	private String srcBranchID;
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
		s = new SeasonsRestApi();
		s.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}

	}
	
	/*
		F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
					->F3 -> CR3 -> CR4
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		
		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);

		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}
		
		//create feature tree in season1
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, feature3, mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		String configID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
		


	}
	
	@Test (dependsOnMethods="addComponents", description="Copy the whole structure under target feature")
	public void copyFeature() throws IOException, JSONException{
		//copy target features:
		String targetFeature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(targetFeature);
		json.put("name", "target");
		targetFeatureID = f.addFeatureToBranch(seasonID2, destBranchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(targetFeatureID.contains("error"), "Feature was not added to the season" + targetFeatureID);
						
		String response = f.copyItemBetweenBranches(featureID1, targetFeatureID, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}