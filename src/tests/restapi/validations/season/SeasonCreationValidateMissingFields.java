package tests.restapi.validations.season;

import java.io.IOException;










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

public class SeasonCreationValidateMissingFields {
	protected String filePath;
	protected String m_url;
	protected SeasonsRestApi s;
	protected String season;
	protected String productID;
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
		
 		
	}
	@Test 
	public void validateMissingMinVersion() throws JSONException, IOException{
		season = JSONUtils.generateUniqueString(season, 8, "name");
		JSONObject json = new JSONObject(season);
		removeKey("minVersion", json);
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test (dependsOnMethods = "validateMissingMinVersion")
	public void validateMissingMaxVersion() throws JSONException, IOException{
		season = JSONUtils.generateUniqueString(season, 8, "name");
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "2.0");
		removeKey("maxVersion", json);
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	
	@Test(dependsOnMethods = "validateMissingMaxVersion")
	public void validateMissingUniqueId() throws JSONException, IOException{
		season = JSONUtils.generateUniqueString(season, 8, "name");
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "3.0");
		removeKey("uniqueId", json);
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	
	@Test (dependsOnMethods = "validateMissingUniqueId")
	public void validateLastModified() throws JSONException, IOException{
		season = JSONUtils.generateUniqueString(season, 8, "name");
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "4.0");
		removeKey("lastModified", json);
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	
	@Test (dependsOnMethods = "validateLastModified")
	public void validateProductId() throws JSONException, IOException{
		season = JSONUtils.generateUniqueString(season, 8, "name");
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "5.0");
		removeKey("productId", json);
		String response = s.addSeason(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	

	private void removeKey(String key, JSONObject json ){
		json.remove(key);
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
