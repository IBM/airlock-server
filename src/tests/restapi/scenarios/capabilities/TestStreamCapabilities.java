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
import tests.restapi.OperationRestApi;

public class TestStreamCapabilities {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	private String productID;
	private String seasonID;
	private String streamID;
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
	

	
	@Test (description = "Set  capabilities in product without stream")
	public void setCapabilities() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("STREAMS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
	}
	
	@Test (dependsOnMethods="setCapabilities", description = "Test create stream without stream capability")
	public void testCreateStream() throws JSONException, IOException{
		String response = allApis.createStream(seasonID, sessionToken);
		validateNegativeTestResult(response, "create stream");

	}
	
	@Test (dependsOnMethods="testCreateStream", description = "Set stream capability and add stream, remove stream capability")
	public void addStream() throws Exception{
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("STREAMS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
		streamID = allApis.createStream(seasonID, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Can't create stream: " + streamID);
		
		capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.remove("STREAMS"); 
		response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");

	}
	
	@Test (dependsOnMethods="addStream", description = "test streams api without capability")
	public void testNegativeCapabilities() throws Exception{
		
		String response = allApis.updateStream(streamID, sessionToken);
		validateNegativeTestResult(response, "update stream");
		
		response = allApis.getStream(streamID, sessionToken);
		validateNegativeTestResult(response, "get stream");
		
		int responseCode = allApis.deleteStream(streamID, sessionToken);
		if (responseCode == 200)
			results.add("delete stream");
		
		response = allApis.getAllStreams(seasonID, sessionToken);
		validateNegativeTestResult(response, "get all streams");
		
		response = allApis.getStreamEvents(seasonID, sessionToken);
		validateNegativeTestResult(response, "get stream events");
		
		response = allApis.updateStreamEvents(seasonID, sessionToken);
		validateNegativeTestResult(response, "update stream events");
		
		response = allApis.filterStreamEvents(seasonID, sessionToken);
		validateNegativeTestResult(response, "filter stream events");

		
		if (results.size() > 0)
			Assert.fail("negative stream capability test failed: " + results.toString());
		
	}
	
	
	@Test (dependsOnMethods="testNegativeCapabilities", description = "Set stream capability")
	public void addStreamCapabilities() throws Exception{		
		JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
		capabilities.add("STREAMS"); 
		String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set capabilities");
		
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods="addStreamCapabilities", description = "test streams api with capability")
	public void testPositiveCapabilities() throws Exception{
		
		/*
		String response = allApis.updateStream(streamID, sessionToken);
		validatePositiveTestResult(response, "update stream");
		
		response = allApis.getStream(streamID, sessionToken);
		validatePositiveTestResult(response, "get stream");
		
	
		response = allApis.getAllStreams(seasonID, sessionToken);
		validatePositiveTestResult(response, "get all streams");
		
		response = allApis.getStreamEvents(seasonID, sessionToken);
		validatePositiveTestResult(response, "get stream events");
		
		response = allApis.updateStreamEvents(seasonID, sessionToken);
		validatePositiveTestResult(response, "update stream events");
		
		response = allApis.filterStreamEvents(seasonID, sessionToken);
		validatePositiveTestResult(response, "filter stream events");
				
		int responseCode = allApis.deleteStream(streamID, sessionToken);
		if (responseCode != 200)
			results.add("delete stream");
			*/
		results = allApis.runAllStreams(seasonID, sessionToken, false);
		
		if (results.size() > 0)
			Assert.fail("positive stream capability test failed: " + results.toString());

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
