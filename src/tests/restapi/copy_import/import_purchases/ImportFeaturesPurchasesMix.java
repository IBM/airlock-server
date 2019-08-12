package tests.restapi.copy_import.import_purchases;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class ImportFeaturesPurchasesMix {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String eMixID;
	private String mixConfigID1;
	private String featureID1;
	private String fMixID;
	private String mixConfigID2;
	private String configID2;
	private String configID1;
	private String oredringRuleID;
	private String orMixID;
	private String purchaseOptionsID1;
	private String poMixID;
	private String rootID;
	private String purchasesRootID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private InputSchemaRestApi schema;
	private StringsRestApi t;
	private UtilitiesRestApi u;
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
		s = new SeasonsRestApi();
		s.setURL(m_url);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		srcBranchID = BranchesRestApi.MASTER;
		destBranchID = BranchesRestApi.MASTER;
	}
	
	/*
		E1 -> PO1
		   ->PO_MIX 
		   ->CR1
		   ->CR_MIX1
		E_MIX
		F1 -> OR1
		   -> OR_MIX
		   -> CR2
		   -> CR_MIX2
		F_MIX					
	 */
	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");
		
		String eMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		eMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eMix, entitlementID1, sessionToken);
		Assert.assertFalse(eMixID.contains("error"), "entitlements mix was not added to the season: " + eMixID);
		
		String purchaseOptions1 = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, purchaseOptions1, entitlementID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions1 was not added to the season: " + purchaseOptionsID1);

		String poMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		poMixID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, poMix, entitlementID1, sessionToken);
		Assert.assertFalse(poMixID.contains("error"), "purchase options  mix was not added to the season: " + poMixID);
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID1.contains("error"), "Configuration mix was not added to the season: " + mixConfigID1);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, jsonCR.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season:" + configID1);
		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season:" + featureID1);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		fMixID = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(fMixID.contains("error"), "Feature mix was not added to the season: " + fMixID);

		mixConfigID2 = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Configuration mix was not added to the season: " + mixConfigID2);

		jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR2");
		configID2 = f.addFeatureToBranch(seasonID, srcBranchID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule was not added to the season:" + configID2);

		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", "OR1");
		oredringRuleID = f.addFeatureToBranch(seasonID, srcBranchID, jsonOR.toString(), featureID1, sessionToken);
		Assert.assertFalse(oredringRuleID.contains("error"), "Ordering rule was not added to the season: " + oredringRuleID);

		String orMix = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		orMixID = f.addFeatureToBranch(seasonID, srcBranchID, orMix, featureID1, sessionToken);
		Assert.assertFalse(orMixID.contains("error"), "Ordering rule mix was not added to the season: " + orMixID);
		
		purchasesRootID = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		Assert.assertFalse(purchasesRootID.contains("error"), "cannot get purchasesRootID " + purchasesRootID);
		
		rootID = f.getBranchRootId(seasonID, srcBranchID, sessionToken);
		Assert.assertFalse(rootID.contains("error"), "cannot get purchasesRootID " + rootID);
	}
	
	@Test (dependsOnMethods="addComponents", description="Try to import feature under purchase items")
	public void importFeatureUnderPurchaseItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(featureID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, entitlementID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a feature under an entitlement."), "can copy feature under entitlement");
		
		response = f.importFeatureToBranch(itemToImport, eMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a feature under an entitlements mutual exclusion group."), "can copy feature under entitlement mix");
		
		response = f.importFeatureToBranch(itemToImport, purchaseOptionsID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a feature under a purchase options item."), "can copy feature under purchaseOptions");
		
		response = f.importFeatureToBranch(itemToImport, poMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a feature under a purchase options mutual exclusion group."), "can copy feature under purchaseOptions mix");
		
		response = f.importFeatureToBranch(itemToImport, purchasesRootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a feature under the entitlements root."), "can copy feature under purcahses root");
	}
	
	@Test (dependsOnMethods="importFeatureUnderPurchaseItems", description="Try to import features mix under purchase items")
	public void importFeatureMixUnderPurchaseItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(fMixID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, entitlementID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a mutual exclusion group under an entitlement item."), "can copy feature mix under entitlement");
		
		response = f.importFeatureToBranch(itemToImport, eMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a mutual exclusion group under an entitlements mutual exclusion group."), "can copy feature mix under entitlement mix");
		
		response = f.importFeatureToBranch(itemToImport, purchaseOptionsID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a mutual exclusion group under a purchase options item."), "can copy feature mix under purchaseOptions");
		
		response = f.importFeatureToBranch(itemToImport, poMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a mutual exclusion group under a purchase options mutual exclusion group."), "can copy feature mix under purchaseOptions mix");
		
		response = f.importFeatureToBranch(itemToImport, purchasesRootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a mutual exclusion group under the entitlements root."), "can copy feature mix under purcahses root");
	}
	
	@Test (dependsOnMethods="importFeatureMixUnderPurchaseItems", description="Try to import entitlement under feature items")
	public void importEntitlementUnderFeatureItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, featureID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement under feature");
		
		response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement under ordering rules mix");
		
		response = f.importFeatureToBranch(itemToImport, rootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add an entitlement under the features root."), "can copy entitlement under features root");
	}

	@Test (dependsOnMethods="importEntitlementUnderFeatureItems", description="Try to import entitlement mix under feature items")
	public void importEntitlementMixUnderFeatureItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(eMixID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, featureID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement mix under feature");
		
		response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement mix under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement mix under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy entitlement mix under ordering rules mix");
		
		response = f.importFeatureToBranch(itemToImport, rootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add an entitlement mutual exclusion group under the features root."), "can copy entitlement under features root");
	}
	
	@Test (dependsOnMethods="importEntitlementMixUnderFeatureItems", description="Try to import purchase options under feature items")
	public void importPurchaseOptionsUnderFeatureItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(purchaseOptionsID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, featureID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options under feature");
		
		response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options under ordering rules mix");
		
		response = f.importFeatureToBranch(itemToImport, rootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options features root");
	}
	
	@Test (dependsOnMethods="importPurchaseOptionsUnderFeatureItems", description="Try to import purchase options mix under feature items")
	public void importPurchaseOptionsMixUnderFeatureItems() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(poMixID, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, featureID1, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options mix under feature");
		
		response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options mix under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options mix under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options mix under ordering rules mix");
		
		response = f.importFeatureToBranch(itemToImport, rootID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Illegal parent."), "can copy purchase options mtx under features root");
	}
	
	@Test (dependsOnMethods="importPurchaseOptionsMixUnderFeatureItems", description="Try to import configuration rule under illegal parents")
	public void importConfigRuleUnderUllegalParents() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(configID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add configuration rule under mutual exclusion group."), "can copy configuration rule under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, poMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add configuration rule under a purchase options mutual exclusion group."), "can copy configuration rule under purchase options mix");

		response = f.importFeatureToBranch(itemToImport, eMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add configuration rule under an entitlemens mutual exclusion group."), "can copy configuration rule under entitlements mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add configuration rule under ordering rule."), "can copy configuration rule under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add configuration rule under ordering rule mutual exclusion group."), "can copy configuration rule under ordering rules mix");
	}

	@Test (dependsOnMethods="importConfigRuleUnderUllegalParents", description="Try to import configuration rule mix under illegal parents")
	public void importConfigRuleMixUnderUllegalParents() throws IOException, JSONException{
		String itemToImport = purchasesApi.getPurchaseItemFromBranch(mixConfigID1, srcBranchID, sessionToken);
		
		String response = f.importFeatureToBranch(itemToImport, fMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a configuration mutual exclusion group under a mutual exclusion group."), "can copy configuration rule under feature mix");
		
		response = f.importFeatureToBranch(itemToImport, poMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a configuration mutual exclusion group under a purchase options mutual exclusion group."), "can copy configuration rule under purchase options mix");
		
		response = f.importFeatureToBranch(itemToImport, eMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a configuration mutual exclusion group under an entitlemens mutual exclusion group."), "can copy configuration rule under entitlements mix");
		
		response = f.importFeatureToBranch(itemToImport, oredringRuleID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a configuration mutual exclusion group under an ordering rule."), "can copy configuration rule under ordering rule");
		
		response = f.importFeatureToBranch(itemToImport, orMixID, "ACT", null, "suffix1", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("error")  && response.contains("Cannot add a configuration mutual exclusion group under an ordering rule mutual exclusion group."), "can copy configuration rule under ordering rules mix");System.out.println(response);
	}
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}