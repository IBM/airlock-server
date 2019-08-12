package tests.restapi.in_app_purchases;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class DuplicateBranch5 {

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
    String mixID2;

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

    @Test (description ="E1 -> MIX -> (E2 + MIX -> (E3 + E4) ),  checkout E3 uncheckout E2 ")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        eJson.put("name", "E1");
        entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

        String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
        mixID1 = purchasesApi.addPurchaseItem(seasonID, featureMix, entitlementID1, sessionToken);
        Assert.assertFalse(mixID1.contains("error"), "entitlement mtx was not added to the season: " + mixID1);

        eJson.put("name", "E2");
        entitlementID2 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

        featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
        mixID2 = purchasesApi.addPurchaseItem(seasonID, featureMix, mixID1, sessionToken);
        Assert.assertFalse(mixID2.contains("error"), "entitlement mtx was not added to the season: " + mixID2);

        eJson.put("name","E3");
        entitlementID3 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID2, sessionToken);
        Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);

        eJson.put("name","E4");
        entitlementID4 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID2, sessionToken);
        Assert.assertFalse(entitlementID4.contains("error"), "entitlement was not added to the season: " + entitlementID4);

        String response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

        //check that entitlement was checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("entitlements");

        JSONArray featuresInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

        //E1
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get branch" );	//get branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get features" );	//get branch
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");	//get entitlement from branch

        //E2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get features" );

        //E3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
                .getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
                .getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get features" );

        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch

        //E4
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
                .getJSONArray("entitlements").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement4 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
                .getJSONArray("entitlements").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement4 status is not checked_out in get features" );

        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get entitlement");	//get entitlement from branch

        //uncheckout E2
        String res = br.cancelCheckoutFeature(branchID, entitlementID2, sessionToken);
        Assert.assertFalse(res.contains("error"), "entitlement was not unchecked out: " + res);
        brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));

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
        JSONObject mx1 = feature.getJSONArray("entitlements").getJSONObject(0);
        String mx1NewId = mx1.getString("uniqueId");

        assertItemDuplicated(feature,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
                0,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        JSONObject mx2 = mx1.getJSONArray("entitlements").getJSONObject(0);
        String mx2NewId = mx2.getString("uniqueId");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,2,new String[]{"ns1.E2","mx." + mx2NewId}, 1 ,null);

        assertItemDuplicated(mx2,"CHECKED_OUT",mixID2,newIds,0,new String[]{},
                0,2,new String[]{"ns1.E3","ns1.E4"}, 2 ,null);

        JSONObject feature3 = mx2.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        JSONObject feature4 = mx2.getJSONArray("entitlements").getJSONObject(1);
        assertItemDuplicated(feature4,"CHECKED_OUT",entitlementID4,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        purchasesApi.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchEntitlements(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out Entitlements in dev branches1 runtime file");

        feature = branchWithFeatureRuntime.getJSONObject(0);
        mx1 = feature.getJSONArray("entitlements").getJSONObject(0);
        mx1NewId = mx1.getString("uniqueId");

        assertItemDuplicated(feature,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
                0,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        mx2 = mx1.getJSONArray("entitlements").getJSONObject(0);
        mx2NewId = mx2.getString("uniqueId");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,2,new String[]{"ns1.E2","mx." + mx2NewId}, 1 ,null);

        assertItemDuplicated(mx2,"CHECKED_OUT",mixID2,newIds,0,new String[]{},
                0,2,new String[]{"ns1.E3","ns1.E4"}, 2 ,null);

        feature3 = mx2.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        feature4 = mx2.getJSONArray("entitlements").getJSONObject(1);
        assertItemDuplicated(feature4,"CHECKED_OUT",entitlementID4,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        Assert.assertTrue(getBranchEntitlements(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out Entitlements in prod branches1 runtime file");

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
        JSONArray branchEntitlementsItems = feature1.getJSONArray("branchEntitlementItems");
        Assert.assertTrue(branchEntitlementsItems.size() == numberOfBranchFeatures);
        for(int i = 0 ; i< numberOfBranchFeatures; ++i) {
            Assert.assertTrue(branchEntitlementsItems.getString(i).equals(branchFeaturesNames[i]));
        }

        //Only for entitlements
        //features
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

