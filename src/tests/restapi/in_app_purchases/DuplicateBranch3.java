package tests.restapi.in_app_purchases;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

public class DuplicateBranch3 {

    protected String productID;
    protected String seasonID;
    protected String seasonID2;
    private String branchID;
    private String branchID2;
    private JSONObject eJson;
    protected String filePath;
    protected ProductsRestApi p;
    protected SeasonsRestApi s;
    protected String m_url;
    private String sessionToken = "";
    private AirlockUtils baseUtils;
    private BranchesRestApi br ;
    protected InAppPurchasesRestApi purchasesApi;
    protected AnalyticsRestApi an;
    protected InputSchemaRestApi schema;
    String entitlementID1;
    String entitlementID2;
    String entitlementID3;
    String entitlementID4;
    String configID1;
    String mixID1;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        filePath = configPath ;
        p = new ProductsRestApi();
        p.setURL(m_url);
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


    @Test (description ="E1 -> MIX -> (E2 + E3), checkout E2, add E4 and CR1 ")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        eJson.put("name", "E1");
        entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

        String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
        mixID1 = purchasesApi.addPurchaseItem(seasonID, featureMix, entitlementID1, sessionToken);
        Assert.assertFalse(mixID1.contains("error"), "entitlement was not added to the season: " + mixID1);

        eJson.put("name", "E2");
        entitlementID2 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(entitlementID2.contains("error"), "entitlement was not added to the season: " + entitlementID2);

        eJson.put("name","E3");
        entitlementID3 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(entitlementID3.contains("error"), "entitlement was not added to the season: " + entitlementID3);

        String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

        //check that entitlement was checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("entitlements");

        //feature is checked out in get entitlements from branch
        JSONArray featuresInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);

        //E1
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get branch" );	//get branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlements" );	//get features
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "entitlement1 status is not checked_out in get entitlement");	//get entitlement from branch

        response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
        Assert.assertTrue(response.contains("error"), "entitlement1 was checked out twice");

        //E2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get entitlement from branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement2 status is not checked_out in get features" );

        response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
        Assert.assertTrue(response.contains("error"), "entitlement2 was checked out twice");

        response = an.addFeatureToAnalytics(entitlementID2,branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement2 was sent to analytics");

        //E3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status was incorrectly changed in get feature");	//get feature from branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "entitlement3 status is not checked_out in get features" );

        eJson.put("name", "E4");
        entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID,eJson.toString(),mixID1,sessionToken);
        Assert.assertFalse(entitlementID4.contains("error"), "entitlement was not added to the season: " + entitlementID4);

        response = an.addFeatureToAnalytics(entitlementID4,branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement2 was sent to analytics");

        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "red");
        newConfiguration.put("size", "small");
        jsonConfig.put("configuration", newConfiguration);
        configID1 = purchasesApi.addPurchaseItemToBranch(seasonID,branchID, jsonConfig.toString(), entitlementID1, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

        JSONArray attributes = new JSONArray();
        JSONObject attr1 = new JSONObject();
        attr1.put("name", "color");
        attr1.put("type", "REGULAR");
        attributes.add(attr1);
        response = an.addAttributesToAnalytics(entitlementID1, branchID, attributes.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "entitlement was not added to analytics" + response);

        JSONArray inputFields = new JSONArray();
        inputFields.put("context.device.locale");
        response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

    }

    @Test(dependsOnMethods = "addBranch1")
    public void duplicateSeason () throws Exception {
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        seasonID2 = s.addSeason(productID, season, sessionToken);
        String allBranches = br.getAllBranches(seasonID2,sessionToken);
        JSONObject jsonBranch = new JSONObject(allBranches);
        branchID2 = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
        assertBranchDuplication(true,seasonID2);
        String respWithQuota = an.getGlobalDataCollection(seasonID2, branchID2, "DISPLAY", sessionToken);
        Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //entitlement1+attribute+inputfield
        Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //entitlement1+attribute+inputfield
    }

    @Test(dependsOnMethods = "duplicateSeason")
    public void duplicateBranchInSameSeason() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        assertBranchDuplication(false,seasonID);
        String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
        Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //entitlement1+attribute+inputfield
        Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //entitlement1+attribute+inputfield

    }

    public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

        JSONObject entitlement = jsonBranchWithFeature.getJSONArray("entitlements").getJSONObject(0);
        JSONObject mx1 = entitlement.getJSONArray("entitlements").getJSONObject(0);
        String mx1NewId = mx1.getString("uniqueId");
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,1,new String[]{"ns1.CR1"},
                1,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,3,new String[]{"ns1.E2","ns1.E3","ns1.E4"}, 3 ,null);

        JSONObject config1 = entitlement.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config1,"NEW",configID1,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject feature2 = mx1.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(feature2,"CHECKED_OUT",entitlementID2,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        JSONObject feature3 = mx1.getJSONArray("entitlements").getJSONObject(1);
        assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        JSONObject feature4 = mx1.getJSONArray("entitlements").getJSONObject(2);
        assertItemDuplicated(feature4,"NEW",entitlementID4,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        purchasesApi.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchEntitlements(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out entitlements in dev branches1 runtime file");
        entitlement = branchWithFeatureRuntime.getJSONObject(0);
        mx1 = entitlement.getJSONArray("entitlements").getJSONObject(0);
        mx1NewId = mx1.getString("uniqueId");
        assertItemDuplicated(entitlement,"CHECKED_OUT",entitlementID1,newIds,1,new String[]{"ns1.CR1"},
                1,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,3,new String[]{"ns1.E2","ns1.E3","ns1.E4"}, 3 ,null);

        config1 = entitlement.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config1,"NEW",configID1,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

        feature2 = mx1.getJSONArray("entitlements").getJSONObject(0);
        assertItemDuplicated(feature2,"CHECKED_OUT",entitlementID2,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        feature3 = mx1.getJSONArray("entitlements").getJSONObject(1);
        assertItemDuplicated(feature3,"CHECKED_OUT",entitlementID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        feature4 = mx1.getJSONArray("entitlements").getJSONObject(2);
        assertItemDuplicated(feature4,"NEW",entitlementID4,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);


        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        Assert.assertTrue(getBranchEntitlements(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out entitlements in prod branches1 runtime file");

    }

    public void assertItemDuplicated(JSONObject entitlement, String status,String id,Boolean newIds, Integer numberOfBranchConfig,String[] branchConfigNames,
    int numberOfConfig, Integer numberOfBranchEntitlements,String[] branchEntitlementsNames,Integer numberOfEntitlements,String branchParentName)throws JSONException{
        Assert.assertTrue(entitlement.getString("branchStatus").equals(status));
        if(newIds) {
            Assert.assertFalse(entitlement.getString("uniqueId").equals(id));
        }
        else {
            Assert.assertTrue(entitlement.getString("uniqueId").equals(id));
        }
        //branch configs
        if(!entitlement.has("branchConfigurationRuleItems")){
            Assert.assertTrue(numberOfBranchConfig == 0);
        }
        else {
            JSONArray branchConfigurationRuleItems = entitlement.getJSONArray("branchConfigurationRuleItems");
            Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
            for (int i = 0; i < numberOfBranchConfig; ++i) {
                Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
            }
        }

        //configs
        if(!entitlement.has("configurationRules")){
            Assert.assertTrue(numberOfConfig == 0);
        }
        else {
            JSONArray configurationRuleItems = entitlement.getJSONArray("configurationRules");
            Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);
        }

        //branch entitlements
        if(!entitlement.has("branchEntitlementItems")){
            Assert.assertTrue(numberOfBranchEntitlements == 0);
        }
        else {
            JSONArray branchFeaturesItems = entitlement.getJSONArray("branchEntitlementItems");
            Assert.assertTrue(branchFeaturesItems.size() == numberOfBranchEntitlements);
            for (int i = 0; i < numberOfBranchEntitlements; ++i) {
                Assert.assertTrue(branchFeaturesItems.getString(i).equals(branchEntitlementsNames[i]));
            }
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

    private JSONArray getBranchEntitlements(String result) throws JSONException{
        JSONObject json = new JSONObject(result);
        return json.getJSONArray("entitlements");
    }

    private int getDevelopmentItemsReportedToAnalytics(String analytics) throws JSONException{
        JSONObject json = new JSONObject(analytics);
        return json.getJSONObject("analyticsDataCollection").getInt("developmentItemsReportedToAnalytics");
    }

    private int getProductionItemsReportedToAnalytics(String analytics) throws JSONException{
        JSONObject json = new JSONObject(analytics);
        return json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics");
    }
    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

