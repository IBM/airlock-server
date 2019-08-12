package tests.restapi.scenarios.utilities;

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

import org.apache.wink.json4j.JSONArray;

public class GetUtilitiesByStageAndMinAppVersion {
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
	
	private String UtilIdVer1Prod;
	private String UtilIdVer2Prod;
	private String UtilIdVer3Prod;
	private String UtilIdVer4Prod;
	private String UtilIdVer1Dev;
	private String UtilIdVer2Dev;
	private String UtilIdVer3Dev;
	private String UtilIdVer4Dev;
	
	protected UtilitiesRestApi utilitiesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	//Changes in tests since minAppVersion was removed from utilities
	@Test(description = "Add utilities in various stages")
	public void addAllUtilities() throws IOException, JSONException{		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("stage", "PRODUCTION");
		UtilIdVer1Prod = utilitiesApi.addUtility(seasonID, utilProps, sessionToken);
	
		//utilProps.setProperty("minAppVersion", "1");		
		/*UtilIdVer1Prod = u.addUtility(seasonID, utilProps, sessionToken);		
		
		utilProps.setProperty("minAppVersion", "2");		
		UtilIdVer2Prod = u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "3");		
		UtilIdVer3Prod = u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "4");		
		UtilIdVer4Prod = u.addUtility(seasonID, utilProps, sessionToken);
		*/
		utilProps.setProperty("stage", "DEVELOPMENT");
		//utilProps.setProperty("minAppVersion", "1");		
		UtilIdVer1Dev = utilitiesApi.addUtility(seasonID, utilProps, sessionToken);
		/*
		utilProps.setProperty("minAppVersion", "2");		
		UtilIdVer2Dev = u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "3");		
		UtilIdVer3Dev = u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "4");		
		UtilIdVer4Dev = u.addUtility(seasonID, utilProps, sessionToken);
		*/		
	}
	
	@Test(dependsOnMethods = "addAllUtilities",  description = "Get all utilities")
	public void getAllUtils() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, null);
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 3, "not all utilities returned from get utilities.");				
	}
	
	@Test(dependsOnMethods = "getAllUtils",  description = "Get all PRODUCTION utilities")
	public void getAllProdUtils() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "PRODUCTION");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		/*Assert.assertTrue(allUtilites.length() == 5, "not all utilities returned from get utilities.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer3Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(4).getString("uniqueId").equals(UtilIdVer4Prod), "utility in production wasn't returned");
		*/
		Assert.assertTrue(allUtilites.length() == 2, "not all utilities returned from get utilities.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility in production wasn't returned");
	}			
	
	@Test(dependsOnMethods = "getAllProdUtils",  description = "Get all DEVELOPMENT utilities")
	public void getAllDevUtils() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 3, "not all utilities returned from get utilities.");		
	}	
	
	//minAppVersion is removed from utilities
	/*
	@Test(dependsOnMethods = "getAllDevUtils",  description = "Get all utilities for given minAppVersion")
	public void getAllUtilsForMinAppVer() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, null, "2");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 5, "not all or too much utilities returned for given minAppVersion.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility for minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility for minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer1Dev), "utility for minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(4).getString("uniqueId").equals(UtilIdVer2Dev), "utility for minAppVersion wasn't returned");
	}
	
	@Test(dependsOnMethods = "getAllDevUtils",  description = "Get all utilities for DEVELOPMENT")
	public void getAllUtilsForDevAndMinAppVer() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 7, "not all or too much utilities returned for DEVELOPMENT and given minAppVersion.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility for DEVELOPMENT and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility for DEVELOPMENT and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer3Prod), "utility for DEVELOPMENT and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(4).getString("uniqueId").equals(UtilIdVer1Dev), "utility for DEVELOPMENT and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(5).getString("uniqueId").equals(UtilIdVer2Dev), "utility for DEVELOPMENT and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(6).getString("uniqueId").equals(UtilIdVer3Dev), "utility for DEVELOPMENT and minAppVersion wasn't returned");
	}	
	
	@Test(dependsOnMethods = "getAllUtilsForDevAndMinAppVer",  description = "Get all utilities for PRODUCTION and given minAppVersion")
	public void getAllUtilsForProdAndMinAppVer() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "PRODUCTION");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 4, "not all or too much utilities returned for PRODUCTION and given minAppVersion.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility for PRODUCTION and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility for PRODUCTION and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer3Prod), "utility for PRODUCTION and minAppVersion wasn't returned");		
	}*/		
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
