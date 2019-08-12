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

public class UtilityVsStreamStage {
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
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	//utility in dev, create stream  in prod
	@Test(dependsOnMethods = "addUtility", description = "utility in dev, create stream  in prod")
	public void addStreamInProdUtilityInDev() throws IOException, JSONException{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(stream);
		fJson.put("processor", "isTrue()");
		fJson.put("stage", "PRODUCTION");
		streamID = streamApi.createStream(seasonID, fJson.toString(), sessionToken);
		Assert.assertTrue(streamID.contains("error"), "Stream in prod uses utility in dev" );
	}
	
	//utility in dev, create stream in dev
	@Test(dependsOnMethods = "addStreamInProdUtilityInDev", description = "utility in dev, create stream in dev")
	public void addStreamInDevUtilityInDev() throws IOException, JSONException{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(stream);
		fJson.put("processor", "isTrue()");
		streamID = streamApi.createStream(seasonID, fJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Can't add stream: " + streamID );
	}
	
	//move utility to prod
	@Test(dependsOnMethods = "addStreamInDevUtilityInDev", description = "utility to prod, stream in dev")
	public void updateUtilityToProd() throws IOException, JSONException{

		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "PRODUCTION");
		String response  =u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update utility: " + response );
		
	}

	//utility in prod, stream to prod
	@Test(dependsOnMethods = "updateUtilityToProd", description = "utility in prod, stream to prod")
	public void updateStreamToProdUtilityInProd() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject fJson = new JSONObject(stream);
		fJson.put("stage", "PRODUCTION");
		streamID = streamApi.updateStream(streamID, fJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Can't update stream: " + streamID );
	}
	
	//utility in prod, stream to dev
	@Test(dependsOnMethods = "updateStreamToProdUtilityInProd", description = "utility in prod, stream to dev")
	public void updateStreamToDevUtilityInProd() throws Exception{
		String stream = streamApi.getStream(streamID, sessionToken);
		JSONObject fJson = new JSONObject(stream);
		fJson.put("stage", "DEVELOPMENT");
		streamID = streamApi.updateStream(streamID, fJson.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Can't update stream: " + streamID );
	}
	
	//utility to dev, stream in dev
	@Test(dependsOnMethods = "updateStreamToDevUtilityInProd", description = "utility to dev, stream in dev")
	public void updateStreamInDevUtilityToDev() throws IOException, JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject uJson = new JSONObject(utility);
		uJson.put("stage", "DEVELOPMENT");
		String response = u.updateUtility(utilityID, uJson, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
