package tests.restapi.validations.streams;

import java.io.IOException;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class StreamEventCreationFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	//protected String stream;
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
		//stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		
	}
	
	@Test
	private void missingName() throws Exception{
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject event = new JSONObject();
		event.put("video", "somevalue");
		JSONArray allEvents = new JSONArray();
		allEvents.add(event);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", allEvents);
		try {
			String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Updated stream event with missing name field ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	private void missingEventDataField() throws Exception{
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject event = new JSONObject();
		event.put("name", "video");
		
		JSONArray allEvents = new JSONArray();
		allEvents.add(event);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", allEvents);
		try {
			String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Updated stream event without eventData field ");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
