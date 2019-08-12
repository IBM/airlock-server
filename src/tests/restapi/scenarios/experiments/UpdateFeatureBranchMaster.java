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
public class UpdateFeatureBranchMaster {

    protected String productID;
    protected String seasonID;
    protected String seasonID2;
    private String branchID;
    private String branchID2;
    private String branchIDNewSeason;
    private JSONObject fJson;
    protected String filePath;
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


    @Test (description ="F1 + (MIXCR->C1+C2) -> MIX -> (F2 + MIX -> (F3 + F4) ),  checkout F2 ")
    public void addBranch1 () throws Exception {

        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        fJson.put("name", "F1");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

        String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
        mixConfigID = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
        Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

        String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonCR = new JSONObject(configuration1);
        jsonCR.put("name", "CR2");
        configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");

        jsonCR.put("name", "CR3");
        configID3 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season");

        String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
        mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
        Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);

        fJson.put("name", "F2");
        featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

        featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
        mixID2 = f.addFeature(seasonID, featureMix, mixID1, sessionToken);
        Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);

        fJson.put("name", "F3");
        featureID3 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
        Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);

        fJson.put("name", "F4");
        featureID4 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
        Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);

        String response = br.checkoutFeature(branchID, featureID2, sessionToken);
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

        //configurations under F1
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule1 status is not checked_out" );

        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get feature");	//get feature from branch

        Assert.assertTrue(features.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("configurationRules").getJSONObject(0)
                .getJSONArray("configurationRules").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Configuration rule2 status is not checked_out" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature1 status is not checked_out in get feature");	//get feature from branch


        //F2
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature2 status is not checked_out in get features" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch

        //F3
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getJSONArray("features").getJSONObject(0)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get features" );

        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch

        //F4
        Assert.assertTrue(features.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getJSONArray("features").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature4 status is not checked_out in get branch" );
        Assert.assertTrue(featuresInBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1)
                .getJSONArray("features").getJSONObject(1)
                .getString("branchStatus").equals("CHECKED_OUT"), "Feature4 status is not checked_out in get branch" );
        Assert.assertTrue(new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get feature");	//get feature from branch

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
      String f1 = f.getFeature(featureID1,sessionToken);
      JSONObject f1Json = new JSONObject(f1);
      f1Json.put("description","helloqw");
      String response = f.updateFeature(seasonID,featureID1,f1Json.toString(),sessionToken);
      Assert.assertFalse(response.contains("error"), "Feature was not updated");
      String f1branch = f.getFeatureFromBranch(featureID1,branchID,sessionToken);
      JSONObject f1branchJson = new JSONObject(f1branch);
      Assert.assertFalse(f1branchJson.getString("description").equals("helloqw"));
      f1branch = f.getFeatureFromBranch(featureID1,branchID2,sessionToken);
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
        String f1 = f.getFeature(featureID1,sessionToken);
        JSONObject f1Json = new JSONObject(f1);
        f1Json.put("name","F1rename");
        String response = f.updateFeature(seasonID,featureID1,f1Json.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");
        String f1branch = f.getFeatureFromBranch(featureID1,branchID,sessionToken);
        JSONObject f1branchJson = new JSONObject(f1branch);
        Assert.assertTrue(f1branchJson.getString("name").equals("F1rename"));
        f1branch = f.getFeatureFromBranch(featureID1,branchID2,sessionToken);
        f1branchJson = new JSONObject(f1branch);
        Assert.assertTrue(f1branchJson.getString("name").equals("F1rename"));
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        Assert.assertTrue(branchWithFeature.contains("F1rename"));
        branchWithFeature = br.getBranchWithFeatures(branchIDNewSeason,sessionToken);
        Assert.assertFalse(branchWithFeature.contains("F1rename"));

        //runtime file
         f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID,dateFormat,sessionToken);
        Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
        branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID2,dateFormat,sessionToken);
        Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");

    }

    @Test(dependsOnMethods = "updateNameInMaster")
    public void updateFeatureInBranch() throws Exception{
        String f1 = f.getFeatureFromBranch(featureID1,branchID,sessionToken);
        JSONObject f1Json = new JSONObject(f1);
        f1Json.put("description","hellobranch");
        String response = f.updateFeatureInBranch(seasonID,branchID,featureID1,f1Json.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");
        String f1Master = f.getFeatureFromBranch(featureID1,BranchesRestApi.MASTER,sessionToken);
        JSONObject f1MasterJson = new JSONObject(f1Master);
        Assert.assertFalse(f1MasterJson.getString("description").equals("hellobranch"));
        String f1branch = f.getFeatureFromBranch(featureID1,branchID2,sessionToken);
        JSONObject f1branchJson = new JSONObject(f1branch);
        Assert.assertFalse(f1branchJson.getString("description").equals("hellobranch"));
        String branchWithFeature = br.getBranchWithFeatures(branchID2,sessionToken);
        Assert.assertFalse(branchWithFeature.contains("hellobranch"));
        branchWithFeature = br.getBranchWithFeatures(branchIDNewSeason,sessionToken);
        Assert.assertFalse(branchWithFeature.contains("hellobranch"));

    }
    @Test(dependsOnMethods = "updateFeatureInBranch")
    public void addUpdateDeleteInBranch() throws Exception {

        String masterString = f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString();
        String branch2String = f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString();
        String branchNewString = f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString();
        fJson.put("name", "FNew");
        String newFeature = f.addFeatureToBranch(seasonID,branchID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(newFeature.contains("error"), "Feature was not added to the season: " + featureID1);

        String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
        String mixConfigID2 = f.addFeatureToBranch(seasonID,branchID, configurationMix, featureID1, sessionToken);
        Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix was not added to the season");

        String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonCR = new JSONObject(configuration1);
        jsonCR.put("name", "CRNew");
        String configIDNew = f.addFeatureToBranch(seasonID,branchID, jsonCR.toString(), mixConfigID, sessionToken);
        Assert.assertFalse(configIDNew.contains("error"), "Configuration rule2 was not added to the season");

        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));

        String f1 = f.getFeatureFromBranch(newFeature,branchID,sessionToken);
        JSONObject f1Json = new JSONObject(f1);
        f1Json.put("description","hellobranch");
        String response = f.updateFeatureInBranch(seasonID,branchID,newFeature,f1Json.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");

        String mix = f.getFeatureFromBranch(mixConfigID2,branchID,sessionToken);
        JSONObject mixJson = new JSONObject(mix);
        mixJson.put("maxFeaturesOn",2);
        response = f.updateFeatureInBranch(seasonID,branchID,mixConfigID2,mixJson.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");

        String cr1 = f.getFeatureFromBranch(configIDNew,branchID,sessionToken);
        JSONObject cr1Json = new JSONObject(cr1);
        f1Json.put("description","hellobranch");
        response = f.updateFeatureInBranch(seasonID,branchID,configIDNew,cr1Json.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");

        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));

        int deleted = f.deleteFeatureFromBranch(newFeature,branchID,sessionToken);
        Assert.assertTrue(deleted == 200, "Feature was not deleted");

        deleted = f.deleteFeatureFromBranch(mixConfigID2,branchID,sessionToken);
        Assert.assertTrue(deleted == 200, "Feature was not deleted");

        deleted = f.deleteFeatureFromBranch(configIDNew,branchID,sessionToken);
        Assert.assertTrue(deleted == 200, "Feature was not deleted");

        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));

    }
    @Test(dependsOnMethods = "addUpdateDeleteInBranch")
    public void reorderInBranch() throws Exception {
        String masterString = f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString();
        String branch2String = f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString();
        String branchNewString = f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString();

        String mix = f.getFeatureFromBranch(mixConfigID,branchID,sessionToken);
        JSONObject mixJson = new JSONObject(mix);
        JSONArray rules = mixJson.getJSONArray("configurationRules");
        JSONObject rule1 = rules.getJSONObject(0);
        rules.set(0,rules.getJSONObject(1));
        rules.set(1,rule1);
        mixJson.put("configurationRules",rules);
        String response = f.updateFeatureInBranch(seasonID,branchID,mixConfigID,mixJson.toString(),sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not updated");


        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,BranchesRestApi.MASTER,sessionToken).toString().equals(masterString));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID,branchID2,sessionToken).toString().equals(branch2String));
        Assert.assertTrue(f.getFeaturesBySeasonFromBranch(seasonID2,branchIDNewSeason,sessionToken).toString().equals(branchNewString));
    }

    @Test(dependsOnMethods = "reorderInBranch")
    public void addTwobranches() throws Exception {

        fJson.put("name", "FNew");
        String newFeature = f.addFeatureToBranch(seasonID,branchID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(newFeature.contains("error"), "Feature was not added to the season: " + featureID1);

        String newFeature2 = f.addFeatureToBranch(seasonID,branchID2, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(newFeature2.contains("error"), "Feature was not added to the season: " + featureID1);

    }

    @Test(dependsOnMethods = "addTwobranches")
    public void analytics() throws Exception {

        fJson.put("name", "Fanalytics");
        String newFeature = f.addFeatureToBranch(seasonID,BranchesRestApi.MASTER, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(newFeature.contains("error"), "Feature was not added to the season: " + featureID1);

        String response = an.addFeatureToAnalytics(newFeature,BranchesRestApi.MASTER, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature2 was sent to analytics");

        String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
        JSONObject jsonConfig = new JSONObject(configuration);
        JSONObject newConfiguration = new JSONObject();
        newConfiguration.put("color", "red");
        newConfiguration.put("size", "small");
        jsonConfig.put("configuration", newConfiguration);
        String configID1 = f.addFeatureToBranch(seasonID,BranchesRestApi.MASTER, jsonConfig.toString(), newFeature, sessionToken);
        Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season");

        JSONArray attributes = new JSONArray();
        JSONObject attr1 = new JSONObject();
        attr1.put("name", "color");
        attr1.put("type", "REGULAR");
        attributes.add(attr1);
        response = an.addAttributesToAnalytics(newFeature, BranchesRestApi.MASTER, attributes.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

        JSONArray inputFields = new JSONArray();
        inputFields.put("context.device.locale");
        response = an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER,  inputFields.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

        response = br.checkoutFeature(branchID,  newFeature, sessionToken);
        Assert.assertFalse(response.contains("error"), "could not check out" + response);

        //try to delete from branch - error
        response = an.deleteFeatureFromAnalytics(newFeature,branchID, sessionToken);
        Assert.assertTrue(response.contains("The status of the item is being sent to analytics from the master branch. To stop sending item status to analytics, first go to the master and stop sending to analytics. Then, return to the branch and stop sending to analytics"), "feature2 was sent to analytics");
        JSONArray attributes2 = new JSONArray();
        response = an.addAttributesToAnalytics(newFeature, branchID, attributes2.toString(), sessionToken);
        Assert.assertTrue(response.contains("You must report all attributes that are reported in the master, in addition to the attributes that you want to report in the branch"), "Feature was not added to analytics" + response);
        JSONArray inputFields2 = new JSONArray();
        response = an.updateInputFieldToAnalytics(seasonID, branchID,  inputFields2.toString(), sessionToken);
        Assert.assertTrue(response.contains("You must report all input fields that are reported in the master, in addition to the input fields that you want to report in the branch."), "Analytics was not updated" + response);

        //delete from master
        response = an.deleteFeatureFromAnalytics(newFeature,BranchesRestApi.MASTER, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature2 was sent to analytics");
        response = an.addAttributesToAnalytics(newFeature, BranchesRestApi.MASTER, attributes2.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
        response = an.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER,  inputFields2.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

        //now remove from branch
        response = an.deleteFeatureFromAnalytics(newFeature,branchID, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature2 was sent to analytics");
        response = an.addAttributesToAnalytics(newFeature, branchID, attributes2.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
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

