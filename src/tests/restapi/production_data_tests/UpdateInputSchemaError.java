package tests.restapi.production_data_tests;

import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class UpdateInputSchemaError {
	   private String sessionToken = "";
	    private String productID;
	    private String sourceSeasonID;
	    private ProductsRestApi p;
	    private FeaturesRestApi f;
	    private AirlockUtils baseUtils;
	    protected String m_url;
	    private String filePath ;
	    protected String m_testUrl;

	    @BeforeClass
		@Parameters({"url", "testUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
		public void init(String url, String testUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
	        m_url = url;
	        m_testUrl = testUrl;
	        filePath = configPath;
	        p = new ProductsRestApi();
	        p.setURL(m_url);
	        
	        f = new FeaturesRestApi();
	        f.setURL(m_url);

			baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
			sessionToken = baseUtils.sessionToken;


	        productID = "66d22040-972a-4cb3-85a4-32a6a9c54156";
	        sourceSeasonID = "3d206742-930a-4eeb-b442-3a3f5c71a82b";

	    }

		@Test (description = "copy product to server")
		public void copy() throws Exception{
			String response  = p.getProduct(productID, sessionToken);
			if (response.contains("Product not found")){	//product  doesn't exists
			
				JSONObject body = new JSONObject();
				body.put("path", "vicky/PRODUCTION_DATA/" + productID + "/" + sourceSeasonID);		
				body.put("productName", "Android4Test");
				body.put("productId", productID);
				body.put("seasonId", sourceSeasonID);
				body.put("minVersion", "8.0");
				RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_testUrl, body.toString(), sessionToken);
				Assert.assertTrue(res.code==200, "Product was not copied");
			}
			
		}
		
		
	    @Test
	    public void testRules() throws IOException, JSONException{
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json1 = new JSONObject(feature);
			String badRule = FileUtils.fileToString(filePath + "production_data/UpdateInputSchemaError_incorrectRule.txt", "UTF-8", false);
			json1.put("minAppVersion", "9.0");
			json1.put("name", RandomStringUtils.randomAlphabetic(5));
			String featureID1 = f.addFeature(sourceSeasonID, json1.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureID1.contains("error"), "Can't add feature with incorrect rule: " + featureID1);
			
			String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configRule);
			jsonCR.put("minAppVersion", "9.0");
			String configuration = "{\"value\":eventCount()}";
			jsonCR.put("configuration", configuration);
			JSONObject rule = new JSONObject();
			rule.put("ruleString", badRule);
			jsonCR.put("rule", rule);
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String response = f.addFeature(sourceSeasonID, jsonCR.toString(), featureID1, sessionToken);
			Assert.assertTrue(response.contains("error"), "Added configuration with incorrect rule");
						
			
						
			String goodRule = FileUtils.fileToString(filePath + "production_data/UpdateInputSchemaError_correctRule.txt", "UTF-8", false);
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			rule.put("ruleString", goodRule);
			jsonCR.put("rule", rule);
			response = f.addFeature(sourceSeasonID, jsonCR.toString(), featureID1, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't add configuration with correct rule " + response);

	    }

	    
	    @AfterTest
	    private void reset(){

	    	//baseUtils.reset(productID, sessionToken);
	    }
}
