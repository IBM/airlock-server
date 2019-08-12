package tests.restapi.scenarios.streams;


import org.apache.wink.json4j.JSON;
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

public class StreamsIntegerFields {
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
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
	}
	
	@Test (dependsOnMethods="createStream", description="Null cacheSizeKB not written in runtime")
	private void nullCacheSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("cacheSizeKB", JSON.NULL);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set cacheSizeKB to null: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		JSONObject jsonRes = new JSONObject(responseDev.message);
		Assert.assertFalse(jsonRes.getJSONArray("streams").getJSONObject(0).containsKey("cacheSizeKB"), "null cacheSizeKB is written in runtime development file");

	}
	
	@Test (dependsOnMethods="nullCacheSizeKB", description="Null queueSizeKB not written in runtime")
	private void nullQueueSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("queueSizeKB", JSON.NULL);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set queueSizeKB to null: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		JSONObject jsonRes = new JSONObject(responseDev.message);
		Assert.assertFalse(jsonRes.getJSONArray("streams").getJSONObject(0).containsKey("queueSizeKB"), "null queueSizeKB is written in runtime development file");

	}
	
	@Test (dependsOnMethods="nullQueueSizeKB", description="Null maxQueuedEvents not written in runtime")
	private void nullMaxQueuedEvents() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("maxQueuedEvents", JSON.NULL);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set maxQueuedEvents to null: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		JSONObject jsonRes = new JSONObject(responseDev.message);
		Assert.assertFalse(jsonRes.getJSONArray("streams").getJSONObject(0).containsKey("maxQueuedEvents"), "null maxQueuedEvents is written in runtime development file");

	}
	
	@Test (dependsOnMethods="nullQueueSizeKB", description="Zero maxQueuedEvents not written in runtime")
	private void zeroMaxQueuedEvents() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("maxQueuedEvents", 0);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't set maxQueuedEvents to zero: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");		
	}
	
	@Test (dependsOnMethods="zeroMaxQueuedEvents", description="Negative maxQueuedEvents not written in runtime")
	private void negativeMaxQueuedEvents() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("maxQueuedEvents", -1);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't set maxQueuedEvents to -1: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");		
	}
	
	@Test (dependsOnMethods="negativeMaxQueuedEvents", description="Negative cacheSizeKB ")
	private void negativeCacheSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("cacheSizeKB", -1);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set cacheSizeKB to -1 " + response);

		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");

	}
	
	@Test (dependsOnMethods="negativeCacheSizeKB", description="Negative queueSizeKB")
	private void negativeQueueSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("queueSizeKB", -1);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set queueSizeKB to -1: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
	}
	
	@Test (dependsOnMethods="negativeQueueSizeKB", description="Zero cacheSizeKB ")
	private void zeroCacheSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("cacheSizeKB", 0);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set cacheSizeKB to 0 " + response);

		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");

	}
	
	@Test (dependsOnMethods="negativeCacheSizeKB", description="Zero queueSizeKB")
	private void zeroQueueSizeKB() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		json.put("queueSizeKB", 0);
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set queueSizeKB to 0: " + response);
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");
	}	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
