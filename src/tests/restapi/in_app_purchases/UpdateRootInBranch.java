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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateRootInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String mixID1;
	private String mixID2;
	private String rootID;
	private String filePath;
	private SeasonsRestApi s;
	private String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private InAppPurchasesRestApi purchasesApi;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*	
	 * change entitlement order in unchecked root
	 * checkout root
	 * add entitlement in master - shouldn't be seen in branch when root is checked out
	 * change entitlement order in root
	 * 	-2 entitlements
	 * 	- entitlement and mix
	 * 	- 2 mix
	 * uncheckout root
	 * 
	 */
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement1);
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);
		
		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlement mix1 was not added to the season: " + mixID1);
		
		jsonE.put("name", "E3");
		String featureID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "entitlement3 was not added to mix: " + featureID3);

		mixID2 = purchasesApi.addPurchaseItem(seasonID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlement mix2 was not added to the season: " + mixID2);
		
		jsonE.put("name", "E4");
		String featureID4 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "entitlement4 was not added to mix: " + featureID4);
		
		rootID = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update unchecked out root") 
	public void updateUncheckedRootInBranch () throws IOException, JSONException {
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		
		JSONArray children = new JSONArray();
		
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		
		children.add(entitlement2);
		children.add(entitlement1);
		children.add(mix1);
		children.add(mix2);
		
		root.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Updated entitlement that is not checked out in branch");
	}
	
	@Test (dependsOnMethods="updateUncheckedRootInBranch", description ="Checked out root") 
	public void checkoutRoot() throws IOException, JSONException{
		String response = br.checkoutFeature(branchID, rootID, sessionToken);
		Assert.assertFalse(response.contains("error"), "root was not checked out");
		
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getString("branchStatus").equals("CHECKED_OUT"), "Incorrect status for checked out root");
	}
	
	@Test (dependsOnMethods="checkoutRoot", description ="Add entitlement in master, it's not seen in root in branch")
	public void addEntitlementInMaster() throws JSONException, IOException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);		
		jsonE.put("name", "dummy");
		String dummyID = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(dummyID.contains("error"), "Dummy entitlement was not added to master: " + dummyID);
		
		String response = purchasesApi.getPurchaseItemFromBranch(dummyID, branchID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "entitlement added to master is seen in checked out root in branch");

	}
	
	@Test (dependsOnMethods="addEntitlementInMaster", description ="Update checked out root") 
	public void updateCheckedRootInBranch () throws IOException, JSONException {
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		JSONArray children = new JSONArray();
		
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject mix1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken));
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		
		children.add(entitlement2);
		children.add(mix2);
		children.add(entitlement1);
		children.add(mix1);
		
		root.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update checked out root in branch");
		
		root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getJSONArray("entitlements").getJSONObject(0).getString("uniqueId").equals(entitlement2.getString("uniqueId")), "Incorrect entitlement in the first place");
		Assert.assertTrue(root.getJSONArray("entitlements").getJSONObject(1).getString("uniqueId").equals(mix2.getString("uniqueId")), "Incorrect entitlement in the second place");
		Assert.assertTrue(root.getJSONArray("entitlements").getJSONObject(2).getString("uniqueId").equals(entitlement1.getString("uniqueId")), "Incorrect entitlement in the third place");
		Assert.assertTrue(root.getJSONArray("entitlements").getJSONObject(3).getString("uniqueId").equals(mix1.getString("uniqueId")), "Incorrect entitlement in the fourth place");
	}
	
	@Test (dependsOnMethods="updateCheckedRootInBranch", description ="unchecked out root") 
	public void uncheckoutRoot() throws IOException, JSONException{
		String response = br.cancelCheckoutFeature(branchID, rootID, sessionToken);
		Assert.assertFalse(response.contains("error"), "root was not unchecked out");
		
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		Assert.assertTrue(root.getString("branchStatus").equals("NONE"), "Incorrect status for checked out root");
	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
