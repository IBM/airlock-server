package tests.restapi.in_app_purchases;
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
import tests.restapi.InAppPurchasesRestApi;import tests.restapi.validations.feature.FeatureValidateJsonStructure;


public class TestPurchasesTreeStructureNegative2 {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	protected InAppPurchasesRestApi purchasesApi;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	private JSONObject fJson;
	private JSONObject jsonCR;
	private JSONObject orderingRuleJson;
	private String inAppPurchaseID1;
	private String purchaseOptionsID1;
	private String inAppPurchaseMixID1;
	private String purchaseOptionsMixID1;
	private String featureID1;
	private String featureMixID;
	private String orderingRuleMixID;
	private String orderingRuleID;
	private String configRuleMixID;
	private String configRuleID;
	private String inAppPurchaseMix;
	private String purcahseOptionsMix;
	private String featureMix;
	private String orderingRuleMix;
	private String configRuleMix;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		fJson = new JSONObject(feature);
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		orderingRuleJson = new JSONObject(orderingRule);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		jsonCR = new JSONObject(configuration);
		
		inAppPurchaseMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		purcahseOptionsMix = FileUtils.fileToString(filePath + "purchases/purchaseOptionsMutual.txt", "UTF-8", false);
		featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		orderingRuleMix = FileUtils.fileToString(filePath + "orderingRule/mtxOrderingRule.txt", "UTF-8", false);
		configRuleMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		

	}

	@Test (description ="Add purchase options under root") 
	public void AddPurchaseOptionsUnderRoot() throws Exception{
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), "ROOT", sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under root");
	}
	
	@Test (dependsOnMethods = "AddPurchaseOptionsUnderRoot", description ="Add feature and in app purchase under root") 
	public void AddFeatureAndInAppPurcahseUnderRoot() throws Exception{
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "can not add feature under root: " + featureID1);
		
		String rootId = f.getRootId(seasonID, sessionToken);
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), rootId, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchaseID under root with features");
	}
	
	@Test (dependsOnMethods = "AddFeatureAndInAppPurcahseUnderRoot", description ="Add purchase options and in app purchase under feature") 
	public void AddPurchasesUnderFeature() throws Exception{
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), featureID1, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under feature");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), featureID1, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under feature");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, featureID1, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under feature");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, featureID1, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under feature");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderFeature", description ="Add purchase options and in app purchase under feature mtx") 
	public void AddPurchasesUnderFeatureMTX() throws Exception{
		featureMixID = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(featureMixID.contains("error"), "can not add feature under root: " + featureMixID);
		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), featureMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under feature mtx");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), featureMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under feature mtx");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, featureMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under feature mtx");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, featureMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under feature mtx");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderFeature", description ="Add purchase options and in app purchase under ordering rule") 
	public void AddPurchasesUnderOrderingRule() throws Exception{
		orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), featureID1, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "can not add ordering rule under feature: " + orderingRuleID);
		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), orderingRuleID, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under ordering rule");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), orderingRuleID, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under ordering rule");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, orderingRuleID, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under ordering rule");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, orderingRuleID, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under ordering rule");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderOrderingRule", description ="Add purchase options and in app purchase under ordering rule mtx") 
	public void AddPurchasesUnderOrderingRuleMTX() throws Exception{
		orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, featureID1, sessionToken);
		Assert.assertFalse(orderingRuleMixID.contains("error"), "can add ordering rule mtx under feature: " + orderingRuleMixID);
		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), orderingRuleMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under ordering rule mtx");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), orderingRuleMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under ordering rule mtx");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, orderingRuleMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under ordering rule mtx");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, orderingRuleMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under ordering rule mtx");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderOrderingRuleMTX", description ="Add purchase options and in app purchase under feature config rule") 
	public void AddPurchasesUnderFeatureConfigRule() throws Exception{
		configRuleID = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configRuleID.contains("error"), "can not add config rule under feature: " + configRuleID);
		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), configRuleID, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under feature config rule");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), configRuleID, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under feature config rule");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, configRuleID, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under feature config rule");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, configRuleID, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under feature config rule");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderFeatureConfigRule", description ="Add purchase options and in app purchase under faeture config rule mtx") 
	public void AddPurchasesUnderFeatureConfigRuleMTX() throws Exception{
		configRuleMixID = f.addFeature(seasonID, configRuleMix, featureID1, sessionToken);
		Assert.assertFalse(configRuleMixID.contains("error"), "can add ordering rule mtx under feature: " + configRuleMixID);
		
		String purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), configRuleMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsID.contains("error"), "can add purchase options under feature config rule mtx");
		
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), configRuleMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add inAppPurchase under feature config rule mtx");
		
		String inAppPurchaseMTXID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, configRuleMixID, sessionToken);
		Assert.assertTrue(inAppPurchaseMTXID.contains("error"), "can add inAppPurchaseMtx under feature config rule mtx");
		
		String purchaseOptionsMTXID = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, configRuleMixID, sessionToken);
		Assert.assertTrue(purchaseOptionsMTXID.contains("error"), "can add purcahseOptionsMix under feature config rule mtx");
	}
	
	@Test (dependsOnMethods = "AddPurchasesUnderFeatureConfigRuleMTX", description ="Add inAppPurcahse and faeture items under it") 
	public void AddFeatureItemsUnderInAppPurcahse() throws Exception{
		
		String purchaseRootId = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), purchaseRootId, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add faetuer under purchases root");
		
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "can not add inAppPurchase under root: " + inAppPurchaseID1);
		
		featureID = f.addFeature(seasonID, fJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under in app purchase");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under in app purchase");
		
		String featureMixID = f.addFeature(seasonID, featureMix, inAppPurchaseID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add feetuer mix under in app purchase");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, inAppPurchaseID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix under in app purchase");
	}
	
	@Test (dependsOnMethods = "AddFeatureItemsUnderInAppPurcahse", description ="Add inAppPurcahse mix and feature items under it") 
	public void AddFeatureItemsUnderInAppPurcahseMix() throws Exception{
		
		inAppPurchaseMixID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(inAppPurchaseMixID1.contains("error"), "can not add inAppPurchase mtx under inAppPurchase: " + inAppPurchaseID1);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), inAppPurchaseMixID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under in app purchase mix");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), inAppPurchaseMixID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under in app purchase mix");
		
		String featureMixID = f.addFeature(seasonID, featureMix, inAppPurchaseMixID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add featuer mix under in app purchase mix");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, inAppPurchaseMixID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix under in app purchase mix");
	}
	
	@Test (dependsOnMethods = "AddFeatureItemsUnderInAppPurcahseMix", description ="Add purcahse options and feature items under it") 
	public void AddFeatureItemsUnderPurcahseOptions() throws Exception{
		
		purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID, purOptJson.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "can not add purchaseOptions under inAppPurchase: " + purchaseOptionsID1);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), purchaseOptionsID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under purchaseOptions");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), purchaseOptionsID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under purchaseOptions");
		
		String featureMixID = f.addFeature(seasonID, featureMix, purchaseOptionsID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add featuer mix under purchaseOptions");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, purchaseOptionsID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix underpurchaseOptions");
	}
	
	@Test (dependsOnMethods = "AddFeatureItemsUnderPurcahseOptions", description ="Add purcahse options mix and feature items under it") 
	public void AddFeatureItemsUnderPurcahseOptionsMix() throws Exception{
		
		purchaseOptionsMixID1 = purchasesApi.addPurchaseItem(seasonID, purcahseOptionsMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "can not add purchaseOptions mtx under inAppPurchase: " + purchaseOptionsID1);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under purchaseOptions mix");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under purchaseOptions mix");
		
		String featureMixID = f.addFeature(seasonID, featureMix, purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add featuer mix under purchaseOptions mix");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix underpurchaseOptions mix");
	}
	

	@Test (dependsOnMethods = "AddFeatureItemsUnderPurcahseOptionsMix", description ="Add purcahse config and feature items under it") 
	public void AddFeatureItemsUnderPurcahseConfig() throws Exception{
		jsonCR.put("name", "CR11");
		
		String purchaseConfigID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseConfigID1.contains("error"), "can not add purchase config under inAppPurchase: " + purchaseConfigID1);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), purchaseConfigID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under purchase config");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), purchaseConfigID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under purchase config");
		
		String featureMixID = f.addFeature(seasonID, featureMix, purchaseConfigID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add featuer mix under purchase config");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, purchaseConfigID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix under purchase config");
	}
	
	@Test (dependsOnMethods = "AddFeatureItemsUnderPurcahseConfig", description ="Add purcahse config mix and feature items under it") 
	public void AddFeatureItemsUnderPurcahseConfigMix() throws Exception{
		
		String purchaseConfigMixID1 = purchasesApi.addPurchaseItem(seasonID, configRuleMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(purchaseConfigMixID1.contains("error"), "can not add purchase config mtx under inAppPurchase: " + purchaseConfigMixID1);
		
		String featureID = f.addFeature(seasonID, fJson.toString(), purchaseConfigMixID1, sessionToken);
		Assert.assertTrue(featureID.contains("error"), "can add featuer under purchaseOptions mix");
		
		String orderingRuleID = f.addFeature(seasonID, orderingRuleJson.toString(), purchaseConfigMixID1, sessionToken);
		Assert.assertTrue(orderingRuleID.contains("error"), "can add ordering rule under purchaseOptions mix");
		
		String featureMixID = f.addFeature(seasonID, featureMix, purchaseConfigMixID1, sessionToken);
		Assert.assertTrue(featureMixID.contains("error"), "can add featuer mix under purchaseOptions mix");
		
		String orderingRuleMixID = f.addFeature(seasonID, orderingRuleMix, purchaseConfigMixID1, sessionToken);
		Assert.assertTrue(orderingRuleMixID.contains("error"), "can add ordering rule mix underpurchaseOptions mix");
	}
	
	@Test (dependsOnMethods = "AddFeatureItemsUnderPurcahseConfigMix", description ="Add inAppPurcahse  under purchase options") 
	public void addInAppPurcahseUnderPurchaseOptions() throws Exception{
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), purchaseOptionsID1, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add in app purchase under purchaseOptions");
		
		String inAppPurchaseMixID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, purchaseOptionsID1, sessionToken);
		Assert.assertTrue(inAppPurchaseMixID.contains("error"), "can add in app purchase mix under purchaseOptions mix");
		
		inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(inAppPurchaseID.contains("error"), "can add in app purchase under purchaseOptions");
		
		inAppPurchaseMixID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseMix, purchaseOptionsMixID1, sessionToken);
		Assert.assertTrue(inAppPurchaseMixID.contains("error"), "can add in app purchase mix under purchaseOptions mix");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
