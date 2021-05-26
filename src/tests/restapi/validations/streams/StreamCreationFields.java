package tests.restapi.validations.streams;

import java.io.IOException;







import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
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

public class StreamCreationFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	protected String stream;
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
		stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
	}
	
	@Test
	private void stageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("stage");
		addStream(json, "stage", true);
		
		json = new JSONObject(stream);
		json.put("stage", "");
		addStream(json, "stage", true);
		
		json = new JSONObject(stream);
		json.put("stage", JSON.NULL);
		addStream(json, "stage", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("name");
		addStream(json, "name", true);
		
		json = new JSONObject(stream);
		json.put("name", "");
		addStream(json, "name", true);
		
		json = new JSONObject(stream);
		json.put("name", JSON.NULL);
		addStream(json, "name", true);
	}
	
	
	@Test
	private void descriptionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("description");
		addStream(json, "description", false);
		
		json = new JSONObject(stream);
		json.put("description", "");
		addStream(json, "description", false);
		
		json = new JSONObject(stream);
		json.put("description", JSON.NULL);
		addStream(json, "description", false);
	}
	
	@Test
	private void enabledField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("enabled");
		addStream(json, "enabled", true);
		
		json = new JSONObject(stream);
		json.put("enabled", "");
		addStream(json, "enabled", true);
		
		json = new JSONObject(stream);
		json.put("enabled", JSON.NULL);
		addStream(json, "enabled", true);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json = new JSONObject(stream);
		json.put("uniqueId", "");
		addStream(json, "uniqueId", true);
		
		json = new JSONObject(stream);
		json.put("uniqueId", JSON.NULL);
		addStream(json, "uniqueId", false);
		
		json = new JSONObject(stream);
		json.put("uniqueId", UUID.randomUUID());
		addStream(json, "uniqueId", true);
	}
	
	@Test
	private void seasonIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json = new JSONObject(stream);
		json.put("seasonId", "");
		addStream(json, "seasonId", true);
		
		json = new JSONObject(stream);
		json.put("seasonId", JSON.NULL);
		addStream(json, "seasonId", false);
		
		json = new JSONObject(stream);
		json.put("seasonId", UUID.randomUUID());
		addStream(json, "seasonId", true);
	}
	
	@Test
	private void creationDateField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json = new JSONObject(stream);
		json.put("creationDate", System.currentTimeMillis());
		addStream(json, "creationDate", true);

		json = new JSONObject(stream);
		json.put("creationDate", "");
		addStream(json, "creationDate", true);
		
		json = new JSONObject(stream);
		json.put("creationDate", JSON.NULL);
		addStream(json, "creationDate", false);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("creator");
		addStream(json, "creator", true);
		
		json = new JSONObject(stream);
		json.put("creator", "");
		addStream(json, "creator", true);
		
		json = new JSONObject(stream);
		json.put("creator", JSON.NULL);
		addStream(json, "creator", true);
	}
	
	
	@Test
	private void internalUserGroupsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("internalUserGroups");
		addStream(json, "internalUserGroups", false);
		
		json = new JSONObject(stream);
		json.put("internalUserGroups", "");
		addStream(json, "internalUserGroups", true);
		
		json = new JSONObject(stream);
		json.put("internalUserGroups", JSON.NULL);
		addStream(json, "internalUserGroups", false);
		
		json = new JSONObject(stream);
		json.put("internalUserGroups", new JSONArray());
		addStream(json, "internalUserGroups", false);

	}
	
	@Test
	private void rolloutPercentageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("rolloutPercentage");
		addStream(json, "rolloutPercentage", true);
		
		json = new JSONObject(stream);
		json.put("rolloutPercentage", "");
		addStream(json, "rolloutPercentage", true);
		
		json = new JSONObject(stream);
		json.put("rolloutPercentage", JSON.NULL);
		addStream(json, "rolloutPercentage", true);
	}
	
	@Test
	private void minAppVersionField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("minAppVersion");
		addStream(json, "minAppVersion", true);
		
		json = new JSONObject(stream);
		json.put("minAppVersion", "");
		addStream(json, "minAppVersion", true);
		
		json = new JSONObject(stream);
		json.put("minAppVersion", JSON.NULL);
		addStream(json, "minAppVersion", true);
	}
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json = new JSONObject(stream);
		json.put("lastModified", System.currentTimeMillis()-10000);
		addStream(json, "lastModified", true);

		json = new JSONObject(stream);
		json.put("lastModified", "");
		addStream(json, "lastModified", true);
		
		json = new JSONObject(stream);
		json.put("lastModified", JSON.NULL);
		addStream(json, "lastModified", false);
	}
	
	@Test
	private void filterField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("filter");
		addStream(json, "filter", true);
		
		json = new JSONObject(stream);
		json.put("filter", "");
		addStream(json, "filter", true);
		
		json = new JSONObject(stream);
		json.put("filter", JSON.NULL);
		addStream(json, "filter", true);
	}
	
	@Test
	private void processorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("processor");
		addStream(json, "processor", true);
		
		json = new JSONObject(stream);
		json.put("processor", "");
		addStream(json, "processor", true);
		
		json = new JSONObject(stream);
		json.put("processor", JSON.NULL);
		addStream(json, "processor", true);
	}
	
	@Test
	private void cacheSizeKBField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("cacheSizeKB");
		addStream(json, "cacheSizeKB", false);
		
		json = new JSONObject(stream);
		json.put("cacheSizeKB", "");
		addStream(json, "cacheSizeKB", true);
		
		json = new JSONObject(stream);
		json.put("cacheSizeKB", JSON.NULL);
		addStream(json, "cacheSizeKB", false);
	}
	
	@Test
	private void queueSizeKBField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("queueSizeKB");
		addStream(json, "queueSizeKB", false);
		
		json = new JSONObject(stream);
		json.put("queueSizeKB", "");
		addStream(json, "queueSizeKB", true);
		
		json = new JSONObject(stream);
		json.put("queueSizeKB", JSON.NULL);
		addStream(json, "queueSizeKB", false);
	}
	
	@Test
	private void maxQueuedEventsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("maxQueuedEvents");
		addStream(json, "maxQueuedEvents", false);
		
		json = new JSONObject(stream);
		json.put("maxQueuedEvents", "");
		addStream(json, "maxQueuedEvents", true);
		
		json = new JSONObject(stream);
		json.put("maxQueuedEvents", JSON.NULL);
		addStream(json, "maxQueuedEvents", false);
	}

	
	@Test
	private void ownerField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("owner");
		addStream(json, "owner", false);
		
		json = new JSONObject(stream);
		json.put("owner", "");
		addStream(json, "owner", false);
		
		json = new JSONObject(stream);
		json.put("owner", JSON.NULL);
		addStream(json, "owner", false);
	}
	
	@Test
	private void resultsSchemaField() throws IOException, JSONException{
		JSONObject json = new JSONObject(stream);
		json.remove("resultsSchema");
		addStream(json, "resultsSchema", true);
		
		json = new JSONObject(stream);
		json.put("resultsSchema", "");
		addStream(json, "resultsSchema", true);
		
		json = new JSONObject(stream);
		json.put("resultsSchema", JSON.NULL);
		addStream(json, "resultsSchema", true);
	}
	
	private void addStream(JSONObject streamJson, String field, boolean expectedResult) throws JSONException{
		if (!field.equals("name"))
			streamJson.put("name", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
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
