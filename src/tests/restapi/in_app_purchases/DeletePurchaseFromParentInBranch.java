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

public class DeletePurchaseFromParentInBranch {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass	
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*delete
		- can't delete unchecked
		- can't delete checked
		- new from unchecked
		- new from checked
		- new from new
		- if sub-entitlement is checked-out, no delete: add new entitlement to branch. checkout entitlement from master and update its parent
	 to this new branch entitlement. delete this new branch entitlement - not allowed
	 */
	
	@Test (description ="Delete unchecked entitlement from new") 
	public void deleteUncheckedEntitlement () throws IOException, JSONException {
		String branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);
		
		jsonF.put("name", "E1");
		String entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);
		
		jsonF.put("name", "E2");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		//move unchecked E1 under new E2
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = entitlement2.getJSONArray("entitlements");
		children.put(entitlement1);
		entitlement2.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checked entitlement under new  feature");

		int code = purchasesApi.deletePurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		Assert.assertFalse (code==200, "Unchecked entitlement was deleted from under the new feature. Code: " + code);
	}
	
	@Test (description ="Delete checked entitlement from new") 
	public void deleteCheckedEntitlement () throws IOException, JSONException {
		String branchID = addBranch("branch2");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		
		jsonE.put("name", "E3");
		String entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement3 was not added: " + entitlementID1);
		
		jsonE.put("name", "E4");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement4 was not added: " + entitlementID2);
		
		//move unchecked E1 under new E2
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = entitlement2.getJSONArray("entitlements");
		children.put(entitlement1);
		entitlement2.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checked entitlement under new  feature");
		
		//checkout E1
		response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement1 was not checked out to branch");

		int code = purchasesApi.deletePurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		Assert.assertFalse (code==200, "Checked entitlement was deleted from under the new entitlement. Code: " + code);
	}
	
	
	@Test (description ="Delete new entitlement") 
	public void deleteNewEntitlementFromChecked () throws IOException, JSONException {
		String branchID = addBranch("branch4");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);
		
		jsonF.put("name", "E7");
		String featureID1 = purchasesApi.addPurchaseItem(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "entitlement7 was not added: " + featureID1);
		
		//checkout E1
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
	
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(featureID1, branchID, sessionToken));
		
		jsonF.put("name", "E8");
		String featureID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonF.toString(), entitlement1.getString("uniqueId"), sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "entitlement8 was not added to branch: " + featureID2);
		
		int code = purchasesApi.deletePurchaseItemFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue (code==200, "new entitlement was not deleted from under the checked entitlement in branch. Code: " + code);
	}
	
	@Test (description ="Delete new entitlement from new") 
	public void deleteNewEntitlementFromNew() throws IOException, JSONException {
		String branchID = addBranch("branch5");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);
		
		jsonF.put("name", "E9");
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID , jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement9 was not added: " + entitlementID1);		
		
		jsonF.put("name", "E10");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonF.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement10 was not added to branch: " + entitlementID2);
		
		int code = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertTrue (code==200, "new entitlement was not deleted from under the new entitlement in branch. Code: " + code);
	}
	
	@Test (description ="Delete checked from new") 
	public void deleteCheckedEntitlementFromNew () throws IOException, JSONException {
		String branchID = addBranch("branch6");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);		
		jsonF.put("name", "E11");
		String entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement11 was not added: " + entitlementID1);
		
		//checkout E1
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");
		
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));		
		jsonF.put("name", "E12");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement12 was not added to branch: " + entitlementID2);
		
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));	
		JSONArray entitlements = new JSONArray();
		entitlements.add(entitlement1);
		entitlement2.put("entitlements", entitlements);
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "checked out sub-entitlement was not added to entitlement12 in branch: " + entitlementID2);
		
		//delete new entitlement to branch with checked out entitlement as a sub-entitlement 
		int code = purchasesApi.deletePurchaseItemFromBranch(entitlementID2, branchID, sessionToken);
		Assert.assertFalse (code==200, "deleted new entitlement when its sub-entitlement is checked-out from branchS");
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
