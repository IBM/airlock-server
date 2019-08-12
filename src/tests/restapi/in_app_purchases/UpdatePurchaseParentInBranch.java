package tests.restapi.in_app_purchases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

public class UpdatePurchaseParentInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
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

		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}

	/*	- update unchecked entitlement in branch
	 * - add new items under unchecked entitlement in branch
	 * move:
	 * 	- unchecked under new
		- unchecked under unchecked
		- unchecked under checked
		- unchecked to root
	  	- checked under new
		- checked under unchecked
		- checked under checked	
		- checked to root	
		- new under unchecked
		- new under checked
		- new under new
		- new to branch root
		add:
		- new under unchecked
		- new under checked
		- new under new
		delete
		- unchecked from new
		- unchecked from unchecked
		- unchecked from checked
	  	- checked from new
		- checked from unchecked
		- checked from new		
		- new from unchecked
		- new from checked
		- new from new
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
		
		//checkout F2
		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement was not checked out to branch");

		jsonE.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement3 was not added: " + entitlementID3);
	
		jsonE.put("name", "E4");
		entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement4 was not added: " + entitlementID3);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update unchecked out entitlement") 
	public void updateUncheckedEntitlementInBranch () throws IOException, JSONException {
		String feature = purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken);
		JSONObject jsonF = new JSONObject(feature);
		jsonF.put("description", "New description");
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, jsonF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated entitlement that is not checked out in branch");
	}
	
	@Test (dependsOnMethods="updateUncheckedEntitlementInBranch", description ="Add children in branch to unchecked out feature") 
	public void addChildrenToUncheckedEntitlementInBranch () throws IOException, JSONException {
		String entitlement3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlement3, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "entitlement3 was added to unchecked feature in branch");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, featureMix, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "MIX was added to unchecked feature in branch");

		String configMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configMix, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration MIX was added to unchecked feature in branch");
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		response = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, configuration1, entitlementID1, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuration rule1 was added to unchecked feature in branch");
	}
	
	@Test (dependsOnMethods="addChildrenToUncheckedEntitlementInBranch", description ="Move unchecked entitlement under checked out feature - ok") 
	public void moveUncheckedEntitlementToChecked() throws IOException, JSONException {
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = entitlement2.getJSONArray("entitlements");
		children.put(entitlement1);
		entitlement2.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't move unchecked entitlement under checked out entitlement");
		
		int res = uniqueness(getRoot(), entitlement1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement1.getString("name") + " was found " + res + " times in the tree");
	}

	@Test (dependsOnMethods="moveUncheckedEntitlementToChecked", description ="Move unchecked entitlement under new feature - ok") 
	public void moveUncheckedFeatureToNewFeature () throws IOException, JSONException {
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = entitlement4.getJSONArray("entitlements");
		children.put(entitlement1);
		entitlement4.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, entitlement4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move unchecked out entitlement under new entitlement");

		int res = uniqueness(getRoot(), entitlement1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveUncheckedFeatureToNewFeature", description ="Move unchecked feature under unchecked feature - fails") 
	public void moveUncheckedEntitlementToUnchecked () throws IOException, JSONException {
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject entitlement3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = entitlement3.getJSONArray("entitlements");
		children.put(entitlement1);
		entitlement3.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, entitlement3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move unchecked out entitlement under unchecked entitlement");

		int res = uniqueness(getRoot(), entitlement1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveUncheckedEntitlementToUnchecked", description ="Move unchecked feature under unchecked feature - ok") 
	public void moveUncheckedEntitlementToRoot () throws IOException, JSONException {
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("entitlements");
		children.put(entitlement1);
		root.put("entitlements", children);
		
		//E1 previous parent is E4 and must be removed from there
		root = removeChildFromEntitlement( root, entitlementID4, entitlementID1);
		
		//root is also unchecked, can't moved under unchecked root
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move unchecked out entitlement under root");

		int res = uniqueness(getRoot(), entitlement1.getString("name"), setCounter());
		Assert.assertTrue(res==1, "Feature " + entitlement1.getString("name") + " was found " + res + " times in the tree");
		
		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}

	@Test (dependsOnMethods="moveUncheckedEntitlementToRoot", description ="Move checked entitlement under unchecked out entitlement - fails") 
	public void moveCheckedEntitlementToUnchecked () throws IOException, JSONException {
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = entitlement1.getJSONArray("entitlements");
		children.put(entitlement2);
		entitlement1.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, entitlement1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked entitlement under unchecked out feature");
		
		int res = uniqueness(getRoot(), entitlement2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	

	@Test (dependsOnMethods="moveCheckedEntitlementToUnchecked", description ="Move checked entitlement under new feature - ok") 
	public void moveCheckedEntitlementToNewEntitlement () throws IOException, JSONException {
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = entitlement4.getJSONArray("entitlements");
		children.put(entitlement2);
		entitlement4.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, entitlement4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move checked out entitlement under new entitlement");

		int res = uniqueness(getRoot(), entitlement2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveCheckedEntitlementToNewEntitlement", description ="Move checked entitlement under checked entitlement - ok") 
	public void moveCheckedEntitlementToChecked () throws IOException, JSONException {
		//checkout E3
		String response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement3 was not checked out to branch");
		
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = entitlement3.getJSONArray("entitlements");
		children.put(entitlement2);
		entitlement3.put("entitlements", children);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, entitlement3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out entitlement under checked feature");

		int res = uniqueness(getRoot(), entitlement2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveCheckedEntitlementToChecked", description ="Move checked entitlement to root - ok") 
	public void moveCheckedEntitlementToRoot () throws IOException, JSONException {
		//checkout root
		String rootId = purchasesApi.getBranchRootId(seasonID, "MASTER", sessionToken);
		String response = br.checkoutFeature(branchID, rootId, sessionToken);
		Assert.assertFalse(response.contains("error"), "can't check out root");
		
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));		
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("entitlements");
		children.put(entitlement2);
		root.put("entitlements", children);
		
		//E2 previous parent is E3 and must be removed from there
		root = removeChildFromEntitlement( root, entitlementID3, entitlementID2);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move checked out entitlement under root: " + response);

		int res = uniqueness(getRoot(), entitlement2.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement2.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}

	@Test (dependsOnMethods="moveCheckedEntitlementToRoot", description ="Move new entitlement under unchecked out entitlement - fails") 
	public void moveNewEntitlementToUnchecked () throws IOException, JSONException {
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		JSONObject entitlement1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = entitlement1.getJSONArray("entitlements");
		children.put(entitlement4);
		entitlement1.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, entitlement1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked entitlement under unchecked out entitlement");
		
		int res = uniqueness(getRoot(), entitlement4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}

	@Test (dependsOnMethods="moveNewEntitlementToUnchecked", description ="Move new entitlement under new entitlement - ok") 
	public void moveNewEntitlementToNewEntitlement() throws IOException, JSONException {
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "FB1");
		String FB1Id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(FB1Id.contains("error"), "New entitlement FB1 was not added to branch: " + FB1Id);
		
		JSONObject feature4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		JSONObject newInBranch = new JSONObject(purchasesApi.getPurchaseItemFromBranch(FB1Id, branchID, sessionToken));
		
		JSONArray children = newInBranch.getJSONArray("entitlements");
		children.put(feature4);
		newInBranch.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, FB1Id, newInBranch.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Cannot move checked out entitlement under new entitlement");
	
		int res = uniqueness(getRoot(), feature4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + feature4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}

	@Test (dependsOnMethods="moveNewEntitlementToNewEntitlement", description ="Move new entitlement to checked entitlement - ok") 
	public void moveNewEntitlementToChecked () throws IOException, JSONException {
		JSONObject entitlement2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = entitlement2.getJSONArray("entitlements");
		children.put(entitlement4);
		entitlement2.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, entitlement2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new entitlement under checked entitlement");
		
		int res = uniqueness(getRoot(), entitlement4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement" + key + " was found more than once in branchFeatureParentName");
	}

	@Test (dependsOnMethods="moveNewEntitlementToChecked", description ="Move new entitlement to root - ok") 
	public void moveNewEntitlementToRoot () throws IOException, JSONException {
		JSONObject entitlement4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		
		JSONArray children = root.getJSONArray("entitlements");
		children.put(entitlement4);
		root.put("entitlements", children);
		
		//E4 previous parent is E2 and must be removed from there
		root = removeChildFromEntitlement( root, entitlementID2, entitlementID4);
	
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't move new feature under root");
	
		int res = uniqueness(getRoot(), entitlement4.getString("name"), setCounter());
		Assert.assertTrue(res==1, "entitlement " + entitlement4.getString("name") + " was found " + res + " times in the tree");

		HashMap<String, Integer> keys = uniqueChildren(getRoot(), new HashMap<String, Integer>());
		String key = checkMapUniqueness(keys);
		Assert.assertTrue(key.equals("0"), "entitlement " + key + " was found more than once in branchFeatureParentName");
	}
	
	@Test (dependsOnMethods="moveNewEntitlementToRoot", description ="Add new entitlement under unchecked entitlement - ok") 
	public void addNewEntitlementToUncheckedEntitlement() throws IOException, JSONException {
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "new1");
		String newId1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), entitlementID1, sessionToken);
		Assert.assertTrue(newId1.contains("error"), "New entitlement  was added to branch under unchecked entitlement: " + newId1);	
	}
	
	@Test (dependsOnMethods="addNewEntitlementToUncheckedEntitlement", description ="Add new entitlement under new entitlement - ok") 
	public void addNewEntitlementToNewEntitlement () throws IOException, JSONException {
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(entitlement);
		jsonF.put("name", "new2");
		String newId1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonF.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(newId1.contains("error"), "New entitlement  was not added to branch under new entitlement: " + newId1);
	}
	
	@Test (dependsOnMethods="addNewEntitlementToNewEntitlement", description ="Add new entitlement under checked entitlement - ok") 
	public void addNewEntitlementToCheckedEntitlement() throws IOException, JSONException {
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		jsonE.put("name", "new3");
		String newId1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(newId1.contains("error"), "New entitlement  was not added to branch under checked entitlement: " + newId1);
	}

	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private  JSONObject removeChildFromEntitlement(JSONObject root,  String parentId, String childId) throws JSONException{
		
		JSONArray features = root.getJSONArray("entitlements");
		for (int i=0; i<features.size(); i++){
			JSONObject feature = features.getJSONObject(i);
			if (feature.getString("uniqueId").equals(parentId)){	//find old parent
				JSONArray children = feature.getJSONArray("entitlements");
				for (int j=0; j< children.size(); j++){
					if (children.getJSONObject(j).getString("uniqueId").equals(childId)){
						children.remove(j);
					}
				}
			}
	
		}
		return root;
		
	}
	
	
	private int uniqueness(JSONObject startFrom, String featureName, ArrayList<Integer>  featureFound) throws JSONException{
		int found = featureFound.get(0);
		for (int i=0; i<startFrom.getJSONArray("entitlements").size(); i++){
			JSONObject feature = startFrom.getJSONArray("entitlements").getJSONObject(i);
			//System.out.println(feature.getString("name"));
			if (feature.getString("name").equals(featureName)) {
				found++;
				featureFound.add(0, found);
			}	
			
			if (feature.getJSONArray("entitlements").size()>0)
				uniqueness(feature, featureName, featureFound);
		}
		
		return featureFound.get(0);
	}
	
	private HashMap<String, Integer> uniqueChildren(JSONObject startFrom, HashMap<String, Integer> children) throws JSONException{		
		for (int i=0; i<startFrom.getJSONArray("entitlements").size(); i++){
			JSONObject feature = startFrom.getJSONArray("entitlements").getJSONObject(i);						
			if (feature.containsKey("branchFeaturesItems")) {
				for(int j=0; j<feature.getJSONArray("branchFeaturesItems").size(); j++){
					if (children.containsKey(feature.getJSONArray("branchFeaturesItems").getString(j)))		{
						children.put(feature.getJSONArray("branchFeaturesItems").getString(j), Integer.valueOf(2));
					}
						
					else
						children.put(feature.getJSONArray("branchFeaturesItems").getString(j), 1);
				}
			}	
			
			if (feature.getJSONArray("entitlements").size()>0)
				uniqueChildren(feature, children);
		}

		return children;
	}
	
	private String checkMapUniqueness(HashMap<String, Integer> children){
		
		
		for (Map.Entry<String, Integer>entry : children.entrySet()) {
			if (entry.getValue()>1)
				return entry.getKey();
		}
		
		return "0";
	}
	
	private JSONObject getRoot() throws JSONException{
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		return new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));

	}
	
	private ArrayList<Integer> setCounter()
	{
		ArrayList<Integer> count = new ArrayList<Integer>();
		count.add(0);
		return count;
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
