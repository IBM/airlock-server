package tests.restapi.scenarios.capabilities;

import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class VerifyProductCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private String productID;
	private TestAllApi allApis;
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		allApis.resetServerCapabilities(sessionToken);

	}
	

	
	@Test (description = "Set empty capabilities")
	public void setEmptyCapabilities() throws Exception{
		JSONArray capabilities = new JSONArray();
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Set empty capabilities in product");

	}
	
	@Test (description = "Duplicate capabilities")
	public void duplicateCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("FEATURES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Set duplicate capabilities");
	}
	
	@Test (description = "invalid capabilities")
	public void invalidCapabilities() throws Exception{

		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("FEAT__URES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Set invalid capabilities");
	}
	
	@Test (description = "No features capabilities")
	public void noFeaturesCapabilities() throws Exception{

		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("FEATURES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Removed required FEATURES from capabilities");
	}

	
	@Test (description = "Experiments in capabilities")
	public void experimentsInCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("BRANCHES"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertTrue(response.contains("error"), "Set EXPERIMENTS without  BRANCHES capabilities");

	}
	
	@AfterTest(alwaysRun = true)
	public void reset() throws Exception{
		//allApis.resetServerCapabilities(sessionToken);
		baseUtils.reset(productID, sessionToken);
	}
}
