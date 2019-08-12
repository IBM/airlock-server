package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class DuplicateBranch2 {

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
    protected AnalyticsRestApi an;
    String entitlementID1;
    String entitlementID2;
    String entitlementID3;

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

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
        eJson = new JSONObject(entitlement);
    }


    @Test (description ="E1 -> E2 -> E3, checkout E3,uncheckout E2")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        eJson.put("name", "E1");
        entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

        eJson.put("name", "E2");
        entitlementID2 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementID1, sessionToken);
        Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

        eJson.put("name", "E3");
        entitlementID3 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), entitlementID2, sessionToken);
        Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);

        String response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

        //check that entitlement was checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray entitlements = brJson.getJSONArray("entitlements");

        //get entitlements from branch
        JSONArray entitlementsInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

        //E1
        Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get branch");    //get branch
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");    //get entitlement from branch
        Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement status is not checked_out in get entitlements from branch");

        response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
        Assert.assertTrue(response.contains("error"), "entitlement1 was checked out twice");

        //E2
        Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get branch");
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get entitlement");    //get entitlement from branch
        Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get entitlements");

        response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
        Assert.assertTrue(response.contains("error"), "entitlement2 was checked out twice");

        //E3
        Assert.assertTrue(entitlements.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch");
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlements");    //get entitlement from branch
        Assert.assertTrue(entitlementsInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch");

        response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
        Assert.assertTrue(response.contains("error"), "entitlement3 was checked out twice");
        String res = br.cancelCheckoutFeature(branchID, entitlementID2, sessionToken);
        Assert.assertFalse(res.contains("error"), "entitlement was not unchecked out: " + res);

        // send E3 to analytics
        response = an.addFeatureToAnalytics(entitlementID3, branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "not send to analytics");
    }

    @Test(dependsOnMethods = "addBranch1")
    public void duplicateSeason () throws Exception {
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        seasonID2 = s.addSeason(productID, season, sessionToken);
        String allBranches = br.getAllBranches(seasonID2,sessionToken);
        JSONObject jsonBranch = new JSONObject(allBranches);
        branchID2 = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
        assertBranchDuplication(true,seasonID2);
        
        //validate analytics
        String analytics = an.getGlobalDataCollection(seasonID2,branchID2, "BASIC", sessionToken);
        JSONObject json = new JSONObject(analytics);
        Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics").size()==1, "Analytics was not updated in master");
    }

    @Test(dependsOnMethods = "duplicateSeason")
    public void duplicateBranchInSameSeason() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        assertBranchDuplication(false,seasonID);
        
        //validate analytics
        String analytics = an.getGlobalDataCollection(seasonID,branchID2, "BASIC", sessionToken);
        JSONObject json = new JSONObject(analytics);
        Assert.assertTrue(json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics").size()==1, "Analytics was not updated in master");
    }

    public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

        JSONObject entitlement = jsonBranchWithFeature.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
                0,1,new String[]{"ns1.E2"}, 0 ,"ROOT");

        JSONObject entitlement3 = jsonBranchWithFeature.getJSONArray("entitlements").getJSONObject(1);
        assertItemDuplicated(entitlement3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,"ns1.E2");

        purchasesApi.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==2, "Incorrect number of checked out entitlements in dev branches1 runtime file");
        entitlement = branchWithFeatureRuntime.getJSONObject(0);
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
                0,1,new String[]{"ns1.E2"}, 0 ,"ROOT");

        entitlement3 = branchWithFeatureRuntime.getJSONObject(1);
        assertItemDuplicated(entitlement3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,"ns1.E2");

        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out entitlements in prod branches1 runtime file");
    }

    public void assertItemDuplicated(JSONObject entitlement, String status,String id, Boolean newIds, Integer numberOfBranchConfig,String[] branchConfigNames,
    int numberOfConfig, Integer numberOfBranchEntitlements,String[] branchFeaturesNames,Integer numberOfEntitlements,String branchParentName)throws JSONException{
        Assert.assertTrue(entitlement.getString("branchStatus").equals(status));
        if(newIds) {
            Assert.assertFalse(entitlement.getString("uniqueId").equals(id));
        }
        else {
            Assert.assertTrue(entitlement.getString("uniqueId").equals(id));
        }
        //branch configs
        JSONArray branchConfigurationRuleItems = entitlement.getJSONArray("branchConfigurationRuleItems");
        Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
        for(int i = 0 ; i< numberOfBranchConfig; ++i) {
            Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
        }

        //configs
        JSONArray configurationRuleItems = entitlement.getJSONArray("configurationRules");
        Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);

        //branch features
        JSONArray branchEntitlementItems = entitlement.getJSONArray("branchEntitlementItems");
        Assert.assertTrue(branchEntitlementItems.size() == numberOfBranchEntitlements);
        for(int i = 0 ; i< numberOfBranchEntitlements; ++i) {
            Assert.assertTrue(branchEntitlementItems.getString(i).equals(branchFeaturesNames[i]));
        }

        //Only for entitlements
        //features
        if(numberOfEntitlements != null) {
            Assert.assertTrue(entitlement.getJSONArray("entitlements").size() == numberOfEntitlements);
        }
        //parent
        if(branchParentName != null) {
            Assert.assertTrue(entitlement.getString("branchFeatureParentName").equals(branchParentName));
        }

    }
    private String addBranch(String branchName,String source) throws JSONException, IOException {
        String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
        JSONObject branchJson = new JSONObject(branch);
        branchJson.put("name", branchName);
        return br.createBranch(seasonID, branchJson.toString(), source, sessionToken);
    }

    private JSONArray getBranchFeatures(String result) throws JSONException{
        JSONObject json = new JSONObject(result);
        return json.getJSONArray("entitlements");
    }

    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

