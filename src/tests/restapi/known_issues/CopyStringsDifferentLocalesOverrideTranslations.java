//package tests.restapi.copy_import.strings;
package tests.restapi.known_issues;

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
//Known issue: if the original status was override, fter copy it is changed to new. - design issue. Bug#49  Q4-2017 
public class CopyStringsDifferentLocalesOverrideTranslations {
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

	}
	
	/*
	 * copy existing string:
		copy when in source 2 locales and translation complete and the string exists in target with same 2 locales and one translation is missing	 */
	
	@Test (description = "Add strings with translations")
	public void addComponents() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);

		//add 2 locales in season1
		String  response = translationsApi.addSupportedLocales(seasonID,"fr",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add french");
		response = translationsApi.addSupportedLocales(seasonID,"es",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add spanish");
        
        seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);
        //add locale without translations to season2
		response = translationsApi.addSupportedLocales(seasonID2,"it",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add italian in season2");

	}
	

	@Test (dependsOnMethods="addComponents", description = "Add locales and translations to season1")
	public void addTranslations() throws Exception{
		
		//add translations in season1
        //mark for translation
       String response = translationsApi.markForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark string ");
        //review for translation
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not review string ");
        //send to translation
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID, stringID2},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send string");
        
 
        response = translationsApi.overrideTranslate(stringID,"fr","salut",sessionToken);
        Assert.assertTrue(response.equals(""),"could not override fr translation ");
        response = translationsApi.overrideTranslate(stringID2,"fr","bonjour",sessionToken);
        
  
        response = translationsApi.overrideTranslate(stringID,"es","hola",sessionToken);
        Assert.assertTrue(response.equals(""),"could not override es translation ");
        response = translationsApi.overrideTranslate(stringID2,"es","salve",sessionToken);
 

	}
	
	
	@Test (dependsOnMethods="addTranslations", description = "Copy strings")
	public void copyStringToDifferentSeason() throws Exception{
		

		JSONObject response = new JSONObject(stringsApi.copyStrings(new String[]{stringID, stringID2}, seasonID2, "ACT", true, sessionToken));
		Assert.assertFalse(response.containsKey("error"), "Strings were not copied");
		
		
		//validate that strings were copied to season2 and its status was changed
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, "INCLUDE_TRANSLATIONS", sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").containsKey("es"), "String1 ES translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").containsKey("fr"), "String1 FR translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").getJSONObject("es").getString("translationStatus").equals("OVERRIDE"), "String1 ES translation incorrect status in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").getJSONObject("fr").getString("translationStatus").equals("OVERRIDE"), "String1 FR translation incorrect status in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("status").equals("IN_TRANSLATION"), "String1 status was not updated in season2");
		
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getJSONObject("translations").containsKey("fr"), "String2 FR translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getJSONObject("translations").containsKey("es"), "String2 ES translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getJSONObject("translations").getJSONObject("fr").getString("translationStatus").equals("OVERRIDE"), "String2 FR translation incorrect status in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getJSONObject("translations").getJSONObject("es").getString("translationStatus").equals("OVERRIDE"), "String2 ES translation incorrect status in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("status").equals("IN_TRANSLATION"), "String2 status was not updated in season2");
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
