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

public class GetUtilitiesInfo {
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
		
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
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
		//utilProps.setProperty("minAppVersion", "1");	
		utilProps.setProperty("utility", "function A(paramA1, paramA2){return true;}");

		u.addUtility(seasonID, utilProps, sessionToken);
		numOfOriginalFuntions++;
		/*
		utilProps.setProperty("minAppVersion", "2");	
		utilProps.setProperty("utility", "function B(paramB1, paramB2, paramB3){return true;}");
		
		u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "3");	
		utilProps.setProperty("utility", "function C(){return true;}");
		
		u.addUtility(seasonID, utilProps, sessionToken);
		*/
		utilProps.setProperty("stage", "DEVELOPMENT");
		//utilProps.setProperty("minAppVersion", "1");
		utilProps.setProperty("utility", "function D(paramD1){return true;}");
		/*
		utilityID1 = u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "2");	
		utilProps.setProperty("utility", "function E(){return true;}");
		
		u.addUtility(seasonID, utilProps, sessionToken);
		
		utilProps.setProperty("minAppVersion", "3");	
		utilProps.setProperty("utility", "function F(paramF1, paramF2, paramF3){return true;}");
		*/
		u.addUtility(seasonID, utilProps, sessionToken);
		numOfOriginalFuntions++;
	}
	
	@Test(dependsOnMethods = "addAllUtilities",  description = "Get all utilities info")
	public void getAllUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject utilitiesInfoJson = new JSONObject(response);
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions, "not all or too much utilities info returned for DEVELOPMENT");
		/*Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+6, "not all or too much utilities info returned for DEVELOPMENT and given minAppVersion.");
		
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A", new String[]{"paramA1", "paramA2"}), "function A does not exists or contain wrong parameters in utilitiesInfo");				
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B", new String[]{"paramB1", "paramB2", "paramB3"}), "function B does not exists or contain wrong parameters in utilitiesInfo");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "C", new String[]{}), "function C does not exists or contain wrong parameters in utilitiesInfo");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "D", new String[]{"paramD1"}), "function D does not exists or contain wrong parameters in utilitiesInfo");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "E", new String[]{}), "function E does not exists or contain wrong parameters in utilitiesInfo");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "F", new String[]{"paramF1", "paramF2", "paramF3"}), "function F does not exists or contain wrong parameters in utilitiesInfo");
		*/
	}
	
	@Test(dependsOnMethods = "getAllUtils",  description = "Get PRODUCTION utilities info")
	public void getProdUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "PRODUCTION");
		JSONObject utilitiesInfoJson = new JSONObject(response);
		
		/*Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+2, "not all or too much utilities info returned for PRODUCTION and given minAppVersion.");
				
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A", new String[]{"paramA1", "paramA2"}), "function A does not exists or contain wrong parameters in utilitiesInfo");				
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B", new String[]{"paramB1", "paramB2", "paramB3"}), "function B does not exists or contain wrong parameters in utilitiesInfo");
		*/
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions-1, "not all or too much utilities info returned for PRODUCTION");
	}
	/*
	
	@Test(dependsOnMethods = "getProdUtils",  description = "Get PRODUCTION, minAppVersion utilities info")
	public void getForProdMinAppVerUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "PRODUCTION");
		JSONObject utilitiesInfoJson = new JSONObject(response);
		
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+3, "not all or too much utilities info returned for PRODUCTION and given minAppVersion.");		
		
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A", new String[]{"paramA1", "paramA2"}), "function A does not exists or contain wrong parameters in utilitiesInfo");				
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "B", new String[]{"paramB1", "paramB2", "paramB3"}), "function B does not exists or contain wrong parameters in utilitiesInfo");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "C", new String[]{}), "function C does not exists or contain wrong parameters in utilitiesInfo");					
	}
	
	@Test(dependsOnMethods = "getForProdMinAppVerUtils",  description = "Get DEVELOPMENT, minAppVersion utilities info")
	public void getForDevMinAppVerUtils() throws IOException, JSONException{
		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject utilitiesInfoJson = new JSONObject(response);
		
		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+2, "not all or too much utilities info returned for DEVELOPMENT and given minAppVersion.");		
		
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "A", new String[]{"paramA1", "paramA2"}), "function A does not exists or contain wrong parameters in utilitiesInfo");				
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "D", new String[]{"paramD1"}), "function D does not exists or contain wrong parameters in utilitiesInfo");						
	}
	@Test(dependsOnMethods = "getForDevMinAppVerUtils",  description = "Get DEVELOPMENT, minAppVersion utilities info")
	public void updateUtility() throws IOException, JSONException{
		String util = utilitiesApi.getUtility(utilityID1,sessionToken);
		util = util.replace("paramD1","paramD1New");
		utilitiesApi.updateUtility(utilityID1,new JSONObject(util),sessionToken);

		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject utilitiesInfoJson = new JSONObject(response);

		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+2, "not all or too much utilities info returned for DEVELOPMENT and given minAppVersion.");
		Assert.assertTrue(containsUtilityInfo(utilitiesInfoJson, "D", new String[]{"paramD1New"}), "function D does not exists or contain wrong parameters in utilitiesInfo");
	}
	@Test(dependsOnMethods = "updateUtility",  description = "Get DEVELOPMENT, minAppVersion utilities info")
	public void deleteUtility() throws IOException, JSONException{
		utilitiesApi.deleteUtility(utilityID1,sessionToken);

		String response = utilitiesApi.getUtilitiesInfo(seasonID, sessionToken, "DEVELOPMENT");
		JSONObject utilitiesInfoJson = new JSONObject(response);

		Assert.assertTrue(utilitiesInfoJson.keySet().size() == numOfOriginalFuntions+1, "not all or too much utilities info returned for DEVELOPMENT and given minAppVersion.");
		Assert.assertFalse(containsUtilityInfo(utilitiesInfoJson, "D", new String[]{"paramD1New"}), "function D should be deleted");
	}
*/

	private boolean containsUtilityInfo(JSONObject utilitiesInfoJson, String functionName, String[] functionParameters) throws JSONException {
		if (!utilitiesInfoJson.containsKey(functionName))
			return false;
		
		JSONArray params = utilitiesInfoJson.getJSONArray(functionName);
		if (params.length() != functionParameters.length)
			return false;
		
		for (int i=0; i<params.length(); i++) {
			if (!params.getString(i).equals(functionParameters[i]))
				return false;
		}
		return true;
	}
/*
	@Test(dependsOnMethods = "getAllUtils",  description = "Get all PRODUCTION utilities")
	public void getAllProdUtils() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "PRODUCTION", null);
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 5, "not all utilities returned from get utilities.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer3Prod), "utility in production wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(4).getString("uniqueId").equals(UtilIdVer4Prod), "utility in production wasn't returned");
	}			
	
	@Test(dependsOnMethods = "getAllProdUtils",  description = "Get all DEVELOPMENT utilities")
	public void getAllDevUtils() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT", null);
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 9, "not all utilities returned from get utilities.");		
	}	
	
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
	
	@Test(dependsOnMethods = "getAllUtilsForMinAppVer",  description = "Get all utilities for DEVELOPMENT and given minAppVersion")
	public void getAllUtilsForDevAndMinAppVer() throws IOException, JSONException{
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "DEVELOPMENT", "3");
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
		String response = utilitiesApi.getAllUtilites(seasonID, sessionToken, "PRODUCTION", "3");
		JSONObject json = new JSONObject(response);
		JSONArray allUtilites = json.getJSONArray("utilities");
		Assert.assertTrue(allUtilites.length() == 4, "not all or too much utilities returned for PRODUCTION and given minAppVersion.");
		Assert.assertTrue (allUtilites.getJSONObject(1).getString("uniqueId").equals(UtilIdVer1Prod), "utility for PRODUCTION and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(2).getString("uniqueId").equals(UtilIdVer2Prod), "utility for PRODUCTION and minAppVersion wasn't returned");
		Assert.assertTrue (allUtilites.getJSONObject(3).getString("uniqueId").equals(UtilIdVer3Prod), "utility for PRODUCTION and minAppVersion wasn't returned");		
	}	*/
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
