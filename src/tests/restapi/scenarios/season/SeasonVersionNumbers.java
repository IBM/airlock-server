package tests.restapi.scenarios.season;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class SeasonVersionNumbers {
	protected String seasonID1;
	protected String seasonID2;
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
		seasonID1 = s.addSeason(productID, season, sessionToken);
	}
	

	@Test 
	public void updateMaxVersionEmptyString() throws Exception{
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", " ");
		String response = s.updateSeason(seasonID1, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test
	public void updateMaxVersionLessThanMinVersion() throws Exception{
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "0.5");
		String response = s.updateSeason(seasonID1, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	
	@Test (dependsOnMethods = "updateMaxVersionLessThanMinVersion")
	public void updateMaxVersionWithLiteralLessThanMinVersion() throws Exception{
		season = s.getSeason(productID, seasonID1, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "0.5a");
		String response = s.updateSeason(seasonID1, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	/*@Test (dependsOnMethods = "updateMaxVersionWithLiteralLessThanMinVersion")
	public void updateMaxVersionWithLiteral() throws Exception{
		season = s.getSeason(productID, seasonID1);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "2.0a");
		s.updateSeason(seasonID1, json.toString());
		//String response = s.getSeason(productID, seasonID1);
		//System.out.println(response);
	}*/
	
	@Test (dependsOnMethods = "updateMaxVersionWithLiteralLessThanMinVersion")
	public void createNewSeasonIllegalMinVersion() throws Exception{
		season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "0.5");
		seasonID2 = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertTrue(seasonID2.contains("error"), "Test should fail, but instead passed: " + seasonID2 );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
