package tests.restapi.validations.streams;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class StreamEventNameValidation {
	protected String seasonID;
	protected String streamID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected StreamsRestApi streamApi;
	protected String stream;
	protected List<String> illegalCharacters;
	protected List<String> legalCharacters;
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
		streamApi.setURL(m_url);

		illegalCharacters= new ArrayList<String>(Arrays.asList("\'", "\""));
	}
	
	
	@Test (description = "Validate legal special characters in name")
	public void legalCharactersInName() throws Exception{
		
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		JSONObject event1 = new JSONObject();
		event1.put("name", "video123");
		JSONObject event2 = new JSONObject();
		event2.put("name", "video.123");
		
		JSONObject eventData = new JSONObject();
		eventData.put("field1", "value1");
		event1.put("eventData", eventData);
		event2.put("eventData", eventData);
		
		JSONArray allEvents = new JSONArray();
		allEvents.add(event1);
		allEvents.add(event2);
		
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", allEvents);
		try {
			String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't create stream events");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Test (description = "Validate illegal special characters in name")
	public void illegalCharactersInName() throws Exception{

		for (String character : illegalCharacters) {

			String obj = streamApi.getStreamEvent(seasonID, sessionToken);
			JSONObject event1 = new JSONObject();
			event1.put("name", "video" + character);
			
			JSONArray allEvents = new JSONArray();
			allEvents.add(event1);
			
			JSONObject streamEvent = new JSONObject(obj);
			streamEvent.put("events", allEvents);
			try {
				String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
				Assert.assertTrue(response.contains("error"), "Create stream event with illegal character");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	


}
