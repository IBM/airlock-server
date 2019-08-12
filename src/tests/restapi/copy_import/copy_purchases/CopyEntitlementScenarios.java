package tests.restapi.copy_import.copy_purchases;

import java.io.IOException;






import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
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

public class CopyEntitlementScenarios {
	private String seasonID1;
	private String seasonID2;
	private String seasonID3;
	private String productID;
	private String entitlementID1;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String mixId = "";
	private InAppPurchasesRestApi purchasesApi;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID1 = baseUtils.createSeason(productID);	
	}

	/*
		create 3 seasons S1, S2, S3
		add  entitlements to each season
		copy entitlement1 from S2 to S1
		for all 3 seasons check: get entitlements, add entitlement, update entitlement, delete entitlement
	 */

	@Test (description="Create 3 seasons with entitlements: mix->e1, e2; cr1 under e2")
	public void createSeasons() throws IOException, JSONException{	
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItem(seasonID1, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID1, entitlement1, mixId, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season " + entitlementID1);

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		String EntitlementID2 = purchasesApi.addPurchaseItem(seasonID1, entitlement2, mixId, sessionToken);
		Assert.assertFalse(EntitlementID2.contains("error"), "Entitlement was not added to the season " + EntitlementID2);

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID = purchasesApi.addPurchaseItem(seasonID1, configuration, EntitlementID2, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Entitlement was not added to the season");

		//create season2
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);

		//create season2
		sJson = new JSONObject();
		sJson.put("minVersion", "5.0");		
		seasonID3 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID3.contains("error"), "The second season was not created: " + seasonID3);
	}

	@Test (dependsOnMethods="createSeasons", description="Copy mix from season2 to season1 under root")
	public void copyEntitlement() throws IOException, JSONException{
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);

		String response = f.copyFeature(mixId, rootId, "ACT", null, "suffix1", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
	}

	@Test(dependsOnMethods="copyEntitlement", description="Validate all seasons")
	public void checkEntitlementsInSeasons() throws Exception{
		//get entitlements in all seasons
		JSONArray entitlements = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(entitlements.size()==2, "Incorrect number of entitlements in season1");

		entitlements = purchasesApi.getPurchasesBySeason(seasonID2, sessionToken);
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of entitlements in season2");

		entitlements = purchasesApi.getPurchasesBySeason(seasonID3, sessionToken);
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of entitlements in season3");

		//Add entitlement to each season
		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		String e3S1Id = purchasesApi.addPurchaseItem(seasonID1, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(e3S1Id.contains("error"), "Entitlement was not added to season1 " + e3S1Id);
		String e3S2Id = purchasesApi.addPurchaseItem(seasonID2, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(e3S2Id.contains("error"), "Entitlement was not added to season1 " + e3S2Id);
		String e3S3Id = purchasesApi.addPurchaseItem(seasonID3, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(e3S3Id.contains("error"), "Entitlement was not added to season1 " + e3S3Id);

		//Update entitlement in each season
		String e3S1 = purchasesApi.getPurchaseItem(e3S1Id, sessionToken);
		JSONObject e3S1Json = new JSONObject(e3S1);
		e3S1Json.put("name", "entitlements in season1");
		String response = purchasesApi.updatePurchaseItem(seasonID1, e3S1Id, e3S1Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement was not updated in season1 " + response);

		String e3S2 = purchasesApi.getPurchaseItem(e3S2Id, sessionToken);
		JSONObject e3S2Json = new JSONObject(e3S2);
		e3S2Json.put("name", "entitlement3 in season2");
		response = purchasesApi.updatePurchaseItem(seasonID2, e3S2Id, e3S2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement was not updated in season2 " + response);

		String e3S3 = purchasesApi.getPurchaseItem(e3S3Id, sessionToken);
		JSONObject e3S3Json = new JSONObject(e3S3);
		e3S3Json.put("name", "entitlement3 in season3");
		response = purchasesApi.updatePurchaseItem(seasonID3, e3S3Id, e3S3Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement was not updated in season3 " + response);

		//delete entitlement in all seasons
		int respCode = purchasesApi.deletePurchaseItem(e3S1Id, sessionToken);
		Assert.assertTrue(respCode==200, "Entitlement3 was not deleted from season1");
		respCode = purchasesApi.deletePurchaseItem(e3S2Id, sessionToken);
		Assert.assertTrue(respCode==200, "Entitlement3 was not deleted from season2");
		respCode = purchasesApi.deletePurchaseItem(e3S3Id, sessionToken);
		Assert.assertTrue(respCode==200, "Entitlement3 was not deleted from season3");

		//get entitlements in all seasons
		entitlements = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(entitlements.size()==2, "Incorrect number of entitlements in season1");

		entitlements = purchasesApi.getPurchasesBySeason(seasonID2, sessionToken);
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of entitlements in season2");

		entitlements = purchasesApi.getPurchasesBySeason(seasonID3, sessionToken);
		Assert.assertTrue(entitlements.size()==1, "Incorrect number of entitlements in season3");

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}