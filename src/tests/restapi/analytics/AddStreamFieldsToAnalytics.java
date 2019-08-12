package tests.restapi.analytics;

import java.io.IOException;






import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class AddStreamFieldsToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String streamID;
	protected StreamsRestApi streamApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		m_branchType = branchType;
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	
	//add dev stream, report field to analytics
	//delete field from stream
	//add field to stream and report again
	//move stream to production
	//move stream to development
	//delete stream
	
	@Test (description="Add a field  to inputFieldsToAnalytics without stream in the season")
	public void addFieldToAnalyticsWithoutSchema() throws IOException, JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.streams.video_played.averageAdsTime");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "There is no input schema, but a context field was added to analytics" + response);
		
	}
	
	@Test (dependsOnMethods="addFieldToAnalyticsWithoutSchema", description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	@Test (dependsOnMethods="addSchema", description="Create stream ")
	private void createStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

	}
	
	@Test (dependsOnMethods="createStream", description="Add a stream field  to inputFieldsToAnalytics")
	public void addStreamFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.streams.video_played.averageAdsTime");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(ifGlobalAnalyticsContainsField(response, "context.streams.video_played.averageAdsTime"), "Field was not found in globalAnalytics response");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addStreamFieldToAnalytics", description="Delete from stream the field reported to analytics ")
	private void updateStreamRemoveField() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema2.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);

		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(ifGlobalAnalyticsContainsField(response, "context.streams.video_played.averageAdsTime"), "Removed Field was  found in globalAnalytics response");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertFalse(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "Removed field \"context.streams.video_played.averageAdsTime\" was  found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="updateStreamRemoveField", description="Add field to stream and report to analytics ")
	private void updateStreamAddField() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/results_schema1.json", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);

		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.streams.video_played.averageAdsTime");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(ifGlobalAnalyticsContainsField(response, "context.streams.video_played.averageAdsTime"), "Field was not found in globalAnalytics response");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}

	@Test (dependsOnMethods="updateStreamAddField", description="Move stream to production ")
	private void moveStreamToProduction() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("stage", "PRODUCTION");
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime production file");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods="moveStreamToProduction", description="Move stream to development ")
	private void moveStreamToDevelopment() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("stage", "DEVELOPMENT");
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertFalse(ifRuntimeContainsField(responseProd.message, "context.streams.video_played.averageAdsTime"), "Development field \"context.streams.video_played.averageAdsTime\" was  found in the runtime production file");

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods="moveStreamToDevelopment", description="Delete stream ")
	private void deleteStream() throws Exception{
		String dateFormat = an.setDateFormat();
		
		int respCode  = streamApi.deleteStream(streamID, sessionToken);
		Assert.assertTrue(respCode==200, "Stream was not deleted");
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(ifGlobalAnalyticsContainsField(response, "context.streams.video_played.averageAdsTime"), "Removed stream field was  found in globalAnalytics response");


		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertFalse(ifRuntimeContainsField(responseDev.message, "context.streams.video_played.averageAdsTime"), "The field \"context.streams.video_played.averageAdsTime\" was not found in the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was  changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was  changed");

	}
	
	
	
	
	private boolean ifGlobalAnalyticsContainsField(String input, String field) throws JSONException{
		JSONObject json = new JSONObject(input);
		JSONArray fields = json.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
		for (Object s : fields) {
			if (s.equals(field)) 
				return true;
		}
		return false;
	}
	
	
	private boolean ifRuntimeContainsField(String input, String field){
		
		try{
			JSONObject json = new JSONObject(input);
			if (json.containsKey("inputFieldsForAnalytics")){
				JSONArray inputFields = json.getJSONArray("inputFieldsForAnalytics");
				for (Object s : inputFields) {
					if (s.equals(field)) 
						return true;
				}
				//Arrays.asList(inputFields).contains(field);
				return false;
			} else {

				return false;
			}
		} catch (Exception e){
				return false;
		}
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
