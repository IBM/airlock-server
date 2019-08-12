package tests.restapi.in_app_purchases;

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
import tests.restapi.InAppPurchasesRestApi;


public class CancelCheckoutWithSubPurchaseItems {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String inAppPurchaseID3;
	private String mixID1;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
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
	
	/*
	PI1 -> MIXPI	->PI2 -> MIXCR ->CR1, CR2
				->PI3 -> CR3 -> CR4
				
	- checkout PI2 - mix and PI1 are also checked out
	- cancel checkout mix with subPurchaseItems
	
	- checkout PI2 - mix and PI1 are also checked out
	- cancel checkout PI1
	
	- checkout PI2 and checkout root
	- cancel checkout root with subPurchaseItems

 */
		
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchID =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		
		String inAppPurchaseStr = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr, "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season");
		
		String piMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, piMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "inAppPurchase mix was not added to the season: " + mixID1);
		
		String inAppPurchaseStr2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr2, mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "inAppPurchase was not added to the season");

		String inAppPurchaseStr3 = FileUtils.fileToString(filePath + "purchases/inAppPurchase3.txt", "UTF-8", false);
		inAppPurchaseID3 = purchasesApi.addPurchaseItem(seasonID, inAppPurchaseStr3, mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID3.contains("error"), "inAppPurchase was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration was not added to the season");
				
		jsonCR.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration was not added to the season");

		jsonCR.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(),inAppPurchaseID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration was not added to the season");

		jsonCR.put("name", "CR4");
		configID4 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration was not added to the season");
	}
	
	@Test (dependsOnMethods = "addComponents", description="Cancel checkout of MIX")
	public void uncheckMix() throws IOException, JSONException{
		//checkout PI2, it also checks out PI1 & mix
		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "inAppPurchase type was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect inAppPurchase branchStatus ");
		
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, mixID1, sessionToken);
		Assert.assertTrue(res.equals("{}"), "MIX was not unchecked: " + response);
		
		JSONObject pi1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(pi1.getString("branchStatus").equals("CHECKED_OUT"), "PI1 status is not CHECKED_OUT");
		JSONObject pi2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(pi2.getString("branchStatus").equals("NONE"), "PI2 status is not NONE");
		JSONObject pi3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(pi3.getString("branchStatus").equals("NONE"), "PI3 status is not NONE");
		JSONObject configmix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		JSONObject config1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		JSONObject config2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		JSONObject config3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		JSONObject config4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	}
	
	@Test (dependsOnMethods = "uncheckMix", description="Cancel checkout of parent inAppPurchase PI1")
	public void uncheckParentInAppPurchase() throws IOException, JSONException{
		//checkout PI2, it also checks out PI1 & mix
		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "inAppPurchase2 was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect inAppPurchase branchStatus ");
		
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("CHECKED_OUT"), "MTX status is not CHECKED_OUT");
		JSONObject pi1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(pi1.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not CHECKED_OUT");
		JSONObject pi2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(pi2.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not CHECKED_OUT");
		JSONObject pi3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(pi3.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase3 status is not CHECKED_OUT");
		JSONObject configmix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("CHECKED_OUT"), "Configuration mtx status is not CHECKED_OUT");
		JSONObject config1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("CHECKED_OUT"), "Configuration1  status is not CHECKED_OUT");
		JSONObject config2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("CHECKED_OUT"), "Configuration2  status is not CHECKED_OUT");
		JSONObject config3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("CHECKED_OUT"), "Configuration3  status is not CHECKED_OUT");
		JSONObject config4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("CHECKED_OUT"), "Configuration4  status is not CHECKED_OUT");
	
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertTrue(res.equals("{}"), "inAppPurchaseID1 was not unchecked: " + response);
		
		mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "MTX status is not NONE");
		pi2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(pi2.getString("branchStatus").equals("NONE"), "inAppPurchase2 status is not NONE");
		pi3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(pi3.getString("branchStatus").equals("NONE"), "inAppPurchase3 status is not NONE");
		configmix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		config1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		config2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		config3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		config4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	
	}
	
	@Test (dependsOnMethods = "uncheckParentInAppPurchase", description="Cancel checkout of root")
	public void uncheckRoot() throws IOException, JSONException{
		//checkout F2, it also checks out F1 & mix
		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "inAppPurchaseID2 was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect inAppPurchase branchStatus ");
		
		//checkout root
		String rootID = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		response = br.checkoutFeature(branchID, rootID, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Root was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect root branchStatus ");

		
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, rootID, sessionToken);
		Assert.assertTrue(res.equals("{}"), "Root was not unchecked: " + response);
		
		JSONObject pi1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		Assert.assertTrue(pi1.getString("branchStatus").equals("NONE"), "PI1 status is not NONE");
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "MTX status is not NONE");
		JSONObject pi2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		Assert.assertTrue(pi2.getString("branchStatus").equals("NONE"), "PI2 status is not NONE");
		JSONObject pi3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID3, branchID, sessionToken));
		Assert.assertTrue(pi3.getString("branchStatus").equals("NONE"), "PI3 status is not NONE");
		JSONObject configmix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		JSONObject config1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		JSONObject config2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		JSONObject config3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		JSONObject config4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	
	}
	
	private String getCheckoutStatus(String id) throws JSONException{
		String item = purchasesApi.getPurchaseItemFromBranch(id, branchID, sessionToken);
		JSONObject json = new JSONObject(item);
		return json.getString("branchStatus");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
