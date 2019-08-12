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

public class CopyEntitlementToMTXAfterImport {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String newEntitlementID3;
	private String newEntitlementID3_2;
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
		seasonID = baseUtils.createSeason(productID);	
	}

	/*
	 * create E1
	 * create E2
	 * create E_MTX
	 * move E1 and E2 under MTX
	 * create E3
	 * Import E3 under E_MTX with suffix1
	 * Copy suffix1_E3 again to E_MTX
	 */

	@Test (description="Create components: E1, E2, E3, E_MTX")
	public void addComponents() throws IOException, JSONException{	
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixId = purchasesApi.addPurchaseItem(seasonID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement mix was not added to the season" + mixId);

		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season " + entitlementID1);

		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season " + entitlementID2);

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to the season " + entitlementID3);
	}

	@Test (dependsOnMethods="addComponents", description="move E1, E2 under mtx")
	public void moveEntitlementsUnderMtx() throws IOException, JSONException{
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID1, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItem(entitlementID2, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItem(mixId, sessionToken));
		
		JSONArray entitlementsArr = new JSONArray();
		entitlementsArr.add(e1);
		entitlementsArr.add(e2);
		mix.put("entitlements", entitlementsArr);
		String response = purchasesApi.updatePurchaseItem(seasonID, mixId, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Entitlement mix was not updated " + response);

		JSONObject updatedMix = new JSONObject(purchasesApi.getPurchaseItem(mixId, sessionToken));
		JSONArray updatedEntitlementsArr = updatedMix.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 2, "wrong updatedEntitlementsArr size");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "wrong updatedEntitlementsArr entitlementID1");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(1).getString("uniqueId").equals(entitlementID2), "wrong updatedEntitlementsArr entitlementID2");
	}
	
	@Test (dependsOnMethods="moveEntitlementsUnderMtx", description="import E3 under mtx")
	public void importEntitlementsUnderMtx() throws IOException, JSONException{
		String e3 = purchasesApi.getPurchaseItem(entitlementID3, sessionToken);
		Assert.assertFalse(e3.contains("error"), "error get entitlement: " + e3);

		String response = f.importFeatureToBranch(e3, mixId, "ACT", null, "suffix1", true, sessionToken, "MASTER");
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);
	
		JSONObject updatedMix = new JSONObject(purchasesApi.getPurchaseItem(mixId, sessionToken));
		JSONArray updatedEntitlementsArr = updatedMix.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 3, "wrong updatedEntitlementsArr size");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "wrong updatedEntitlementsArr entitlementID1");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(1).getString("uniqueId").equals(entitlementID2), "wrong updatedEntitlementsArr entitlementID2");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(2).getString("name").equals("inAppPurchase3suffix1"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(2).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		
		newEntitlementID3 = updatedEntitlementsArr.getJSONObject(2).getString("uniqueId");
		
		response = purchasesApi.getAllPurchaseItems(seasonID, sessionToken);
		JSONObject allPI = new JSONObject(response);
		JSONArray eArr = allPI.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		Assert.assertTrue(eArr.size() == 2, "wrong num of entitlements under root");
		Assert.assertTrue(eArr.getJSONObject(0).getJSONArray("entitlements").size() == 3, "wrong num of entitlements under mtx");
		
		String rootId = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		JSONObject rootObj = new JSONObject(purchasesApi.getPurchaseItem(rootId, sessionToken));
		eArr = rootObj.getJSONArray("entitlements");
		Assert.assertTrue(eArr.size() == 2, "wrong num of entitlements under root");
		Assert.assertTrue(eArr.getJSONObject(0).getJSONArray("entitlements").size() == 3, "wrong num of entitlements under mtx");
	}
	
	@Test (dependsOnMethods="importEntitlementsUnderMtx", description="copy new E3 under mtx")
	public void copyNewEntitlementsUnderMtx() throws IOException, JSONException{
		String e3 = purchasesApi.getPurchaseItem(entitlementID3, sessionToken);
		Assert.assertFalse(e3.contains("error"), "error get entitlement: " + e3);

		String response = f.copyFeature(newEntitlementID3, mixId, "ACT", null, "suffix2", sessionToken);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
	
		JSONObject updatedMix = new JSONObject(purchasesApi.getPurchaseItem(mixId, sessionToken));
		JSONArray updatedEntitlementsArr = updatedMix.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 4, "wrong updatedEntitlementsArr size");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "wrong updatedEntitlementsArr entitlementID1");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(1).getString("uniqueId").equals(entitlementID2), "wrong updatedEntitlementsArr entitlementID2");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(2).getString("name").equals("inAppPurchase3suffix1"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(2).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(3).getString("name").equals("inAppPurchase3suffix1suffix2"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(3).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(3).getString("uniqueId").equals(newEntitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		
		newEntitlementID3_2 = updatedEntitlementsArr.getJSONObject(3).getString("uniqueId");	
	}
	
	@Test (dependsOnMethods="copyNewEntitlementsUnderMtx", description="import new E3 under mtx")
	public void importNewEntitlementsUnderMtx() throws IOException, JSONException{
		String e3_2 = purchasesApi.getPurchaseItem(newEntitlementID3_2, sessionToken);
		Assert.assertFalse(e3_2.contains("error"), "error get entitlement: " + e3_2);

		String response = f.importFeatureToBranch(e3_2, mixId, "ACT", null, "suffix3", true, sessionToken, "MASTER");
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not imported: " + response);
	
		JSONObject updatedMix = new JSONObject(purchasesApi.getPurchaseItem(mixId, sessionToken));
		JSONArray updatedEntitlementsArr = updatedMix.getJSONArray("entitlements");
		Assert.assertTrue(updatedEntitlementsArr.size() == 5, "wrong updatedEntitlementsArr size");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(0).getString("uniqueId").equals(entitlementID1), "wrong updatedEntitlementsArr entitlementID1");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(1).getString("uniqueId").equals(entitlementID2), "wrong updatedEntitlementsArr entitlementID2");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(2).getString("name").equals("inAppPurchase3suffix1"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(2).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(3).getString("name").equals("inAppPurchase3suffix1suffix2"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(3).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(3).getString("uniqueId").equals(newEntitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertTrue(updatedEntitlementsArr.getJSONObject(4).getString("name").equals("inAppPurchase3suffix1suffix2suffix3"), "wrong updatedEntitlementsArr entitlementID3  name");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(4).getString("uniqueId").equals(entitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(4).getString("uniqueId").equals(newEntitlementID3), "wrong updatedEntitlementsArr entitlementID3");
		Assert.assertFalse(updatedEntitlementsArr.getJSONObject(4).getString("uniqueId").equals(newEntitlementID3_2), "wrong updatedEntitlementsArr entitlementID3");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}