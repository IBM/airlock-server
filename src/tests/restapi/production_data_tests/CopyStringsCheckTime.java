package tests.restapi.production_data_tests;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;

public class CopyStringsCheckTime {
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
	private StringsRestApi stringsApi;
	int stringsToCopy = 30;
	private String productName = "AndroidProduct";
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "productId", "seasonId", "productName"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String prId, String seasId, String prName) throws Exception{
		m_url = url;
		m_testUrl = url.replace("/api/admin", "/api/test/import");
		filePath = configPath;
		productID = prId;
		sourceSeasonID = seasId;
		productName = prName;
		
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		//sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);
		sessionToken = sToken;
		//targetSeasonID= "acf85dd3-8cef-4ec4-b2dc-5cc427a3c9cb";
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
	public void updateStage() throws Exception{
		//add season
		String season = "{\"minVersion\":\"9.5\"}";
		targetSeasonID = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(targetSeasonID.contains("error"), "Can't create second season");
		
		System.out.println("Starting season1");
		String str = stringsApi.getAllStrings(sourceSeasonID, "BASIC", sessionToken);
		JSONObject temp = new JSONObject(str);
		JSONArray allstr = temp.getJSONArray("strings");
		System.out.println("There are " + allstr.size() + " strings in Android product");
		//for (int i=0; i<allstr.size(); i++){
		for (int i=0; i<stringsToCopy+10; i++){
			System.out.println("In season1. Working on string #" + i);
			JSONObject single = allstr.getJSONObject(i);
			if (single.getString("stage").equals("DEVELOPMENT")){
				single.put("stage", "PRODUCTION");
				stringsApi.updateString(single.getString("uniqueId"), single.toString(), "ACT", sessionToken);
			}	
		}

		System.out.println("Starting season2 ");
		str = stringsApi.getAllStrings(targetSeasonID, "BASIC", sessionToken);
		temp = new JSONObject(str);
		allstr = temp.getJSONArray("strings");
		for (int i=0; i<stringsToCopy+10; i++){
			System.out.println("In season2. Working on string #" + i);
			JSONObject single = allstr.getJSONObject(i);
			if (single.getString("stage").equals("PRODUCTION")){
				single.put("stage", "DEVELOPMENT");
				stringsApi.updateString(single.getString("uniqueId"), single.toString(), "ACT", sessionToken);
			}	
		}
		
	}
	

	
	@Test (dependsOnMethods="updateStage")
	public void copyStrings() throws Exception{
		
		
		
		String str = stringsApi.getAllStrings(sourceSeasonID, "BASIC", sessionToken);
		JSONObject temp = new JSONObject(str);
		JSONArray allstr = temp.getJSONArray("strings");
		
		String[] arr=new String[stringsToCopy];
		//select 30 strings to copy
		for (int i=0; i< stringsToCopy; i++){
			arr[i] = allstr.getJSONObject(i).getString("uniqueId");
		}

		long startTime = System.currentTimeMillis();
		System.out.println("Copy started at: " + RuntimeDateUtilities.getCurrentTimeStamp());
		
		String response = stringsApi.copyStrings(arr, targetSeasonID, "ACT", true, sessionToken);
		Assert.assertFalse(response.contains("error"), "Strings were not copied");

		long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Copy/paste of " + stringsToCopy + " strings took " + estimatedTime +  " milliseconds");


	}
	
	@Test (dependsOnMethods="copyStrings")
	public void deleteString() throws Exception{
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(str);
		//json.put("stage", "PRODUCTION");
		json.put("key", "test.checkTime");
		stringID = stringsApi.addString(sourceSeasonID, json.toString(), sessionToken);
		long startTime = System.currentTimeMillis();
		System.out.println("Delete started at: " + RuntimeDateUtilities.getCurrentTimeStamp());
		
		int response = stringsApi.deleteString(stringID, sessionToken);
		Assert.assertTrue(response==200, "Can't delete string " + response);
		long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Delete string took " + estimatedTime +  " milliseconds");


	}
	
	@AfterTest
	private void reset(){
		//baseUtils.reset(productID, sessionToken);
	}

}
