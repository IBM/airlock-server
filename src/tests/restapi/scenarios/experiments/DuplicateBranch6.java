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
public class DuplicateBranch6 {

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
    String featureID3;
    String featureID4;
    String mixConfigID;
    String configID1;
    String configID2;
    String configID3;
    String configID4;



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


    @Test (description ="F1 -> CR1 -> MIXCR ->(CR2+ CR3), checkout F1")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);
        String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);

        fJson.put("name", "F1");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

        JSONObject jsonCR = new JSONObject(configuration1);
        jsonCR.put("name", "CR1");
        configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");

        String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
        mixConfigID = f.addFeature(seasonID, configurationMix, configID1, sessionToken);
        Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

        jsonCR.put("name", "CR2");
        configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");

        jsonCR.put("name", "CR3");
        configID3 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");

        String response = br.checkoutFeature(branchID, featureID1, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

        jsonCR.put("name", "CR4");
        configID4 = f.addFeatureToBranch(seasonID,branchID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season");

        JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);

        //check that configuration rules were checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("features");

        //F1
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get features" );

        //CR1
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status status is not checked_out in get feature");	//get configuration rule from branch


        //CR2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status status is not checked_out in get feature");	//get configuration rule from branch

        //CR3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0).getJSONArray("configurationRules").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status status is not checked_out in get feature");	//get configuration rule from branch

        //MIXCR
        //MIXCR
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "MIX configuration status is not CHECKED_OUT" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule3 status status is not CHECKED_OUT in get feature");	//get configuration rule from branch

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

        JSONObject feature = jsonBranchWithFeature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,1,new String[]{"ns1.CR1"},
                1,0,new String[]{}, 0 ,"ROOT");

        JSONObject config1 = feature.getJSONArray("configurationRules").getJSONObject(0);
        JSONObject mx1 = config1.getJSONArray("configurationRules").getJSONObject(0);
        String mx1NewId = mx1.getString("uniqueId");

        assertItemDuplicated(config1,"CHECKED_OUT",configID1,newIds,1,new String[]{"mx."+ mx1NewId},
                1,0,new String[]{}, null ,null);

        assertItemDuplicated(mx1,"CHECKED_OUT",mixConfigID,newIds,3,new String[]{"ns1.CR2","ns1.CR3","ns1.CR4"},
                3,0,new String[]{}, null ,null);

        JSONObject config2 = mx1.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config2,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject config3 = mx1.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config3,"CHECKED_OUT",configID3,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        JSONObject config4 = mx1.getJSONArray("configurationRules").getJSONObject(2);
        assertItemDuplicated(config4,"NEW",configID4,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

        f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");

        feature = branchWithFeatureRuntime.getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,1,new String[]{"ns1.CR1"},
                1,0,new String[]{}, 0 ,"ROOT");

        config1 = feature.getJSONArray("configurationRules").getJSONObject(0);
        mx1 = config1.getJSONArray("configurationRules").getJSONObject(0);
        mx1NewId = mx1.getString("uniqueId");

        assertItemDuplicated(config1,"CHECKED_OUT",configID1,newIds,1,new String[]{"mx."+ mx1NewId},
                1,0,new String[]{}, null ,null);

        assertItemDuplicated(mx1,"CHECKED_OUT",mixConfigID,newIds,3,new String[]{"ns1.CR2","ns1.CR3","ns1.CR4"},
                3,0,new String[]{}, null ,null);

        config2 = mx1.getJSONArray("configurationRules").getJSONObject(0);
        assertItemDuplicated(config2,"CHECKED_OUT",configID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config3 = mx1.getJSONArray("configurationRules").getJSONObject(1);
        assertItemDuplicated(config3,"CHECKED_OUT",configID3,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        config4 = mx1.getJSONArray("configurationRules").getJSONObject(2);
        assertItemDuplicated(config4,"NEW",configID4,true,0,new String[]{},
                0,0,new String[]{}, null ,null);

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

    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

