package tests.restapi.scenarios.streams;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.StreamsRestApi;

public class StreamsInRuntime {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
	protected StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		
	}
	
	@Test (description="Create dev stream ")
	private void createStream() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");

	}
	
	@Test (dependsOnMethods="createStream", description="Move stream to production ")
	private void moveToProduction() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("stage", "PRODUCTION");
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move stream to production");
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		JSONObject jsonRes = new JSONObject(responseProd.message);
		Assert.assertEquals(jsonRes.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime production file");
		

	}
	
	@Test (dependsOnMethods="moveToProduction", description="Move stream to development ")
	private void moveToDevelopment() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("stage", "DEVELOPMENT");
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move stream to development");
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		JSONObject jsonRes = new JSONObject(responseProd.message);
		Assert.assertEquals(jsonRes.getJSONArray("streams").size(), 0, "Stream was not removed from runtime production file");
		

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
