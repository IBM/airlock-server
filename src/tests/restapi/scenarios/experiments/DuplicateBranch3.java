package tests.restapi.scenarios.experiments;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;

/**
 * Created by amitaim on 15/06/2017.
 */
public class DuplicateBranch3 {

    protected String productID;
    protected String seasonID;
    protected String seasonID2;
    private String branchID;
    private String branchID2;
    private JSONObject fJson;
    protected String filePath;
    protected ProductsRestApi p;
    protected SeasonsRestApi s;
    protected String m_url;
    private String sessionToken = "";
    private AirlockUtils baseUtils;
    private BranchesRestApi br ;
    private FeaturesRestApi f;
    protected AnalyticsRestApi an;
    protected InputSchemaRestApi schema;
    String featureID1;
    String featureID2;
    String featureID3;
    String featureID4;
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
        f = new FeaturesRestApi();
        f.setURL(m_url);
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
        String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
        fJson = new JSONObject(feature);

        String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_update_device_locale_to_production.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
    }


    @Test (description ="F1 -> MIX -> (F2 + F3), checkout F2, add F4 and CR1 ")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        fJson.put("name", "F1");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

        String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
        mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
        Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

        fJson.put("name", "F2");
        featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

        fJson.put("name","F3");
        featureID3 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);

        String response = br.checkoutFeature(branchID, featureID2, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

        //check that feature was checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("features");

        //feature is checked out in get features from branch
        JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);


        //F1
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get branch" );	//get branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get features" );	//get features
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get feature");	//get feature from branch

        response = br.checkoutFeature(branchID, featureID1, sessionToken);
        Assert.assertTrue(response.contains("error"), "feature1 was checked out twice");

        //F2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );

        response = br.checkoutFeature(branchID, featureID2, sessionToken);
        Assert.assertTrue(response.contains("error"), "feature2 was checked out twice");

        response = an.addFeatureToAnalytics(featureID2,branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature2 was sent to analytics");


        //F3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status was incorrectly changed in get feature");	//get feature from branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get features" );

        fJson.put("name", "F4");
        featureID4 = f.addFeatureToBranch(seasonID,branchID,fJson.toString(),mixID1,sessionToken);
        Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);

        response = an.addFeatureToAnalytics(featureID4,branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature2 was sent to analytics");

        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "red");
        newConfiguration.put("size", "small");
        jsonConfig.put("configuration", newConfiguration);
        configID1 = f.addFeatureToBranch(seasonID,branchID, jsonConfig.toString(), featureID1, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

        JSONArray attributes = new JSONArray();
        JSONObject attr1 = new JSONObject();
        attr1.put("name", "color");
        attr1.put("type", "REGULAR");
        attributes.add(attr1);
        response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

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
        Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
        Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //feature1+attribute+inputfield
    }

    @Test(dependsOnMethods = "duplicateSeason")
    public void duplicateBranchInSameSeason() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        assertBranchDuplication(false,seasonID);
        String respWithQuota = an.getGlobalDataCollection(seasonID, branchID2, "DISPLAY", sessionToken);
        Assert.assertTrue(getDevelopmentItemsReportedToAnalytics(respWithQuota)==4, "Incorrect number of development items"); //feature1+attribute+inputfield
        Assert.assertTrue(getProductionItemsReportedToAnalytics(respWithQuota)==1, "Incorrect number of production items"); //feature1+attribute+inputfield

    }

    public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

        JSONObject feature = jsonBranchWithFeature.getJSONArray("features").getJSONObject(0);
        JSONObject mx1 = feature.getJSONArray("features").getJSONObject(0);
        String mx1NewId = mx1.getString("uniqueId");
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,1,new String[]{"ns1.CR1"},
                1,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,3,new String[]{"ns1.F2","ns1.F3","ns1.F4"}, 3 ,null);

        JSONObject config1 = feature.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config1,"NEW",configID1,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject feature2 = mx1.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature2,"CHECKED_OUT",featureID2,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        JSONObject feature3 = mx1.getJSONArray("features").getJSONObject(1);
        assertItemDuplicated(feature3,"CHECKED_OUT",featureID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        JSONObject feature4 = mx1.getJSONArray("features").getJSONObject(2);
        assertItemDuplicated(feature4,"NEW",featureID4,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");
        feature = branchWithFeatureRuntime.getJSONObject(0);
        mx1 = feature.getJSONArray("features").getJSONObject(0);
        mx1NewId = mx1.getString("uniqueId");
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,1,new String[]{"ns1.CR1"},
                1,1,new String[]{"mx."+mx1NewId}, 1 ,"ROOT");

        assertItemDuplicated(mx1,"CHECKED_OUT",mixID1,newIds,0,new String[]{},
                0,3,new String[]{"ns1.F2","ns1.F3","ns1.F4"}, 3 ,null);

        config1 = feature.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config1,"NEW",configID1,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

        feature2 = mx1.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature2,"CHECKED_OUT",featureID2,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        feature3 = mx1.getJSONArray("features").getJSONObject(1);
        assertItemDuplicated(feature3,"CHECKED_OUT",featureID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        feature4 = mx1.getJSONArray("features").getJSONObject(2);
        assertItemDuplicated(feature4,"NEW",featureID4,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);


        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of checked out features in prod branches1 runtime file");

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
        if(!feature1.has("branchConfigurationRuleItems")){
            Assert.assertTrue(numberOfBranchConfig == 0);
        }
        else {
            JSONArray branchConfigurationRuleItems = feature1.getJSONArray("branchConfigurationRuleItems");
            Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
            for (int i = 0; i < numberOfBranchConfig; ++i) {
                Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
            }
        }

        //configs
        if(!feature1.has("configurationRules")){
            Assert.assertTrue(numberOfConfig == 0);
        }
        else {
            JSONArray configurationRuleItems = feature1.getJSONArray("configurationRules");
            Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);
        }

        //branch features
        if(!feature1.has("branchFeaturesItems")){
            Assert.assertTrue(numberOfBranchFeatures == 0);
        }
        else {
            JSONArray branchFeaturesItems = feature1.getJSONArray("branchFeaturesItems");
            Assert.assertTrue(branchFeaturesItems.size() == numberOfBranchFeatures);
            for (int i = 0; i < numberOfBranchFeatures; ++i) {
                Assert.assertTrue(branchFeaturesItems.getString(i).equals(branchFeaturesNames[i]));
            }
        }

        //Only for features
        //features
        if(numberOfFeatures != null) {
            Assert.assertTrue(feature1.getJSONArray("features").size() == numberOfFeatures);
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

    private JSONArray getBranchFeatures(String result) throws JSONException{
        JSONObject json = new JSONObject(result);
        return json.getJSONArray("features");
    }

    private int getDevelopmentItemsReportedToAnalytics(String analytics) throws JSONException{
        JSONObject json = new JSONObject(analytics);
        return json.getJSONObject("analyticsDataCollection").getInt("developmentItemsReportedToAnalytics");
    }

    private int getProductionItemsReportedToAnalytics(String analytics) throws JSONException{
        JSONObject json = new JSONObject(analytics);
        //System.out.println("productionItem = " + json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics"));
        return json.getJSONObject("analyticsDataCollection").getInt("productionItemsReportedToAnalytics");
    }
    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

