package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateChildEntitlementeStageInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String configID;
	private String filePath;
	private SeasonsRestApi s;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	private ExperimentsRestApi exp ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		m_analyticsUrl = analyticsUrl;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*	
		- Checkout parent entitlement. A list of its children appear in branchChildrenList even if they are not checked out. 
		- Verify that dev sub items are removed from the branchConfigRuleItems or branchEntitlementItems lists in branch production runtime

	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws Exception {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);		

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("branchName", "branch1");
		variantJson.put("stage", "PRODUCTION");
		exp.createVariant(experimentID, variantJson.toString(), sessionToken);

		//enable experiment
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement1);
		
		jsonE.put("name", "E1");
		jsonE.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);
		
		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
        String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonCR = new JSONObject(configuration1);
        jsonCR.put("name", "CR2");
        jsonCR.put("stage", "PRODUCTION");
        configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID1, sessionToken);
        Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to the season");

		br.checkoutFeature(branchID, entitlementID1, sessionToken);
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Update sub-items stage in branch") 
	public void updateSubEntitlementStageInBranch () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));

		entitlement2.put("stage", "DEVELOPMENT");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Sub-item was not updated in branch: " + response);
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject contentDev = new JSONObject(responseDev.message);
		Assert.assertTrue(contentDev.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchEntitlementItems").size()==1, "Development feature is not listed as a child of production feature in development runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		JSONObject contentProd = new JSONObject(responseProd.message);
		Assert.assertTrue(contentProd.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchEntitlementItems").size()==0, "Development feature is listed as a child of production feature in production runtime");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		contentDev = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(contentDev.getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchEntitlementItems").size()==1, "Development entitlement is not listed as a child of production feature in branches development runtime");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		contentProd = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(contentProd.getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchEntitlementItems").size()==0, "Development entitlement is listed as a child of production feature in branches production runtime");

	}
	
	@Test (dependsOnMethods="updateSubEntitlementStageInBranch", description ="Update configuration rule stage in branch") 
	public void updateConfigurationRuleStageInBranch () throws Exception {

		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);

		JSONObject config = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID, branchID, sessionToken));

		config.put("stage", "DEVELOPMENT");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID, config.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuration rule was not updated in branch: " + response);
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject contentDev = new JSONObject(responseDev.message);
		Assert.assertTrue(contentDev.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchConfigurationRuleItems").size()==1, "Development CR is not listed as a child of production feature in development runtime");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production file was changed");
		JSONObject contentProd = new JSONObject(responseProd.message);
		Assert.assertTrue(contentProd.getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchConfigurationRuleItems").size()==0, "Development CR is listed as a child of production feature in production runtime");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		contentDev = new JSONObject(branchesRuntimeDev.message);
		Assert.assertTrue(contentDev.getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchConfigurationRuleItems").size()==1, "Development CR is not listed as a child of production feature in branches development runtime");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		contentProd = new JSONObject(branchesRuntimeProd.message);
		Assert.assertTrue(contentProd.getJSONArray("entitlements").getJSONObject(0).getJSONArray("branchConfigurationRuleItems").size()==0, "Development CR is listed as a child of production feature in branches production runtime");
	}	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
