package tests.restapi.scenarios.experiments;


import org.apache.commons.lang3.RandomStringUtils;
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
public class DuplicateSeasonWithExperimentFromBranch {

    protected String productID;
    protected String seasonID;
    protected String seasonID2;
    private String branchID1;
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
    String experimentID;
    String m_analyticsUrl;
    private ExperimentsRestApi exp ;



    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        m_analyticsUrl = analyticsUrl;
        filePath = configPath ;
        s = new SeasonsRestApi();
        s.setURL(m_url);
        f = new FeaturesRestApi();
        f.setURL(m_url);
        br = new BranchesRestApi();
        br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 

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
    public void addBranches() throws Exception {
        //add branch1    	
    	branchID1 = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);
        
        //add branch2    	
     	 branchID2 = addBranch("branch2",BranchesRestApi.MASTER);
         Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created: " + branchID2);

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

        String response = br.checkoutFeature(branchID1, featureID1, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch1");

        //check that feature was checked out in branch
        response = br.getBranchWithFeatures(branchID1, sessionToken);
        JSONObject brJson = new JSONObject(response);
        JSONArray features = brJson.getJSONArray("features");
        Assert.assertTrue(features.size()==1, "Incorrect number of checked out features");
        Assert.assertTrue(features.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get branch" );

        //feature is checked out in get feature from branch
        String feature = f.getFeatureFromBranch(featureID1, branchID1, sessionToken);
        JSONObject fJson2 = new JSONObject(feature);
        Assert.assertTrue(fJson2.getString("branchStatus").equals("CHECKED_OUT"), "Feature status is not checked_out in get feature");

        fJson.put("name", "F2");
        fJson.put("stage", "PRODUCTION");
        featureID2 = f.addFeatureToBranch(seasonID,branchID1,fJson.toString(),featureID1,sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "feature 2 was not added to the season");
        
        //checkout the same feature to branch2
        response = br.checkoutFeature(branchID2, featureID1, sessionToken);
        Assert.assertFalse(response.contains("error"), "feature was not checked out to branch2: " + response);



    }
    
    @Test(dependsOnMethods="addBranches", description="add experiment with variants from branches")
    public void addExperiment() throws IOException, JSONException{
    	
		experimentID = baseUtils.addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), m_analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		
		JSONObject variantJson1 = new JSONObject(variant);		
		variantJson1.put("name", "variant1");
		variantJson1.put("branchName", "branch1");
		String variantID1 =  exp.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "variant1 was not added to experiment");
		
		JSONObject variantJson2 = new JSONObject(variant);		
		variantJson2.put("name", "variant2");
		variantJson2.put("branchName", "branch2");
		String variantID2 =  exp.createVariant(experimentID, variantJson2.toString(), sessionToken);
		Assert.assertFalse(variantID2.contains("error"), "variant2 was not added to experiment");

    }

    @Test(dependsOnMethods = "addExperiment")
    public void duplicateSeasonWithVariantsFromBranches () throws Exception {
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        seasonID2 = s.addSeason(productID, season, sessionToken);
       
        String allBranches = br.getAllBranches(seasonID2,sessionToken);
        JSONObject jsonBranch = new JSONObject(allBranches);
        Assert.assertTrue(jsonBranch.getJSONArray("branches").size()==3, "Incorrect number of branches in the new season");
 
    }
    
    @Test(dependsOnMethods="duplicateSeasonWithVariantsFromBranches", description="add variant from MASTER")
    public void addVariantFromMaster() throws IOException, JSONException{
    	
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		
		JSONObject variantJson1 = new JSONObject(variant);		
		variantJson1.put("name", "variant3");
		variantJson1.put("branchName", BranchesRestApi.MASTER);
		String variantID3 =  exp.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID3.contains("error"), "variant3 was not added to experiment");

    }
    
    @Test(dependsOnMethods = "addVariantFromMaster")
    public void duplicateSeasonWithVariantsFromMaster () throws Exception {
        String season = FileUtils.fileToString(filePath + "season3.txt", "UTF-8", false);
        String seasonID3 = s.addSeason(productID, season, sessionToken);
       
        String allBranches = br.getAllBranches(seasonID3,sessionToken);
        JSONObject jsonBranch = new JSONObject(allBranches);
        Assert.assertTrue(jsonBranch.getJSONArray("branches").size()==3, "Incorrect number of branches in the new season");
 
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

