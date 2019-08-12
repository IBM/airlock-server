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

public class SeasonServerVersion {
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
	public void getServerVersion() throws Exception{
		String response = s.getServerVersion(seasonID, sessionToken);
		JSONObject json = new JSONObject(response);

		Assert.assertTrue(json.getString("serverVersion").equals("5.5"), "Incorrect server version: " + json.getString("serverVersion") );

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
