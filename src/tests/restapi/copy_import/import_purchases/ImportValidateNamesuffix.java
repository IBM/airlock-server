package tests.restapi.copy_import.import_purchases;

import java.io.IOException;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;


public class ImportValidateNamesuffix {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementID2;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
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
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
	}
	
	@Test (description="Add two entitlement")
	public void addComponents() throws IOException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not added to the season");
	}
	
	@Test (dependsOnMethods="addComponents", description="Use illegal characters in the namesuffix")
	public void simulateInvalidSuffixName() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "VALIDATE", null, "-new$suffix", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was imported with illegal characters in name ");
	}
	
	@Test (dependsOnMethods="simulateInvalidSuffixName", description="Use illegal characters in the namesuffix")
	public void invalidSuffixName() throws IOException{
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, entitlementID2, "ACT", null, "-new$suffix", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Entitlement was imported with illegal characters in name ");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}