package tests.restapi.copy_import.import_features;

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


public class ImportFeatureInProductionStageSameSeason {
	protected String seasonID;
	protected String productID;
	protected String mixIdToCopy;
	private String featureID1;
	protected String featureID2;
	private String configID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
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
	
	@Test (description="Create feature structure in production stage ")
	public void addProductionComponents() throws IOException, JSONException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixIdToCopy = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixIdToCopy.contains("error"), "Feature was not added to the season" + mixIdToCopy);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, jsonF.toString(), mixIdToCopy, sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added to the season" + featureID1);
		
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season" + mixConfigID);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("stage", "PRODUCTION");
		configID = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season" + configID);

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR2 = new JSONObject(configuration2);
		jsonCR.put("stage", "PRODUCTION");
		String configID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR2.toString(), configID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule was not added to the season" + configID2);

		
		//mixId tree will be copied under feature2
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject jsonF2 = new JSONObject(feature2);
		jsonF2.put("stage", "PRODUCTION");
		featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonF2.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature 2 was not added to the season" + featureID2);
		

	}
	
	@Test (dependsOnMethods="addProductionComponents", description="Copy feature tree in production stage under another feature")
	public void importFeature() throws IOException{
		String featureToImport = f.getFeatureFromBranch(mixIdToCopy, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(featureToImport, featureID2, "ACT", null, "suffix1", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
	}
	
	
	@Test (dependsOnMethods="importFeature", description="Validate feature tree that should be converted to development")
	public void validateFeaturesStage() throws IOException, JSONException{
		
		String feature2 = f.getFeatureFromBranch(featureID2, srcBranchID, sessionToken);
		JSONObject jsonF2 = new JSONObject(feature2);
		Assert.assertFalse(jsonF2.getString("stage").equals("DEVELOPMENT"), "The target feature was converted to development during copy");
		
		JSONObject jsonF1 = jsonF2.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(jsonF1.getString("stage").equals("DEVELOPMENT"), "The feature was not converted to development during copy");
		
		JSONObject jsonCR = jsonF1.getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0);	
		Assert.assertTrue(jsonCR.getString("stage").equals("DEVELOPMENT"), "The configuration rule was not converted to development during copy");

		JSONObject jsonCR2 = jsonCR.getJSONArray("configurationRules").getJSONObject(0);	
		Assert.assertTrue(jsonCR2.getString("stage").equals("DEVELOPMENT"), "The configuration rule was not converted to development during copy");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}