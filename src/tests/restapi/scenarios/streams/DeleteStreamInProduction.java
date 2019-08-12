package tests.restapi.scenarios.streams;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;

public class DeleteStreamInProduction {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
	protected StreamsRestApi streamApi;
	private SeasonsRestApi s;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private InputSchemaRestApi schema;
	private FeaturesRestApi f;
	private String featureID;
	
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
		
		f = new FeaturesRestApi();
		f.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
	    schema = new InputSchemaRestApi();
	    schema.setURL(url);		
	}
	
//it is allowed to delete a stream in production stage
// it is not allowed to delete a season with a stream in production stage
	@Test (description="add regular schema")
	public void addInputSchema() {
	    try {
	        String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android_schema_v3.0.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String results =  schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(results.contains("error"), "Input schema was not added");
	    }catch (Exception e){
	        Assert.fail(e.getMessage());
	    }
	}
	
	
	@Test (dependsOnMethods="addInputSchema", description="Create dev stream ")
	private void createStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamJson.put("stage", "PRODUCTION");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
	}
	
	
	@Test (dependsOnMethods="createStream", description="Delete season ")
	private void deleteSeason() throws Exception{
		int respCode = s.deleteSeason(seasonID);
		Assert.assertFalse(respCode == 200, "Deleted season with stream in production");
	}

	
	@Test (dependsOnMethods="deleteSeason", description="Delete stream ")
	private void deleteStream() throws Exception{
		
		int codeResponse = streamApi.deleteStream(streamID, sessionToken);
		Assert.assertFalse(codeResponse == 200, "Stream was deleted");
	}
	

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
