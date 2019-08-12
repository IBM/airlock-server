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
public class DuplicateBranch1 {

    protected String productID;
    protected String seasonID;
    protected String seasonID2;
    private String branchID;
    private String branchID2;
    private JSONObject fJson;
    protected String filePath;
    protected SeasonsRestApi s;
    protected String m_url;
    private String sessionToken = "";
    private AirlockUtils baseUtils;
    private BranchesRestApi br ;
    private FeaturesRestApi f;
    String featureID1;
    String featureID2;
    String configID1;
    String configID2;

    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        filePath = configPath ;
        s = new SeasonsRestApi();
        s.setURL(m_url);
        f = new FeaturesRestApi();
        f.setURL(m_url);
        br = new BranchesRestApi();
        br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
        fJson = new JSONObject(feature);

    }


    //F1 -> CR1, CR2
    @Test(description ="F1 -> CR1, CR2, checkout F1, add PROD F2 under F1")
    public void addBranch1() throws Exception {
        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        //add feature with configuration
        fJson.put("name", "F1");
        fJson.put("stage", "PRODUCTION");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        configID1 = f.addFeature(seasonID, configuration, featureID1, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

        String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
        configID2 = f.addFeature(seasonID, configuration2, featureID1, sessionToken);
        Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season");

        String response = br.checkoutFeature(branchID, featureID1, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

        //check that feature was checked out in branch
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("features");
        Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );

        //feature is checked out in get feature from branch
        String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
        JSONObject fJson2 = new JSONObject(feature);
        Assert.assertTrue(fJson2.getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get feature");

        fJson.put("name", "F2");
        fJson.put("stage", "PRODUCTION");
        featureID2 = f.addFeatureToBranch(seasonID,branchID,fJson.toString(),featureID1,sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "feature 2 was not added to the season");

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
        fJson.put("name", "F3");
        fJson.put("stage", "DEVELOPMENT");
        String featureID3 = f.addFeatureToBranch(seasonID2,branchID2,fJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(featureID3.contains("error"), "feature 3 was not added to the season");
        fJson.put("name", "F4");
        String featureID4 = f.addFeatureToBranch(seasonID2,"MASTER",fJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(featureID4.contains("error"), "feature 4 was not added to the season");
        String response = br.checkoutFeature(branchID2, featureID4, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");


    }
    @Test(dependsOnMethods = "duplicateSeason")
    public void duplicateBranchInSameSeason() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        assertBranchDuplication(false,seasonID);
        //assert everything still works
        fJson.put("name", "F5");
        fJson.put("stage", "DEVELOPMENT");
        String featureID3 = f.addFeatureToBranch(seasonID,branchID2,fJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(featureID3.contains("error"), "feature 3 was not added to the season");
        fJson.put("name", "F6");
        String featureID4 = f.addFeatureToBranch(seasonID,"MASTER",fJson.toString(),"ROOT",sessionToken);
        Assert.assertFalse(featureID4.contains("error"), "feature 4 was not added to the season");
        String response = br.checkoutFeature(branchID2, featureID4, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

    }


    public void assertBranchDuplication (Boolean newIds, String season) throws Exception {
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        JSONObject jsonBranchWithFeature = new JSONObject(branchWithFeature);

        JSONObject feature = jsonBranchWithFeature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,2,new String[]{"ns1.CR1","ns2.CR2"},
                2,1,new String[]{"ns1.F2"}, 1 ,"ROOT");

        JSONObject config = feature.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",configID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config = feature.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject feature2 = feature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature2,"NEW",featureID2,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");
        feature = branchWithFeatureRuntime.getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,2,new String[]{"ns1.CR1","ns2.CR2"},
                2,1,new String[]{"ns1.F2"}, 1 ,"ROOT");
        config = feature.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",configID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config = feature.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        feature2 = feature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature2,"NEW",featureID1,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_PRODUCTION,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntimeProd = getBranchFeatures(branchesRuntimeProd.message);
        Assert.assertTrue(branchWithFeatureRuntimeProd.size()==1, "Incorrect number of checked out features in prod branches1 runtime file");

        feature = branchWithFeatureRuntimeProd.getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,0,new String[]{},
                0,1,new String[]{"ns1.F2"}, 1 ,"ROOT");
        feature2 = feature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature2,"NEW",featureID2,true,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

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
        JSONArray configurationRuleItems = feature1.getJSONArray("configurationRules");
        Assert.assertTrue(configurationRuleItems.size() == numberOfConfig);

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

    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

