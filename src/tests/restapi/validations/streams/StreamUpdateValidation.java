package tests.restapi.validations.streams;

import java.io.IOException;

import java.util.UUID;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class StreamUpdateValidation {
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
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
	}
	
	@Test
	private void astageField() throws Exception{
		JSONObject json = getStream();
		json.put("stage", "PRODUCTION");
		updateStringField(json, "stage", "PRODUCTION");
		
		json = getStream();
		json.put("stage", "DEVELOPMENT");
		updateStringField(json, "stage", "DEVELOPMENT");

	}
	
	@Test
	private void nameField() throws Exception{
		JSONObject json = getStream();
		json.put("name", "stream1aa");
		updateStringField(json, "name", "stream1aa");

	}

	
	@Test
	private void descriptionField() throws Exception{
		JSONObject json = getStream();
		json.put("description", "new stream descrption1");
		updateStringField(json, "description", "new stream descrption1");
	}
	
	
	@Test
	private void minAppVersionField() throws Exception{
		JSONObject json = getStream();
		json.put("minAppVersion", "10.0");
		updateStringField(json, "minAppVersion", "10.0");
	}
	
	@Test
	private void processorField() throws Exception{
		JSONObject json = getStream();
		json.put("processor", "function isTrue(){return true;}");
		updateStringField(json, "processor", "function isTrue(){return true;}");
	}
	
	@Test
	private void filterField() throws Exception{
		JSONObject json = getStream();
		json.put("filter", "app.name===stream1");
		updateStringField(json, "filter", "app.name===stream1");
	}
	
	@Test
	private void ownerField() throws Exception{
		JSONObject json = getStream();
		json.put("owner", "new owner");
		updateStringField(json, "owner", "new owner");
	}
	
	@Test
	private void resultsSchemaField() throws Exception{
		JSONObject json = getStream();
		String newSchema = json.getString("resultsSchema").replaceAll("userNumberOfSessionPerDay", "userNumbers");
		json.put("resultsSchema", newSchema);
		updateStringField(json, "resultsSchema", newSchema);
	}
	
	@Test
	private void enabledField() throws Exception{
		JSONObject json = getStream();
		json.put("enabled", false);
		try {
			String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update stream: " + response);
			
			String stream = streamApi.getStream(streamID, sessionToken);
			json = new JSONObject(stream);
			Assert.assertFalse (json.getBoolean("enabled"),  "enabled field was not updated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = getStream();
		json.put("uniqueId", UUID.randomUUID());
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " Stream was updated with incorrect uniqueId: " + response);

	}
	
	@Test
	private void seasonIdField() throws Exception{
		JSONObject json = getStream();
		json.put("seasonId", UUID.randomUUID());
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " Stream was updated with incorrect seasonId: " + response);

	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = getStream();
		json.put("creator", "new creator");
		String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), " Stream shouldn't be updated ");

	}
	


	@Test
	private void internalUserGroupsField() throws Exception{
		JSONObject json = getStream();
		JSONArray groups = new JSONArray();
		groups.add("QA");
		json.put("internalUserGroups", groups);
		
		try {
			String response = streamApi.updateStream(streamID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update stream: " + response);
			
			String stream = streamApi.getStream(streamID, sessionToken);
			json = new JSONObject(stream);
			Assert.assertTrue (json.getJSONArray("internalUserGroups").size()==1, "internalUserGroups field was not updated");
			Assert.assertTrue (json.getJSONArray("internalUserGroups").get(0).equals("QA"), "internalUserGroups field incorrect");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	private void rolloutPercentageField() throws Exception{
		JSONObject json = getStream();
		
		json = getStream();
		json.put("rolloutPercentage", 50);
		updateIntegerField(json, "rolloutPercentage", 50);
	}
	

	
	@Test
	private void lastModifiedField() throws Exception{
		JSONObject json = getStream();
		long newTime = System.currentTimeMillis()-1000;
		json.put("lastModified", newTime);
		updateDateField(json, "lastModified", newTime);

	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = getStream();
		long newTime = System.currentTimeMillis()-1000;
		json.put("creationDate", newTime);
		updateDateField(json, "creationDate", newTime);

	}
	
	@Test
	private void cacheSizeKBField() throws Exception{
		JSONObject json = getStream();
		json.put("cacheSizeKB", 512);
		updateIntegerField(json, "cacheSizeKB", 512);
	}
	
	@Test
	private void queueSizeKBField() throws Exception{
		JSONObject json = getStream();
		json.put("queueSizeKB", 512);
		updateIntegerField(json, "queueSizeKB", 512);
	}
	
	@Test
	private void maxQueuedEventsField() throws Exception{
		JSONObject json = getStream();
		json.put("maxQueuedEvents", 4);
		updateIntegerField(json, "maxQueuedEvents", 4);
	}

	

	
	private void updateStringField(JSONObject streamJson, String field, String expectedValue) throws Exception{

		try {
			String response = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update stream: " + response);
			
			String stream = streamApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(stream);
			Assert.assertTrue (json.getString(field).equals(expectedValue),  field + " was not updated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void updateIntegerField(JSONObject streamJson, String field, int expectedValue) throws Exception{

		try {
			String response = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), " Can't update stream: " + response);
			
			String stream = streamApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(stream);
			Assert.assertTrue (json.getInt(field)==expectedValue, field + " field was not updated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void updateDateField(JSONObject streamJson, String field, long expectedValue) throws Exception{

		try {
			String response = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
			//Assert.assertTrue(response.contains("error"), "Stream shouldn't be updated");
			
			String stream = streamApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(stream);
			Assert.assertFalse (json.getLong(field)==expectedValue,  field + " was updated");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	
	private JSONObject getStream() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject json = new JSONObject(stream);
		return json;
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
