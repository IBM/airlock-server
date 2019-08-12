package tests.restapi.in_app_purchases;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class BuildProductForSdkTests {
	protected String seasonID1;
	protected String seasonID2;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected InAppPurchasesRestApi purchasesApi;
	protected String inAppPurID1;
	protected String inAppPurID2;
	protected String bundleID;
	protected String inAppPurID3;
	protected String inAppPurID4;
	protected String inAppPurID5;
	protected String purchaseOptionsID1;
	protected String purchaseOptionsID2;
	protected String purchaseOptionsID3;
	protected String purchaseOptionsID4;
	protected String purchaseOptionsID5;
	protected String addInAppPurcahseMTXID1;
	protected String addInAppPurcahseMTXID2;
	protected String purcahseOptionsMTXID1;
	protected String purcahseOptionsMTXID2;
	protected String experimentID;
	protected String branchID1;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private SeasonsRestApi s;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProductWithSpecifiedName("SDKTests");
		Assert.assertFalse (productID.contains("error"), "Can't add product: " + productID);
		
		baseUtils.printProductToFile(productID);
		seasonID1 = baseUtils.createSeason(productID);
		Assert.assertFalse (seasonID1.contains("error"), "Can't add season1: " + seasonID1);
	}
	
	/*
	 * root-> inAppPurID1, inAppPurID2, bundleID
	 *             |
	 *       purchaseOptionsID1     
	 */
	@Test (description = "add 2 in-app-purchase one purchase including the 2")
	public void addComponents() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject obj = new JSONObject(inAppPur);
		obj.put("namespace", "airlockEntitlement");
		inAppPurID1 = purchasesApi.addPurchaseItem(seasonID1, obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID1.contains("error"), "Can't add inAppPurchase: " + inAppPurID1);
		
		inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		obj = new JSONObject(inAppPur);
		obj.put("namespace", "airlockEntitlement");		
		inAppPurID2 = purchasesApi.addPurchaseItem(seasonID1, obj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (inAppPurID2.contains("error"), "Can't add inAppPurchase: " + inAppPurID2);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("namespace", "airlockEntitlement");		
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID1.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID1);

		JSONArray includedPurchases = new JSONArray();
		includedPurchases.add(inAppPurID1);
		includedPurchases.add(inAppPurID2);
		
		JSONObject iapObj = new JSONObject(inAppPur);
		iapObj.put("name", "bundle");
		iapObj.put("namespace", "airlockEntitlement");		
		iapObj.put("includedEntitlements", includedPurchases);
		bundleID = purchasesApi.addPurchaseItem(seasonID1, iapObj.toString(), "ROOT", sessionToken);
		Assert.assertFalse (bundleID.contains("error"), "Can't add inAppPurchase: " + bundleID);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").size()==2, "wrong number of includedPurchases in bundle");
				
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(0).equals(inAppPurID1), "wrong includedPurchase");
		Assert.assertTrue(bundleObj.getJSONArray("includedEntitlements").get(1).equals(inAppPurID2), "wrong includedPurchase");	
		
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 3, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		JSONObject thirdSubPur = inAppPurchases.getJSONObject(2);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(inAppPurID2), "wrong inAppPurchase");
		Assert.assertTrue(thirdSubPur.getString("uniqueId").equals(bundleID), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(secondSubPur.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(thirdSubPur.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(thirdSubPur.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(secondSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(thirdSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!thirdSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!thirdSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
		
		
		
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");	
	}
	
	/*
	 *         root-> inAppPurID1, inAppPurID2, bundleID
	 *                 |        |
	 * purchaseOptionsID1      inAppPurID3   
	 */
	@Test (dependsOnMethods = "addComponents", description = "add sub inAppPurchase")
	public void addSubInAppPurcahse() throws JSONException, IOException, InterruptedException{
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		JSONObject obj = new JSONObject(inAppPur);
		obj.put("namespace", "airlockEntitlement");
		inAppPurID3 = purchasesApi.addPurchaseItem(seasonID1, obj.toString(), inAppPurID1, sessionToken);
		Assert.assertFalse (inAppPurID3.contains("error"), "Can't add sub inAppPurchase: " + inAppPurID3);
		
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 3, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		JSONObject thirdSubPur = inAppPurchases.getJSONObject(2);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(inAppPurID2), "wrong inAppPurchase");
		Assert.assertTrue(thirdSubPur.getString("uniqueId").equals(bundleID), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(secondSubPur.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(thirdSubPur.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(thirdSubPur.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(secondSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(thirdSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!thirdSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!thirdSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
	}
	
	/*
	 *         root-> inAppPurID1,                inAppPurcahsesMTXID
	 *                 |        |                            |             
	 * purchaseOptionsID1      inAppPurID3          inAppPurID2, bundleID
	 */
	@Test (dependsOnMethods = "addSubInAppPurcahse", description = "add  inAppPurchase mtx")
	public void addInAppPurcahseMTX() throws JSONException, IOException, InterruptedException{
		//add inAppPurchase MIX
		String ipMtx = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		addInAppPurcahseMTXID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, "MASTER", ipMtx, "ROOT", sessionToken);
		Assert.assertFalse(addInAppPurcahseMTXID1.contains("error"), "inAppPurchase MTX was not added to the season: " + addInAppPurcahseMTXID1);
			
		String mtxStr = purchasesApi.getPurchaseItem(addInAppPurcahseMTXID1, sessionToken);
		JSONObject mtxObj = new JSONObject(mtxStr);
		JSONArray inAppPurcahsesArr = new JSONArray();
		
		String pur2Str = purchasesApi.getPurchaseItem(inAppPurID2, sessionToken);
		JSONObject pur2Obj = new JSONObject(pur2Str);
		inAppPurcahsesArr.add(pur2Obj);
		
		String bundleStr = purchasesApi.getPurchaseItem(bundleID, sessionToken);
		JSONObject bundleObj = new JSONObject(bundleStr);
		inAppPurcahsesArr.add(bundleObj);
		
		mtxObj.put("entitlements", inAppPurcahsesArr);
		String response = purchasesApi.updatePurchaseItem(seasonID1, addInAppPurcahseMTXID1, mtxObj.toString(), sessionToken);
		Assert.assertNotNull(response);
        Assert.assertFalse(response.contains("error"), "InAppPurchase mtx update fails: " + response);
				
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 2, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(!secondSubPur.containsKey("purchaseOptions"), "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 2, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!secondSubPur.containsKey("configurationRules"), "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
		
		JSONObject subMTXInAppPurcahse1 = secondSubPur.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPurcahse2 = secondSubPur.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPurcahse1.getString("uniqueId").equals(inAppPurID2), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse2.getString("uniqueId").equals(bundleID), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("orderingRules"), "wrong orderingRules");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("orderingRules"), "wrong orderingRules");
		
	}
	
	/*
	 *         root-> inAppPurID1,                inAppPurcahsesMTXID
	 *                 |        |                            |             
	 * purchaseOptionsID1      inAppPurID3          inAppPurID2, bundleID
	 *                                                     |
	 *                                              purcahseOptionsMTXID    
	 *                                              |                  |
	 *                                   purchaseOptionsID2       purchaseOptionsID3
	 */
	@Test (dependsOnMethods = "addInAppPurcahseMTX", description = "add purchaseOptions mtx")
	public void addPurcahseOptionsMTX() throws JSONException, IOException, InterruptedException{
		//add purchase options MIX
		String poMtx = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		purcahseOptionsMTXID1 = purchasesApi.addPurchaseItemToBranch(seasonID1, "MASTER", poMtx, inAppPurID2, sessionToken);
		Assert.assertFalse(purcahseOptionsMTXID1.contains("error"), "inAppPurchase MTX was not added to the season: " + purcahseOptionsMTXID1);
			
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name","PO2");
		jsonIP.put("namespace", "airlockEntitlement");		
		purchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), purcahseOptionsMTXID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID2.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID2);

		jsonIP.put("name","PO3");
		jsonIP.put("namespace", "airlockEntitlement");		
		purchaseOptionsID3 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), purcahseOptionsMTXID1, sessionToken);
		Assert.assertFalse (purchaseOptionsID3.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID3);
				
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 2, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(!secondSubPur.containsKey("purchaseOptions"), "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 2, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!secondSubPur.containsKey("configurationRules"), "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(secondSubPur.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		
		JSONObject subMTXInAppPurcahse1 = secondSubPur.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPurcahse2 = secondSubPur.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPurcahse1.getString("uniqueId").equals(inAppPurID2), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse2.getString("uniqueId").equals(bundleID), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("orderingRules"), "wrong orderingRules");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject poMTXObj = subMTXInAppPurcahse1.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(poMTXObj.getString("uniqueId").equals(purcahseOptionsMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(poMTXObj.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong mtx Purcahse options type");	
		Assert.assertTrue(poMTXObj.getJSONArray("purchaseOptions").size() == 2, "wrong mtx sub Purcahse options size");	
		
		JSONObject subMTXurcahseOptions1 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(0);
		JSONObject subMTXurcahseOptions2 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(1);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID3), "wrong purchase options id");
	}
	
	/*
	 *         root-> inAppPurID1,                inAppPurcahsesMTXID
	 *                 |        |                        |         |        
	 * purchaseOptionsID1      inAppPurID3          inAppPurID2, bundleID
	 *                                                     |
	 *                                              purcahseOptionsMTXID1    
	 *                                              |         |       |
	 *                                   purOptionsID2 purOptionsID3  purOptionsMTXID2
	 *                                   							 |			|
	 *                                   						purOptionsID4   purOptionsID5
	 */
	@Test (dependsOnMethods = "addPurcahseOptionsMTX", description = "add purchaseOptions mtx under mtx")
	public void addPurcahseOptionsMTXUnderMTX() throws JSONException, IOException, InterruptedException{
		//add purchase options MIX
		String poMtx = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		purcahseOptionsMTXID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, "MASTER", poMtx, purcahseOptionsMTXID1, sessionToken);
		Assert.assertFalse(purcahseOptionsMTXID2.contains("error"), "inAppPurchase MTX was not added to the season: " + purcahseOptionsMTXID2);
			
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name", "PO4");
		jsonIP.put("namespace", "airlockEntitlement");		
		purchaseOptionsID4 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), purcahseOptionsMTXID2, sessionToken);
		Assert.assertFalse (purchaseOptionsID2.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID2);

		jsonIP.put("name","PO5");
		jsonIP.put("namespace", "airlockEntitlement");		
		purchaseOptionsID5 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), purcahseOptionsMTXID2, sessionToken);
		Assert.assertFalse (purchaseOptionsID3.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID3);
				
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 2, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(!secondSubPur.containsKey("purchaseOptions"), "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 2, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!secondSubPur.containsKey("configurationRules"), "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(secondSubPur.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		
		JSONObject subMTXInAppPurcahse1 = secondSubPur.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPurcahse2 = secondSubPur.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPurcahse1.getString("uniqueId").equals(inAppPurID2), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse2.getString("uniqueId").equals(bundleID), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("orderingRules"), "wrong orderingRules");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject poMTXObj = subMTXInAppPurcahse1.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(poMTXObj.getString("uniqueId").equals(purcahseOptionsMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(poMTXObj.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong mtx Purcahse options type");	
		Assert.assertTrue(poMTXObj.getJSONArray("purchaseOptions").size() == 3, "wrong mtx sub Purcahse options size");	
		
		JSONObject subMTXurcahseOptions1 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(0);
		JSONObject subMTXurcahseOptions2 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(1);
		JSONObject subMTXurcahseOptionsMtx = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(2);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID3), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("uniqueId").equals(purcahseOptionsMTXID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong purchase options type");
		
		Assert.assertTrue(subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").size() == 2, "wrong mtx sub Purcahse options size");	
		
		subMTXurcahseOptions1 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(0);
		subMTXurcahseOptions2 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(1);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID4), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID5), "wrong purchase options id");
	}

	
	/*
	 *         root-> inAppPurID1,                 inAppPurcahsesMTXID1
	 *                 |        |                  |         |       | 
	 * purchaseOptionsID1  inAppPurID3      inAppPurID2, bundleID    inAppPurcahsesMTXID2
	 *                                                |                     |         |
	 *                                      purcahseOptionsMTXID1      inAppPurID4  inAppPurID5
	 *                                       |         |       |
	 *                        purOptionsID2 purOptionsID3  purOptionsMTXID2
	 *                                   					|			|
	 *                                       	    purOptionsID4   purOptionsID5
	 */
	@Test (dependsOnMethods = "addPurcahseOptionsMTXUnderMTX", description = "add inAppPurcahses mtx under mtx")
	public void addInAppPurcahseMTXUnderMTX() throws JSONException, IOException, InterruptedException{
		//add inAppPurchase MIX
		String ipMtx = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		addInAppPurcahseMTXID2 = purchasesApi.addPurchaseItemToBranch(seasonID1, "MASTER", ipMtx, addInAppPurcahseMTXID1, sessionToken);
		Assert.assertFalse(addInAppPurcahseMTXID2.contains("error"), "inAppPurchase MTX was not added to the season: " + addInAppPurcahseMTXID2);
			
		String inAppPurchase = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(inAppPurchase);
		jsonIP.put("name", "IAP4");
		jsonIP.put("namespace", "airlockEntitlement");			
		inAppPurID4 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), addInAppPurcahseMTXID2, sessionToken);
		Assert.assertFalse (inAppPurID4.contains("error"), "Can't add inAppPurcahse: " + inAppPurID4);

		jsonIP.put("name", "IAP5");
		jsonIP.put("namespace", "airlockEntitlement");			
		inAppPurID5 = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), addInAppPurcahseMTXID2, sessionToken);
		Assert.assertFalse (inAppPurID5.contains("error"), "Can't add inAppPurcahse: " + inAppPurID5);
				
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 2, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong inAppPurchase");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(!secondSubPur.containsKey("purchaseOptions"), "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 3, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!secondSubPur.containsKey("configurationRules"), "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchase options id");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(secondSubPur.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		
		JSONObject subMTXInAppPurcahse1 = secondSubPur.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPurcahse2 = secondSubPur.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPurcahse1.getString("uniqueId").equals(inAppPurID2), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse2.getString("uniqueId").equals(bundleID), "wrong sub inAppPurcahse id");
		
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("orderingRules"), "wrong orderingRules");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject poMTXObj = subMTXInAppPurcahse1.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(poMTXObj.getString("uniqueId").equals(purcahseOptionsMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(poMTXObj.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong mtx Purcahse options type");	
		Assert.assertTrue(poMTXObj.getJSONArray("purchaseOptions").size() == 3, "wrong mtx sub Purcahse options size");	
		
		JSONObject subMTXurcahseOptions1 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(0);
		JSONObject subMTXurcahseOptions2 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(1);
		JSONObject subMTXurcahseOptionsMtx = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(2);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID3), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("uniqueId").equals(purcahseOptionsMTXID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong purchase options type");
		
		Assert.assertTrue(subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").size() == 2, "wrong mtx sub Purcahse options size");	
		
		subMTXurcahseOptions1 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(0);
		subMTXurcahseOptions2 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(1);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID4), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID5), "wrong purchase options id");
		
		JSONObject subMTXInAppPurcahseUnderMtx = secondSubPur.getJSONArray("entitlements").getJSONObject(2);
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getString("uniqueId").equals(addInAppPurcahseMTXID2), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").size()==2, "wrong mtx inAppPurcahse sub items size");
		
		JSONObject subMTXInAppPur1 = subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPur2 = subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPur1.getString("uniqueId").equals(inAppPurID4), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPur2.getString("uniqueId").equals(inAppPurID5), "wrong sub inAppPurcahse id");
	}

	@Test (dependsOnMethods = "addInAppPurcahseMTXUnderMTX", description = "add feature attached to sub inAppPurcahse")
	public void addFeatureAttachedToPurchase() throws JSONException, IOException, InterruptedException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("name", "F1");
		jsonF.put("namespace", "ns1");
		jsonF.put("entitlement", inAppPurID5);
		jsonF.put("premium", true);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		jsonF.put("premiumRule", rule);
		String featureID1 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID1.contains("error"), "can create feature attached to entitlements without stored prod id");
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("name","PO6");
		jsonIP.put("namespace", "airlockEntitlement");		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID1, jsonIP.toString(), inAppPurID5, sessionToken);
		Assert.assertFalse (purchaseOptionsID.contains("error"), "Can't add purchaseOptions: " + purchaseOptionsID);

		featureID1 = f.addFeature(seasonID1, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "can create feature");
	}
	
	@Test (dependsOnMethods="addFeatureAttachedToPurchase", description="Add season")
	public void addSeason() throws Exception{
		String season = "{\"minVersion\":\"2.0\"}";
		seasonID2 = s.addSeason(productID, season, sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "Can't add second season: " + seasonID2);
	}
	
	@Test (dependsOnMethods="addSeason", description="Add experiments")
	public void addExperiment() throws Exception{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		expJson.put("name", "experiment1");
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		expJson.put("stage", "PRODUCTION");
		experimentID = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID1 = addBranch(seasonID1, "branch1");
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created in season1: " + branchID1);
		
		String branchID2 = addBranch(seasonID2, "branch1");
		Assert.assertFalse(branchID2.contains("error"), "Branch1 was not created in season2: " + branchID2);

		String variantID = addVariant("variant1", "branch1", "PRODUCTION");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);
		
		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		
	}

	@Test (dependsOnMethods="addExperiment", description="checkout InAppPurchase")
	public void checkoutInAppPurchase() throws Exception{
		String response = br.checkoutFeature(branchID1, inAppPurID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout inAppPurchase");
		
		String iapInBranch = purchasesApi.getPurchaseItemFromBranch(inAppPurID2, branchID1, sessionToken);
		JSONObject iapInBranchObj = new JSONObject(iapInBranch);
		
		iapInBranchObj.put("rolloutPercentage", 40);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID1, branchID1, inAppPurID2, iapInBranchObj.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update inAppPurcahse in branch");
		
		///
		JSONArray inAppPurchases = purchasesApi.getPurchasesBySeasonFromBranch(seasonID1, branchID1, sessionToken);
		Assert.assertTrue(inAppPurchases.size() == 2, "wrong number of inAppPurchases");
		JSONObject firstSubPur = inAppPurchases.getJSONObject(0);
		JSONObject secondSubPur = inAppPurchases.getJSONObject(1);
		
		Assert.assertTrue(firstSubPur.getString("uniqueId").equals(inAppPurID1), "wrong inAppPurchase");
		Assert.assertTrue(firstSubPur.getString("branchStatus").equals("NONE"), "wrong branchStatus");
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong inAppPurchase");
		Assert.assertTrue(secondSubPur.getString("branchStatus").equals("CHECKED_OUT"), "wrong branchStatus");
		
		Assert.assertTrue(firstSubPur.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(!secondSubPur.containsKey("purchaseOptions"), "wrong purchaseOptions");
		
		Assert.assertTrue(firstSubPur.getJSONArray("entitlements").size() == 1, "wrong inAppPurchases");
		Assert.assertTrue(secondSubPur.getJSONArray("entitlements").size() == 3, "wrong inAppPurchases");
		
		Assert.assertTrue(firstSubPur.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!secondSubPur.containsKey("configurationRules"), "wrong configurationRules");
		
		Assert.assertTrue(!firstSubPur.containsKey("features"), "wrong features");
		Assert.assertTrue(!secondSubPur.containsKey("features"), "wrong features");
		
		Assert.assertTrue(!firstSubPur.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!secondSubPur.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject subPurOptions = firstSubPur.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(subPurOptions.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(!subPurOptions.containsKey("features"), "wrong features");
		Assert.assertTrue(!subPurOptions.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(!subPurOptions.containsKey("entitlements"), "wrong inAppPurchases");
		Assert.assertTrue(!subPurOptions.containsKey("purchaseOptions"), "wrong purchaseOptions");
		Assert.assertTrue(subPurOptions.getString("uniqueId").equals(purchaseOptionsID1 ), "wrong purchase options id");
		Assert.assertTrue(subPurOptions.getString("branchStatus").equals("NONE" ), "wrong purchase options branchStatus");
				
		String rootId = purchasesApi.getBranchRootId(seasonID1, "MASTER", sessionToken);
		String rootStr = purchasesApi.getPurchaseItem(rootId, sessionToken);
		JSONObject rootObj = new JSONObject(rootStr);
		Assert.assertTrue(!rootObj.containsKey("purchaseOptions"), "purchaseOptions in root");
			
		JSONObject subInAppPurcahse = firstSubPur.getJSONArray("entitlements").getJSONObject(0);
		Assert.assertTrue(subInAppPurcahse.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subInAppPurcahse.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subInAppPurcahse.containsKey("features"), "wrong features");
		Assert.assertTrue(!subInAppPurcahse.containsKey("orderingRules"), "wrong orderingRules");
		Assert.assertTrue(subInAppPurcahse.getString("uniqueId").equals(inAppPurID3), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subInAppPurcahse.getString("branchStatus").equals("NONE"), "wrong sub inAppPurcahse branchStatus");
		
		Assert.assertTrue(secondSubPur.getString("uniqueId").equals(addInAppPurcahseMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(secondSubPur.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		Assert.assertTrue(secondSubPur.getString("branchStatus").equals("CHECKED_OUT"), "wrong mtx inAppPurcahse branchStatus");
		
		JSONObject subMTXInAppPurcahse1 = secondSubPur.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPurcahse2 = secondSubPur.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPurcahse1.getString("uniqueId").equals(inAppPurID2), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse1.getString("branchStatus").equals("CHECKED_OUT"), "wrong sub inAppPurcahse branchStatus");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getString("uniqueId").equals(bundleID), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahse2.getString("branchStatus").equals("CHECKED_OUT"), "wrong sub inAppPurcahse branchStatus");
		
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse1.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse1.containsKey("orderingRules"), "wrong orderingRules");
		
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("configurationRules").size() == 0, "wrong configurationRules");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("purchaseOptions").size() == 0, "wrong purchaseOptions");
		Assert.assertTrue(subMTXInAppPurcahse2.getJSONArray("entitlements").size() == 0, "wrong inAppPurchases");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("features"), "wrong features");
		Assert.assertTrue(!subMTXInAppPurcahse2.containsKey("orderingRules"), "wrong orderingRules");
		
		JSONObject poMTXObj = subMTXInAppPurcahse1.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertTrue(poMTXObj.getString("uniqueId").equals(purcahseOptionsMTXID1), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(poMTXObj.getString("branchStatus").equals("NONE"), "wrong mtx inAppPurcahse branchStatus");
		Assert.assertTrue(poMTXObj.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong mtx Purcahse options type");	
		Assert.assertTrue(poMTXObj.getJSONArray("purchaseOptions").size() == 3, "wrong mtx sub Purcahse options size");	
		
		JSONObject subMTXurcahseOptions1 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(0);
		JSONObject subMTXurcahseOptions2 = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(1);
		JSONObject subMTXurcahseOptionsMtx = poMTXObj.getJSONArray("purchaseOptions").getJSONObject(2);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions1.getString("branchStatus").equals("NONE"), "wrong purchase options branchStatus");
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID3), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("branchStatus").equals("NONE"), "wrong purchase options branchStatus");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("uniqueId").equals(purcahseOptionsMTXID2), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("branchStatus").equals("NONE"), "wrong purchase options branchStatus");
		Assert.assertTrue(subMTXurcahseOptionsMtx.getString("type").equals("PURCHASE_OPTIONS_MUTUAL_EXCLUSION_GROUP"), "wrong purchase options type");
		
		Assert.assertTrue(subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").size() == 2, "wrong mtx sub Purcahse options size");	
		
		subMTXurcahseOptions1 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(0);
		subMTXurcahseOptions2 = subMTXurcahseOptionsMtx.getJSONArray("purchaseOptions").getJSONObject(1);
		
		Assert.assertTrue(subMTXurcahseOptions1.getString("uniqueId").equals(purchaseOptionsID4), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions1.getString("branchStatus").equals("NONE"), "wrong purchase options branchStatus");	
		Assert.assertTrue(subMTXurcahseOptions2.getString("uniqueId").equals(purchaseOptionsID5), "wrong purchase options id");
		Assert.assertTrue(subMTXurcahseOptions2.getString("branchStatus").equals("NONE"), "wrong purchase options branchStatus");
		
		JSONObject subMTXInAppPurcahseUnderMtx = secondSubPur.getJSONArray("entitlements").getJSONObject(2);
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getString("uniqueId").equals(addInAppPurcahseMTXID2), "wrong mtx inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getString("branchStatus").equals("CHECKED_OUT"), "wrong mtx inAppPurcahse branchStatus");
		
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getString("type").equals("ENTITLEMENT_MUTUAL_EXCLUSION_GROUP"), "wrong mtx inAppPurcahse type");
		Assert.assertTrue(subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").size()==2, "wrong mtx inAppPurcahse sub items size");
		
		JSONObject subMTXInAppPur1 = subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").getJSONObject(0);
		JSONObject subMTXInAppPur2 = subMTXInAppPurcahseUnderMtx.getJSONArray("entitlements").getJSONObject(1);
		Assert.assertTrue(subMTXInAppPur1.getString("uniqueId").equals(inAppPurID4), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPur1.getString("branchStatus").equals("CHECKED_OUT"), "wrong sub inAppPurcahse branchStatus");
		Assert.assertTrue(subMTXInAppPur2.getString("uniqueId").equals(inAppPurID5), "wrong sub inAppPurcahse id");
		Assert.assertTrue(subMTXInAppPur2.getString("branchStatus").equals("CHECKED_OUT"), "wrong sub inAppPurcahse branchStatus");
	}
	
	private String addVariant(String variantName, String branchName, String stage) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		if (stage!=null)
			variantJson.put("stage", stage);
		
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String seasonId, String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonId, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
	}
	
	@AfterTest
	private void reset(){
		//baseUtils.reset(productID, sessionToken);
	}
}
