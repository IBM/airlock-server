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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CopyMIXConfigurationDifferentProduct {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	private String productID2;
	protected String featureID2;
	protected String configID;
	private String mixConfigID;
	private String mixConfigID2;
	private String mixId;
	private String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;

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
		this.runOnMaster = runOnMaster;
	}
	
	/*
	MIX Config under feature - allowed
	MIX Config under config - allowed
	MIX Config under mix of configs - allowed
	MIX Config under root - not allowed
	MIX Config under mix of features - not allowed
		
	 */
	

	
	@Test (description="Create first season with feature and configuration")
	public void addComponents() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration was not added to the season");
		
	}
	
	@Test (dependsOnMethods="addComponents", description="Create new product with all components")
	public void createNewProduct() throws IOException, JSONException{
		//create second product
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);

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
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID2, destBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixId = f.addFeatureToBranch(seasonID2, destBranchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID2 = f.addFeatureToBranch(seasonID2, destBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");
		
		String mixConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID2 = f.addFeatureToBranch(seasonID2, destBranchID, mixConfiguration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Feature was not added to the season");

	}
	

	@Test (dependsOnMethods="createNewProduct", description="Copy configuration under feature in the new product. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderFeature() throws IOException, JSONException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, featureID2, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was copied with existing name ");

		response = f.copyItemBetweenBranches(mixConfigID, featureID2, "ACT", null, "suffix1", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderFeature", description="Copy mix configuration under mix feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixFeature() throws IOException{
						
		String response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under features mix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderMixFeature", description="Copy mix configuration under root in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		
		String response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, null, sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied under root ");
		
		response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, "suffix3", sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under root");
	}
	
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy mix configuration under configuration in the same feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, null, sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was copied with existing name");
		
		response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, "suffix5", sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}

	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy mix configuration under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderMixConfiguration() throws IOException, JSONException{
		String response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, null, sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuration mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, "suffix6", sessionToken,srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	
	@AfterTest
	private void reset(){

		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}