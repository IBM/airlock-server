package tests.restapi.scenarios.experiments;

import java.io.IOException;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;


public class CheckoutRoot {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private JSONObject fJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
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
	
	@Test (description ="F1, F2 -> F3") 
	public void scenario2 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", "F1");
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
				
		fJson.put("name", "F2");
		featureID2 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		
		fJson.put("name", "F3");
		featureID3 = f.addFeature(seasonID, fJson.toString(), featureID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
		
	
		
	}
	
	@Test (dependsOnMethods="scenario2", description="Add F4 to branch and move F2 to F4 in branch")
	public void addFeatureToBranch() throws JSONException, Exception{				
		fJson.put("name", "F4");
		featureID4 = f.addFeatureToBranch(seasonID, branchID,  fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
	
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		JSONArray children = feature4.getJSONArray("features");
		children.put(feature2);
		feature4.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, feature4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't move unchecked feature under new feature");
		
	}
	
	
	@Test (dependsOnMethods="addFeatureToBranch", description="Checkout root")
	public void checkoutRoot() throws JSONException, Exception{				
		String rootId = f.getRootId(seasonID, sessionToken);
		String response = br.checkoutFeature(branchID, rootId, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't check out root");
		
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		JSONArray expected = new JSONArray();
		expected.add("ns1.F1");
		expected.add("ns1.F4");
		Assert.assertEqualsNoOrder(expected.toArray(), root.getJSONArray("branchFeaturesItems").toArray(), "Incorrect items in branchFeaturesItems");
		
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(f1.getJSONArray("features").size()==0, "Incorrect number of features under F1");
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(f4.getJSONArray("features").size()==1, "Incorrect number of features under F4");
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(f2.getJSONArray("features").size()==1, "Incorrect number of features under F2");

	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
