package tests.restapi.scenarios.orderingRules;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;


public class DuplicateBranchWithOrderingRules1 {

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
    String orderingRuleID1;
    String orderingRuleID2;



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


    //F1 -> F2, OR1
    @Test(description ="F1 -> F2, OR1, OR2, checkout F1")
    public void addBranch1() throws Exception {
        branchID = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

        //add feature with configuration
        fJson.put("name", "F1");
        fJson.put("stage", "PRODUCTION");
        featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
        
        fJson.put("name", "F2");
        featureID2 = f.addFeature(seasonID, fJson.toString(), featureID1, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);


		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR1");
		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.5");
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID1 = f.addFeature(seasonID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);

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

		jsonOR.put("name", "OR2");
		configJson = new JSONObject();
		configJson.put(featureID2, "2.5");
		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID2 = f.addFeatureToBranch(seasonID, branchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID2);


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
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,2,new String[]{"OR1.OR1","OR1.OR2"},
                2,1,new String[]{"ns1.F2"}, null ,"ROOT");

        JSONObject config = feature.getJSONArray("orderingRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",orderingRuleID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);
        config = feature.getJSONArray("orderingRules").getJSONObject(1);
        assertItemDuplicated(config,"NEW",orderingRuleID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

        
        
        f.setSleep();
        RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getRuntimeBranchFileContent(RuntimeDateUtilities.RUNTIME_BRANCHES_DEVELOPMENT,  m_url, productID, season, branchID2, sessionToken);
        JSONArray branchWithFeatureRuntime = getBranchFeatures(branchesRuntimeDev.message);
        Assert.assertTrue(branchWithFeatureRuntime.size()==1, "Incorrect number of checked out features in dev branches1 runtime file");
        feature = branchWithFeatureRuntime.getJSONObject(0);
        
        assertItemDuplicated(feature,"CHECKED_OUT",featureID1,newIds,2,new String[]{"OR1.OR1","OR1.OR2"},
                2,1,new String[]{"ns1.F2"}, null ,"ROOT");

        config = feature.getJSONArray("orderingRules").getJSONObject(0);
        assertItemDuplicated(config,"CHECKED_OUT",orderingRuleID1,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);
        config = feature.getJSONArray("orderingRules").getJSONObject(1);
        assertItemDuplicated(config,"NEW",orderingRuleID2,newIds,0,new String[]{},
                0,0,new String[]{}, null ,null);

    }

    public void assertItemDuplicated(JSONObject feature1, String status,String id,Boolean newIds, Integer numberOfBranchConfig,String[] branchConfigNames,
    int numberOfConfig, Integer numberOfBranchFeatures,String[] branchFeaturesNames,Integer numberOfFeatures,String branchParentName)throws JSONException{
        Assert.assertTrue(feature1.getString("branchStatus").equals(status));
       
        if (status.equals("CHECKED_OUT")){
	        if(newIds) {
	            Assert.assertFalse(feature1.getString("uniqueId").equals(id));
	        }
	        else {
	            Assert.assertTrue(feature1.getString("uniqueId").equals(id));
	        }
        }else if (status.equals("NEW"))  {
        	Assert.assertFalse(feature1.getString("uniqueId").equals(id));
        }

        //branch configs
        if(!feature1.has("branchOrderingRuleItems")){
            Assert.assertTrue(numberOfBranchConfig == 0);
        }
        else {
            JSONArray branchConfigurationRuleItems = feature1.getJSONArray("branchOrderingRuleItems");
            Assert.assertTrue(branchConfigurationRuleItems.size() == numberOfBranchConfig);
            for (int i = 0; i < numberOfBranchConfig; ++i) {
                Assert.assertTrue(branchConfigurationRuleItems.getString(i).equals(branchConfigNames[i]));
            }
        }

        //configs
        JSONArray configurationRuleItems = feature1.getJSONArray("orderingRules");
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

