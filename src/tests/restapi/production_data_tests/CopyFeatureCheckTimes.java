package tests.restapi.production_data_tests;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.SeasonsRestApi;

public class CopyFeatureCheckTimes {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected String sessionToken = "";
	protected String m_url;
	protected String m_fromVersion;
	private JSONObject body = new JSONObject();
	private String featureToCopy;
	private String m_testUrl;
	protected ProductsRestApi p;
	private String productName;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "productId", "seasonId", "productName"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String prId, String seasId, String prName) throws Exception{
		m_url = url;
		m_testUrl = url.replace("/api/admin", "/api/test/import");
		if (sToken != null)
			sessionToken = sToken;
		
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		
		featureToCopy = "f80b281e-c216-4971-9909-a6285059281c"; //HeadsUp With AB
		productID = prId;
		seasonID = seasId;
		productName = prName;
	}
	
	@Test (description = "copy product to server")
	public void copy() throws Exception{
		String response  = p.getProduct(productID, sessionToken);
		if (response.contains("Product not found")){	//product  doesn't exists
		
			JSONObject body = new JSONObject();
			body.put("path", "vicky/PRODUCTION_DATA/" + productID + "/" + seasonID);		
			body.put("productName", productName);
			body.put("productId", productID);
			body.put("seasonId", seasonID);
			body.put("minVersion", "9.0");
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_testUrl, body.toString(), sessionToken);
			Assert.assertTrue(res.code==200, "Product was not copied");
		}
		
	}
	
	@Test (dependsOnMethods="copy", description = "copy feature")
	public void copyFeature() throws IOException, JSONException{

			
		//copy feature
		String rootId = f.getRootId(seasonID, sessionToken);
		
		long startTime = System.currentTimeMillis();
		System.out.println("Copy started at: " + RuntimeDateUtilities.getCurrentTimeStamp());
		
		String response = f.copyFeature(featureToCopy, rootId, "ACT", null, "suffix1", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Copy/paste feature " + featureToCopy + " took " + estimatedTime +  " milliseconds");


	}
}
