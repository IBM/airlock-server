package tests.restapi.scenarios.streams;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
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
import tests.restapi.UtilitiesRestApi;

public class DuplicateSeasonWithStreams {
	protected String seasonID;
	private String seasonID2;
	protected String filePath;
	protected String m_url;
	protected StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	protected UtilitiesRestApi u;
	private SeasonsRestApi s;
	
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
		u = new UtilitiesRestApi();
		u.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
	}
	
	@Test (description="add stream")
	public void addStreams() throws IOException, JSONException{
		//add utility
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		String utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );

		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(stream);
		
		json.put("name", "stream1");
		String streamID1 = streamApi.createStream(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(streamID1.contains("error"), "Stream1 was not created: " + streamID1);
		
		json.put("name", "stream2");
		json.put("stage", "DEVELOPMENT");
		json.put("processor", "isTrue()");
		String streamID2 = streamApi.createStream(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(streamID2.contains("error"), "Stream2 was not created: " + streamID2);

	}
	
	@Test (dependsOnMethods="addStreams", description="Add stream events")
	private void addStreamEvents() throws Exception{
		
		String events = FileUtils.fileToString(filePath + "streams/global_events.json", "UTF-8", false);		
		JSONObject newEvents = new JSONObject(events);
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", newEvents.getJSONArray("events"));
		String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add the stream events: " + response);
	}
	
	@Test(dependsOnMethods="addStreamEvents", description="Create and validate season2")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"6.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		
		String allStreams = streamApi.getAllStreams(seasonID2, sessionToken);
		JSONArray streams = new JSONObject(allStreams).getJSONArray("streams");
		Assert.assertEquals(streams.size(), 2, "Incorrect number of streams in season2");
	
		String allUtils = u.getAllUtilites(seasonID2, sessionToken, "DEVELOPMENT");
		JSONArray utilities =  new JSONObject(allUtils).getJSONArray("utilities");
		Assert.assertTrue(utilities.getJSONObject(1).getString("utility").contains("isTrue"), "Incorrect utility in season2");
		Assert.assertEquals(utilities.getJSONObject(1).getString("type"), UtilitiesRestApi.STREAM_UTILITY, "Incorrect utility type in season2");
		
		//verify that the minAppVersion of the streams was not changed
		JSONObject newStream1 = streams.getJSONObject(0);
		JSONObject newStream2 = streams.getJSONObject(1);
		
		String allStreamsFirstSeason = streamApi.getAllStreams(seasonID, sessionToken);
		JSONArray streamsFirstSeason  = new JSONObject(allStreamsFirstSeason).getJSONArray("streams");
		Assert.assertEquals(streamsFirstSeason.size(), 2, "Incorrect number of streams in season1");
	
		JSONObject orgStream1 = streamsFirstSeason.getJSONObject(0);
		JSONObject orgStream2 = streamsFirstSeason.getJSONObject(1);
		
		Assert.assertTrue(newStream1.getString("minAppVersion").equals(orgStream1.getString("minAppVersion")), "minAppVersion was changed for stream1");
		Assert.assertTrue(newStream2.getString("minAppVersion").equals(orgStream2.getString("minAppVersion")), "minAppVersion was changed for stream2");
		
	}
	
	@Test(dependsOnMethods="addSeason", description="Add new stream utilities to season1 & season2")
	public void verifyUtilitiesInSeasons() throws IOException, JSONException{
		//add stream utility to season2
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isSeason2(){return true;}");
		String utilityID = u.addUtility(seasonID2, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
		
		
		String allUtils2 = u.getAllUtilites(seasonID2, sessionToken, "DEVELOPMENT");
		Assert.assertTrue(allUtils2.contains("isSeason2"), "New stream utility not found in season2");

		String allUtils1 = u.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT");
		Assert.assertFalse(allUtils1.contains("isSeason2"), "Stream utility from season2 found in season1");

		//add stream utility to season1
		String utility2 = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps2 = new Properties();
		utilProps2.load(new StringReader(utility2));
		utilProps2.setProperty("utility", "function isSeason1(){return true;}");
		String utilityID2 = u.addUtility(seasonID, utilProps2, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID2.contains("error"), "Can't add utility to season1: " + utilityID2 );
		
		allUtils1 = u.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT");
		Assert.assertTrue(allUtils1.contains("isSeason1"), "New stream utility not found in season1");

		allUtils2 = u.getAllUtilites(seasonID2, sessionToken, "DEVELOPMENT");
		Assert.assertFalse(allUtils2.contains("isSeason1"), "Stream utility from season1 found in season2");

	}
	
	@Test(dependsOnMethods="verifyUtilitiesInSeasons", description="Add new streams to season1 & season2")
	public void verifyStreamsInSeasons() throws Exception {
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(stream);		
		json.put("name", "stream3");
		String streamID1 = streamApi.createStream(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(streamID1.contains("error"), "Stream3 was not created in season1: " + streamID1);
		
		String allStreams1 = streamApi.getAllStreams(seasonID, sessionToken);
		JSONArray streams1 = new JSONObject(allStreams1).getJSONArray("streams");
		Assert.assertEquals(streams1.size(), 3, "Incorrect number of streams in season1");

		String allStreams2 = streamApi.getAllStreams(seasonID2, sessionToken);
		JSONArray streams2 = new JSONObject(allStreams2).getJSONArray("streams");
		Assert.assertEquals(streams2.size(), 2, "Incorrect number of streams in season2");
		
		
		json.put("name", "stream4");
		String streamID2 = streamApi.createStream(seasonID2, json.toString(), sessionToken);
		Assert.assertFalse(streamID2.contains("error"), "Stream4 was not created in season2: " + streamID2);
		
		allStreams1 = streamApi.getAllStreams(seasonID, sessionToken);
		streams1 = new JSONObject(allStreams1).getJSONArray("streams");
		Assert.assertEquals(streams1.size(), 3, "Incorrect number of streams in season1");

		allStreams2 = streamApi.getAllStreams(seasonID2, sessionToken);
		streams2 = new JSONObject(allStreams2).getJSONArray("streams");
		Assert.assertEquals(streams2.size(), 3, "Incorrect number of streams in season2");


	}
	
	@Test(dependsOnMethods="verifyStreamsInSeasons", description="Verify events in new season")
	public void verifyEventsInSeason() throws Exception {
		String originalEvents = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject streamEventOldSeason = new JSONObject(originalEvents);

		String newEvents = streamApi.getStreamEvent(seasonID2, sessionToken);
		JSONObject streamEventNewSeason = new JSONObject(newEvents);

		Assert.assertTrue(streamEventNewSeason.getJSONArray("events").size() == streamEventOldSeason.getJSONArray("events").size(), "Incorrect number of events in new season");

	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
