package tests.restapi.scenarios.streams;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class StreamsEvents {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
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
	
	@Test (description="Add stream events")
	private void addStreamEvents() throws Exception{
		
		String events = FileUtils.fileToString(filePath + "streams/stream_event1.txt", "UTF-8", false);		
		JSONObject newEvents = new JSONObject(events);
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", newEvents.getJSONArray("events"));
		String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add the stream events: " + response);
	}
	
	@Test (dependsOnMethods="addStreamEvents", description="Filter events")
	private void filterEvents() throws Exception{
		String result = streamApi.getStreamEventFields(seasonID, "event.name==\"video-played\"", sessionToken);
		JSONObject json = new JSONObject(result);
		Assert.assertTrue(json.getJSONObject("event").getJSONObject("eventData").keySet().size()==4, "One or more eventData missing in filter");

	}
	
	
	@Test (dependsOnMethods="filterEvents", description="Filter events")
	private void filterEventsByFilter() throws Exception{
		String result = streamApi.getStreamEventsByFilter(seasonID, "event.name==\"video-played\" || event.name==\"ad-played\"", "DEVELOPMENT", sessionToken);
		JSONObject json = new JSONObject(result);
		Assert.assertTrue(json.getJSONArray("events").getJSONObject(0).getJSONObject("eventData").keySet().size()==4, "One or more events missing in filter");

		result = streamApi.getStreamEventsByFilter(seasonID, "event.name==\"video-played\"", "DEVELOPMENT", sessionToken);
		json = new JSONObject(result);
		Assert.assertTrue(json.getJSONArray("events").getJSONObject(0).getJSONObject("eventData").keySet().size()==2, "One or more events missing in filter for DEV stage");

		result = streamApi.getStreamEventsByFilter(seasonID, "event.name==\"ad-played\"", "PRODUCTION", sessionToken);
		json = new JSONObject(result);
		Assert.assertTrue(json.getJSONArray("events").getJSONObject(0).getJSONObject("eventData").keySet().size()==2, "One or more events missing in filter for PROD stage");

	}
	
	@Test (dependsOnMethods="filterEventsByFilter", description="Update stream events")
	private void updateStreamEvents() throws Exception{
		
		String events = FileUtils.fileToString(filePath + "streams/stream_event2.txt", "UTF-8", false);		
		JSONObject newEvents = new JSONObject(events);
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", newEvents.getJSONArray("events"));
		String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add the stream events: " + response);
		
		String result = streamApi.getStreamEventFields(seasonID, "event.name==\"audio-played\"", sessionToken);
		JSONObject json = new JSONObject(result);
		Assert.assertTrue(json.getJSONObject("event").getJSONObject("eventData").keySet().size()==7, "One or more eventData missing in filter");

	}

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
