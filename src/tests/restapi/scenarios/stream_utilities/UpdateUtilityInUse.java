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
import tests.restapi.UtilitiesRestApi;

public class UpdateUtilityInUse {
	protected String seasonID;
	protected String streamID;
	protected String productID;
	protected String utilityID;
	protected String filePath;
	protected StreamsRestApi streamApi;
	protected UtilitiesRestApi u;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;

		u = new UtilitiesRestApi();
		u.setURL(url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test(description = "Add valid utility in development stage")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function A(){return true}; function B(){return true};");
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Utility was not added: " + utilityID );
	}
	
	//can't delete utility in use by processor
	@Test(dependsOnMethods = "addUtility", description = "create stream which uses utility")
	public void addStream() throws IOException, JSONException{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(stream);
		fJson.put("processor", "A()");
		streamID = streamApi.createStream(seasonID, fJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream in prod uses utility in dev" );
	}
	
	
	@Test(dependsOnMethods = "addStream", description = "delete utility in use")
	public void updateUtility() throws JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("utility", "function B(){return true};");
		String response = u.updateUtility(utilityID, json, sessionToken);
		Assert.assertTrue(response.contains("error"), "Utility in use by stream was updated and function used in stream was deleted"); 
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
