package tests.restapi.scenarios.season;


import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class SeasonPlatforms {
	protected String seasonID;
	protected String season;
	protected String productID;
	protected String config;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;
		p = new ProductsRestApi();
		s = new SeasonsRestApi();
		p.setURL(url);
		s.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		
		baseUtils.printProductToFile(productID);
		season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
		seasonID = s.addSeason(productID, season, sessionToken);
	}
	

	@Test 
	public void getSupportedPaltforms() throws Exception{
		String productStr = p.getProduct(productID, sessionToken);
		JSONObject prodObj = new JSONObject(productStr);
		
		JSONArray seasons = prodObj.getJSONArray("seasons");
		Assert.assertTrue(seasons.size() == 1, "wrong number of seasons");
		JSONObject seasonObj = seasons.getJSONObject(0);
		
		JSONArray platforms = seasonObj.getJSONArray("platforms");
		Assert.assertTrue(platforms.size() == 3, "wrong number of platforms");
		
		Assert.assertTrue(platforms.get(0).equals("Android"), "wrong platform");
		Assert.assertTrue(platforms.get(1).equals("iOS"), "wrong platform");
		Assert.assertTrue(platforms.get(2).equals("c_sharp"), "wrong platform");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
