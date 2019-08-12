package tests.restapi.copy_import.copyPurchasesBetweenMasterAndBranch;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

//when running with branches this is a copy with in the same branch
public class CopyFromBranchToMaster {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementD3;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private BranchesRestApi br ;
	private AnalyticsRestApi an;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		try {			
			srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);			
		}catch (Exception e){
			srcBranchID = null;
		}		
	}
	
	/*
		E1
		E2 -> MIXCR ->CR1, CR2, MIXPO -> PO1
		E3 -> CR3 -> CR4
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Feature was not added to the season");
			
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Feature was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementD3 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(entitlementD3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, configurationMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		String purchaseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		String mixPurchaseOptionsID = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, purchaseOptionsMix, entitlementID2, sessionToken);
		Assert.assertFalse(mixPurchaseOptionsID.contains("error"), "purchaseOptions mix was not added to the season");

		String po1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject jsonPO = new JSONObject(po1);
		jsonPO.put("name", "PO1");
		String orID1 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonPO.toString(), mixPurchaseOptionsID, sessionToken);
		Assert.assertFalse(orID1.contains("error"), "purchaseOptions was not added to the mtx");
		
		jsonCR.put("name", "CR3");
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), entitlementD3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(), configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Copy a none checked out entitlement from barnch to its master")
	public void copyNonCheckedOutEntitlementToMaster() throws IOException, JSONException{
		//copy E2 under E1
		String response = f.copyItemBetweenBranches(entitlementID2, entitlementID1, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID2, entitlementID1, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID2, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
		
		int responseCode = purchasesApi.deletePurchaseItem(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New entitlement was not deleted");
	}
	
	@Test (dependsOnMethods="copyNonCheckedOutEntitlementToMaster", description="Copy a checked out entitlement from barnch to its master")
	public void copyCheckedOutEntitlementToMaster() throws IOException, JSONException{
		String response = br.checkoutFeature(srcBranchID, entitlementID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Entitlement was not checked out: " + response);
		
		//copy E2 under E3		
		response = f.copyItemBetweenBranches(entitlementID2, entitlementD3, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID2, entitlementD3, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID2, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
		
		int responseCode = purchasesApi.deletePurchaseItem(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New entitlement was not deleted");
	}
	
	@Test (dependsOnMethods="copyCheckedOutEntitlementToMaster", description="Copy a new entitlement from barnch to its master")
	public void copyNewEntitlementToMaster() throws IOException, JSONException{
		//checkout E3
		String response = br.checkoutFeature(srcBranchID, entitlementD3, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Entitlement was not checked out: " + response);
		
		//create E4->E5 in branch under E3
		String entitlement4 = FileUtils.fileToString(filePath + "purchases/inAppPurchase4.txt", "UTF-8", false);
		String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement4, entitlementD3, sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "Entitlement was not added to the season");
			
		String entitlement5 = FileUtils.fileToString(filePath + "purchases/inAppPurchase5.txt", "UTF-8", false);
		String entitlementID5 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement5, entitlementID4, sessionToken);
		Assert.assertFalse(entitlementID5.contains("error"), "Entitlement was not added to the season");

		//copy E4 under E1		
		response = f.copyItemBetweenBranches(entitlementID4, entitlementID1, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID4, entitlementID1, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID4, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
		
		int responseCode = purchasesApi.deletePurchaseItem(result.getString("newSubTreeId"), sessionToken);
		Assert.assertTrue(responseCode==200, "New entitlement was not deleted");
	}
	
	@Test (dependsOnMethods="copyNewEntitlementToMaster", description="Copy a new+checkedOut+none entitlements from barnch to its master")
	public void copyMixBranchStatusesToMaster() throws IOException, JSONException{
		//createInMaster E6,CR6 under E1
		//checkout E1 (F6 remains un-checked out) 
		//add E7 and under CR9, ConfigMtx -> CR7, CR8 under E1 in branch
		
		String entitlement6 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement6);
		jsonE.put("description", "entitlement6 desc");
		jsonE.put("name", "entitlement6");
				
		String entitlementID6 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID6.contains("error"), "Entitlement was not added to the season");
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR6");
		String configID6 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),entitlementID1, sessionToken);
		Assert.assertFalse(configID6.contains("error"), "cr was not added to the season");

		String response = br.checkoutFeature(srcBranchID, entitlementID1, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Entitlement was not checked out: " + response);
		
		String entitlement7 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		jsonE = new JSONObject(entitlement7);
		jsonE.put("description", "entitlement7 desc");
		jsonE.put("name", "entitlement7");
				
		String entitlementID7 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID7.contains("error"), "Entitlement was not added to the season");
		
		jsonCR.put("name", "CR9");
		String configID9 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), entitlementID7, sessionToken);
		Assert.assertFalse(configID9.contains("error"), "cr was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID7, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		jsonCR.put("name", "CR7");
		String configID7 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID7.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR8");
		String configID8 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID8.contains("error"), "Feature was not added to the season");
		
		//create E8 in master
		String entitlement8 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		jsonE = new JSONObject(entitlement8);
		jsonE.put("description", "entitlement8 desc");
		jsonE.put("name", "entitlement8");
		
		String entitlementD8 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementD8.contains("error"), "Feature was not added to the season");
		
		//copy E1 from branch to master under E8		
		response = f.copyItemBetweenBranches(entitlementID1, entitlementD8, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID1, entitlementD8, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));		
	}
	
	@Test (dependsOnMethods="copyMixBranchStatusesToMaster", description="Copy a new production entitlement from barnch to its master")
	public void copyNewProductionEntitlementToMaster() throws Exception{
		//create new production entitlement in branch
		String entitlement10 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement10);
		jsonE.put("description", "entitlement 10 desc");
		jsonE.put("name", "entitlement10");
		jsonE.put("stage", "PRODUCTION");
		
		String featureID10 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID10.contains("error"), "Entitlement was not added to the season");
					
		//copy E10 under ROOT
		String rootId = purchasesApi.getBranchRootId(seasonID, BranchesRestApi.MASTER, sessionToken);
		
		String response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, null, sessionToken, srcBranchID, BranchesRestApi.MASTER);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		String dateFormat = an.setDateFormat();
		
		response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, "suffix1", sessionToken, srcBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		
		JSONObject newEntitlementJson = new JSONObject(newEntitlement);
		Assert.assertTrue(newEntitlementJson.getString("stage").equals("DEVELOPMENT"), "copied entitlement is in production");
		
		
		//validate that only master development runtime was changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, srcBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development file was updated");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, srcBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");
		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was not updated");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}