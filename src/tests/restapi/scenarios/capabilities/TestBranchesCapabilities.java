package tests.restapi.scenarios.capabilities;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.OperationRestApi;

public class TestBranchesCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String branchID;
	private ArrayList<String> results = new ArrayList<String>();
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "isAuthenticated"})
	public void init(String url,  String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String isAuthenticated) throws Exception{
		
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);

		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);


	}
	

	
	@Test (description = "Set  capabilities in product without branch")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("BRANCHES"); 
		capabilities.remove("EXPERIMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create branch without stream branch")
	public void testCreateBranch() throws JSONException, IOException{
		String response = allApis.createBranch(seasonID, sessionToken);
		validateNegativeTestResult(response, "create branch");

	}
	
	@Test (dependsOnMethods="testCreateBranch", description = "Set branch capability and add branch, remove branch capability")
	public void addBranch() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("BRANCHES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		branchID = allApis.createBranch(seasonID, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Can't create branch: " + branchID);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("BRANCHES"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addBranch", description = "test branch api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.updateBranch(branchID, sessionToken);
		validateNegativeTestResult(response, "update branch");
		
		response = allApis.getBranch(branchID, sessionToken);
		validateNegativeTestResult(response, "get branch");
				
		response = allApis.getAllBranches(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all branches");
				
		response = allApis.updateBranch(branchID, sessionToken);
		validateNegativeTestResult(response, "updateBranch");
		
		String featureID = allApis.createFeature(seasonID, BranchesRestApi.MASTER, sessionToken);
		response = allApis.checkOut(branchID, featureID, sessionToken);
		validateNegativeTestResult(response, "checkOut");
		
		response = allApis.cancelCheckOut(branchID, featureID, sessionToken);
		validateNegativeTestResult(response, "cancelCheckOut");
		
		int responseCode = allApis.deleteBranch(branchID, sessionToken);
		if (responseCode == 200)
			results.add("delete branch");
	
		if (results.size() > 0)
			Assert.fail("negative stream capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set branch capability")
	public void addBranchesCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("BRANCHES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addBranchesCapabilities", description = "test branches api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		results = allApis.runAllBranches(seasonID, sessionToken, false);
		
		if (results.size() > 0)
			Assert.fail("positive branches capability test failed: " + results.toString());

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	
	
	private void validateNegativeTestResult(String response, String error){
		if (!response.contains("error"))
			results.add(error);
	}


}
