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
public class CopyFromMasterToBranch {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String targetEntitlementID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private BranchesRestApi br ;
	private AnalyticsRestApi an;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String destBranchID;
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
			destBranchID = baseUtils.createBranchInExperiment(analyticsUrl);			
		}catch (Exception e){
			destBranchID = null;
		}		
	}
	
	/*
		E1 -> MIX	->E2 -> MIXCR ->CR1, CR2
					->E3 -> CR3 -> CR4
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");
			
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");

		String entitlement3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		entitlementID3 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, entitlement3, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "Entitlement was not added to the season");

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

		jsonCR.put("name", "CR3");
		String configID3 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),entitlementID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Copy a entitlement from master under root in branch")
	public void copyEntitlementUnderRoot() throws IOException, JSONException{
		//copy E2 under ROOT
		String rootId = f.getBranchRootId(seasonID, destBranchID, sessionToken);		
		
		String response = f.copyItemBetweenBranches(entitlementID2, rootId, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID2, entitlementID1, "ACT", null, "suffix1", sessionToken, destBranchID, BranchesRestApi.MASTER);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), BranchesRestApi.MASTER, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID2, destBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));				
	}
	
	@Test (dependsOnMethods="copyEntitlementUnderRoot", description="Copy a entitlement from master under new entitlement in branch")
	public void copyEntitlementUnderNew() throws IOException, JSONException{
		//create E4 in branch
		String rootId = purchasesApi.getBranchRootId(seasonID, destBranchID, sessionToken);		
		
		String entitlement4 = FileUtils.fileToString(filePath + "purchases/inAppPurchase4.txt", "UTF-8", false);
		String entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, destBranchID, entitlement4, rootId, sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "Entitlement was not added to the season:" + entitlementID4);
		
		//copy E2 under E4		
		String response = f.copyItemBetweenBranches(entitlementID2, entitlementID4, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(entitlementID2, entitlementID4, "ACT", null, "suffix2", sessionToken, BranchesRestApi.MASTER, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(entitlementID2, BranchesRestApi.MASTER, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));			
	}
	
	@Test (dependsOnMethods="copyEntitlementUnderNew", description="Copy a entitlement from master under none entitlement in branch")
	public void copyEntitlementUnderNone() throws IOException, JSONException{
		//copy E2 under E4		
		String response = f.copyItemBetweenBranches(entitlementID2, entitlementID1, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("Cannot paste an item under an item that is not checked out"), "Entitlement was copied under none entitlement");						
	}
	
	@Test (dependsOnMethods="copyEntitlementUnderNone", description="Copy a production entitlement from master to branch")
	public void copyProductionFeatureToBranch() throws Exception{
		//create new production feature in master
		String entitlement10 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement10);
		jsonE.put("description", "entitlement 10 desc");
		jsonE.put("name", "entitlement10");
		jsonE.put("stage", "PRODUCTION");
		
		String featureID10 = purchasesApi.addPurchaseItemToBranch(seasonID, BranchesRestApi.MASTER, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID10.contains("error"), "Entitlement was not added to the season");
					
		an.setSleep();
		
		//copy E10 under ROOT
		String rootId = purchasesApi.getBranchRootId(seasonID, destBranchID, sessionToken);
		
		String response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, null, sessionToken, BranchesRestApi.MASTER, destBranchID);					
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		String dateFormat = an.setDateFormat();
		
		response = f.copyItemBetweenBranches(featureID10, rootId, "ACT", null, "suffix1", sessionToken, BranchesRestApi.MASTER, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement tree was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), destBranchID, sessionToken);
		
		JSONObject newEntitlementJson = new JSONObject(newEntitlement);
		Assert.assertTrue(newEntitlementJson.getString("stage").equals("DEVELOPMENT"), "copied entitlement is in production");
		Assert.assertTrue(newEntitlementJson.getString("branchStatus").equals("NEW"), "copied string is not in status new");
		
		//validate that only master development runtime was changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, destBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development file was updated");
				
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, destBranchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was updated");
		
		responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development file was not updated");
		
		responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production file was changed");
	}
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}