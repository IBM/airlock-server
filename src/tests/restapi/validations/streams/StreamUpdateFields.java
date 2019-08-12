package tests.restapi.validations.streams;

import java.io.IOException;

import org.apache.wink.json4j.JSON;
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
import tests.restapi.StreamsRestApi;

public class StreamUpdateFields {
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
	private void stageField() throws Exception{
		JSONObject json = getStream();
		json.remove("stage");
		updateStream(json, "stage", true);
		
		json = getStream();
		json.put("stage", "");
		updateStream(json, "stage", true);
		
		json = getStream();
		json.put("stage", JSON.NULL);
		updateStream(json, "stage", true);
	}

	@Test
	private void nameField() throws Exception{
		JSONObject json = getStream();
		json.remove("name");
		updateStream(json, "name", true);
		
		json = getStream();
		json.put("name", "");
		updateStream(json, "name", true);
		
		json = getStream();
		json.put("name", JSON.NULL);
		updateStream(json, "name", true);
	}
	
	
	@Test
	private void descriptionField() throws Exception{
		JSONObject json = getStream();
		json.remove("description");
		updateStream(json, "description", false);
		
		json = getStream();
		json.put("description", "");
		updateStream(json, "description", false);
		
		json = getStream();
		json.put("description", JSON.NULL);
		updateStream(json, "description", false);
	}
	
	@Test
	private void enabledField() throws Exception{
		JSONObject json = getStream();
		json.remove("enabled");
		updateStream(json, "enabled", true);
		
		json = getStream();
		json.put("enabled", "");
		updateStream(json, "enabled", true);
		
		json = getStream();
		json.put("enabled", JSON.NULL);
		updateStream(json, "enabled", true);
	}
	
	@Test
	private void uniqueIdField() throws Exception{
		JSONObject json = getStream();
		json = getStream();
		json.put("uniqueId", "");
		updateStream(json, "uniqueId", true);
		
		json = getStream();
		json.put("uniqueId", JSON.NULL);
		updateStream(json, "uniqueId", false);
		
		json = getStream();
		json.remove("uniqueId");
		updateStream(json, "uniqueId", false);
	}
	
	@Test
	private void seasonIdField() throws Exception{
		JSONObject json = getStream();
		json = getStream();
		json.put("seasonId", "");
		updateStream(json, "seasonId", true);
		
		json = getStream();
		json.put("seasonId", JSON.NULL);
		updateStream(json, "seasonId", true);
		
		json = getStream();
		json.remove("seasonId");
		updateStream(json, "seasonId", true);
	}
	
	@Test
	private void creationDateField() throws Exception{
		JSONObject json = getStream();
		json = getStream();
		json.put("creationDate", System.currentTimeMillis());
		updateStream(json, "creationDate", true);

		json = getStream();
		json.put("creationDate", "");
		updateStream(json, "creationDate", true);
		
		json = getStream();
		json.put("creationDate", JSON.NULL);
		updateStream(json, "creationDate", true);
	}
	
	@Test
	private void creatorField() throws Exception{
		JSONObject json = getStream();
		json.remove("creator");
		updateStream(json, "creator", true);
		
		json = getStream();
		json.put("creator", "");
		updateStream(json, "creator", true);
		
		json = getStream();
		json.put("creator", JSON.NULL);
		updateStream(json, "creator", true);
	}
	
	
	@Test
	private void internalUserGroupsField() throws Exception{
		JSONObject json = getStream();
		json.remove("internalUserGroups");
		updateStream(json, "internalUserGroups", false);
		
		json = getStream();
		json.put("internalUserGroups", "");
		updateStream(json, "internalUserGroups", true);
		
		json = getStream();
		json.put("internalUserGroups", JSON.NULL);
		updateStream(json, "internalUserGroups", false);
		
		json = getStream();
		json.put("internalUserGroups", new JSONArray());
		updateStream(json, "internalUserGroups", false);

	}
	
	@Test
	private void rolloutPercentageField() throws Exception{
		JSONObject json = getStream();
		json.remove("rolloutPercentage");
		updateStream(json, "rolloutPercentage", true);
		
		json = getStream();
		json.put("rolloutPercentage", "");
		updateStream(json, "rolloutPercentage", true);
		
		json = getStream();
		json.put("rolloutPercentage", JSON.NULL);
		updateStream(json, "rolloutPercentage", true);
	}
	
	@Test
	private void minAppVersionField() throws Exception{
		JSONObject json = getStream();
		json.remove("minAppVersion");
		updateStream(json, "minAppVersion", true);
		
		json = getStream();
		json.put("minAppVersion", "");
		updateStream(json, "minAppVersion", true);
		
		json = getStream();
		json.put("minAppVersion", JSON.NULL);
		updateStream(json, "minAppVersion", true);
	}
	
	@Test
	private void lastModifiedField() throws Exception{
		JSONObject json = getStream();
		json = getStream();
		json.remove("lastModified");
		updateStream(json, "lastModified", true);

		json = getStream();
		json.put("lastModified", "");
		updateStream(json, "lastModified", true);
		
		json = getStream();
		json.put("lastModified", JSON.NULL);
		updateStream(json, "lastModified", true);
	}
	
	@Test
	private void filterField() throws Exception{
		JSONObject json = getStream();
		json.remove("filter");
		updateStream(json, "filter", true);
		
		//moved to separate test
		/*json = getStream();
		json.put("filter", "");
		updateStream(json, "filter", true);
		*/
		
		json = getStream();
		json.put("filter", JSON.NULL);
		updateStream(json, "filter", true);
	}
	
	@Test
	private void processorField() throws Exception{
		JSONObject json = getStream();
		json.remove("processor");
		updateStream(json, "processor", true);
		
		//moved to separate test
		/*json = getStream();
		json.put("processor", "");
		updateStream(json, "processor", true);
		*/
		
		json = getStream();
		json.put("processor", JSON.NULL);
		updateStream(json, "processor", true);
	}
	
	@Test
	private void cacheSizeKBField() throws Exception{
		JSONObject json = getStream();
		json.remove("cacheSizeKB");
		updateStream(json, "cacheSizeKB", false);
		
		
		json = getStream();
		json.put("cacheSizeKB", "");
		updateStream(json, "cacheSizeKB", true);
		
		
		json = getStream();
		json.put("cacheSizeKB", JSON.NULL);
		updateStream(json, "cacheSizeKB", false);
	}
	
	@Test
	private void queueSizeKBField() throws Exception{
		JSONObject json = getStream();
		json.remove("queueSizeKB");
		updateStream(json, "queueSizeKB", false);
		
		json = getStream();
		json.put("queueSizeKB", "");
		updateStream(json, "queueSizeKB", true);
		
		json = getStream();
		json.put("queueSizeKB", JSON.NULL);
		updateStream(json, "queueSizeKB", false);
	}
	
	@Test
	private void maxQueuedEventsField() throws Exception{
		JSONObject json = getStream();
		json.remove("maxQueuedEvents");
		updateStream(json, "maxQueuedEvents", false);
		
		json = getStream();
		json.put("maxQueuedEvents", "");
		updateStream(json, "maxQueuedEvents", true);
		
		json = getStream();
		json.put("maxQueuedEvents", JSON.NULL);
		updateStream(json, "maxQueuedEvents", false);
	}

	
	@Test
	private void ownerField() throws Exception{
		JSONObject json = getStream();
		json.remove("owner");
		updateStream(json, "owner", false);
		
		json = getStream();
		json.put("owner", "");
		updateStream(json, "owner", false);
		
		json = getStream();
		json.put("owner", JSON.NULL);
		updateStream(json, "owner", false);
	}
	
	@Test
	private void resultsSchemaField() throws Exception{
		JSONObject json = getStream();
		json.remove("resultsSchema");
		updateStream(json, "resultsSchema", true);
		
		//moved to separate test
		/*json = getStream();
		json.put("resultsSchema", "");
		updateStream(json, "resultsSchema", true);
		*/
		
		json = getStream();
		json.put("resultsSchema", JSON.NULL);
		updateStream(json, "resultsSchema", true);
	}
	
	private void updateStream(JSONObject streamJson, String field, boolean expectedResult) throws JSONException{

		try {
			String response = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
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
