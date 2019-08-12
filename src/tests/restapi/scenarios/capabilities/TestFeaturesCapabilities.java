package tests.restapi.scenarios.capabilities;


import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.OperationRestApi;

public class TestFeaturesCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
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
		
		if (isAuthenticated.equals("true"))
			allApis.isAuthenticated = true;

		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);


	}
	

	
	@Test (description = "Set  only FEATURES capabilities in product")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = new JSONArray();
		capabilities.add("FEATURES");
		capabilities.add("RUNTIME_ENCRYPTION");
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	

	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="setCapabilities", description = "test capability")
	public void testCapabilities() throws Exception{
		
		results = allApis.runAllProduct(sessionToken, false);		
		if (results.size() > 0)
			Assert.fail("positive product capability test failed: " + results.toString());
		
		String productID = allApis.createProduct(sessionToken);
		
		results = allApis.runAllSeason(productID, sessionToken, false);		
		if (results.size() > 0)
			Assert.fail("positive season capability test failed: " + results.toString());
		
		String seasonID = allApis.createSeason(productID, sessionToken);

		results = allApis.runAllFeatures(seasonID, BranchesRestApi.MASTER, sessionToken, false);		
		if (results.size() > 0)
			Assert.fail("positive feature capability test failed: " + results.toString());

		results = allApis.runAllInputSchema(seasonID, sessionToken, false);		
		if (results.size() > 0)
			Assert.fail("positive InputSchema capability test failed: " + results.toString());

		results = allApis.runAllUtilities(seasonID, sessionToken, false);		
		if (results.size() > 0)
			Assert.fail("positive utilities capability test failed: " + results.toString());

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	


}
