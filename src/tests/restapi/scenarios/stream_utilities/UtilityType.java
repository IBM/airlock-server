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
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityType {
	protected String seasonID;
	protected String utilityID;
	private String streamUtilityID;
	protected String deepFreezeID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		
	}



	@Test (description="Regular utility")
	public void addRegularUtility() throws Exception{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.MAIN_UTILITY, sessionToken);
			
		utility = u.getUtility(utilityID, sessionToken);
		Assert.assertFalse(utility.contains("error"), "Can't create regular utility: " + utility);
		JSONObject json = new JSONObject(utility);
		Assert.assertTrue(json.getString("type").equals("MAIN_UTILITY"), "Incorrect type of the main utility");

	}
	
	@Test (dependsOnMethods="addRegularUtility", description="Stream utility")
	public void addStreamUtility() throws Exception{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		streamUtilityID = u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
			
		utility = u.getUtility(streamUtilityID, sessionToken);
		Assert.assertFalse(utility.contains("error"), "Can't create stream utility: " + utility);
		JSONObject json = new JSONObject(utility);
		Assert.assertTrue(json.getString("type").equals(UtilitiesRestApi.STREAM_UTILITY), "Incorrect type of the stream utility");
		
	}
	
	@Test (dependsOnMethods="addStreamUtility", description="Get utilities info")
	public void getUtilitiesInfo(){
		String response = u.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		Assert.assertTrue(response.contains("isTrue"), "Regular utility not found");
		Assert.assertFalse(response.contains("isFalse"), "Stream utility found in the list of all utilities");
		
		//get by main_type
		response = u.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT", UtilitiesRestApi.MAIN_UTILITY);
		Assert.assertTrue(response.contains("isTrue"), "Regular utility not found");
		Assert.assertFalse(response.contains("isFalse"), "Stream utility found in the list of all utilities");

		//get by stream_type
		response = u.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT", UtilitiesRestApi.STREAM_UTILITY);
		Assert.assertFalse(response.contains("isTrue") ,"Regular utility found in a list of stream utlities");
		Assert.assertTrue(response.contains("isFalse"), "Stream utility not found in a list of stream utlities");

	}
	
	@Test (dependsOnMethods="getUtilitiesInfo", description="Get runtime utilities info")
	public void getRuntimeUtilities() throws InterruptedException, JSONException, IOException{
		Thread.sleep(2000);
		RuntimeRestApi.DateModificationResults responseStream = RuntimeDateUtilities.getRuntimeFile(m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_STREAM_UTILITY, productID, seasonID, sessionToken);
		Assert.assertTrue(responseStream.message.contains("isFalse"), "Stream utlity not found in stream development runtime");
		Assert.assertFalse(responseStream.message.contains("isTrue"), "Regular utlity found in stream development runtime");
		
		RuntimeRestApi.DateModificationResults responseUtil = RuntimeDateUtilities.getRuntimeFile(m_url, RuntimeDateUtilities.RUNTIME_DEVELOPMENT_UTILITY, productID, seasonID, sessionToken);
		Assert.assertFalse(responseUtil.message.contains("isFalse"), "Stream utlity found in regular development runtime");
		Assert.assertTrue(responseUtil.message.contains("isTrue"), "Regular utlity not found in regular development runtime");

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
