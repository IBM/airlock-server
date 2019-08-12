package tests.restapi.scenarios.utilities;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityName {
	protected String seasonID;
	protected String utilityID;
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
	}


	@Test (description="Utility without name parameter")
	public void addUtilityWithoutName() throws Exception{
		String utility = "function isTrue(){true;}";
		try {
			
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_url+"/products/seasons/" + seasonID + "/utilities?stage="+"DEVELOPMENT", utility, sessionToken);
			Assert.assertTrue(res.message.contains("error") && res.message.contains("is missing"), "Added utility without name parameters");
			
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
	}


	@Test (description="Non unique name")
	public void addNonUniqueName() throws Exception{
		
		try {
			String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
			Properties utilProps = new Properties();
			utilProps.load(new StringReader(utility));
			utilProps.setProperty("utility", "function isTrue(){return true;}");
			utilProps.setProperty("name", "util1");
			String response = u.addUtilityNoNameChange(seasonID, utilProps, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't add utility");
			
			utilProps.setProperty("utility", "function isFalse(){return true;}");
			utilProps.setProperty("name", "util1");
			response = u.addUtilityNoNameChange(seasonID, utilProps, sessionToken);
			Assert.assertTrue(response.contains("error"), "Added utility with existing name");
			
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
	}
	
	@Test (description="Non unique name in update")
	public void updateNonUniqueName() throws Exception{
		
		try {
			String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
			Properties utilProps = new Properties();
			utilProps.load(new StringReader(utility));
			utilProps.setProperty("utility", "function isTrue(){return true;}");
			String name = RandomStringUtils.randomAlphabetic(5);
			utilProps.setProperty("name", name);
			String response = u.addUtilityNoNameChange(seasonID, utilProps, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't add utility1");
			
			utilProps.setProperty("utility", "function isFalse(){return true;}");
			utilProps.setProperty("name", "util123");
			String utilityId = u.addUtilityNoNameChange(seasonID, utilProps, sessionToken);
			Assert.assertFalse(utilityId.contains("error"), "Can't add utility2 " + utilityId);
			
			utility = u.getUtility(utilityId, sessionToken);
			JSONObject utilityObj = new JSONObject(utility);
			utilityObj.put("name", name);
			response = u.updateUtility(utilityId, utilityObj, sessionToken);
			Assert.assertTrue(response.contains("error"), "Updated utility non unique utility name " + response);
			
			
		} catch (IOException e) {
			Assert.fail("An exception was thrown when trying  to add a utility. Message: "+e.getLocalizedMessage()) ;
		}
	}
	
	
	@Test (description = "Validate illegal special characters in name")
	public void illegalCharactersInName() throws JSONException, IOException{
		List<String> illegalCharacters= new ArrayList<String>(Arrays.asList("(", ")", "[", "]", "{", "}", "|", "/", "\\", "\"", ">", "<", 
				",", "!", "?", "@", "#", "$", "%", "^", "&", "*", "~", ";", "'", "-","_"));
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));	
		
		for (String character : illegalCharacters) {
			utilProps.setProperty("name", "name" + character + "123");
			String response = u.addUtility(seasonID, utilProps, sessionToken);
			Assert.assertTrue(response.contains("error"), "Added utility with illegal character in name: " + utilProps.getProperty("name") );
		}	

	}

	@Test (description = "Validate legal special characters in name")
	public void legalCharactersInName() throws JSONException, IOException{
		List<String> legalCharacters= new ArrayList<String>(Arrays.asList(" ", "."));
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));	
		
		for (String character : legalCharacters) {
			utilProps.setProperty("name", "name" + character + "123");
			String response = u.addUtility(seasonID, utilProps, sessionToken);
			Assert.assertFalse(response.contains("error"), "Can't add utility with legal character in name: " + utilProps.getProperty("name") );
		}	

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
