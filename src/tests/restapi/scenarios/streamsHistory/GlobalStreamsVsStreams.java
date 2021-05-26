package tests.restapi.scenarios.streamsHistory;


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

public class GlobalStreamsVsStreams {
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
	private void createStream() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays default is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate default is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate default is not null");
		Assert.assertTrue(!streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents	 default is not false");	
		
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		streamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays default is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate default is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate default is not null");
		Assert.assertTrue(!streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents	 default is not false");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
	}
	
	@Test (dependsOnMethods="createStream", description="Create stream with illegal history settings")
	private void streamWithIllegalSettings() throws Exception{
		//create
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject streamObj = new JSONObject(stream);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.put("name", "negativeTest");
		String response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was created with operateOnHistoricalEvents = true while global stream setting does not support history.");
		
		//update
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was updated to operateOnHistoricalEvents = true while global stream setting does not support history.");
		
		
		//update global setting to support history
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 17);
		seasonStreamsObj.put("filter", "true");
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "Global streams setting were not updated: " + seasonStreams);
	
		//create
		streamObj.put("processEventsOfLastNumberOfDays", 18);
		response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was created with processEventsOfLastNumberOfDays>global keepHistoryOfLastNumberOfDays");
		
		//update
		existingStreamObj.put("processEventsOfLastNumberOfDays", 18);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Stream was updated to processEventsOfLastNumberOfDays>global keepHistoryOfLastNumberOfDays");		
	}
	
	@Test (dependsOnMethods="streamWithIllegalSettings", description="Create stream with illegal history settings")
	private void streamWithSettings() throws Exception{
		
		//update global setting to support history
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		//seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 19);
		seasonStreamsObj.put("filter", "true");
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "Global streams setting were not updated: " + seasonStreams);
	
		//create
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject streamObj = new JSONObject(stream);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.put("name", "test1");
		String response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(!response.contains("error"), "Stream was not created with operateOnHistoricalEvents = true and global stream setting does support history.");
		
		//update
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(!response.contains("error"), "Stream was updated to operateOnHistoricalEvents = true while global stream setting does support history.");
		
		//create
		streamObj.put("processEventsOfLastNumberOfDays", 18);
		streamObj.put("name", "test2");
		response = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertTrue(!response.contains("error"), "Stream was not created with processEventsOfLastNumberOfDays<global keepHistoryOfLastNumberOfDays");
		
		//update
		existingStream = streamApi.getStream(streamID, sessionToken);
		existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("processEventsOfLastNumberOfDays", 18);
		response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertTrue(!response.contains("error"), "Stream was not updated to processEventsOfLastNumberOfDays<global keepHistoryOfLastNumberOfDays");		
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
