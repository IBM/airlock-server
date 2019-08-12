package tests.restapi.scenarios.experiments;

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


public class AddConfigurationAfterFeatureCheckout {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureIDMaster;
	private String featureIDBranch;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/*
	 Add feature to master
	 Add configuration to feature in master
	 Checkout feature
	 Add configuration to feature in master and try to add the same config name to feature in branch
	 Add configuration to feature in branch and try to add the same config name to feature in matser 
	*/
	

	@Test (description ="Add new branch") 
	public void addBranch () throws IOException, JSONException, InterruptedException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "branch was not created");
	}
	
	@Test (dependsOnMethods="addBranch", description ="Add feature and configuration to master and check them out") 
	public void addFeatureAndConfigToMaster () throws IOException, JSONException, InterruptedException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureIDMaster = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureIDMaster.contains("error"), "Feature was not added to master: " + featureIDMaster);
			
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		
		String configID = f.addFeatureToBranch(seasonID, "MASTER", jsonCR.toString(), featureIDMaster, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the master: " + configID);
	
		String response = br.checkoutFeature(branchID, featureIDMaster, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out");
		
	}
	
	@Test(dependsOnMethods="addFeatureAndConfigToMaster", description ="Add configuration to feature in master and try to add the same config name to feature in branch")
	public void addConfigToMasterAndBranch() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		
		String configIDInMaster = f.addFeatureToBranch(seasonID, "MASTER", jsonCR.toString(), featureIDMaster, sessionToken);
		Assert.assertFalse(configIDInMaster.contains("error"), "Configuration rule was not added to the master: " + configIDInMaster);
	
		String configID = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureIDMaster, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to the branch even though the same config name exists in master");
	}
	
	@Test(dependsOnMethods="addConfigToMasterAndBranch", description ="Add configuration to feature in branch and try to add the same config name to feature in matser")
	public void addConfigToBranchAndMaster() throws Exception{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR3");
		
		String configIDInBranch = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureIDMaster, sessionToken);		
		Assert.assertFalse(configIDInBranch.contains("error"), "Configuration rule was not added to the branch: " + configIDInBranch);
	
		String configIDInMaster = f.addFeatureToBranch(seasonID, "MASTER", jsonCR.toString(), featureIDMaster, sessionToken);		
		Assert.assertTrue(configIDInMaster.contains("error"), "Configuration rule was added to the master even though the same config name exists in branch");
	}	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
