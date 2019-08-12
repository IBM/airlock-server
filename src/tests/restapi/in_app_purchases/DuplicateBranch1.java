package tests.restapi.in_app_purchases;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class DuplicateBranch1 {

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
    String configID1;
    String configID2;

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


    //E1 -> CR1, CR2
    @Test(description ="E1 -> CR1, CR2, checkout E1, add PROD E2 under E1")
    public void addBranch1() throws Exception {
        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        //add entitlement with configuration
        eJson.put("name", "E1");
        eJson.put("stage", "PRODUCTION");
        entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        configID1 = purchasesApi.addPurchaseItem(seasonID, configuration, entitlementID1, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

        String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
        configID2 = purchasesApi.addPurchaseItem(seasonID, configuration2, entitlementID1, sessionToken);
        Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season");

        String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

        //check that entitlement was checked out in branch
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray entitlements = brJson.getJSONArray("entitlements");
        Assert.assertTrue(entitlements.size()==1, "Incorrect number of checked out entitlements");
        Assert.assertTrue(entitlements.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement status is not checked_out in get branch" );

        //entitlement is checked out in get entitlement from branch
        String entitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
        JSONObject eJson2 = new JSONObject(entitlement);
        Assert.assertTrue(eJson2.getString("branchStatus").equals("CHECKED_OUT"), "entitlement status is not checked_out in get feature");

        eJson.put("name", "E2");
        eJson.put("stage", "PRODUCTION");
        entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID,eJson.toString(),entitlementID1,sessionToken);
        Assert.assertFalse(entitlementID2.contains("error"), "entitlement 2 was not added to the season");
    }

    @Test(dependsOnMethods = "addBranch1")
    public void duplicateSeason () throws Exception {
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        seasonID2 = s.addSeason(productID, season, sessionToken);
        String allBranches = br.getAllBranches(seasonID2,sessionToken);
        JSONObject jsonBranch = new JSONObject(allBranches);
        branchID2 = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
        assertBranchDuplication(true,seasonID2);
       //assert everything still works
        eJson.put("name", "E3");
        eJson.put("stage", "DEVELOPMENT");
        String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID2,branchID2,eJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(entitlementID3.contains("error"), "entitlement 3 was not added to the season");
        eJson.put("name", "E4");
        String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID2,"MASTER",eJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(entitlementID4.contains("error"), "entitlement 4 was not added to the season");
        String response = br.checkoutFeature(branchID2, entitlementID4, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
    }
    
    @Test(dependsOnMethods = "duplicateSeason")
    public void duplicateBranchInSameSeason() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        assertBranchDuplication(false,seasonID);
        //assert everything still works
        eJson.put("name", "E5");
        eJson.put("stage", "DEVELOPMENT");
        String entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID2,eJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(entitlementID3.contains("error"), "entitlement 3 was not added to the season");
        eJson.put("name", "E6");
        String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID,"MASTER",eJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(entitlementID4.contains("error"), "entitlement 4 was not added to the season");
        String response = br.checkoutFeature(branchID2, entitlementID4, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
    }

    public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

        JSONObject entitlement = jsonBranchWithFeature.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,2,new String[]{"ns1.CR1","ns2.CR2"},
                2,1,new String[]{"ns1.E2"}, 1 ,"ROOT");

        JSONObject config = entitlement.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",configID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config = entitlement.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject entitlement2 = entitlement.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(entitlement2,"NEW",entitlementID2,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        purchasesApi.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchEntitlements(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out entitlements in dev branches1 runtime file");
        entitlement = branchWithFeatureRuntime.getJSONObject(0);
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,2,new String[]{"ns1.CR1","ns2.CR2"},
                2,1,new String[]{"ns1.E2"}, 1 ,"ROOT");
        config = entitlement.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",configID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config = entitlement.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        entitlement2 = entitlement.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(entitlement2,"NEW",entitlementID1,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntimeProd = getBranchEntitlements(branchesRuntimeProd.message);
        Assert.assertTrue(branchWithFeatureRuntimeProd.size()==1, "Incorrect number of checked out entitlements in prod branches1 runtime file");

        entitlement = branchWithFeatureRuntimeProd.getJSONObject(0);
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,0,new String[]{},
                0,1,new String[]{"ns1.E2"}, 1 ,"ROOT");
        entitlement2 = entitlement.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(entitlement2,"NEW",entitlementID2,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);
    }

    public void assertItemDuplicated(JSONObject entitlement1, String status,String id,Boolean newIds, Integer numberOfBranchConfig,
    									String[] branchConfigNames, int numberOfConfig, Integer numberOfBranchentitlements, String[] branchEntitlementsNames,
    									Integer numberOfEntitlements, String branchParentName) throws JSONException{
        Assert.assertTrue(entitlement1.getString("branchStatus").equals(status));
        if(newIds) {
            Assert.assertFalse(entitlement1.getString("uniqueId").equals(id));
        }
        else {
            Assert.assertTrue(entitlement1.getString("uniqueId").equals(id));
        }

        //branch configs
        if(!entitlement1.has("branchConfigurationRuleItems")){
            Assert.assertTrue(numberOfBranchConfig == 0);
        }
        else {
            JSONArray branchConfigurationRuleItems = entitlement1.getJSONArray("branchConfigurationRuleItems");
            Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
            for (int i = 0; i < numberOfBranchConfig; ++i) {
                Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
            }
        }

        //configs
        JSONArray configurationRuleItems = entitlement1.getJSONArray("configurationRules");
        Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);

        //branch entitlements
        if(!entitlement1.has("branchEntitlementItems")){
            Assert.assertTrue(numberOfBranchentitlements == 0);
        }
        else {
            JSONArray branchEntitlementsItems = entitlement1.getJSONArray("branchEntitlementItems");
            Assert.assertTrue(branchEntitlementsItems.size() == numberOfBranchentitlements);
            for (int i = 0; i < numberOfBranchentitlements; ++i) {
                Assert.assertTrue(branchEntitlementsItems.getString(i).equals(branchEntitlementsNames[i]));
            }
        }

        //Only for entitlements
        //entitlements
        if(numberOfEntitlements != null) {
            Assert.assertTrue(entitlement1.getJSONArray("entitlements").size() == numberOfEntitlements);
        }
        //parent
        if(branchParentName != null) {
            Assert.assertTrue(entitlement1.getString("branchFeatureParentName").equals(branchParentName));
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

