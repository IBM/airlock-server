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

public class UpdateChildEntitlementNameInMaster {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String configID1;
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
		- Update name/namespace of a child entitlement in master. It should be updated also in the list of branchChildrenList in runtime.
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws Exception {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		String expID = baseUtils.addExperiment(m_analyticsUrl, false, false);
		
		JSONObject variant = new JSONObject( FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false));
		variant.put("branchName", "branch1");
		String variantID = exp.createVariant(expID, variant.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "variant was not created: " + variantID);

		//enable experiment
		String airlockExperiment = exp.getExperiment(expID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + expID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(expID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement1);
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);
		
		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = purchasesApi.addPurchaseItem(seasonID, configuration1, entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		br.checkoutFeature(branchID, entitlementID1, sessionToken);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update name") 
	public void updateChildrenInMaster () throws Exception {
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID2, sessionToken));
		JSONObject config = new JSONObject(purchasesApi.getPurchaseItem(configID1, sessionToken));
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONObject entitlement = branch.getJSONArray("entitlements").getJSONObject(0);
		
		Assert.assertTrue(entitlement.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Incorrect configuration name");
		Assert.assertTrue(entitlement.getJSONArray("branchEntitlementItems").getString(0).equals(e2.getString("namespace")+"."+e2.getString("name")), "Incorrect subfeature name");

		e2.put("name", "newE2");
		e2.put("namespace", "newE2namespace");
		purchasesApi.updatePurchaseItem(seasonID, entitlementID2, e2.toString(), sessionToken);
		config.put("name", "newCR1");
		config.put("namespace", "newCR1namespace");
		purchasesApi.updatePurchaseItem(seasonID, configID1, config.toString(), sessionToken);
		
		branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		entitlement = branch.getJSONArray("entitlements").getJSONObject(0);
		e2 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID2, sessionToken));
		config = new JSONObject(purchasesApi.getPurchaseItem(configID1, sessionToken));
		Assert.assertTrue(entitlement.getJSONArray("branchEntitlementItems").getString(0).equals(e2.getString("namespace")+"."+e2.getString("name")), "Sub-entitlement name was not updated in branch");
		Assert.assertTrue(entitlement.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Incorrect configuration name");
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject feature1 = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature1.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "Configuration name was not updated in branch");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		feature1 = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature1.getJSONArray("branchConfigurationRuleItems").getString(0).equals(config.getString("namespace")+"."+config.getString("name")), "entitlement name was not updated in branch");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");	
	}
	
	@Test (dependsOnMethods="updateChildrenInMaster", description ="Update name") 
	public void updateParentInMaster () throws Exception {
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		String res = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot cancel checkout");
		
		String dateFormat = RuntimeDateUtilities.getCurrentTimeStamp();
		Thread.sleep(2000);
		
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID1, sessionToken));
		
		JSONObject branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		JSONObject feature = branch.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature.getString("branchFeatureParentName").equals(e1.getString("namespace")+"."+e1.getString("name")), "Incorrect parent name");

		e1.put("name", "newE1");
		e1.put("namespace", "newE1namespace");
		purchasesApi.updatePurchaseItem(seasonID, entitlementID1, e1.toString(), sessionToken);
		
		e1 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID1, sessionToken));
		branch = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		feature = branch.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature.getString("branchFeatureParentName").equals(e1.getString("namespace")+"."+e1.getString("name")), "Parent name was not updated in branch");
		
		//check if files were changed
		Thread.sleep(3000);
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		JSONObject feature1 = new JSONObject(responseDev.message).getJSONArray("branches").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature1.getString("branchFeatureParentName").equals(e1.getString("namespace")+"."+e1.getString("name")), "entitlement name was not updated in branch");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
		//branches		
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was  changed");		
		feature1 = new JSONObject(branchesRuntimeDev.message).getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(feature1.getString("branchFeatureParentName").equals(e1.getString("namespace")+"."+e1.getString("name")), "entitlement name was not updated in branch");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
