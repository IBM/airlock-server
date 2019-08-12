package tests.restapi.scenarios.experiments;

import java.io.IOException;


import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.SeasonsRestApi;

public class RenameBranchInUse {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;

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
Can't rename branch in use by variant
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Can't rename branch in use by variant") 
	public void updateBranchInUse() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "branch123");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Changed name of the branch in use by variant");
		
	}
	
	@Test (dependsOnMethods="updateBranchInUse", description ="Rename branch not in use by variant") 
	public void updateBranchNotInUse() throws Exception{
		//delete variant
		int responseCode = exp.deleteVariant(variantID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Variant was not deleted");
		
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "branch123");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update name of the branch not in use by variant");
		
	}


	
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);
	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private String addVariant(String variantName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
