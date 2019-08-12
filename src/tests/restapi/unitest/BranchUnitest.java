package tests.restapi.unitest;

import java.io.IOException;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class BranchUnitest {
	protected String productID;
	protected String seasonID;
	private String branchID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description="Add branch")
	public void addBranch() throws IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID = br.createBranch(seasonID, branch, "MASTER", sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch was not created " + branchID);
	}
	
	@Test (dependsOnMethods="addBranch", description="Get all branches")
	public void getAllBranches() throws Exception{
		
		String response = br.getAllBranches(seasonID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Failes to get all branches " + response);
	}
	

	@Test (dependsOnMethods="getAllBranches", description="Update branch")
	public void updateBranch() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject brJson = new JSONObject(branch);
		brJson.put("name", "newName");
		String response = br.updateBranch(branchID, brJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Failes to update branch " + response);
	}
	

	@Test (dependsOnMethods="updateBranch", description="Delete branch")
	public void deleteBranch() throws Exception{
		int respCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue(respCode==200, "Branch was not deleted ");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
