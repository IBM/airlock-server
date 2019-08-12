package tests.restapi.scenarios.utilities;

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
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityValidateInDefaultFile {
	protected String seasonID;
	protected String utilityID;
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected UtilitiesRestApi u;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	@Test(description = "Add valid utility")
	public void addUtility() throws IOException, JSONException{
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isTrue(){return true;}");
		
		utilityID= u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Test should pass, but instead failed: " + utilityID );
	}
	
	/* //In V3 the javascript utilities are in separate file and not in defaults file
	@Test(dependsOnMethods = "addUtility", description = "Check utility in defaults file")	
	public void getDefaultFile() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		String javascriptUtilities = json.getString("javascriptUtilities");
		Assert.assertTrue(javascriptUtilities.contains("function isTrue"), "The new utility was not added to defaults file");
	}*/
	
	@Test(dependsOnMethods = "addUtility", description = "Update utility")	
	public void updateUtility() throws JSONException{
		String utility = u.getUtility(utilityID, sessionToken);
		JSONObject json = new JSONObject(utility);
		json.put("utility", "function isTrue(parameter1){return true;}");
		String response = u.updateUtility(utilityID, json, sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	/* //In V3 the javascript utilities are in separate file and not in defaults file
	@Test(dependsOnMethods = "updateUtility", description = "Check updated utility in defaults file")	
	public void getDefaultFileAfterUpdate() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		JSONObject json = new JSONObject(defaults);
		String javascriptUtilities = json.getString("javascriptUtilities");
		Assert.assertTrue(javascriptUtilities.contains("function isTrue(parameter1)"), "The updated utility was not added to defaults file");
	}*/
	
	@Test(dependsOnMethods = "updateUtility", description = "Add invalid utility")	
	public void addInvalidUtility() throws JSONException, IOException{
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.put("utility", "function isFalse(){return true;");
		
		String response = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test(dependsOnMethods = "addInvalidUtility", description = "Check that invalid utility is not in defaults file")	
	public void getDefaultFileAfterInvalidUtility() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		//In V3 the javascript utilities are in separate file and not in defaults file
		/*JSONObject json = new JSONObject(defaults);
		String javascriptUtilities = json.getString("javascriptUtilities");
		Assert.assertTrue(!javascriptUtilities.contains("function isFalse()"), "The invalid utility was added to defaults file");
		*/
	}
	
	@Test(dependsOnMethods = "getDefaultFileAfterInvalidUtility", description = "Delete utility")
	public void deleteUtility(){
		int responseCode = u.deleteUtility(utilityID, sessionToken);
		Assert.assertTrue(responseCode == 200, "Utility was not deleted");
	}
	
	@Test(dependsOnMethods = "deleteUtility", description = "Check that deleted utility is not in defaults file")	
	public void getDefaultFileAfterDeleteUtility() throws JSONException{
		String defaults = s.getDefaults(seasonID, sessionToken);
		//In V3 the javascript utilities are in separate file and not in defaults file
		/*
		JSONObject json = new JSONObject(defaults);
		String javascriptUtilities = json.getString("javascriptUtilities");
		Assert.assertTrue(!javascriptUtilities.contains("function isTrue"), "The deleted utility was not deleted from defaults file");
		*/
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
