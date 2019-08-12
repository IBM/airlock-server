package tests.restapi.scenarios.streams;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.StreamsRestApi;

public class StreamInputSample {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
	protected StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private InputSchemaRestApi schema;
	
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
		
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);

		
	}
	
	@Test (description="Create dev stream ")
	private void createStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "stream name");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
		
		String result = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(result);
		
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").containsKey("stream_name"), "Stream name object not found in input sample");
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").getJSONObject("stream_name").keySet().size()==3, "Incorrect number of keys in stream input sample result");
	}
	
	
	@Test (dependsOnMethods="createStream", description="Update stream name ")
	public void updateStreamName() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "new stream name");
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);
		
		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema);
		
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").containsKey("new_stream_name"), "Stream name object not found in input sample");
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").getJSONObject("new_stream_name").keySet().size()==3, "Incorrect number of keys in stream input sample result");
		
	}
	
	
	@Test (dependsOnMethods="updateStreamName", description="Update stream schema - add field")
	public void updateResultsSchemaAddField() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema1.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);
		
		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema).getJSONObject("context").getJSONObject("streams").getJSONObject("new_stream_name");
		
		Assert.assertTrue(schemaJson.containsKey("adsTotalNumberOfSession"), "New schema field is not found in input sample");
		Assert.assertTrue(schemaJson.keySet().size()==4, "Incorrect number of keys in stream input sample result");
		
	}
	
	@Test (dependsOnMethods="updateResultsSchemaAddField", description="Update stream schema - remove field")
	public void updateResultsSchemaRemoveField() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema2.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);
		
		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		Assert.assertFalse(resultSchema.contains("averageAdsTime"), "Old schema field was found in input sample");
		
	}
	
	@Test (dependsOnMethods="updateResultsSchemaRemoveField", description="Create dev stream ")
	public void updateStreamNameWithDot() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "stream.name");
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);
		
		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema);
		
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").containsKey("stream_name"), "Stream name object not found in input sample");
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").getJSONObject("stream_name").keySet().size()==2, "Incorrect number of keys in stream input sample result");
		
	}
	
	@Test (dependsOnMethods="updateStreamNameWithDot", description="Create dev stream ")
	public void addInputSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		
		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema);
		
		Assert.assertTrue(schemaJson.getJSONObject("context").keySet().size()>1, "Incorrect number of keys in the input schema");
		
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").containsKey("stream_name"), "Stream name object not found in input sample");
		Assert.assertTrue(schemaJson.getJSONObject("context").getJSONObject("streams").getJSONObject("stream_name").keySet().size()==2, "Incorrect number of keys in stream input sample result");
		
	}
	
	@Test (/*dependsOnMethods="addInputSchema", */description="Add empty stream schema")
	public void addEmptySchema() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream_empty.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "empty");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema).getJSONObject("context").getJSONObject("streams").getJSONObject("empty");
		
		
		Assert.assertFalse(schemaJson.containsKey("stage"), "Field stage found in empty schema");
		Assert.assertFalse(schemaJson.containsKey("minAppVersion"), "Field minAppVersion found in empty schema");
	}
	

	@Test (description="Add stream schema with deep hierarchy")
	public void addHierarchySchema() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream_hierarchy.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "hier");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

		String resultSchema = schema.getInputSample(seasonID, "DEVELOPMENT", "9.5", sessionToken, "MAXIMAL", 0.7);
		JSONObject schemaJson = new JSONObject(resultSchema).getJSONObject("context").getJSONObject("streams").getJSONObject("hier").getJSONObject("maps");
		Assert.assertTrue(schemaJson.containsKey("fiveSessions"), "Expected key not found in schema");
		//count number of keys
		
		Assert.assertTrue(schemaJson.getJSONObject("fiveSessions").keySet().size()==5, "Incorrect number of schema fields in maps/fiveSessions");
		
	}

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
