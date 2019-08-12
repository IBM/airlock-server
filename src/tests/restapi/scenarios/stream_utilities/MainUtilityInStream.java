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

public class MainUtilityInStream {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private StreamsRestApi streamApi;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;

		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);		
		
	}

	//main utility can't be used in stream

	@Test (description="Stream utility")
	public void addMainUtility() throws Exception{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.MAIN_UTILITY, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Stream utility can't be created: " + utilityID);		
	}
	
	@Test (dependsOnMethods="addMainUtility", description="main utility can't be used in stream in create")
	public void createStream() throws JSONException, IOException{
		//add stream with main utility
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("processor", "isFalse()");
		String response = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use main utility in stream in create" );
	}
	
	@Test (dependsOnMethods="createStream", description="main utility can't be used in stream in update")
	public void updateStream() throws Exception{

		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		String streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Can't create stream: " + streamID );
		
		JSONObject streamJson = new JSONObject(streamApi.getStream(streamID, sessionToken));
		streamJson.put("processor", "isFalse()");
		String response = streamApi.updateStream(streamID, streamJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Shouldn't allow to use main utility in stream in update" );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
