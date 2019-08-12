package tests.restapi.scenarios.capabilities;

import java.io.IOException;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;

public class VerifyProductsDifferentCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private String productID1;
	private String seasonID1;
	private String productID2;
	private String seasonID2;
	private TestAllApi allApis;
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		seasonID1 = baseUtils.createSeason(productID1);

		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);
		seasonID2 = baseUtils.createSeason(productID2);
	}
	
//product1 - no streams
// product 2 - no notifications	
	
	@Test (description = "Products capabilities")
	public void setCapabilitiesInProducts() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID1, sessionToken);
		capabilities.remove("STREAMS"); 
		String response = allApis.setCapabilitiesInProduct(productID1, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities for product1");
		
		capabilities = allApis.getCapabilitiesInProduct(productID2, sessionToken);
		capabilities.remove("NOTIFICATIONS"); 
		response = allApis.setCapabilitiesInProduct(productID2, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities for product2");
	}
	
	@Test (dependsOnMethods="setCapabilitiesInProducts", description = "Test products capabilities")
	public void testProducts() throws JSONException, IOException{
		String response = allApis.createStream(seasonID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Add stream without capability");
		
		response = allApis.createStream(seasonID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add stream");
		
		response = allApis.addNotification(seasonID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add notification");
		
		response = allApis.addNotification(seasonID2, sessionToken);
		Assert.assertTrue(response.contains("error"), "Add notification without capability");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID1, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
	

}
