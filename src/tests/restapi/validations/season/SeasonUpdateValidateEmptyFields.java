package tests.restapi.validations.season;

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

public class SeasonUpdateValidateEmptyFields {
	protected String filePath;
	protected String m_url;
	protected SeasonsRestApi s;
	protected String season;
	protected String productID;
	protected String seasonID;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_url = url;

		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		
		season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(season);
		json.put("productId", productID);
		season = json.toString();
		s = new SeasonsRestApi();
		s.setURL(m_url);
		//season = JSONUtils.generateUniqueString(season, 8, "name");
		seasonID = s.addSeason(productID, season, sessionToken);
		season = s.getSeason(productID, seasonID, sessionToken);		
	}

	@Test 
	public void validateMissingMinVersion() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "");
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test 
	public void validateMissingMaxVersion() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "");
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	
	@Test 
	public void validateMissingUniqueId() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("uniqueId", "");
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test
	public void validateLastModified() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("lastModified", "");
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test 
	public void validateProductId() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("productId", "");
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
