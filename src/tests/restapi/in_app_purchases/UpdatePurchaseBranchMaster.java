package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class UpdatePurchaseBranchMaster {

	protected String productID;
	protected String seasonID;
	protected String seasonID2;
	private String branchID;
	private String branchID2;
	private String branchIDNewSeason;
	private JSONObject eJson;
	private String filePath;
	private SeasonsRestApi s;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	private AnalyticsRestApi an;
	private InputSchemaRestApi schema;
	String entitlementID1;
	String entitlementID2;
	String entitlementID3;
	String entitlementID4;
	String mixConfigID;
	String mixID1;
	String mixID2;
	String configID2;
	String configID3;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		schema = new InputSchemaRestApi();
		schema.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		eJson = new JSONObject(entitlement);
		String sch = schema.getInputSchema(seasonID, sessionToken);
		JSONObject jsonSchema = new JSONObject(sch);
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
		jsonSchema.put("inputSchema", new JSONObject(schemaBody));
		String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}


	@Test (description ="E1 + (MIXCR->C1+C2) -> MIX -> (E2 + MIX -> (E3 + E4) ),  checkout E2 ")
	public void addBranch1 () throws Exception {
		branchID = addBranch("branch1",BranchesRestApi.MASTER);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		eJson.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");

		jsonCR.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");

		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, entitlementMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlement was not added to the season: " + mixID1);

		eJson.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

		entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID2 = purchasesApi.addPurchaseItem(seasonID, entitlementMix, mixID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "entitlement was not added to the season: " + mixID2);

		eJson.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);

		eJson.put("name", "E4");
		entitlementID4 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement was not added to the season: " + entitlementID4);

		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

		//check that entitlement was checked out
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray entitlements = brJson.getJSONArray("entitlements");

		JSONArray entitlementsInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

		//E1
		Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get branch" );	//get branch
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlements" );	//get branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");	//get entitlement from branch

		//configurations under E1
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );

		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");	//get entitlement from branch

		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");	//get entitlement from branch

		//E2
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get branch" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get entitlements" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch

		//E3
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlements" );

		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch

		//E4
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getJSONArray("entitlements").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement4 status is not checked_out in get branch" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getJSONArray("entitlements").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement4 status is not checked_out in get branch" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement4 status is not checked_out in get entitlement");	//get entitlement from branch

	}

	@Test(dependsOnMethods = "addBranch1")
	public void duplicateSeason () throws Exception {
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		String allBranches = br.getAllBranches(seasonID2,sessionToken);
		JSONObject jsonBranch = new JSONObject(allBranches);
		branchIDNewSeason = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
	}

	@Test(dependsOnMethods = "duplicateSeason")
	public void duplicateBranchInSameSeason() throws Exception{
		branchID2 = addBranch("branch2",branchID);
	}

	@Test(dependsOnMethods = "duplicateBranchInSameSeason")
	public void updateDescriptionInMaster() throws Exception{
		String e1 = purchasesApi.getPurchaseItem(entitlementID1,sessionToken);
		JSONObject f1Json = new JSONObject(e1);
		f1Json.put("description","helloqw");
		String response = purchasesApi.updatePurchaseItem(seasonID,entitlementID1,f1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");
		String f1branch = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID,sessionToken);
		JSONObject f1branchJson = new JSONObject(f1branch);
		Assert.assertFalse(f1branchJson.getString("description").equals("helloqw"));
		f1branch = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID2,sessionToken);
		f1branchJson = new JSONObject(f1branch);
		Assert.assertFalse(f1branchJson.getString("description").equals("helloqw"));
		String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
		Assert.assertFalse(branchWithFeature.contains("helloqw"));
		branchWithFeature = br.getBranchWithFeatures(branchIDNewSeason,sessionToken);
		Assert.assertFalse(branchWithFeature.contains("helloqw"));
	}

	@Test(dependsOnMethods = "updateDescriptionInMaster")
	public void updateNameInMaster() throws Exception{
		//runtime file
		String dateFormat = an.setDateFormat();
		String e1 = purchasesApi.getPurchaseItem(entitlementID1,sessionToken);
		JSONObject f1Json = new JSONObject(e1);
		f1Json.put("name","E1rename");
		String response = purchasesApi.updatePurchaseItem(seasonID,entitlementID1,f1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");
		String f1branch = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID,sessionToken);
		JSONObject f1branchJson = new JSONObject(f1branch);
		Assert.assertTrue(f1branchJson.getString("name").equals("E1rename"));
		f1branch = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID2,sessionToken);
		f1branchJson = new JSONObject(f1branch);
		Assert.assertTrue(f1branchJson.getString("name").equals("E1rename"));
		String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
		Assert.assertTrue(branchWithFeature.contains("E1rename"));
		branchWithFeature = br.getBranchWithFeatures(branchIDNewSeason,sessionToken);
		Assert.assertFalse(branchWithFeature.contains("E1rename"));

		//runtime file
		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID,dateFormat,sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID2,dateFormat,sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
	}

	@Test(dependsOnMethods = "updateNameInMaster")
	public void updateEntitlementInBranch() throws Exception{
		String e1 = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID,sessionToken);
		JSONObject f1Json = new JSONObject(e1);
		f1Json.put("description","hellobranch");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,entitlementID1,f1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");
		String e1Master = purchasesApi.getPurchaseItemFromBranch(entitlementID1,BranchesRestApi.MASTER,sessionToken);
		JSONObject f1MasterJson = new JSONObject(e1Master);
		Assert.assertFalse(f1MasterJson.getString("description").equals("hellobranch"));
		String e1branch = purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID2,sessionToken);
		JSONObject e1branchJson = new JSONObject(e1branch);
		Assert.assertFalse(e1branchJson.getString("description").equals("hellobranch"));
		String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
		Assert.assertFalse(branchWithFeature.contains("hellobranch"));
		branchWithFeature = br.getBranchWithFeatures(branchIDNewSeason,sessionToken);
		Assert.assertFalse(branchWithFeature.contains("hellobranch"));

	}
	@Test(dependsOnMethods = "updateEntitlementInBranch")
	public void addUpdateDeleteInBranch() throws Exception {
		String masterString = purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString();
		String branch2String = purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString();
		String branchNewString = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString();
		eJson.put("name", "ENew");
		String newEntitlement = purchasesApi.addPurchaseItemToBranch(seasonID,branchID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(newEntitlement.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID2 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CRNew");
		String configIDNew = purchasesApi.addPurchaseItemToBranch(seasonID,branchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configIDNew.contains("error"), "Configuration rule2 was not added to the season");

		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));

		String e1 = purchasesApi.getPurchaseItemFromBranch(newEntitlement,branchID,sessionToken);
		JSONObject e1Json = new JSONObject(e1);
		e1Json.put("description","hellobranch");
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,newEntitlement,e1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");

		String mix = purchasesApi.getPurchaseItemFromBranch(mixConfigID2,branchID,sessionToken);
		JSONObject mixJson = new JSONObject(mix);
		mixJson.put("maxFeaturesOn",2);
		response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,mixConfigID2,mixJson.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");

		String cr1 = purchasesApi.getPurchaseItemFromBranch(configIDNew,branchID,sessionToken);
		JSONObject cr1Json = new JSONObject(cr1);
		e1Json.put("description","hellobranch");
		response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,configIDNew,cr1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");

		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));

		int deleted = purchasesApi.deletePurchaseItemFromBranch(newEntitlement,branchID,sessionToken);
		Assert.assertTrue(deleted == 200, "entitlement was not deleted");

		deleted = purchasesApi.deletePurchaseItemFromBranch(mixConfigID2,branchID,sessionToken);
		Assert.assertTrue(deleted == 200, "entitlement was not deleted");

		deleted = purchasesApi.deletePurchaseItemFromBranch(configIDNew,branchID,sessionToken);
		Assert.assertTrue(deleted == 200, "entitlement was not deleted");

		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));
	}
	
	@Test(dependsOnMethods = "addUpdateDeleteInBranch")
	public void reorderInBranch() throws Exception {
		String masterString = purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString();
		String branch2String = purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString();
		String branchNewString = purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString();

		String mix = purchasesApi.getPurchaseItemFromBranch(mixConfigID,branchID,sessionToken);
		JSONObject mixJson = new JSONObject(mix);
		JSONArray rules = mixJson.getJSONArray("configurationRules");
		JSONObject rule1 = rules.getJSONObject(0);
		rules.set(0,rules.getJSONObject(1));
		rules.set(1,rule1);
		mixJson.put("configurationRules",rules);
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,mixConfigID,mixJson.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not updated");
		
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
		Assert.assertTrue(purchasesApi.getPurchasesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));
	}

	@Test(dependsOnMethods = "reorderInBranch")
	public void addTwobranches() throws Exception {

		eJson.put("name", "ENew");
		String newEntitlement = purchasesApi.addPurchaseItemToBranch(seasonID,branchID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(newEntitlement.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		String newEntitlement2 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID2, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(newEntitlement2.contains("error"), "entitlement was not added to the season: " + entitlementID1);
	}

	@Test(dependsOnMethods = "addTwobranches")
	public void analytics() throws Exception {

		eJson.put("name", "Eanalytics");
		String newEntitlement = purchasesApi.addPurchaseItemToBranch(seasonID,BranchesRestApi.MASTER, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(newEntitlement.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		String response = an.addFeatureToAnalytics(newEntitlement,BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement2 was sent to analytics");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(configuration);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		String configID1 = purchasesApi.addPurchaseItemToBranch(seasonID,BranchesRestApi.MASTER, jsonConfig.toString(), newEntitlement, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		response = an.addAttributesToAnalytics(newEntitlement, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not added to analytics" + response);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		response = an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER,  inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		response = br.checkoutFeature(branchID,  newEntitlement, sessionToken);
		Assert.assertFalse(response.contains("error"), "could not check out" + response);

		//try to delete from branch - error
		response = an.deleteFeatureFromAnalytics(newEntitlement,branchID, sessionToken);
		Assert.assertTrue(response.contains("The status of the item is being sent to analytics from the master branch. To stop sending item status to analytics, first go to the master and stop sending to analytics. Then, return to the branch and stop sending to analytics"), "entitlement2 was sent to analytics");
		JSONArray attributes2 = new JSONArray();
		response = an.addAttributesToAnalytics(newEntitlement, branchID, attributes2.toString(), sessionToken);
		Assert.assertTrue(response.contains("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch"), "entitlement was not added to analytics" + response);
		JSONArray inputFields2 = new JSONArray();
		response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields2.toString(), sessionToken);
		Assert.assertTrue(response.contains("You must report all input fields that are reported in the master, in addition to the input fields that you want to report in the branch."), "Analytics was not updated" + response);

		//delete from master
		response = an.deleteFeatureFromAnalytics(newEntitlement,BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement2 was sent to analytics");
		response = an.addAttributesToAnalytics(newEntitlement, BranchesRestApi.MASTER, attributes2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not added to analytics" + response);
		response = an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER,  inputFields2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		//now remove from branch
		response = an.deleteFeatureFromAnalytics(newEntitlement,branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Fanalytics2 was sent to analytics");
		response = an.addAttributesToAnalytics(newEntitlement, branchID, attributes2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Fanalytics was not added to analytics" + response);
		inputFields.put("context.device.locale");
		response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
	}

	private String addBranch(String branchName,String source) throws JSONException, IOException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), source, sessionToken);
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}

