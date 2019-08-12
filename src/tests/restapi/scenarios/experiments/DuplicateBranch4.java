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
public class DuplicateBranch4 {

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
    String mixID1;



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


    @Test (description ="F1 -> F2 -> (F3 + F4), checkout F3 then move F4 under F1")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        fJson.put("name", "F1");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

        fJson.put("name","F2");
        featureID2 = f.addFeature(seasonID, fJson.toString(), featureID1, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

        fJson.put("name", "F3");
        featureID3 = f.addFeature(seasonID, fJson.toString(), featureID2, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

        fJson.put("name","F4");
        featureID4 = f.addFeature(seasonID, fJson.toString(), featureID2, sessionToken);
        Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);

        String response = br.checkoutFeature(branchID, featureID3, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

        //check that feature was checked out
        response = br.getBranchWithFeatures(branchID, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("features");

        JSONArray featuresInBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID, sessionToken);

        //F1
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get branch" );	//get branch
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get features" );	//get branch
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get feature");	//get feature from branch

        response = br.checkoutFeature(branchID, featureID1, sessionToken);
        Assert.assertTrue(response.contains("error"), "feature1 was checked out twice");

        //F2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );

        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch
        response = br.checkoutFeature(branchID, featureID2, sessionToken);
        Assert.assertTrue(response.contains("error"), "feature2 was checked out twice");

        //F3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get features" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status was incorrectly changed in get feature");	//get feature from branch

        //F4 is not in branch array
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").size()==1 );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getString("branchStatus").equals("NONE"), "Feature4 status is not NONE in get features" );

        //move F4.
        JSONObject F1Json = new JSONObject(f.getFeatureFromBranch(featureID1,branchID,sessionToken));
        JSONObject F4Json = new JSONObject(f.getFeature(featureID4,sessionToken));
        F1Json.getJSONArray("features").getJSONObject(0).getJSONArray("features").remove(1);
        F1Json.getJSONArray("features").add(F4Json);
        response = f.updateFeatureInBranch(seasonID,branchID,featureID1,F1Json.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not added to the season: " + featureID4);

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
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,0,new String[]{},
                0,2,new String[]{"ns1.F2","ns1.F4"}, 1 ,"ROOT");

        JSONObject features2 = feature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(features2,"CHECKED_OUT",featureID2,newIds,0,new String[]{},
                0,1,new String[]{"ns1.F3"}, 1 ,null);

        JSONObject feature3 = features2.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature3,"CHECKED_OUT",featureID3,newIds,0,new String[]{},
                0,0,new String[]{}, 0 ,null);

        f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");
        feature = branchWithFeatureRuntime.getJSONObject(0);
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,0,new String[]{},
                0,2,new String[]{"ns1.F2","ns1.F4"}, 1 ,"ROOT");

        features2 = feature.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(features2,"CHECKED_OUT",featureID2,newIds,0,new String[]{},
                0,1,new String[]{"ns1.F3"}, 1 ,null);

        feature3 = features2.getJSONArray("features").getJSONObject(0);
        assertItemDuplicated(feature3,"CHECKED_OUT",featureID3,newIds,0,new String[]{},
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

        //branch features
        JSONArray branchFeaturesItems = feature1.getJSONArray("branchFeaturesItems");
        Assert.assertTrue(branchFeaturesItems.size() == numberOfBranchFeatures);
        for(int i = 0 ; i< numberOfBranchFeatures; ++i) {
            Assert.assertTrue(branchFeaturesItems.getString(i).equals(branchFeaturesNames[i]));
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

