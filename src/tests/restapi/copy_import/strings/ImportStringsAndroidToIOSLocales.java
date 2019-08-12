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

public class ImportStringsAndroidToIOSLocales {
	private String seasonID;
	private String seasonID2;
	private String stringID;
	private String filePath;
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
	 * copy new string 
	 */
	
	@Test (description = "Add strings with translations")
	public void addComponents() throws Exception{
		 //create season2
		 seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);
		 
		//add Android specific locales to season1
		String  response = translationsApi.addSupportedLocales(seasonID,"iw",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add iw");
		response = translationsApi.addSupportedLocales(seasonID,"zh_CN",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add zh_CN");
		response = translationsApi.addSupportedLocales(seasonID,"zh_TW",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add zh_TW");
		
       
       //add iOS specific locales to season2       
		response = translationsApi.addSupportedLocales(seasonID2,"he",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add he");
		response = translationsApi.addSupportedLocales(seasonID2,"zh_Hans",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add zh_Hans");
		response = translationsApi.addSupportedLocales(seasonID2,"zh_Hant",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add zh_Hant");


		//add string to season1
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
	}
	

	@Test (dependsOnMethods="addComponents", description = "Add locales and translations to season1")
	public void addTranslations() throws Exception{
		
		
        //mark for translation
       String  response = translationsApi.markForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not mark string ");
        //review for translation
        response = translationsApi.reviewForTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not review string ");
        //send to translation
        response = translationsApi.sendToTranslation(seasonID,new String[]{stringID},sessionToken);
        Assert.assertTrue(response.equals(""),"could not send string");
        //update translation
        String translations = FileUtils.fileToString(filePath + "strings/translationAndroidMultipleLocales.txt", "UTF-8", false);
        JSONObject translationsJson = new JSONObject(translations);
        response = translationsApi.updateTranslation(seasonID,"iw",translationsJson.getJSONObject("iw").toString(),sessionToken);
        Assert.assertTrue(response.equals(""),"could not add translation for iw");
        response = translationsApi.updateTranslation(seasonID,"zh_CN",translationsJson.getJSONObject("zh_CN").toString(),sessionToken);
        Assert.assertTrue(response.equals(""),"could not add translation for zh_CN");
        response = translationsApi.updateTranslation(seasonID,"zh_TW",translationsJson.getJSONObject("zh_TW").toString(),sessionToken);
        Assert.assertTrue(response.equals(""),"could not add translation for zh_TW");
 
	}

	
	@Test (dependsOnMethods="addTranslations", description = "Copy new strings")
	public void copyStringToDifferentSeason() throws Exception{
		
		String content = stringsApi.getAllStrings(seasonID, "INCLUDE_TRANSLATIONS", sessionToken);
		String response = stringsApi.importStrings(content, seasonID2, "ACT", false, sessionToken);
		Assert.assertFalse(response.contains("error"), "Strings were not imported: " + response);
	
		//validate that strings were copied to season2 and its status was changed
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, "INCLUDE_TRANSLATIONS", sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").containsKey("he"), "String1 HE translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").containsKey("zh_Hans"), "String1 zh_Hans translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getJSONObject("translations").containsKey("zh_Hant"), "String1 zh_Hant translation was not found in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("status").equals("TRANSLATION_COMPLETE"), "String1 status incorrect in season2");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
