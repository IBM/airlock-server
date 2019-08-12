package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class DuplicateBranch4 {

	protected String productID;
	protected String seasonID;
	protected String seasonID2;
	private String branchID;
	private String branchID2;
	private JSONObject eJson;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	String entitlementID1;
	String entitlementID2;
	String entitlementID3;
	String entitlementID4;
	String mixID1;

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

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		eJson = new JSONObject(entitlement);
	}


	@Test (description ="E1 -> E2 -> (E3 + E4), checkout E3 then move E4 under E1")
	public void addBranch1 () throws Exception {
		branchID = addBranch("branch1",BranchesRestApi.MASTER);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		eJson.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

		eJson.put("name","E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

		eJson.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

		eJson.put("name","E4");
		entitlementID4 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement was not added to the season: " + entitlementID4);

		String response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
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

		response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement1 was checked out twice");

		//E2
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get branch" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get entitlements" );

		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch
		response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement2 was checked out twice");

		//E3
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch" );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlements" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status was incorrectly changed in get entitlement");	//get entitlement from branch

		//E4 is not in branch array
		Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").size()==1 );
		Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
				.getString("branchStatus").equals("NONE"), "entitlement4 status is not NONE in get entitlements" );

		//move E4.
		JSONObject F1Json = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1,branchID,sessionToken));
		JSONObject F4Json = new JSONObject(purchasesApi.getPurchaseItem(entitlementID4,sessionToken));
		F1Json.getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").remove(1);
		F1Json.getJSONArray("entitlements").add(F4Json);
		response = purchasesApi.updatePurchaseItemInBranch(seasonID,branchID,entitlementID1,F1Json.toString(),sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not added to the season: " + entitlementID4);
	}

	@Test(dependsOnMethods = "addBranch1")
	public void duplicateSeason () throws Exception {
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		String allBranches = br.getAllBranches(seasonID2,sessionToken);
		JSONObject jsonBranch = new JSONObject(allBranches);
		branchID2 = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		assertBranchDuplication(true,seasonID2);
	}

	@Test(dependsOnMethods = "duplicateSeason")
	public void duplicateBranchInSameSeason() throws Exception{
		branchID2 = addBranch("branch2",branchID);
		assertBranchDuplication(false,seasonID);
	}

	public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
		String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
		JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

		JSONObject feature = jsonBranchWithFeature.getJSONArray("entitlements").getJSONObject(0);
		assertItemDuplicated(feature,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
				0,2,new String[]{"ns1.E2","ns1.E4"}, 1 ,"ROOT");

		JSONObject features2 = feature.getJSONArray("entitlements").getJSONObject(0);
		assertItemDuplicated(features2,"CHECKED_OUT",entitlementID2,newIds,0,new String[]{},
				0,1,new String[]{"ns1.E3"}, 1 ,null);

		JSONObject feature3 = features2.getJSONArray("entitlements").getJSONObject(0);
		assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
				0,0,new String[]{}, 0 ,null);

		purchasesApi.setSleep();
		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
		JSONArray branchWithFeatureRuntime = getBranchEntitlements(branchesRuntimeDev.message);
		Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");
		feature = branchWithFeatureRuntime.getJSONObject(0);
		assertItemDuplicated(feature,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
				0,2,new String[]{"ns1.E2","ns1.E4"}, 1 ,"ROOT");

		features2 = feature.getJSONArray("entitlements").getJSONObject(0);
		assertItemDuplicated(features2,"CHECKED_OUT",entitlementID2,newIds,0,new String[]{},
				0,1,new String[]{"ns1.E3"}, 1 ,null);

		feature3 = features2.getJSONArray("entitlements").getJSONObject(0);
		assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
				0,0,new String[]{}, 0 ,null);


		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
		Assert.assertTrue(getBranchEntitlements(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out entitlements in prod branches1 runtime file");

	}

	public void assertItemDuplicated(JSONObject feature1, String status,String id,Boolean newIds, Integer numberOfBranchConfig,String[] branchConfigNames,
			int numberOfConfig, Integer numberOfBranchFeatures,String[] branchFeaturesNames,Integer numberOfFeatures,String branchParentName)throws JSONException{
		Assert.assertTrue(feature1.getString("branchStatus").equals(status));
		if(newIds) {
			Assert.assertFalse(feature1.getString("uniqueId").equals(id));
		}
		else {
			Assert.assertTrue(feature1.getString("uniqueId").equals(id));
		}
		//branch configs
		JSONArray branchConfigurationRuleItems = feature1.getJSONArray("branchConfigurationRuleItems");
		Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
		for(int i = 0 ; i< numberOfBranchConfig; ++i) {
			Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
		}

		//configs
		if(!feature1.has("configurationRules")){
			Assert.assertTrue(numberOfConfig == 0);
		}
		else {
			JSONArray configurationRuleItems = feature1.getJSONArray("configurationRules");
			Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);
		}

		//branch entitlements
		JSONArray branchFeaturesItems = feature1.getJSONArray("branchEntitlementItems");
		Assert.assertTrue(branchFeaturesItems.size() == numberOfBranchFeatures);
		for(int i = 0 ; i< numberOfBranchFeatures; ++i) {
			Assert.assertTrue(branchFeaturesItems.getString(i).equals(branchFeaturesNames[i]));
		}

		//Only for entitlements
		//entitlements
		if(numberOfFeatures != null) {
			Assert.assertTrue(feature1.getJSONArray("entitlements").size() == numberOfFeatures);
		}
		//parent
		if(branchParentName != null) {
			Assert.assertTrue(feature1.getString("branchFeatureParentName").equals(branchParentName));
		}

	}
	private String addBranch(String branchName,String source) throws JSONException, IOException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), source, sessionToken);
	}

	private JSONArray getBranchEntitlements(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		return json.getJSONArray("entitlements");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}

