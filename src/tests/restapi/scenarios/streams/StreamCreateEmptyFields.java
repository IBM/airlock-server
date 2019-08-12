package tests.restapi.scenarios.streams;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;

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

public class StreamCreateEmptyFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String streamID;
	protected StreamsRestApi streamApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String stream;
	
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
		stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		
		
	}
		
	@Test (description="Filter can't be empty if stream is enabled")
	private void filterEmptyEnabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("filter", "");
		json.put("enabled", true);
		updateStream(json, "filter", true);
	}
	
	@Test (description="Processor can't be empty if stream is enabled")
	private void processorEmptyEnabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("processor", "");
		json.put("enabled", true);
		updateStream(json, "processor", true);
	}
	
	
	@Test (description="Schema can't be empty if stream is enabled")
	private void resultsSchemaEmptyEnabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("resultsSchema", "");
		json.put("enabled", true);
		updateStream(json, "resultsSchema", true);
	}
	
	@Test (description="Filter can be empty if stream is disabled")
	private void filterEmptyDisabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("filter", "");
		json.put("enabled", false);
		updateStream(json, "filter", false);
	}
	
	@Test (description="Processor can be empty if stream is disabled")
	private void processorEmptyDisabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("processor", "");
		json.put("enabled", false);
		updateStream(json, "processor", false);
	}
	
	
	@Test (description="Schema can be empty if stream is disabled")
	private void resultsSchemaEmptyDisabled() throws Exception{
		JSONObject json = new JSONObject(stream);
		json.put("resultsSchema", "");
		json.put("enabled", false);
		updateStream(json, "resultsSchema", false);
	}
	
	private void updateStream(JSONObject streamJson, String field, boolean expectedResult) throws JSONException{
		streamJson.put("name", RandomStringUtils.randomAlphabetic(5));
		try {
			String response = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  field + " : " + response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}



	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
