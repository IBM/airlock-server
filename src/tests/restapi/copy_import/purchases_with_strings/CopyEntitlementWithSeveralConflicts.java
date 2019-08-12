package tests.restapi.copy_import.purchases_with_strings;


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

public class CopyEntitlementWithSeveralConflicts {
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
	private UtilitiesRestApi u;
	private SeasonsRestApi s;
	private InAppPurchasesRestApi purchasesApi;
	
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

	
	//create an entitlement with 3 strings - conflict in value, conflict in variant, new string
	@Test (description = "Add string and entitlement with configuration rule using this string")
	public void addStrings() throws Exception{
		//key app.hello
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = stringsApi.addString(seasonID, str, sessionToken);
		
		//key app.hi
		str = FileUtils.fileToString(filePath + "strings/string2.txt", "UTF-8", false);
		stringID2 = stringsApi.addString(seasonID, str, sessionToken);
	
		//add entitlement with strings to season2
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Entitlement was not added to the season " + entitlementID);
		
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
		
		//add season2 - it will include string1, string2, string3 and entitlement with 2 configurations
		seasonID2 = s.addSeason(productID, "{\"minVersion\": \"5.0\"}", sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Season2 was not created: " + seasonID2);
	}

	@Test (dependsOnMethods="addStrings", description = "Add configuration with string3")
	public void addNewConfiguration() throws Exception{
		//key app.newhello
		String str = FileUtils.fileToString(filePath + "strings/string6.txt", "UTF-8", false);
		String stringID3 = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID3.contains("error"), "String3 was not added " + stringID3);
		
		String config3 =  "{ \"text\" :  translate(\"app.newhello\")	}" ;
		String configuration3 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR3 = new JSONObject(configuration3);
		jsonCR3.put("name", "CR3");
		jsonCR3.put("configuration", config3);
		String configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR3.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "cr was not added to the season: " + configID3);
	}
	
	/*
	@Test (dependsOnMethods="addNewConfiguration", description = "Copy entitlement to season2 with 2 conflicts and 1 new string")
	public void copyEntitlementDifferentVariant() throws Exception{
		String str = stringsApi.getString(stringID, sessionToken);
		JSONObject strJson = new JSONObject(str);
		strJson.put("variant", "Hello");
		String response = stringsApi.updateString(stringID, strJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "String1 was not updated in season1: " + response);
		
		String str2 = stringsApi.getString(stringID2, sessionToken);
		JSONObject strJson2 = new JSONObject(str2);
		strJson2.put("value", "NewHi [[[1]]]");
		String response2 = stringsApi.updateString(stringID2, strJson2.toString(), sessionToken);
		Assert.assertFalse(response2.contains("error"), "String2 was not updated in season1: " + response2);

		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		response = f.copyFeature(entitlementID, rootId2, "ACT", null, "suffix1", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject json  =new JSONObject(response);
		Assert.assertTrue(json.getJSONArray("stringsInConflict").size()==2, "Strings in conflict incorrect");
		
		//get string variant
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);	
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==3, "String3 was not copied to season2");
	}*/
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
