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

public class StreamEventUniqueName {
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
	
	@Test (description="Create the same stream event name twice")
	private void createStream() throws Exception{
		String obj = streamApi.getStreamEvent(seasonID, sessionToken);
		
		JSONObject event = new JSONObject();
		event.put("name", "test-event");
		JSONObject eventData = new JSONObject();
		eventData.put("field1", "value1");
		event.put("eventData", eventData);
				
		JSONArray allEvents = new JSONArray();
		allEvents.add(event);
		allEvents.add(event);
		JSONObject streamEvent = new JSONObject(obj);
		streamEvent.put("events", allEvents);
		try {
			String response = streamApi.updateStreamEvent(seasonID, streamEvent.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't add the same event twice: " + response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
