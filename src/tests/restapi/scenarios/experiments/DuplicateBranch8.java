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
 * Created by iritma on 09/02/2020.
 */
public class DuplicateBranch8 {

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


    //F1 -> CR1, CR2, F2
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

        fJson.put("name", "F2");
        fJson.put("stage", "PRODUCTION");
        featureID2 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

        
        //check out F1
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

        //check out F2
        response = br.checkoutFeature(branchID, featureID2, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");
    }

    @Test(dependsOnMethods = "addBranch1")
    public void addMTXToBranch () throws Exception {
	    	String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "MTX was not added to the season: " + mixID1);
		
		//move F1 and F2 under mtx in branch
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject mtx =  new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		
		JSONArray children = mtx.getJSONArray("features");
		children.put(feature1);
		children.put(feature2);
		mtx.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID1, mtx.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't move features under mtx in branch");
    }
    
    @Test(dependsOnMethods = "addMTXToBranch")
    public void craeteBranchFronBranch() throws Exception{
        branchID2 = addBranch("branch2",branchID);
        Assert.assertFalse(branchID2.contains("error"), "Cannot create branch from branch");
        
        JSONArray featuresInNewBranch = f.getFeaturesBySeasonFromBranch(seasonID, branchID2, sessionToken);
         
         String feature1InNewBranchID = featuresInNewBranch.getJSONObject(0).getJSONArray("features").getJSONObject(0).getString("uniqueId");
         String mixIdInNewBranch = featuresInNewBranch.getJSONObject(0).getString("uniqueId");
         Assert.assertFalse(mixIdInNewBranch.equals(mixID1), "mix id is identical in new branch even though is new");
         Assert.assertTrue(feature1InNewBranchID.equals(featureID1), "Feature1 id is not identical in new branch even though is checked out");
         
         JSONObject feature1InNewBranch = new JSONObject(f.getFeatureFromBranch(feature1InNewBranchID, branchID2, sessionToken));
         Assert.assertTrue(feature1InNewBranch.getBoolean("enabled") == true, "Feature1 enabled is false");
         
         feature1InNewBranch.put("enabled", false);
         feature1InNewBranch.put("description", "aaa");
         
         String response = f.updateFeatureInBranch(seasonID, branchID2, feature1InNewBranchID, feature1InNewBranch.toString(), sessionToken);
 		 Assert.assertFalse(response.contains("error"), "Couldn't update feature 1 in new branch");
 		 
 		feature1InNewBranch = new JSONObject(f.getFeatureFromBranch(feature1InNewBranchID, branchID2, sessionToken));
 		Assert.assertTrue(feature1InNewBranch.getBoolean("enabled") == false, "Feature1 id was not updated");
 		Assert.assertTrue(feature1InNewBranch.get("description").equals("aaa"), "Feature1 id was not updated");
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

