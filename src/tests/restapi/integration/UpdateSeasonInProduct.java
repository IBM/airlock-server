package tests.restapi.integration;

import java.util.UUID;


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

public class UpdateSeasonInProduct {
	protected String filePath;
	protected String m_url;
	protected SeasonsRestApi s;
	protected String season;
	protected String productID;
	protected String seasonID;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_url = url;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		p = new ProductsRestApi();
		p.setURL(m_url);
		
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		
		season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(season);
		json.put("productId", productID);
		season = json.toString();
		s = new SeasonsRestApi();
		s.setURL(m_url);
		season = JSONUtils.generateUniqueString(season, 8, "name");
		seasonID = s.addSeason(productID, season, sessionToken);
		season = s.getSeason(productID, seasonID, sessionToken);		
	}

	@Test (description = "Update seasonId")
	public void updateSeasonId() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		UUID id = UUID.randomUUID();
		json.put("uniqueId", id);
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Update minVersion")
	public void updateMinVersion() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "10.01");
		s.updateSeason(seasonID, json.toString(), sessionToken);
		season = s.getSeason(productID, seasonID, sessionToken);
		json = new JSONObject(season);
		Assert.assertTrue(json.getString("minVersion").equals("10.01"), "Season  minVersion was not updated");
	}
	
	@Test (description = "Update maxVersion")
	public void updateMaxVersion() throws Exception{
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "100.001");
		String response = s.updateSeason(seasonID, json.toString());
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );


	}
	
	
	@Test (description = "Change productId in season")
	public void updateProductId() throws Exception{
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		String productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		season = s.getSeason(productID, seasonID, sessionToken);
		JSONObject json = new JSONObject(season);
		json.put("productId", productID2);
		String response = s.updateSeason(seasonID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@AfterTest
	private void reset(){		
		baseUtils.reset(productID, sessionToken);
	}
}
