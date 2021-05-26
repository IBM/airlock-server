package tests.restapi.scenarios.capabilities;



import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class TestExperimentsCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String experimentID;
	private String variantID;
	private String allExperiments;
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
		 
		capabilities.remove("EXPERIMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create experiment without experiment capability")
	public void testCreateExperiment() throws Exception{
		
		String response = allApis.createExperiment(productID, sessionToken);
		validateNegativeTestResult(response, "create experiment");

	}
	
	@Test (dependsOnMethods="testCreateExperiment", description = "Set experiment capability and add experiment, remove experiment capability")
	public void addExperiment() throws Exception{
		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("EXPERIMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		//branch required for variant testing
		String branchID = allApis.createBranch(seasonID, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Can't create branch: " + branchID);		
		JSONObject branch = new JSONObject(allApis.getBranch(branchID, sessionToken));

		
		experimentID = allApis.createExperiment(productID, sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create experiment: " + experimentID);
		
		variantID = allApis.createVariant(experimentID, branch.getString("name"), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Can't create variant: " + variantID);

		allExperiments = allApis.getAllExperiments(productID, sessionToken);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("EXPERIMENTS"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addExperiment", description = "test experiment api without capability")
	public void testNegativeCapabilities() throws Exception{
		//branch required for variant testing
		String branchID1 = allApis.createBranch(seasonID, sessionToken);
		Assert.assertFalse(branchID1.contains("error"), "Can't create branch: " + branchID1);		
		JSONObject branch = new JSONObject(allApis.getBranch(branchID1, sessionToken));

		String response = allApis.updateExperiment(experimentID, sessionToken);
		validateNegativeTestResult(response, "update experiment");
		
		response = allApis.getExperiment(experimentID, sessionToken);
		validateNegativeTestResult(response, "get experiment");
				
		response = allApis.getAllExperiments(productID, sessionToken);
		validateNegativeTestResult(response, "get all experiment");
	
		response = allApis.resetDashboard(experimentID, sessionToken);
		validateNegativeTestResult(response, "resetDashboard");
		
		response = allApis.updateAllExperiments(productID, allExperiments, sessionToken);
		validateNegativeTestResult(response, "updateAllExperiments");
		
		response = allApis.getExperimentInputSample(experimentID, sessionToken);
		validateNegativeTestResult(response, "getExperimentInputSample");

		response = allApis.getExperimentUtilitiesInfo(experimentID, sessionToken);
		validateNegativeTestResult(response, "getExperimentUtilitiesInfo");
		
		response = allApis.getBranchesInExperiment(experimentID, sessionToken);
		validateNegativeTestResult(response, "getBranchesInExperiment");
		
		response = allApis.getExperimentIndexingInfo(experimentID, sessionToken);
//		validateNegativeTestResult(response, "getExperimentIndexingInfo");
		if (!response.equals("{}")) {
			results.add("getExperimentIndexingInfo");
		}

		response = allApis.getExperimentGlobalDataCollection(experimentID, sessionToken);
		validateNegativeTestResult(response, "getExperimentGlobalDataCollection");

		response = allApis.createVariant(experimentID, branch.getString("name"), sessionToken);
		validateNegativeTestResult(response, "create variant");
		
		response = allApis.getVariant(variantID, sessionToken);
		validateNegativeTestResult(response, "get variant");
		
		response = allApis.updateVariant(variantID, sessionToken);
		validateNegativeTestResult(response, "update variant");
		
		int responseCode = allApis.deleteVariant(variantID, sessionToken);
		if (responseCode == 200)
			results.add("delete variant");
		
		responseCode = allApis.deleteExperiment(experimentID, sessionToken);
		if (responseCode == 200)
			results.add("delete experiment");
	
		if (results.size() > 0)
			Assert.fail("negative stream capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set experiment capability")
	public void addExperimentCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("EXPERIMENTS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addExperimentCapabilities", description = "test experiment api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		results = allApis.runAllExperiments(productID, seasonID, sessionToken, false);
		
		if (results.size() > 0)
			Assert.fail("positive experiments capability test failed: " + results.toString());

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
