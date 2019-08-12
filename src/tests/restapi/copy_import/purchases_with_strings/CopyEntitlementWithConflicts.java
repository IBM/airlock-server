package tests.restapi.copy_import.purchases_with_strings;

import org.apache.wink.json4j.JSONArray;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CopyEntitlementWithConflicts {
	private String seasonID;
	private String seasonID2;
	private String stringID;
	private String entitlementID;
	private String configID;
	private String configID2;
	private String filePath;
	private String stringID2;
	private StringsRestApi stringsApi;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	private TranslationsRestApi translationsApi;
	private String productID;
	private String m_url;
	private String sessionToken = "";
	private String m_translationsUrl;
	private FeaturesRestApi f;
	private InAppPurchasesRestApi purchasesApi;
	
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	
	//in each test string1 is existing in season2 and string2 is new and should be copied
	
	@Test (description = "Add string and an entitlement with configuration rule using this string")
	public void addComponents() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);
	
		//add entitlement with strings to season2
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added to the season " + entitlementID);
		
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", configuration);
		configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season: " + configID);
		
		String config2 =  "{ \"text\" :  translate(\"app.hi\", \"param1\", \"param2\")	}" ;
		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonCR2 = new JSONObject(configuration2);
		jsonCR2.put("name", "CR2");
		jsonCR2.put("configuration", config2);
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR2.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "cr was not added to the season: " + configID2);
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
        
        String string2 = stringsApi.getString(stringID2, "INCLUDE_TRANSLATIONS", sessionToken);
        JSONObject string2Json = new JSONObject(string2);
        Assert.assertTrue(string2Json.getString("status").equals("TRANSLATION_COMPLETE"), "Incorrect status in translated string2 in season1");

		//add season2 - it will include string1 and string2 with translations
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Season2 was not created: " + seasonID2);
	}

	@Test (dependsOnMethods="addTranslations", description = "Add new locale to season2")
	public void addLocale() throws Exception{		
		//add new  locale to season2
		String response = translationsApi.addSupportedLocales(seasonID2,"de",sessionToken);
        Assert.assertTrue(response.equals(""),"could not add german to new season");
	}
	
	@Test (dependsOnMethods="addLocale", description = "Copy entitlement to season2 with internationalFallback - copy entitlement, do not copy strings")
	public void copyEntitlementInternationalFallback() throws Exception{
		String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJson = new JSONObject(str);
		strJson.put("internationalFallback", "Hello 2");
		String response = stringsApi.updateString(stringID, strJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String1 was not updated in season1: " + response);
		
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		response = f.copyFeature(entitlementID, rootId2, "ACT", null, "suffix2", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		//validate that string1 was not copied to season2
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("internationalFallback").equals("Hello"), "String1 internationalFallback was updated in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");

		String newId = new JSONObject(response).getString("newSubTreeId");
		cleanSeason2(newId);
	}
	
	@Test (dependsOnMethods="copyEntitlementInternationalFallback", description = "Copy entitlement to season2 with different stage - copy entitlement, do not copy strings")
	public void copyEntitlementDifferentStage() throws Exception{
		String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJson = new JSONObject(str);
		strJson.put("stage", "PRODUCTION");
		String response = stringsApi.updateString(stringID, strJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String1 was not updated in season1: " + response);
		
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		response = f.copyFeature(entitlementID, rootId2, "ACT", null, "suffix3", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		//validate that string1 was not copied to season2
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("stage").equals("DEVELOPMENT"), "String1 stage was updated in season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");

		String newId = new JSONObject(response).getString("newSubTreeId");
		cleanSeason2(newId);
	}
	
	@Test (dependsOnMethods="copyEntitlementDifferentStage", description = "Copy entitlement to season2 with translation - copy entitlement, do not copy strings")
	public void copyEntitlementDifferentTranslation() throws Exception{
        //update translation
        String translationFr = FileUtils.fileToString(filePath + "strings/translationFR6.txt", "UTF-8", false);
        String response = translationsApi.updateTranslation(seasonID,"fr",translationFr,sessionToken);
        Assert.assertTrue(response.equals(""), "Translation was not copied");
		
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		response = f.copyFeature(entitlementID, rootId2, "ACT", null, "suffix4", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		//validate that string1 was not copied to season2
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("key").equals("app.hello"), "Incorrect key1 copied to season2");
		
		//get string translation
		String str1 = stringsApi.getString(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("uniqueId"), "INCLUDE_TRANSLATIONS", sessionToken);
		JSONObject str1Json = new JSONObject(str1);		
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");
		Assert.assertTrue(str1Json.getJSONObject("translations").getJSONObject("fr").getString("translatedValue").equals("Bonjour"), "Translation for string1 in french was updated");
		
		String newId = new JSONObject(response).getString("newSubTreeId");
		cleanSeason2(newId);
	}
	
	@Test (dependsOnMethods="copyEntitlementDifferentTranslation", description = "Copy entitlement to season2 with different value - conflict, no string copy")
	public void copyEntitlementDifferentValue() throws Exception{
		String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJson = new JSONObject(str);
		strJson.put("value", "Hello1 [[[1]]]");
		String response = stringsApi.updateString(stringID, strJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String1 was not updated in season1: " + response);
		
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		
		response = f.copyFeature(entitlementID, rootId2, "VALIDATE", null, "suffix5", sessionToken);		
		response = f.copyFeature(entitlementID, rootId2, "ACT", null, "suffix5", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject json  =new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("stringsInConflict").getJSONObject(0).getString("key").equals("app.hello"), "String in conflict incorrect");
		
		//get string value
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		String str1 = stringsApi.getString(stringsInSeasonJson.getJSONArray("strings").getJSONObject(0).getString("uniqueId"), "BASIC", sessionToken);
		JSONObject str1Json = new JSONObject(str1);
		Assert.assertFalse(str1Json.getString("value").equals("Hello1 [[[1]]]"), "Value for string1 in french was updated");		
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==2, "String was not copied to season2");
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").getJSONObject(1).getString("key").equals("app.hi"), "Incorrect key2 copied to season2");

		String newId = new JSONObject(response).getString("newSubTreeId");
		cleanSeason2(newId);
	}

	private void cleanSeason2(String id) throws Exception{
		JSONArray entitlements = purchasesApi.getPurchasesBySeason(seasonID2, sessionToken);
		for(int i=0; i< entitlements.size(); i++){
			purchasesApi.deletePurchaseItem(entitlements.getJSONObject(i).getString("uniqueId"), sessionToken);
		}	
		
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONArray stringsInSeasonJson = new JSONObject(stringsInSeason).getJSONArray("strings");
		for (int i=0; i<stringsInSeasonJson.size(); i++){
			JSONObject str = stringsInSeasonJson.getJSONObject(i);
			if (str.getString("key").equals("app.hi")) {
				int codeStr = stringsApi.deleteString(str.getString("uniqueId"), sessionToken);
				Assert.assertTrue(codeStr==200, "String2 was not deleted");
			}
		}
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
