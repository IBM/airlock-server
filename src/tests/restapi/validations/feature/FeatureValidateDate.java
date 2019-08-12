package tests.restapi.validations.feature;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureValidateDate {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected JSONObject json;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String feature;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		m_url = url;
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
	}
	
	@Test(description = "lastModified set to a characters string")
	public void ValidateLastModified1() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("lastModified", "test");
		addFeature(json.toString());
	}
	
	@Test(description = "lastModified set to a timestamp")
	public void ValidateLastModified2() throws JSONException, IOException{
		json = new JSONObject(feature);
		long timestamp = System.currentTimeMillis();
		json.put("lastModified", timestamp);
		addFeature(json.toString());
	}
	
	@Test(description = "lastModified set to a negative number")
	public void ValidateLastModified3() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("lastModified", -1);
		addFeature(json.toString());
	}
	
	@Test(description = "creationDate set to a characters string")
	public void ValidateCreationDate1() throws JSONException, IOException{
	
		json = new JSONObject(feature);
		json.put("creationDate", "test");
		addFeature(json.toString());
	}
	
	@Test(description = "creationDate set to a timestamp")
	public void ValidateCreationDate2() throws JSONException, IOException{
		json = new JSONObject(feature);
		long timestamp = System.currentTimeMillis();
		json.put("creationDate", timestamp);
		addFeature(json.toString());
	}
	
	@Test(description = "creationDate set to a negative number")
	public void ValidateCreationDate3() throws JSONException, IOException{
		json = new JSONObject(feature);
		json.put("creationDate", -1);
		addFeature(json.toString());
	}
	
	private void addFeature(String featureJson){

		try {
			String response = f.addFeature(seasonID, featureJson, "ROOT", sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
