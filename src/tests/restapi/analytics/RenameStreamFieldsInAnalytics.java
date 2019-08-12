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

public class RenameStreamFieldsInAnalytics {
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
	
	
	@Test (description="Add input schema to the season & create stream")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
        
        
		String stream = FileUtils.fileToString(filePath + "streams/stream_hierarchy.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "hier");
		streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
	}
	

	
	@Test (dependsOnMethods="addSchema", description="Add a stream field  to inputFieldsToAnalytics")
	public void addStreamFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		
		String input = an.addInputFieldsToAnalytics(response, "context.streams.hier.maps.fiveSessions.share");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertTrue(ifGlobalAnalyticsContainsField(response, "context.streams.hier.maps.fiveSessions.share"), "Field was not found in globalAnalytics response");
		
	}
	
	@Test (dependsOnMethods="addStreamFieldToAnalytics", description="rename middle node in stream schema ")
	private void renameMiddleNode() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject streamJson = new JSONObject(stream);
		JSONObject newSchema = new JSONObject(FileUtils.fileToString(filePath + "streams/stream_hierarchy_renamed.txt", "UTF-8", false));
		streamJson.put("resultsSchema", newSchema);
		String result = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertFalse(result.contains("error"), "Stream was not updated: " + result);

		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(ifGlobalAnalyticsContainsField(response, "context.streams.hier.maps.fiveSessions.share"), "Removed Field was  found in globalAnalytics response");

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
