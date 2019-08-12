package tests.restapi.integration;



import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
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

public class CreateBranchFromMasterAndBranch {
	protected String seasonID1;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String featureID4;
	private String branchID1;
	protected String productID;
	protected String config;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	private BranchesRestApi br;


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;

		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();

		p.setURL(url);
		s.setURL(url);
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		br = new BranchesRestApi();
		br.setURL(url);
	}
	
	//
	@Test (description = "Add product, season and features")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID1.contains("error"), "Test should pass, but instead failed: " + seasonID1 );
		
		//create features
		
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonF.put("minAppVersion", "0.5");
		featureID1 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not created: " + featureID1 );
		
		String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "0.6");
		String configID1 = f.addFeature(seasonID1, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "CR1 was not created: " + configID1);

		
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonF.put("minAppVersion", "1.0");
		featureID2 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not created: " + featureID2 );
		
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "1.6");
		String configID2 = f.addFeature(seasonID1, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "CR2 was not created: " + configID2);

		
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonF.put("minAppVersion", "0.8");
		featureID3 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not created: " + featureID3 );
		
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "0.8");
		String configID3 = f.addFeature(seasonID1, jsonCR.toString(), featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "CR3 was not created: " + configID3);
	}
	

	@Test (dependsOnMethods="addComponents", description = "Create branch1")
	public void createBranchFromMaster() throws Exception{
		String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID1 = br.createBranch(seasonID1, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		
		br.checkoutFeature(branchID1, featureID3, sessionToken);
		
		Assert.assertTrue(validateMinAppVersion(featureID1, branchID1, "0.5", "0.6"), "Incorrect minAppVersions for feature1 in branch1");
		Assert.assertTrue(validateMinAppVersion(featureID2, branchID1, "1.0", "1.6"), "Incorrect minAppVersions for feature2 in branch1");
		Assert.assertTrue(validateMinAppVersion(featureID3, branchID1, "0.8", "0.8"), "Incorrect minAppVersions for feature3 in branch1");
		
		
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonF.put("minAppVersion", "2.5");
		featureID4 = f.addFeatureToBranch(seasonID1, branchID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not created: " + featureID1 );
		
		String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "2.6");
		String configID4 = f.addFeatureToBranch(seasonID1, branchID1, jsonCR.toString(), featureID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "CR4 was not created: " + configID4);
		
		

	}
	
	@Test (dependsOnMethods="createBranchFromMaster", description = "Create branch2")
	public void createBranchFromBranch() throws Exception{
		String branch = FileUtils.fileToString(config + "experiments/branch2.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch2");
		String branchID2 = br.createBranch(seasonID1, branchJson.toString(), branchID1, sessionToken);
		
		Assert.assertTrue(validateMinAppVersion(featureID1, branchID2, "0.5", "0.6"), "Incorrect minAppVersions for feature1 in branch2");
		Assert.assertTrue(validateMinAppVersion(featureID2, branchID2, "1.0", "1.6"), "Incorrect minAppVersions for feature2 in branch2");
		Assert.assertTrue(validateMinAppVersion(featureID3, branchID2, "0.8", "0.8"), "Incorrect minAppVersions for feature3 in branch2");
		
		String input = br.getBranchWithFeatures(branchID2, sessionToken);
		Assert.assertTrue(validateMinAppVersionGetBranch(input, "2.5", "2.6"), "Incorrect minAppVersions for feature4 in branch2");
		
		
		String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonF.put("minAppVersion", "2.5");
		String featureID5 = f.addFeatureToBranch(seasonID1, branchID2, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature1 was not created: " + featureID5 );
		
		String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		jsonCR.put("minAppVersion", "2.6");
		String configID5 = f.addFeatureToBranch(seasonID1, branchID2, jsonCR.toString(), featureID5, sessionToken);
		Assert.assertFalse(configID5.contains("error"), "CR1 was not created: " + configID5);
		
		Assert.assertTrue(validateMinAppVersion(featureID5, branchID2, "2.5", "2.6"), "Incorrect minAppVersions for feature5 in branch2");

	}
	
	private boolean validateMinAppVersionGetBranch(String features, String featureVer, String configVersion) throws JSONException{
		boolean correctVersion = true;
		
		JSONArray jsonFeatures = new JSONObject(features).getJSONArray("features");
		for (int i=0; i<jsonFeatures.size(); i++){
			JSONObject json = jsonFeatures.getJSONObject(i);
			if (json.getString("branchStatus").equals("NEW")){
				if (!json.getString("minAppVersion").equals(featureVer))
					return false;
				else if (!json.getJSONArray("configurationRules").getJSONObject(0).getString("minAppVersion").equals(configVersion))
					return false;
			}

		}	
		return correctVersion;
		
	}
	

	private boolean validateMinAppVersion(String featureId, String branchId, String featureVer, String configVersion) throws JSONException{
		boolean correctVersion = true;
	
		String feature = f.getFeatureFromBranch(featureId, branchId, sessionToken);
		
		JSONObject json = new JSONObject(feature);
		if (!json.getString("minAppVersion").equals(featureVer))
			return false;
		else if (!json.getJSONArray("configurationRules").getJSONObject(0).getString("minAppVersion").equals(configVersion))
			return false;
			
		return correctVersion;
		
	}

	
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
