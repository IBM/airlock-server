package tests.restapi.scenarios.stream_utilities;

import java.io.File;


import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONException;
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
import tests.restapi.UtilitiesRestApi;

public class UseContextFieldsInStreamUtility {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String  utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	//protected StringsRestApi t;
	protected InputSchemaRestApi schemaApi;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	private String m_url;
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_url = url;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		u = new UtilitiesRestApi();
		u.setURL(url);
				
		schemaApi = new InputSchemaRestApi();
		schemaApi.setURL(url);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
			
	}
	
	@Test(description = "update inputSchema")
	public void updateInputSchema() throws Exception{
		String schemaStr = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
		String schemaJsonStr = schemaApi.getInputSchema(seasonID, sessionToken);
		JSONObject schemaJson = new JSONObject(schemaJsonStr);
		schemaJson.put("inputSchema", new JSONObject(schemaStr));
		String response = schemaApi.updateInputSchema(seasonID, schemaJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@Test( dependsOnMethods = "updateInputSchema",  description = "Add utility that uses a context field")
	public void addUtility() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function A(){return context.device.locale == \"aaa\";}");

		
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Stream utility should be allowed to use a context field" );
	}
	
	@Test (dependsOnMethods="addUtility", description = "Use translate() in processor - not allowed")
	
	public void useContextFieldInStream() throws Exception{
		
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject json = new JSONObject(stream);
		json.put("processor", "function A(){return context.device.locale == \"aaa\";}");	
		
		StreamsRestApi streamApi = new StreamsRestApi();
		streamApi.setURL(m_url);

		String streamID = streamApi.createStream(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Used context field in stream" );
	}


	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
