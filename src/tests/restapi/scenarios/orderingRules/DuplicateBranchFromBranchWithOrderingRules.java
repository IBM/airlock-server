package tests.restapi.scenarios.orderingRules;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

import java.io.IOException;


public class DuplicateBranchFromBranchWithOrderingRules {

    protected String productID;
    protected String seasonID;
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
    String orderingRuleID1;
    


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
    @Test(description ="add F1 -> F2, OR1 to branch1")
    public void addBranch1() throws Exception {
        branchID1 = addBranch("branch1",BranchesRestApi.MASTER);
        Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

        //add feature with configuration
        fJson.put("name", "F1");
        fJson.put("stage", "PRODUCTION");
        featureID1 = f.addFeatureToBranch(seasonID, branchID1, fJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
        
        fJson.put("name", "F2");
        featureID2 = f.addFeatureToBranch(seasonID, branchID1, fJson.toString(), featureID1, sessionToken);
        Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);


		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR1");
		JSONObject configJson = new JSONObject();
		configJson.put(featureID2, "1.5");
		
		jsonOR.put("configuration", configJson.toString());
		orderingRuleID1 = f.addFeatureToBranch(seasonID, branchID1, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);
    }

    @Test(dependsOnMethods = "addBranch1", description ="duplicate branch2 from branch1")
    public void duplicateBranch2FromBranch1 () throws Exception {
    	branchID2 = addBranch("branch2",branchID1);
        Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID1);
	
    	//verify F1 id was changed
    	JSONArray branch1Features = f.getFeaturesBySeasonFromBranch(seasonID, branchID1, sessionToken);
    	Assert.assertTrue(branch1Features.length() == 1, "unexpected number of features in branch1");
    	String feature1IdInBranch1 = branch1Features.getJSONObject(0).getString("uniqueId");
    	
    	JSONArray branch2Features = f.getFeaturesBySeasonFromBranch(seasonID, branchID2, sessionToken);
    	Assert.assertTrue(branch2Features.length() == 1, "unexpected number of features in branch2");
    	String feature1IdInBranch2 = branch2Features.getJSONObject(0).getString("uniqueId");
    	
    	Assert.assertTrue(feature1IdInBranch1!=feature1IdInBranch2, "feature1 id wasnt changed during branch duplication");
    	
    	//verify F2 id was changed    	
    	JSONArray subFeaturesBranch1 = branch1Features.getJSONObject(0).getJSONArray("features");
    	Assert.assertTrue(subFeaturesBranch1.length() == 1, "unexpected number of sub features in branch1");
    	JSONObject feature2Branch1 =  subFeaturesBranch1.getJSONObject(0);
    	String feature2Branch1Id = feature2Branch1.getString("uniqueId");
    	
    	JSONArray subFeaturesBranch2 = branch2Features.getJSONObject(0).getJSONArray("features");
    	Assert.assertTrue(subFeaturesBranch2.length() == 1, "unexpected number of sub features in branch2");
    	JSONObject feature2Branch2 =  subFeaturesBranch2.getJSONObject(0);
    	String feature2Branch2Id = feature2Branch2.getString("uniqueId");
    	
    	Assert.assertTrue(feature2Branch1Id!=feature2Branch2Id, "feature2 id wasnt changed during branch duplication");
    	
    	//verify ordering rule id was changed    	
    	JSONArray orderingRulesBranch1 = branch1Features.getJSONObject(0).getJSONArray("orderingRules");
    	Assert.assertTrue(orderingRulesBranch1.length() == 1, "unexpected number of ordering rules in branch1");
    	JSONObject orderingRuleBranch1 =  orderingRulesBranch1.getJSONObject(0);
    	String orderingRuleBranch1Id = orderingRuleBranch1.getString("uniqueId");
    	
    	
    	JSONArray orderingRulesBranch2 = branch2Features.getJSONObject(0).getJSONArray("orderingRules");
    	Assert.assertTrue(orderingRulesBranch2.length() == 1, "unexpected number of ordering rules in branch2");
    	JSONObject orderingRuleBranch2 =  orderingRulesBranch2.getJSONObject(0);
    	String orderingRuleBranch2Id = orderingRuleBranch2.getString("uniqueId");
    	
    	Assert.assertTrue(orderingRuleBranch1Id!=orderingRuleBranch2Id, "ordering rule's id wasnt changed during branch duplication");
        
    	//verify that the ordering rule configuration was updated with new F2 id
    	String orderingRuleBranch1Config = orderingRuleBranch1.getString("configuration");
    	Assert.assertTrue(orderingRuleBranch1Config.contains(feature2Branch1Id) && !orderingRuleBranch1Config.contains(feature2Branch2Id), "ordering rule in branch1 contains f2 id from branch 1");
        
    	String orderingRuleBranch2Config = orderingRuleBranch2.getString("configuration");
    	Assert.assertTrue(!orderingRuleBranch2Config.contains(feature2Branch1Id) && orderingRuleBranch2Config.contains(feature2Branch2Id), "ordering rule in branch2 contains f2 id from branch 2");
        
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

