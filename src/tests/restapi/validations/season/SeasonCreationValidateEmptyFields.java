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
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class SeasonCreationValidateEmptyFields {
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
	public void validateMissingMinVersion() throws JSONException{
		JSONObject json = new JSONObject(season);
		json.put("minVersion", "");
		addSeason(json.toString());
	}
	
	@Test 
	public void validateMissingMaxVersion() throws JSONException{
		JSONObject json = new JSONObject(season);
		json.put("maxVersion", "");
		addSeason(json.toString());
	}
	
	@Test
	public void validateMissingUniqueId() throws JSONException{
		JSONObject json = new JSONObject(season);
		json.put("uniqueId", "");
		addSeason(json.toString());
	}
	
	@Test
	public void validateLastModified() throws JSONException{
		JSONObject json = new JSONObject(season);
		json.put("lastModified", "");
		addSeason(json.toString());
	}
	
	@Test
	public void validateProductId() throws JSONException{
		JSONObject json = new JSONObject(season);
		json.put("productId", "");
		addSeason(json.toString());
	}
	

	private void addSeason(String seasonJson){
		try {
			String response = s.addSeason(productID, seasonJson, sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
