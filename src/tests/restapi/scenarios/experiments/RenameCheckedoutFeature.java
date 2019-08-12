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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;

public class RenameCheckedoutFeature {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID;
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
        f.setURL(m_url);
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
		Can't rename checked out feature
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Feature1 was not added: " + featureID);
		
		br.checkoutFeature(branchID, featureID, sessionToken);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Add components") 
	public void renameFeatureInBranch () throws IOException, JSONException {
		String feature = f.getFeatureFromBranch(featureID, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "Newname");
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID, jsonF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Checked out feature was renamed in branch");
		
		jsonF = new JSONObject(feature);
		jsonF.put("namespace", "Newnamespace");
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID, jsonF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Checked out feature namespace was changed in branch");
		
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
