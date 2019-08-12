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
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class DeleteBranchInUse {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	private String branchID2;
	protected String filePath;
	protected ProductsRestApi p;
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
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	/*
- delete season deletes branch
- branch in use by variant can't be deleted
- if there are more than 1 season with the same branch in use, this branch can be deleted 
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment1");
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);	

	}
	

	@Test (dependsOnMethods="addComponents", description ="Delete season with branch in use by variant") 
	public void deleteSeason1() throws Exception{
		//can't delete season with branch in use if it's the only season
		int responseCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertFalse(responseCode==200, "Season was deleted");
				
	}
	
	
	@Test (dependsOnMethods="deleteSeason1", description ="Delete branch in use by variant") 
	public void deleteBranchInUse() throws Exception{
		//branch in use by variant can't be deleted
		int responseCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertFalse(responseCode==200, "Deleted branch in use by variant");
		
		//create a new branch and update variant with it
		branchID2 = addBranch("branch2");
		Assert.assertFalse(branchID2.contains("error"), "Branch2 was not created: " + branchID2);
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("branchName", "branch2");
		String response = exp.updateVariant(variantID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not updated: " + response);
		
		//delete first branch
		responseCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "Branch not in use was not deleted");
		
		response = br.getBranch(branchID, sessionToken); 
		Assert.assertTrue(response.contains("not found"), "Got deleted branch");
		
	}

	
	@Test (dependsOnMethods="deleteBranchInUse", description ="Delete season with branch not in use") 
	public void deleteSeason2() throws Exception{
		
		//delete experiment
		int responseCode = exp.deleteExperiment(experimentID, sessionToken);
		Assert.assertTrue(responseCode==200, "Experiment was not deleted");
		
		//branch is not is use, can delete season
		responseCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertTrue(responseCode==200, "Season was not deleted");
		
		String response = br.getBranch(branchID2, sessionToken); 
		Assert.assertTrue(response.contains("not found"), "Got deleted branch");
		
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
		return baseUtils.addVariant(experimentID, "variant1", "branch1", m_analyticsUrl, false);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
