package tests.restapi.production_data_tests;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class FeatureStageValidation {
	protected String sourceSeasonID;
	protected String targetSeasonID;
	protected String stringID;
	protected String filePath;
	protected ProductsRestApi p;
	private SeasonsRestApi s;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private String m_testUrl;
	private FeaturesRestApi f;
	private String productName;
	
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "productId", "seasonId", "productName"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String prId, String seasId, String prName) throws Exception{
		m_url = url;
		m_testUrl = url.replace("/api/admin", "/api/test/import");
		filePath = configPath;
		productID = prId;
		sourceSeasonID = seasId;
		productName = prName;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = sToken;
	}
	

	@Test (description = "copy product to server")
	public void copy() throws Exception{
		String response  = p.getProduct(productID, sessionToken);
		if (response.contains("Product not found")){	//product  doesn't exists
		
			JSONObject body = new JSONObject();
			body.put("path", "vicky/PRODUCTION_DATA/"+ productID + "/"  + sourceSeasonID);			
			body.put("productName", productName);
			body.put("productId", productID);
			body.put("seasonId", sourceSeasonID);
			body.put("minVersion", "9.0");
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_testUrl, body.toString(), sessionToken);
			Assert.assertTrue(res.code==200, "Product was not copied");
		}
		
	}
	


	@Test (dependsOnMethods="copy")
	public void createFeature() throws Exception{
		String videoCardId = "e48ac12b-c356-4779-8460-6c875f4ee537";
		String str = f.getFeature(videoCardId, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("stage", "DEVELOPMENT");
		
		String  response = f.updateFeature(sourceSeasonID, videoCardId, json.toString(), sessionToken);
		
		SoftAssert sa = new SoftAssert();
		sa.assertFalse(response.contains("error"), "Can't add feature: " + response);


	}

	@Test (dependsOnMethods="copy")
	public void updateMTX() throws Exception{
		String mtx = "99362bad-3c6e-4e87-9f63-41dcfba4e632";
		String str = f.getFeature(mtx, sessionToken);
		
		String  response = f.updateFeature(sourceSeasonID, mtx, str, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add feature: " + response);


	}
	
	@AfterTest
	private void reset(){
		//baseUtils.reset(productID, sessionToken);
	}

}
