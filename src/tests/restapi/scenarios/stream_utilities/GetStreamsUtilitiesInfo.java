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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;


public class GetStreamsUtilitiesInfo {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String utilityID1;
	protected String deepFreezeID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected UtilitiesRestApi u;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	
	private int numOfOriginalFuntions = 0; 
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT", UtilitiesRestApi.STREAM_UTILITY);
		JSONObject utilitiesInfoJson = new JSONObject(response);
		
		numOfOriginalFuntions = utilitiesInfoJson.keySet().size();
	}
	
	//minAppVersion was removed from utility
	@Test(description = "Add utilities in various minAppVersions and stages")
	public void addAllUtilities() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("stage", "PRODUCTION");

		utilProps.setProperty("utility", "function A(paramA1, paramA2){return true;}");

		u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		numOfOriginalFuntions++;

		utilProps.setProperty("stage", "DEVELOPMENT");
		
		utilProps.setProperty("utility", "function D(paramD1){return true;}");

		u.addUtility(seasonID, utilProps, UtilitiesRestApi.STREAM_UTILITY, sessionToken);
		numOfOriginalFuntions++;
	}
	
	@Test(dependsOnMethods = "addAllUtilities",  description = "Get all utilities info")
	public void getAllUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT", UtilitiesRestApi.STREAM_UTILITY);
		JSONObject utilitiesInfoJson = new JSONObject(response);
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions, "not all or too much utilities info returned for DEVELOPMENT");
	}
	
	@Test(dependsOnMethods = "getAllUtils",  description = "Get PRODUCTION utilities info")
	public void getProdUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "PRODUCTION", UtilitiesRestApi.STREAM_UTILITY);
		JSONObject utilitiesInfoJson = new JSONObject(response);
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions-1, "not all or too much utilities info returned for PRODUCTION");
	}


	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
