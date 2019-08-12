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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateRootInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String mixID1;
	private String mixID2;
	private String rootID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		

	}

	/*	
	 * change feature order in unchecked root
	 * checkout root
	 * add feature in master - shouldn't be seen in branch when root is checked out
	 * change features order in root
	 * 	-2 features
	 * 	- feature and mix
	 * 	- 2 mix
	 * uncheckout root
	 * 
	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature mix1 was not added to the season: " + mixID1);
		
		jsonF.put("name", "F3");
		String featureID3 = f.addFeature(seasonID, jsonF.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added to mix: " + featureID3);

		mixID2 = f.addFeature(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature mix2 was not added to the season: " + mixID2);
		
		jsonF.put("name", "F4");
		String featureID4 = f.addFeature(seasonID, jsonF.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to mix: " + featureID4);
		
		rootID = f.getRootId(seasonID, sessionToken);

	}
	
	@Test (dependsOnMethods="addComponents", description ="Update unchecked out root") 
	public void updateUncheckedRootInBranch () throws IOException, JSONException {
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		
		JSONArray children = new JSONArray();
		
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		
		children.add(feature2);
		children.add(feature1);
		children.add(mix1);
		children.add(mix2);
		
		root.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Updated feature that is not checked out in branch");
	}
	
	@Test (dependsOnMethods="updateUncheckedRootInBranch", description ="Checked out root") 
	public void checkoutRoot() throws IOException, JSONException{
		String response = br.checkoutFeature(branchID, rootID, sessionToken);
		Assert.assertFalse(response.contains("error"), "root was not checked out");
		
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect status for checked out root");
	}
	
	@Test (dependsOnMethods="checkoutRoot", description ="Add feature in master, it's not seen in root in branch")
	public void addFeatureInMaster() throws JSONException, IOException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "dummy");
		String dummyID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(dummyID.contains("error"), "Dummy feature was not added to master: " + dummyID);
		
		String response = f.getFeatureFromBranch(dummyID, branchID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Feature added to master is seen in checked out root in branch");

	}
	
	@Test (dependsOnMethods="addFeatureInMaster", description ="Update checked out root") 
	public void updateCheckedRootInBranch () throws IOException, JSONException {
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		JSONArray children = new JSONArray();
		
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject mix1 = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		
		children.add(feature2);
		children.add(mix2);
		children.add(feature1);
		children.add(mix1);
		
		root.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update checked out root in branch");
		
		root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(feature2.getString("uniqueId")), "Incorrect feature in the first place");
		Assert.assertTrue(root.getJSONArray("features").getJSONObject(1).getString("uniqueId").equals(mix2.getString("uniqueId")), "Incorrect feature in the second place");
		Assert.assertTrue(root.getJSONArray("features").getJSONObject(2).getString("uniqueId").equals(feature1.getString("uniqueId")), "Incorrect feature in the third place");
		Assert.assertTrue(root.getJSONArray("features").getJSONObject(3).getString("uniqueId").equals(mix1.getString("uniqueId")), "Incorrect feature in the fourth place");
	}
	
	@Test (dependsOnMethods="updateCheckedRootInBranch", description ="unchecked out root") 
	public void uncheckoutRoot() throws IOException, JSONException{
		String response = br.cancelCheckoutFeature(branchID, rootID, sessionToken);
		Assert.assertFalse(response.contains("error"), "root was not unchecked out");
		
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getString("branchStatus").equals("NONE"), "Incorrect status for checked out root");

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
