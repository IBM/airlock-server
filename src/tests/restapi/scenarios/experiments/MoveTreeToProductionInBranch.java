package tests.restapi.scenarios.experiments;

import java.io.IOException;


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

public class MoveTreeToProductionInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
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
		br = new BranchesRestApi();
		br.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		

	}

	/*	
		F1->F2+F3 in master. Add F4 under F1 in branch. All features in production.
		Checkout F1 & F2, F3 has status NONE
		Get the whole feature tree from branch (including new and none), change stage to development and update
		It should fail as F3 is not checked out, is in production and can't change stage in branch

	 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID= br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature1);
		jsonF.put("stage", "PRODUCTION");
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);
		
		jsonF.put("name", "F2");
		String featureID2 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		jsonF.put("name", "F3");
		String featureID3 = f.addFeature(seasonID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID3);

		br.checkoutFeature(branchID, featureID1, sessionToken);
		br.checkoutFeature(branchID, featureID2, sessionToken);
		
		jsonF.put("name", "F4");
		String featureID4 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), featureID1, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to branch: " + featureID4);
		

	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Move feature tree to production stage in branch") 
	public void updateStageInBranch () throws Exception {

		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		feature = feature.replaceAll("PRODUCTION", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, feature, sessionToken);
		Assert.assertTrue(response.contains("cannot update an item"), "Feature tree was incorrectly updated");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
