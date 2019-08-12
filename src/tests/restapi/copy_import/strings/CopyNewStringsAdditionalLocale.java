package tests.restapi.copy_import.strings;

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CopyNewStringsAdditionalLocale {
	private String seasonID;
	private String seasonID2;
	private String stringID;
	private String filePath;
	private String stringID2;
	private StringsRestApi stringsApi;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected TranslationsRestApi translationsApi;
	private String productID;
	private String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private FeaturesRestApi f;
	private UtilitiesRestApi u;
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		stringsApi = new StringsRestApi();
		stringsApi.setURL(m_translationsUrl);
        translationsApi = new TranslationsRestApi();
        translationsApi.setURL(translationsUrl);

		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);

	}
	
	/*
	 * copy new string with additional locale in target
	 */
	
	@Test (description = "Add strings with translations")
	public void addComponents() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);

	}
	

	@Test (dependsOnMethods="addComponents", description = "Add locales and translations to season1")
	public void addTranslations() throws Exception{
		
		//add 2 locales
		String  response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
		response = translationsApi.addSupportedLocales(seasonID,"es",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add spanish");
        //mark for translation
        response = translationsApi.markForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark string ");
        //review for translation
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not review string ");
        //send to translation
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send string");
        //update translation
        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        String translationEs = FileUtils.fileToString(filePath + "strings/translationES.txt", "UTF-8", false);
        response = translationsApi.updateTranslation(seasonID,"es",translationEs,sessionToken);
        
        String string1 = stringsApi.getString(stringID, "INCLUDE_TRANSLATIONS", sessionToken);
        JSONObject string1Json = new JSONObject(string1);
        Assert.assertTrue(string1Json.getString("status").equals("TRANSLATION_COMPLETE"), "Incorrect status in translated string1 in season1");
 
	}

	
	@Test (dependsOnMethods="addTranslations", description = "Copy new strings")
	public void copyStringToDifferentSeason() throws Exception{
		//add new  locale to season2
		String response = translationsApi.addSupportedLocales(seasonID2,"de",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add german to new season");

		
		response = stringsApi.copyStrings(new String[]{stringID, stringID2}, seasonID2, "ACT", false, sessionToken);
		Assert.assertFalse(response.contains("error"), "Strings were not copied");
		
		
		//validate that strings were copied to season2 and its status was changed
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("status").equals("IN_TRANSLATION"), "String1 status was not updated in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("status").equals("IN_TRANSLATION"), "String2 status was not updated in season2");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
