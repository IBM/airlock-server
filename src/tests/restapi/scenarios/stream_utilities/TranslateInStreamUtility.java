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
import tests.restapi.StreamsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class TranslateInStreamUtility {
	protected String seasonID;
	protected String streamID;
	protected String productID;
	protected String filePath;
	private StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	protected StringsRestApi t;
	private UtilitiesRestApi utilitiesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);

	}
	
	@Test (description = "Use translate() in processor - not allowed")
	
	public void useTranslateInStream() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		t.addString(seasonID, str, sessionToken);
		
		//add stream with processor using translate function
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);	
		JSONObject json = new JSONObject(stream);
		json.put("processor", "translate(\"app.hello\", \"value1\")");	
		
		streamID = streamApi.createStream(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(streamID.contains("error"), "Used translate function in stream" );
	}
	
	@Test(dependsOnMethods="useTranslateInStream", description = "Add stream utility that uses a string")
	public void addUtility() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function streamUseTranslate(placeholder){translate (\"app.hello\", placeholder);}");
		utilProps.setProperty("stage", "DEVELOPMENT");
		
		String utilityID = utilitiesApi.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Can't add stream utility");
		
		//add stream to trigger validation
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("processor", "streamUseTranslate(\"hello\")");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertTrue(streamID.contains("error"), "Incorrect utility validation in stream ");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
