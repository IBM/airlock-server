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

public class DeleteFeatureFromParentInBranch {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	
	@BeforeClass	
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		f = new FeaturesRestApi();
		f.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}

	/*delete
		- can't delete unchecked
		- can't delete checked
		- new from unchecked
		- new from checked
		- new from new
		- if sub-feature is checked-out, no delete: add new feature to branch. checkout feature from master and update its parent
	 to this new branch feature. delete this new branch feature - not allowed
	 */
	
	@Test (description ="Delete unchecked feature from new") 
	public void deleteUncheckedFeature () throws IOException, JSONException {
		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F1");
		String featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		//move unchecked F1 under new F2
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = feature2.getJSONArray("features");
		children.put(feature1);
		feature2.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checked feature under new  feature");

		int code = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertFalse (code==200, "Unchecked feature was deleted from under the new feature. Code: " + code);
	}
	
	@Test (description ="Delete checked feature from new") 
	public void deleteCheckedFeature () throws IOException, JSONException {
		String branchID = addBranch("branch2");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F3");
		String featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature3 was not added: " + featureID1);
		
		jsonF.put("name", "F4");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature4 was not added: " + featureID2);
		
		//move unchecked F1 under new F2
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = feature2.getJSONArray("features");
		children.put(feature1);
		feature2.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checked feature under new  feature");
		
		//checkout F1
		response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature1 was not checked out to branch");

		int code = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		Assert.assertFalse (code==200, "Checked feature was deleted from under the new feature. Code: " + code);
	}
	
	
	@Test (description ="Delete new feature") 
	public void deleteNewFeatureFromChecked () throws IOException, JSONException {
		String branchID = addBranch("branch4");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F7");
		String featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature7 was not added: " + featureID1);
		
		//checkout F1
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		jsonF.put("name", "F8");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), feature1.getString("uniqueId"), sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature8 was not added to branch: " + featureID2);
		
		int code = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue (code==200, "new feature was not deleted from under the checked feature in branch. Code: " + code);
	}
	
	@Test (description ="Delete new feature from new") 
	public void deleteNewFeatureFromNew() throws IOException, JSONException {
		String branchID = addBranch("branch5");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F9");
		String featureID1 = f.addFeatureToBranch(seasonID, branchID , jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature9 was not added: " + featureID1);		
		
		jsonF.put("name", "F10");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature10 was not added to branch: " + featureID2);
		
		int code = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue (code==200, "new feature was not deleted from under the new feature in branch. Code: " + code);
	}
	
	@Test (description ="Delete checked from new") 
	public void deleteCheckedFeatureFromNew () throws IOException, JSONException {
		String branchID = addBranch("branch6");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "F11");
		String featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature11 was not added: " + featureID1);
		
		//checkout F1
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not checked out to branch");

		
		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));		
		jsonF.put("name", "F12");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature12 was not added to branch: " + featureID2);
		
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));	
		JSONArray features = new JSONArray();
		features.add(feature1);
		feature2.put("features", features);
		response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2.toString(), sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "checked out sub-feature was not added to Feature12 in branch: " + featureID2);
		
		//delete new feature to branch with checked out feature as a sub-feature 
		int code = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertFalse (code==200, "deleted new feature when its sub-feature is checked-out from branchS");
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
