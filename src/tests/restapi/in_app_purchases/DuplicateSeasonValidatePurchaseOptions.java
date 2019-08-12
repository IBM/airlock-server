package tests.restapi.in_app_purchases;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class DuplicateSeasonValidatePurchaseOptions {

    private String productID;
    private String seasonID;
    private String seasonID2;
    private JSONObject eJson;
    private JSONObject poJson;
    private String filePath;
    private SeasonsRestApi s;
    private String m_url;
    private String sessionToken = "";
    private AirlockUtils baseUtils;
    private BranchesRestApi br ;
    private InAppPurchasesRestApi purchasesApi;
    private String entitlementID1;
    private String purchaseOptionsID1;
    private String purchaseOptionsID2;
    private String poMixID;
    private String poInNewSeason1ID;
    private String poInNewSeason2ID;
    private ExperimentsRestApi exp ;
    
    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        m_url = url;
        filePath = configPath ;
        s = new SeasonsRestApi();
        s.setURL(m_url);
        purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
        br = new BranchesRestApi();
        br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);
        String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
        eJson = new JSONObject(entitlement);
        
        String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
        poJson = new JSONObject(purchaseOptions);
    }


    @Test(description ="E1 -> PO_MTX->PO1, PO2")
    public void addComponents() throws Exception {
        //add entitlement with purchaseOptions
        eJson.put("name", "E1");
        eJson.put("stage", "PRODUCTION");
        entitlementID1 = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
        Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);

        String purchaseOptionsMtx = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
        poMixID = purchasesApi.addPurchaseItem(seasonID, purchaseOptionsMtx, entitlementID1, sessionToken);
        Assert.assertFalse(poMixID.contains("error"), "purchaseOptions mutual was not added to the season: " + poMixID);

        poJson.put("name", "PO1");
        poJson.put("stage", "PRODUCTION");
        JSONArray storeProductIdsArr = new JSONArray();
		JSONObject storeProdId1 = new JSONObject();
		storeProdId1.put("storeType", "Apple App Store");
		storeProdId1.put("productId", "1");
		storeProductIdsArr.add(storeProdId1);
		JSONObject storeProdId2 = new JSONObject();
		storeProdId2.put("storeType", "Google Play Store");
		storeProdId2.put("productId", "2");
		storeProductIdsArr.add(storeProdId2);
		poJson.put("storeProductIds", storeProductIdsArr);	
        purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, poJson.toString(), poMixID, sessionToken);
        Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID1);

        poJson.put("name", "PO2");
        poJson.put("stage", "PRODUCTION");
        storeProductIdsArr = new JSONArray();
		storeProdId2 = new JSONObject();
		storeProdId2.put("storeType", "Google Play Store");
		storeProdId2.put("productId", "12345");
		storeProductIdsArr.add(storeProdId2);
		poJson.put("storeProductIds", storeProductIdsArr);	
        purchaseOptionsID2 = purchasesApi.addPurchaseItem(seasonID, poJson.toString(), poMixID, sessionToken);
        Assert.assertFalse(purchaseOptionsID2.contains("error"), "purchaseOptions was not added to the season: " + purchaseOptionsID2);
    }

    @Test(dependsOnMethods = "addComponents")
    public void duplicateSeasonWithVariantsFromBranches () throws Exception {
        String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
        seasonID2 = s.addSeason(productID, season, sessionToken);
       
        //verify purchase items in new season
        String response = purchasesApi.getAllPurchaseItems(seasonID2, season);
        response = purchasesApi.getAllPurchaseItems(seasonID2, sessionToken);
		JSONObject allPI = new JSONObject(response);
		JSONArray eArr = allPI.getJSONObject("entitlementsRoot").getJSONArray("entitlements");
		
		Assert.assertTrue(eArr.size() == 1, "wrong entitlements number ");
		JSONObject entitlementInNewSeason = eArr.getJSONObject(0);
		Assert.assertTrue(entitlementInNewSeason.getString("name").equals("E1"), "wrong entitlements name ");
		Assert.assertFalse(entitlementInNewSeason.getString("uniqueId").equals(entitlementID1), "wrong entitlements id ");
		Assert.assertTrue(entitlementInNewSeason.getJSONArray("purchaseOptions").size() == 1, "wrong purchaseOptions number ");
		
		JSONObject purchaseOptionsMixInNewSeason = entitlementInNewSeason.getJSONArray("purchaseOptions").getJSONObject(0);
		Assert.assertFalse(purchaseOptionsMixInNewSeason.getString("uniqueId").equals(poMixID), "wrong purchaseOptions mix id");
		Assert.assertTrue(purchaseOptionsMixInNewSeason.getJSONArray("purchaseOptions").size() == 2, "wrong purchaseOptions number ");
	
		JSONObject po1InNewSeason = purchaseOptionsMixInNewSeason.getJSONArray("purchaseOptions").getJSONObject(0);
		JSONObject po2InNewSeason = purchaseOptionsMixInNewSeason.getJSONArray("purchaseOptions").getJSONObject(1);
		
		Assert.assertTrue(po1InNewSeason.getString("name").equals("PO1"), "wrong purchaseOptions name ");
		Assert.assertFalse(po1InNewSeason.getString("uniqueId").equals(purchaseOptionsID1), "wrong purchaseOptions id ");
		Assert.assertTrue(po1InNewSeason.getJSONArray("storeProductIds").size() == 2, "wrong storeProductIds size ");
		
		Assert.assertTrue(po2InNewSeason.getString("name").equals("PO2"), "wrong purchaseOptions name ");
		Assert.assertFalse(po2InNewSeason.getString("uniqueId").equals(purchaseOptionsID2), "wrong purchaseOptions id ");
		Assert.assertTrue(po2InNewSeason.getJSONArray("storeProductIds").size() == 1, "wrong storeProductIds size ");
		
		poInNewSeason1ID = po1InNewSeason.getString("uniqueId");
		poInNewSeason2ID = po2InNewSeason.getString("uniqueId");
    }
    
  
    @Test(dependsOnMethods = "duplicateSeasonWithVariantsFromBranches")
    public void validatePurchaseOptionsDuplication () throws Exception {
    		String response = purchasesApi.getPurchaseItemFromBranch(poInNewSeason1ID, "MASTER", sessionToken);
    		JSONObject po1InNewSeason = new JSONObject(response);
    		JSONArray storeProductIdsArr = new JSONArray();
    		JSONObject storeProdId1 = new JSONObject();
    		storeProdId1.put("storeType", "Google Play Store");
    		storeProdId1.put("productId", "abcdefg");
    		storeProductIdsArr.add(storeProdId1);
    		po1InNewSeason.put("storeProductIds", storeProductIdsArr);
    		
    		response = purchasesApi.updatePurchaseItem(seasonID2, poInNewSeason1ID, po1InNewSeason.toString(), sessionToken);
    		Assert.assertFalse(response.contains("error"), "cannot update purchaseOptions:" + response);
    		
    		response = purchasesApi.getPurchaseItemFromBranch(poInNewSeason1ID, "MASTER", sessionToken);
    		po1InNewSeason = new JSONObject(response);
    		Assert.assertTrue(po1InNewSeason.getJSONArray("storeProductIds").size() == 1, "wrong storeProductIds size ");
    		Assert.assertTrue(po1InNewSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("storeType").equals("Google Play Store"), "wrong storeProductIds storeType ");
    		Assert.assertTrue(po1InNewSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("productId").equals("abcdefg"), "wrong storeProductIds productId ");
    		
    		response = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, "MASTER", sessionToken);
    		JSONObject po1InOldSeason = new JSONObject(response);
    		Assert.assertTrue(po1InOldSeason.getJSONArray("storeProductIds").size() == 2, "wrong storeProductIds size ");
    		Assert.assertTrue(po1InOldSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("storeType").equals("Apple App Store"), "wrong storeProductIds storeType ");
    		Assert.assertTrue(po1InOldSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("productId").equals("1"), "wrong storeProductIds productId ");
    		
    		Assert.assertTrue(po1InOldSeason.getJSONArray("storeProductIds").getJSONObject(1).getString("storeType").equals("Google Play Store"), "wrong storeProductIds storeType ");
    		Assert.assertTrue(po1InOldSeason.getJSONArray("storeProductIds").getJSONObject(1).getString("productId").equals("2"), "wrong storeProductIds productId ");
    		
    		response = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID2, "MASTER", sessionToken);
    		JSONObject po2InOldSeason = new JSONObject(response);
    		po2InOldSeason.put("storeProductIds",  new JSONArray());
    		
    		response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID2, po2InOldSeason.toString(), sessionToken);
    		Assert.assertFalse(response.contains("error"), "cannot update purchaseOptions:" + response);
    		
    		response = purchasesApi.getPurchaseItemFromBranch(poInNewSeason2ID, "MASTER", sessionToken);
    		JSONObject po2InNewSeason = new JSONObject(response);
    		Assert.assertTrue(po2InNewSeason.getJSONArray("storeProductIds").size() == 1, "wrong storeProductIds size ");
    		Assert.assertTrue(po2InNewSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("storeType").equals("Google Play Store"), "wrong storeProductIds storeType ");
    		Assert.assertTrue(po2InNewSeason.getJSONArray("storeProductIds").getJSONObject(0).getString("productId").equals("12345"), "wrong storeProductIds productId ");
    		
    		response = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID2, "MASTER", sessionToken);
    		po2InOldSeason = new JSONObject(response);
    		Assert.assertTrue(po2InOldSeason.getJSONArray("storeProductIds").size() == 0, "wrong storeProductIds size ");
    }
    
    @AfterTest
    private void reset(){
        baseUtils.reset(productID, sessionToken);
    }
}

