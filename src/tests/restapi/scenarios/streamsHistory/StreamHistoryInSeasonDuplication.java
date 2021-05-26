package tests.restapi.scenarios.streamsHistory;


import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;

public class StreamHistoryInSeasonDuplication {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID1;
	private String streamID2;
	protected StreamsRestApi streamApi;
	protected SeasonsRestApi s;
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
		 s = new SeasonsRestApi();
	     s.setURL(m_url);
	}
	
	@Test (description="update global settings ")
	private void updateGlobalSettings() throws Exception {
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 1700);
		seasonStreamsObj.put("bulkSize", 2);
		seasonStreamsObj.put("filter", "true");
		seasonStreamsObj.put("maxHistoryTotalSizeKB", 13);
		seasonStreamsObj.put("historyBufferSize", 400);
		seasonStreamsObj.put("historyFileMaxSizeKB",167);
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "Global streams setting were not updated: " + seasonStreams);
	}
	
	@Test (dependsOnMethods="updateGlobalSettings", description="Create stream 1 ")
	private void createStream1() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject streamObj = new JSONObject(stream);
		streamObj.put("name", "test1");
		streamObj.put("processEventsOfLastNumberOfDays", 365);
		streamObj.put("operateOnHistoricalEvents", true);
		streamObj.remove("queueSizeKB");
		
		streamID1 = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertFalse(streamID1.contains("error"), "Stream was not created: " + streamID1);
		
		stream = streamApi.getStream(streamID1, sessionToken);
		streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.getInt("processEventsOfLastNumberOfDays") == 365, "processEventsOfLastNumberOfDays is not 365");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate default is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate default is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
	}
	
	
	@Test (dependsOnMethods="createStream1", description="Create stream 2 ")
	private void createStream2() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject streamObj = new JSONObject(stream);
		streamObj.put("name", "test2");
		streamObj.put("limitByStartDate", 100);
		streamObj.put("limitByEndDate", 365);
		streamObj.put("operateOnHistoricalEvents", true);
		
		streamID2 = streamApi.createStream(seasonID, streamObj.toString(), sessionToken);
		Assert.assertFalse(streamID2.contains("error"), "Stream was not created: " + streamID2);
		
		stream = streamApi.getStream(streamID2, sessionToken);
		streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.getInt("limitByEndDate") == 365, "limitByEndDate default is not 365");
		Assert.assertTrue(streamObj.getInt("limitByStartDate") == 100, "limitByStartDate default is not 100");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		String streams = streamApi.getAllStreams(seasonID, sessionToken);
        JSONObject newStreams = new JSONObject(streams);
        Assert.assertTrue(newStreams.getJSONArray("streams").size() == 2, "wrong number of streams");
	}
	
	@Test (dependsOnMethods="createStream2", description="duplicateSeason")
	private void duplicateSeason() throws Exception{
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        String seasonID2 = s.addSeason(productID, season, sessionToken);	
        Assert.assertFalse(streamID2.contains("error"), "Season was not created: " + streamID2);
        
        String streams = streamApi.getAllStreams(seasonID2, sessionToken);
        JSONObject newStreams = new JSONObject(streams);
        Assert.assertTrue(newStreams.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents is not true");
        Assert.assertTrue(newStreams.getInt("keepHistoryOfLastNumberOfDays") == 1700, "keepHistoryOfLastNumberOfDays is not 1700");
        Assert.assertTrue(newStreams.getString("filter").equals("true"), "filter id not 'true'"); 
        Assert.assertTrue(newStreams.getInt("bulkSize") == 2, "bulkSize is not 2");
        Assert.assertTrue(newStreams.getInt("maxHistoryTotalSizeKB") == 13, "maxHistoryTotalSizeKB is not 13");
        Assert.assertTrue(newStreams.getInt("historyBufferSize") == 400, "historyBufferSize is not 400");
        Assert.assertTrue(newStreams.getInt("historyFileMaxSizeKB") == 167, "historyFileMaxSizeKB is not 167");
        Assert.assertTrue(newStreams.getJSONArray("streams").size() == 2, "not 2 streams");
        JSONObject stream1 = newStreams.getJSONArray("streams").getJSONObject(0);
        JSONObject stream2 = newStreams.getJSONArray("streams").getJSONObject(1);
     
        Assert.assertTrue(stream1.getString("name").equals("test1"), "wrong name");
        Assert.assertTrue(stream1.getInt("processEventsOfLastNumberOfDays") == 365, "processEventsOfLastNumberOfDays is not 365");
		Assert.assertTrue(stream1.get("limitByEndDate") == null, "limitByEndDate default is not null");
		Assert.assertTrue(stream1.get("limitByStartDate") == null, "limitByStartDate default is not null");
		Assert.assertTrue(stream1.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");
		
        Assert.assertTrue(stream2.getString("name").equals("test2"), "wrong name");
        Assert.assertTrue(stream2.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(stream2.getInt("limitByEndDate") == 365, "limitByEndDate default is not 365");
		Assert.assertTrue(stream2.getInt("limitByStartDate") == 100, "limitByStartDate default is not 100");
		Assert.assertTrue(stream2.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
