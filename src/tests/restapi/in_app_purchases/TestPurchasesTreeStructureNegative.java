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
import tests.restapi.InAppPurchasesRestApi;


public class TestPurchasesTreeStructureNegative {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	protected InAppPurchasesRestApi purchasesApi;
	
	
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
	}

	@Test (description ="Add inAppPurchase. Then add configuration using features api") 
	public void AddConfigToPurchaseItemUsingFeatureApi() throws Exception{
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		String inAppPurchaseID = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID.contains("error"), "inAppPurchase was not added: " + inAppPurchaseID);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		
		String configID = f.addFeatureToBranch(seasonID, "MASTER", jsonCR.toString(), inAppPurchaseID, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to purchaseItem using features api");
		
		configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), inAppPurchaseID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to purchaseItem using purchases api");
		
		jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		
		String configID2 = f.addFeatureToBranch(seasonID, "MASTER", jsonCR.toString(), configID, sessionToken);
		Assert.assertTrue(configID2.contains("error"), "Configuration rule was added to purchaseItem using features api");
		
	}
	
	@Test (dependsOnMethods="AddConfigToPurchaseItemUsingFeatureApi", description ="Add feature. Then add configuration using purchases api") 
	public void AddConfigToFeatureItemUsingPurchasesApi() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		String featureID = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "cannot create feature: " + featureID);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR1");
		
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuration rule was added to feature using purcahses api");
		
		jsonCR.put("name", "CR12");
		
		configID = f.addFeature(seasonID, jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuration rule was not added to feature using features api");
		
		jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, "MASTER", jsonCR.toString(), configID, sessionToken);
		Assert.assertTrue(configID2.contains("error"), "Configuration rule was added to featuer using purchases api");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
