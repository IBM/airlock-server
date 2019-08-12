package tests.restapi.scenarios.utilities;

import java.io.File;

import java.io.StringReader;
import java.util.Properties;

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

public class UpdateUtilityAndCompareParameters {
	protected String seasonID;
	protected String utilityID;
	protected String deepFreezeID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		
		utilityID = u.addUtility(seasonID, utilProps, sessionToken);		
	}


	//minAppVersion is removed from utilities
	/*
	@Test 
	public void updateMinAppVersion() throws Exception{
			String utility = u.getUtility(utilityID, sessionToken);
			JSONObject json = new JSONObject(utility);
			json.put("minAppVersion", "1.6");
			u.updateUtility(utilityID, json, sessionToken);
			utility = u.getUtility(utilityID, sessionToken);
			json = new JSONObject(utility);
			Assert.assertTrue(json.getString("minAppVersion").equals("1.6"), "Failed to updated parameter \"minAppVersion\"."); 

	}*/
				

	@Test 
	public void updateUtility() throws Exception{
			String utility = u.getUtility(utilityID, sessionToken);
			JSONObject json = new JSONObject(utility);
			json.put("utility", "function isMetric(){return true;}");
			u.updateUtility(utilityID, json, sessionToken);
			utility = u.getUtility(utilityID, sessionToken);
			json = new JSONObject(utility);
			Assert.assertTrue(json.getString("utility").equals("function isMetric(){return true;}"), "Failed to updated parameter \"utility\"."); 

	}
	
	@Test 
	public void updateStage() throws Exception{
		
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("stage", "PRODUCTION");
		u.updateUtility(utilityID, json, sessionToken);
		utility = u.getUtility(utilityID, sessionToken);
		json = new JSONObject(utility);
		Assert.assertTrue(json.getString("stage").equals("PRODUCTION"), "Failed to updated parameter \"stage\"."); 

		//move back to dev stage to remove product
		json.put("stage", "DEVELOPMENT");
		u.updateUtility(utilityID, json, sessionToken);
	}

	
	@Test 
	public void updateLastModified() throws Exception{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("lastModified", json.getLong("lastModified")-10); //the last modified in update is older than in the server
		String response = u.updateUtility(utilityID, json, sessionToken);	
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
