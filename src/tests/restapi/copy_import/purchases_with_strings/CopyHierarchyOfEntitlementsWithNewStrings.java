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

public class CopyHierarchyOfEntitlementsWithNewStrings {
	private String seasonID;
	private String seasonID2;
	private String entitlementID1;
	private String filePath;
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
        purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

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
		E1 -> MIX	->E2 -> MIXCR ->CR1, CR2
					->E3 -> CR3 -> CR4
	 */
	
	@Test (description = "Add string and entitlement with configuration rule using this string")
	public void addComponents() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		String stringID = stringsApi.addString(seasonID, str, sessionToken);
		Assert.assertFalse(stringID.contains("Error"), "String was not added:" + stringID);
		
		JSONObject strJson = new JSONObject(str);
		strJson.put("key", "app.hello2");
		String stringID2 = stringsApi.addString(seasonID, strJson.toString(), sessionToken);
		Assert.assertFalse(stringID2.contains("Error"), "String was not added:" + stringID2);
		
		strJson.put("key", "app.hello3");
		String stringID3 = stringsApi.addString(seasonID, strJson.toString(), sessionToken);
		Assert.assertFalse(stringID3.contains("Error"), "String was not added:" + stringID3);
		
		strJson.put("key", "app.hello4");
		String stringID4 = stringsApi.addString(seasonID, strJson.toString(), sessionToken);
		Assert.assertFalse(stringID4.contains("Error"), "String was not added:" + stringID4);
		
		//create entitlement tree in season1
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");
		
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixID1 = purchasesApi.addPurchaseItem(seasonID, entitlementMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Entitlement mix was not added to the season: " + mixID1);
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2, mixID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String entitlementID3 = purchasesApi.addPurchaseItem(seasonID, entitlement3, mixID1, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		jsonCR.put("configuration", "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}");
		String configID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "cr was not added to the season");
				
		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", "{ \"text\" :  translate(\"app.hello2\", \"testing string\")	}");
		String configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "cr was not added to the season");

		jsonCR.put("name", "CR3");
		jsonCR.put("configuration", "{ \"text\" :  translate(\"app.hello3\", \"testing string\")	}");
		String configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(),entitlementID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "cr was not added to the season");

		jsonCR.put("name", "CR4");
		jsonCR.put("configuration", "{ \"text\" :  translate(\"app.hello4\", \"testing string\")	}");
		String configID4 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "cr was not added to the season");
	}

	
	@Test (dependsOnMethods="addComponents", description = "Copy entitlement to season2 - no string conflict")
	public void copyEntitlementDifferentSeason() throws Exception{
		String rootId2 = purchasesApi.getBranchRootId(seasonID2, "MASTER", sessionToken);
		String response = f.copyFeature(entitlementID1, rootId2, "ACT", null, null, sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		//validate that strings were copied to season2
		String stringsInSeason = stringsApi.getAllStrings(seasonID2, sessionToken);
		JSONObject stringsInSeasonJson = new JSONObject(stringsInSeason);
		Assert.assertTrue(stringsInSeasonJson.getJSONArray("strings").size()==4, "Not all strings were copied to season2");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
