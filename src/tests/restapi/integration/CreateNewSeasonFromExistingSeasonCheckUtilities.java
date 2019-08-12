package tests.restapi.integration;

import java.io.File;

import java.io.StringReader;
import java.util.Properties;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CreateNewSeasonFromExistingSeasonCheckUtilities {
	protected String seasonID1;
	protected String seasonID2;
	protected String productID;
	protected String config;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected UtilitiesRestApi u;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";


	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		p.setURL(url);
		s.setURL(url);
		
		u = new UtilitiesRestApi();
		u.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	
	}
	
	//
	@Test (description = "Add product, season and 3 utilities")
	public void addComponents() throws Exception{
		String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "name");
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID1 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID1.contains("error"), "Test should pass, but instead failed: " + seasonID1 );
		
		String utility1 = FileUtils.fileToString(config + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps1 = new Properties();
		utilProps1.load(new StringReader(utility1));		
		String response = u.addUtility(seasonID1, utilProps1, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		
		
		String utility2 = FileUtils.fileToString(config +"utilities" + File.separator +  "utility2.txt", "UTF-8", false);
		Properties utilProps2 = new Properties();
		utilProps2.load(new StringReader(utility2));	
		response = u.addUtility(seasonID1, utilProps2, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	

		
	/* create a new season - it should copy the previous season
	 * validate that 3 utilities exist in the new season
	 * 
	 */
	

	@Test (dependsOnMethods="addComponents", description = "Create new season and check its utilities")
	public void createNewSeason() throws Exception{
		String season2 = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season2, sessionToken);
		
		String oldUtilities = u.getAllUtilites(seasonID1, sessionToken, null);
		String newUtilities = u.getAllUtilites(seasonID2, sessionToken, null);
		
		JSONObject oldJson = new JSONObject(oldUtilities);
		JSONObject newJson = new JSONObject(newUtilities);
		
		JSONArray oldUtilitiesArr = oldJson.getJSONArray("utilities");
		JSONArray newUtilitiesArr = newJson.getJSONArray("utilities");
		
		Assert.assertTrue(oldUtilitiesArr.size() == newUtilitiesArr.size(), "The number of utilities in seasons is different.");
		boolean compare = compareUtilities(oldUtilitiesArr, newUtilitiesArr);
		Assert.assertTrue(compare, "The utilities are different in 2 seasons");
		
		season2 = s.getSeason(productID, seasonID2, sessionToken);
//		JSONObject season2Json = new JSONObject(season2);
		
		//minAppVersion is removed from strings
		//Assert.assertTrue(newUtilitiesArr.getJSONObject(0).getString("minAppVersion").equals(oldUtilitiesArr.getJSONObject(0).getString("minAppVersion")), "The new season utility's minAppVersion is different from the old season utility's minAppVersion");
		
	}
	
	private boolean compareUtilities(JSONArray oldSchemasArr, JSONArray newSchemasArr) throws JSONException{
		boolean compare = true;
		for (int i= 0; i < oldSchemasArr.size(); i++){
			if (!oldSchemasArr.getJSONObject(i).getString("utility").equals(newSchemasArr.getJSONObject(i).getString("utility")))
				compare = false;
			
			if (!oldSchemasArr.getJSONObject(i).getString("stage").equals(newSchemasArr.getJSONObject(i).getString("stage")))
				compare = false;
			
		}
		
		return compare;
	}
	
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
