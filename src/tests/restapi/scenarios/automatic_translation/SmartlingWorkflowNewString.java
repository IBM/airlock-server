package tests.restapi.scenarios.automatic_translation;

import java.io.IOException;




import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;

public class SmartlingWorkflowNewString {
	   protected String seasonID;
	    protected String stringID;
	    protected String filePath;
	    protected StringsRestApi stringsApi;
	    protected TranslationsRestApi translationsApi;
	    private AirlockUtils baseUtils;
	    protected ProductsRestApi p;
	    protected String productID;
	    protected String m_url;
	    private String sessionToken = "";
	    private String translationsUrl;

	    @BeforeClass
		@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "smartlingProjectId"})
		public void init(String url, String analyticsUrl, String tUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String smartlingProjectId) throws Exception{
	        m_url = url;
	        translationsUrl = tUrl;
	        filePath = configPath;
	        stringsApi = new StringsRestApi();
	        stringsApi.setURL(translationsUrl);
	        translationsApi = new TranslationsRestApi();
	        translationsApi.setURL(translationsUrl);
	        p = new ProductsRestApi();
	        p.setURL(m_url);
			baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
			sessionToken = baseUtils.sessionToken;
	        
			String product = FileUtils.fileToString(configPath + "product1.txt", "UTF-8", false);
			JSONObject jsonProduct = new JSONObject(product);
			jsonProduct.put("codeIdentifier", RandomStringUtils.randomAlphabetic(5));
			jsonProduct.put("name", RandomStringUtils.randomAlphabetic(5));
			jsonProduct.put("smartlingProjectId", smartlingProjectId);
			productID = p.addProduct(jsonProduct.toString(), sessionToken);
			
	        baseUtils.printProductToFile(productID);
	        seasonID = baseUtils.createSeason(productID);
	        
	    	String res = translationsApi.traceOn(sessionToken);
	    	Assert.assertTrue(res.equals(""), res);

	    }
	    
	    @Test (description="add new string")
	    public void addExistingString() throws Exception{
	    	//System.out.println("Season ID: " + seasonID);
	    	
	    	translationsApi.addSupportedLocales(seasonID, "de", sessionToken);
	    	translationsApi.addSupportedLocales(seasonID, "he", sessionToken);
	    	
	    	//add new string        
	    	String str = FileUtils.fileToString(filePath + "/strings/smartlingString1.txt", "UTF-8", false);
	        stringID = stringsApi.addString(seasonID, str, sessionToken);
	        translationsApi.markForTranslation(seasonID, new String[]{stringID}, sessionToken);
	        translationsApi.reviewForTranslation(seasonID, new String[]{stringID}, sessionToken);
	        
	        Thread.sleep(120000);
	        /*
	        JSONObject log = new JSONObject(translationsApi.getLog(sessionToken));
	        System.out.println(log.write(true));
	       // Assert.assertTrue(log.contains("[season " + seasonID + "] 1 new strings found"), "Incorrect trace for add string");
	        * */
	        String log = translationsApi.getLog(sessionToken);
	        Assert.assertTrue(log.contains("[season " + seasonID + "] 1 new strings found"), "new string not found");
	        Assert.assertTrue(log.contains("[season " + seasonID + "] 1 strings to translate found"), "string to translate not found");
	        
	        JSONObject strWithTr = new JSONObject(stringsApi.getString(stringID, "INCLUDE_TRANSLATIONS", sessionToken));
	        Assert.assertTrue(strWithTr.getString("status").equals("TRANSLATION_COMPLETE"));
	        Assert.assertTrue(strWithTr.getJSONObject("translations").getJSONObject("de").getString("translationStatus").equals("TRANSLATED"));
	        Assert.assertTrue(strWithTr.getJSONObject("translations").getJSONObject("he").getString("translationStatus").equals("TRANSLATED"));
	        
	        translationsApi.addSupportedLocales(seasonID, "fr", sessionToken);
	        Thread.sleep(120000);
	        log = translationsApi.getLog(sessionToken);
	        
	        Assert.assertTrue(log.contains("[season " + seasonID + "] 1 strings to translate found"), "string to translate in fr not found");
	        Assert.assertTrue(log.contains("[season " + seasonID + "] saving translations for fr"), "string to translate in fr not found");
	       
	  }
	    
	    @AfterTest
	    private void reset() throws IOException{
	    	String res = translationsApi.traceOff(sessionToken);
	    	Assert.assertTrue(res.equals(""), res);

	        baseUtils.reset(productID, sessionToken);
	    }
}
