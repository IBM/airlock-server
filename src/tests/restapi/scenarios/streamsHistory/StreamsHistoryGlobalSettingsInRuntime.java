package tests.restapi.scenarios.streamsHistory;

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.restapi.AirlockUtils;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.StreamsRestApi;

public class StreamsHistoryGlobalSettingsInRuntime {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
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
	
	@Test (description="verify default global streams settings ")
	private void verifyDefaultGlobalStreamsSettings() throws Exception{
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		Assert.assertTrue(seasonStreamsObj.get("keepHistoryOfLastNumberOfDays") == null, "keepHistoryOfLastNumberOfDays default is not null");
		Assert.assertTrue(seasonStreamsObj.get("historyBufferSize") == null, "historyBufferSize default is not null");
		Assert.assertTrue(seasonStreamsObj.getInt("maxHistoryTotalSizeKB") == (15*1024), "maxHistoryTotalSizeKB default is not 15M");
		Assert.assertTrue(seasonStreamsObj.getInt("historyFileMaxSizeKB") == (1*1024), "historyFileMaxSizeKB default is not 1M");
		Assert.assertTrue(!seasonStreamsObj.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents default is not false");	
		Assert.assertTrue(seasonStreamsObj.getString("filter").isEmpty(), "filter default is not empty");	
	}
		
	@Test (dependsOnMethods="verifyDefaultGlobalStreamsSettings", description="Create dev stream ")
	private void updateGlobalStreamsSettings() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		JSONObject seasonStreamsObj = new JSONObject(seasonStreams);
		
		seasonStreamsObj.remove("streams");
		seasonStreamsObj.put("enableHistoricalEvents", true);
		seasonStreamsObj.put("keepHistoryOfLastNumberOfDays", 17);
		seasonStreamsObj.put("historyFileMaxSizeKB", 3*1024);
		seasonStreamsObj.put("maxHistoryTotalSizeKB", 20*1024);
		seasonStreamsObj.put("filter", "true");
		seasonStreamsObj.put("historyBufferSize", 400);
		
		seasonStreams = streamApi.updateGlobalStreamSettings(seasonID, seasonStreamsObj.toString(), seasonStreams);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot update global streams settings: " + seasonStreams);
		
		//validate update results
		seasonStreamsObj = new JSONObject(seasonStreams);
		Assert.assertTrue(seasonStreamsObj.getInt("keepHistoryOfLastNumberOfDays") == 17, "keepHistoryOfLastNumberOfDays is not null");
		Assert.assertTrue(seasonStreamsObj.getInt("maxHistoryTotalSizeKB") == (20*1024), "maxHistoryTotalSizeKB is not 2-M");
		Assert.assertTrue(seasonStreamsObj.getInt("historyFileMaxSizeKB") == (3*1024), "historyFileMaxSizeKB is not 3M");
		Assert.assertTrue(seasonStreamsObj.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents is not true");
		Assert.assertTrue(seasonStreamsObj.getString("filter").equals("true"), "filter is not true"); 
		Assert.assertTrue(seasonStreamsObj.getInt("historyBufferSize") == 400, "historyBufferSize is not null");
		
		//get the global streams settings and validate new values
		seasonStreams = streamApi.getAllStreams(seasonID, sessionToken);
		Assert.assertFalse(seasonStreams.contains("error"), "cannot get season's streams: " + seasonStreams);
		seasonStreamsObj = new JSONObject(seasonStreams);
		Assert.assertTrue(seasonStreamsObj.getInt("keepHistoryOfLastNumberOfDays") == 17, "keepHistoryOfLastNumberOfDays is not null");
		Assert.assertTrue(seasonStreamsObj.getInt("maxHistoryTotalSizeKB") == (20*1024), "maxHistoryTotalSizeKB is not 2-M");
		Assert.assertTrue(seasonStreamsObj.getInt("historyFileMaxSizeKB") == (3*1024), "historyFileMaxSizeKB is not 3M");
		Assert.assertTrue(seasonStreamsObj.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents is not true");
		Assert.assertTrue(seasonStreamsObj.getString("filter").equals("true"), "filter is not true"); 
		Assert.assertTrue(seasonStreamsObj.getInt("historyBufferSize") == 400, "historyBufferSize is not null");
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject json = new JSONObject(responseDev.message);
		Assert.assertTrue(json.getInt("keepHistoryOfLastNumberOfDays") == 17, "keepHistoryOfLastNumberOfDays is not null");
		Assert.assertTrue(json.getInt("maxHistoryTotalSizeKB") == (20*1024), "maxHistoryTotalSizeKB is not 2-M");
		Assert.assertTrue(json.getInt("historyFileMaxSizeKB") == (3*1024), "historyFileMaxSizeKB is not 3M");
		Assert.assertTrue(json.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents is not true");
		Assert.assertTrue(json.getString("filter").equals("true"), "filter is not true"); 
		Assert.assertTrue(json.getInt("historyBufferSize") == 400, "historyBufferSize is not 400");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionStreamsDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		json = new JSONObject(responseProd.message);
		Assert.assertTrue(json.getInt("keepHistoryOfLastNumberOfDays") == 17, "keepHistoryOfLastNumberOfDays is not null");
		Assert.assertTrue(json.getInt("maxHistoryTotalSizeKB") == (20*1024), "maxHistoryTotalSizeKB is not 2-M");
		Assert.assertTrue(json.getInt("historyFileMaxSizeKB") == (3*1024), "historyFileMaxSizeKB is not 3M");
		Assert.assertTrue(json.getBoolean("enableHistoricalEvents"), "enableHistoricalEvents is not true");
		Assert.assertTrue(json.getString("filter").equals("true"), "filter is not true");
		Assert.assertTrue(json.getInt("historyBufferSize") == 400, "historyBufferSize is not null");
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
