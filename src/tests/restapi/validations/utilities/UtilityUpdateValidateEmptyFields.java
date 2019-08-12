package tests.restapi.validations.utilities;

import java.io.File;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

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
import tests.restapi.UtilitiesRestApi;

public class UtilityUpdateValidateEmptyFields {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	protected String utility;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);

	}


	@Test 
	public void emptyUtility() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("utility", "");
		updateUtility(json);
	}
	
	@Test 
	public void emptyStage() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("stage", "");
		updateUtility(json);
	}
	
	@Test 
	public void emptyName() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("name", "");
		updateUtility(json);
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("uniqueId", "");
		String response =  u.updateUtility(utilityID, json, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );

	}
	
/*	@Test 
	public void emptyLastModified() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("lastModified", "");
		updateUtility(json);
	}
	*/

	//minAppVersion is removed from utilities
	/*
	@Test 
	public void emptyMinAppVersion() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("minAppVersion", "");
		updateUtility(json);
	}*/
	
	@Test //OK
	public void emptySeasonId() throws Exception{
		utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("seasonId", "");
		String response =  u.updateUtility(utilityID, json, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	}
	
	private void updateUtility(JSONObject json) throws IOException, JSONException{
		String response =  u.updateUtility(utilityID, json, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
