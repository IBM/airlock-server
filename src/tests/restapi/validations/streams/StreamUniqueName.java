package tests.restapi.validations.streams;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.StreamsRestApi;

public class StreamUniqueName {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
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
		
	}
	
	@Test (description="Create the same stream name twice")
	private void createStream() throws Exception{
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);		
		streamID = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);

		String response = streamApi.createStream(seasonID, stream, sessionToken);
		Assert.assertTrue(response.contains("already exists"), "Stream was created" );
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
