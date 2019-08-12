package tests.restapi.scenarios.stream_utilities;

import java.io.File;



import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.StreamsRestApi;
import tests.restapi.UtilitiesRestApi;

public class MainUtilityInStreamUtility {
	protected String seasonID;
	protected String mainUtilityID;
	protected String streamUtilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private StreamsRestApi streamApi;
	private FeaturesRestApi f;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;

		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		streamApi = new StreamsRestApi();
		streamApi.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);		
		
	}

	//main utility can't be used in stream utility. Validation is done only when a stream uses this utility
	//stream utility can't be used in main utility. Validation is done only when a feature uses this utility

	@Test (description="Use main utility inside stream utility ")
	public void mainUtilityInStreamUtility() throws Exception{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isMainUtil(){return true}");
		mainUtilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.MAIN_UTILITY, sessionToken);
		Assert.assertFalse(mainUtilityID.contains("error"), "Main utility can't be created: " + mainUtilityID);
		
		//add stream utility that uses main utility
		

		Properties utilProps2 = new Properties();
		utilProps2.load(new StringReader(utility));
		utilProps2.setProperty("utility", "function isStreamUtil(){return isMainUtil()}");
		String response = u.addUtility(seasonID, utilProps2, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(response.contains("error"), "Stream utility can't be created: " + response );
		
		//add stream to trigger validation
		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamJson.put("processor", "isStreamUtil()");
		String streamID = streamApi.createStream(seasonID, streamJson.toString(), sessionToken);
		Assert.assertTrue(streamID.contains("error"), "Incorrect utility validation in stream ");

	}
	
	@Test (description="Use stream utility inside main utility")
	public void streamUtilityInMainUtility() throws Exception{
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));	
		utilProps.setProperty("utility", "function isStreamUtil(){return true}");
		streamUtilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		Assert.assertFalse(streamUtilityID.contains("error"), "Stream utility can't be created: " + streamUtilityID);
	
		
		Properties utilProps2 = new Properties();
		utilProps2.load(new StringReader(utility));
		utilProps2.setProperty("utility", "function isMainUtil(){return isStreamUtil()}");
		String response = u.addUtility(seasonID, utilProps2, UtilitiesRestApi.MAIN_UTILITY, sessionToken);
		Assert.assertFalse(response.contains("error"), "Main utility can't be created: " + response);
		
		//add feature to trigger utility validation
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("minAppVersion", "10.5");
		JSONObject jj = new JSONObject();
		jj.put("ruleString", "isMainUtil(); false;");
		json.put("rule", jj);
		String fFesponse =  f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(fFesponse.contains("error"), "Incorrect utility validation in feature" );
		
		//use utility in configuration
		String featureID =  f.addFeature(seasonID, feature, "ROOT", sessionToken);
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configRule);
		jsonCR.put("minAppVersion", "10.5");
		String configuration =  "{ \"text\" :  isMainUtil()	}" ;		
		jsonCR.put("configuration", configuration);
		String crResonse =  f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertTrue(crResonse.contains("error"), "Incorrect utility validation");

		

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
