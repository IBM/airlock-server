package tests.restapi.scenarios.streamsHistory;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class HistoryStreamsNegative {
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
	
	@Test (description="Create stream ")
	private void createValidStream() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays default is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate default is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate default is not null");
		Assert.assertTrue(!streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents	 default is not false");	
	}
	
	@Test (dependsOnMethods="createValidStream", description="update Global Streams Settings to support history")
	private void updateGlobalStreamsSettings() throws Exception{
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 17);
		seasonStreamsObj.put("filter", "true");
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "Global streams setting were not updated: " + seasonStreams);
	
	}
	
	@Test (dependsOnMethods="updateGlobalStreamsSettings", description="Create stream with illegal history settings")
	private void createInvalidStream() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject streamObj = new JSONObject(stream);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.put("name", "negativeTest");
		streamObj.put("processEventsOfLastNumberOfDays", 3);
		streamObj.put("limitByStartDate", 3);
		
		String response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was created with processEventsOfLastNumberOfDays and limitByStartDate");
		
		streamObj = new JSONObject(stream);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.put("name", "negativeTest");
		streamObj.put("processEventsOfLastNumberOfDays", 3);
		streamObj.put("limitByEndDate", 3);
		
		response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was created with processEventsOfLastNumberOfDays and limitByEndDate");
		
		streamObj = new JSONObject(stream);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.put("name", "negativeTest");
		streamObj.put("limitByStartDate", 13);
		streamObj.put("limitByEndDate", 3);
		
		response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was created with limitByStartDate > limitByEndDate");
	}
	
	@Test (dependsOnMethods="createInvalidStream", description="update stream with illegal history settings")
	private void updateInvalidStream() throws Exception{
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		existingStreamObj.put("name", "negativeTest");
		existingStreamObj.put("processEventsOfLastNumberOfDays", 3);
		existingStreamObj.put("limitByStartDate", 3);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was updated with processEventsOfLastNumberOfDays and limitByStartDate.");
		
		existingStream = streamApi.getStream(streamID, sessionToken);
		existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		existingStreamObj.put("name", "negativeTest");
		existingStreamObj.put("processEventsOfLastNumberOfDays", 3);
		existingStreamObj.put("limitByEndDate", 3);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was updated with processEventsOfLastNumberOfDays and limitByEndDate.");
		
		existingStream = streamApi.getStream(streamID, sessionToken);
		existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		existingStreamObj.put("name", "negativeTest");
		existingStreamObj.put("limitByStartDate", 13);
		existingStreamObj.put("limitByEndDate", 3);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was updated with limitByEndDate < limitByStartDate.");
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
