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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class DeleteProductWithProductionPurchaseItems {
	private String productID;
	private String seasonID;
	private String entitlementID1;
	private String filePath;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private SeasonsRestApi s;
	private InAppPurchasesRestApi purchasesApi;
	private String puOptID;
	private ProductsRestApi p;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		br = new BranchesRestApi();
		br.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description ="Add entitlements to master and add new branch") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		
		JSONObject entitlement = new JSONObject(FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false));
		entitlement.put("stage", "PRODUCTION");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, entitlement.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement was not added to the season: " + entitlementID1);
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonIP = new JSONObject(purchaseOptions);
		jsonIP.put("stage", "DEVELOPMENT");
		puOptID = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonIP.toString(), entitlementID1, sessionToken);
		Assert.assertFalse (puOptID.contains("error"), "Can't add purchaseOptions: " + puOptID);
	}

	
	@Test(dependsOnMethods="addComponents", description ="Delete season/product with production entitlement")
	public void deleteSeasonWithProdEntitlement() throws Exception{
		int codeResponse = s.deleteSeason(seasonID);
		Assert.assertFalse(codeResponse == 200, "can delete season with prod entitlment");
		
		codeResponse = p.deleteProduct(productID);
		Assert.assertFalse(codeResponse == 200, "can delete product with prod entitlment");
	}
	
	@Test(dependsOnMethods="deleteSeasonWithProdEntitlement", description ="Delete season/product with production purchaseOptions")
	public void deleteSeasonWithProdPurchaseOptions() throws Exception{
		JSONObject ent = new JSONObject(purchasesApi.getPurchaseItem(entitlementID1, sessionToken));
		ent.put("stage", "DEVELOPMENT");
		String res = purchasesApi.updatePurchaseItem(seasonID, entitlementID1, ent.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update entitlement: " + res);
		
		JSONObject po = new JSONObject(purchasesApi.getPurchaseItem(puOptID, sessionToken));
		po.put("stage", "PRODUCTION");
		res = purchasesApi.updatePurchaseItem(seasonID, puOptID, po.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update purchaseOptions: " + res);
		
		int codeResponse = s.deleteSeason(seasonID);
		Assert.assertFalse(codeResponse == 200, "can delete season with prod purchaseOptions");
		
		codeResponse = p.deleteProduct(productID);
		Assert.assertFalse(codeResponse == 200, "can delete product with prod purchaseOptions");
	}
	
	@Test(dependsOnMethods="deleteSeasonWithProdPurchaseOptions", description ="Delete product with no production items")
	public void deleteProductWithNoProd() throws Exception{
		JSONObject po = new JSONObject(purchasesApi.getPurchaseItem(puOptID, sessionToken));
		po.put("stage", "DEVELOPMENT");
		String res = purchasesApi.updatePurchaseItem(seasonID, puOptID, po.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot update purchaseOptions: " + res);
		
		
		int codeResponse = p.deleteProduct(productID);
		Assert.assertTrue(codeResponse == 200, "cannot delete product with no prod items");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
