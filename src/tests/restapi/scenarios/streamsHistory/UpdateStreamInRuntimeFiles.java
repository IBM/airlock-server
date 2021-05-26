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

public class UpdateStreamInRuntimeFiles {
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
	
	@Test (dependsOnMethods="createStream", description="Update stream")
	private void updateFlobalSettings() throws Exception{
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 365);
		seasonStreamsObj.put("filter", "true");
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "Global streams setting were not updated: " + seasonStreams);
	}
	
	@Test (dependsOnMethods="updateFlobalSettings", description="Update stream")
	private void updateStream1() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("operateOnHistoricalEvents", true);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	
	@Test (dependsOnMethods="updateStream1", description="Update stream")
	private void updateStream2() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("limitByEndDate", 30);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.getInt("limitByEndDate") == 30, "limitByEndDate  is not 30");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getInt("limitByEndDate") == 30, "limitByEndDate is not 30 in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	
	@Test (dependsOnMethods="updateStream2", description="Update stream")
	private void updateStream3() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("limitByEndDate", JSONObject.NULL);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	
	@Test (dependsOnMethods="updateStream3", description="Update stream")
	private void updateStream4() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("limitByStartDate", 30);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.getInt("limitByStartDate") == 30, "limitByStartDate  is not 30");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getInt("limitByStartDate") == 30, "limitByStartDate is not 30 in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	@Test (dependsOnMethods="updateStream4", description="Update stream")
	private void updateStream5() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("limitByStartDate", JSONObject.NULL);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	@Test (dependsOnMethods="updateStream5", description="Update stream")
	private void updateStream6() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("processEventsOfLastNumberOfDays", 100);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.getInt("processEventsOfLastNumberOfDays") == 100, "processEventsOfLastNumberOfDays is not 100");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.getInt("processEventsOfLastNumberOfDays") == 100, "processEventsOfLastNumberOfDays is not 100 in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	@Test (dependsOnMethods="updateStream6", description="Update stream")
	private void updateStream7() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String existingStream = streamApi.getStream(streamID, sessionToken);
		JSONObject existingStreamObj = new JSONObject(existingStream);
		existingStreamObj.put("processEventsOfLastNumberOfDays", JSONObject.NULL);
		String response = streamApi.updateStream(streamID, existingStreamObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream was not updated");
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamObj = new JSONObject(stream);
		Assert.assertTrue(streamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null");
		Assert.assertTrue(streamObj.get("limitByEndDate") == null, "limitByEndDate  is not null");
		Assert.assertTrue(streamObj.get("limitByStartDate") == null, "limitByStartDate  is not null");
		Assert.assertTrue(streamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true");	
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertEquals(json.getJSONArray("streams").size(), 1, "Incorrect number of stream in runtime development file");
		existingStreamObj =  json.getJSONArray("streams").getJSONObject(0);
		Assert.assertTrue(existingStreamObj.get("processEventsOfLastNumberOfDays") == null, "processEventsOfLastNumberOfDays is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByEndDate") == null, "limitByEndDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.get("limitByStartDate") == null, "limitByStartDate is not null in runtime file");
		Assert.assertTrue(existingStreamObj.getBoolean("operateOnHistoricalEvents"), "operateOnHistoricalEvents is not true in runtime file");	
			
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");		
	}
	
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
