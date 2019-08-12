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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.SeasonsRestApi;

public class UpdateMIXParentInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
	private String entitlementID5;
	private String entitlementID6;
	private String entitlementID7;
	private String dummyE8Id;
	private String dummyE10Id ;
	private String mixID;
	private String mixID2;
	private String mixID3;
	private String mixID4;	
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
	in master add E1-> MIX -> E2&E3, add E4 under root in master, checkout E4
	- add MIX2 to master, MIX2 unchecked
	in branch: 		
 	- add MIX3 to root (status new MIX)
	unchecked MIX:	
				- update MIX maxFeaturesOn - fails
				- change order of MIX entitlements - fails	
				- unchecked MIX under new entitlement (mix under E5)
				- unchecked MIX under unchecked entitlement (mix under E1)
				- unchecked MIX under checked entitlement (mix under E4 - ok)
				- unchecked MIX to root
				- unchecked MIX under new mix (move MIX2 under MIX3 - ok)
				- unchecked MIX under unchecked mix (move MIX under MIX2 - fails) 
				- unchecked MIX under checked mix (mix2 under mix)
	checked MIX:
				- checkout MIX
				- update MIX maxFeatureOn - ok
				- change order of MIX entitlements - ok			
	  			- checked MIX under new entitlement  (mix to E5)
				- checked MIX under unchecked entitlement (mix under E1)
				- checked MIX under checked	entitlement (mix under E4)
				- checked MIX to root
				- checked MIX under new mix (move MIX under MIX3 - ok)
				- checked MIX under unchecked mix (mix under mix2)
				- checked MIX under checked mix (move MIX2 under MIX - ok)	
	new MIX:
				- update MIX maxFeatureOn - ok
				- change order of MIX features - ok				
				- new MIX under unchecked feature
				- new MIX under checked feature
				- new MIX under new feature
				- new MIX to branch root		
				- new MIX under unchecked mix
				- new MIX under checked mix
				- new MIX under new mix

	
	move feature to mix:
				- unchecked feature to unchecked mix (move E1 under MIX - fails)
				- unchecked feature to checked mix (E1 to mix)
				- new feature to unchecked mix (move E5 under MIX)
				- checked feature to unchecked mix
				- checked feature to new mix ( move E5 under MIX3 (new under new))
				- checked feature to checked mix
				- new feature to new mix ( move E5 under MIX3 (new under new))
				- new feature to checked mix (move E5 to mix)	
 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);

		String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID = purchasesApi.addPurchaseItem(seasonID, featureMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "entitlement was not added to the season: " + mixID);

		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), mixID, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		jsonE.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), mixID, sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement3 was not added: " + entitlementID3);

		jsonE.put("name", "E4");
		entitlementID4 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement4 was not added: " + entitlementID4);
		
		//checkout E4
		String response = br.checkoutFeature(branchID, entitlementID4, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement4 was not checked out to branch");

		//new entitlement in branch
		jsonE.put("name", "E5");
		entitlementID5 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID5.contains("error"), "entitlement5 was not added to branch: " + entitlementID5);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update maxFeaturesOn in unchecked out MIX") 
	public void updateUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject jsonE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		jsonE.put("maxFeaturesOn", 3);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, jsonE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated mix that is not checked out in branch");
	}

	@Test (dependsOnMethods="updateUncheckedMIXInBranch", description ="Change entitlements order in unchecked out MIX") 
	public void reorderInUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));

		JSONArray children = new JSONArray();
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		children.put(e3);
		children.put(e2);
		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Reordered features in mix that is not checked out in branch");
	}

	@Test (dependsOnMethods="reorderInUncheckedMIXInBranch", description ="Move unchecked MIX to checked out entitlement") 
	public void moveUncheckedMIXToCheckedEntitlement () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		int initialChildren = mixE.getJSONArray("entitlements").size();
		
		JSONArray children = e4.getJSONArray("entitlements");
		children.put(mixE);
		
		e4.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIX to checked out entitlement in branch: " + response);
		
		 mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixE.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
		e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		Assert.assertTrue(e4.getJSONArray("branchEntitlementItems").getString(0).contains(mixE.getString("uniqueId")), "MTX is not listed in branchEntitlementItems");
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToCheckedEntitlement", description ="Move unchecked MIX to new entitlement") 
	public void moveUncheckedMIXToNewEntitlement() throws IOException, JSONException {
		JSONObject mixE = new JSONObject (purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID5, branchID, sessionToken));
		int initialChildren = mixE.getJSONArray("entitlements").size();
		
		JSONArray children = e5.getJSONArray("entitlements");
		children.put(mixE);
		
		e5.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID5, e5.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIX to new entitlement in branch: " + response);
		
		 mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixE.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
		e5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID5, branchID, sessionToken));
		Assert.assertTrue(e5.getJSONArray("branchEntitlementItems").getString(0).contains(mixE.getString("uniqueId")), "MTX is not listed in branchEntitlementItems of the new parent");
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		Assert.assertTrue(e4.getJSONArray("branchEntitlementItems").size()==0, "MTX is listed in branchEntitlementItems of the old parent");
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToNewEntitlement", description ="Move unchecked entitlement under unchecked MIX") 
	public void moveUncheckedEntitlementUnderUncheckedMIX () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		int initialChildren = mixE.getJSONArray("entitlements").size();
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(e1);
		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked entitlement under unchecked MIX in branch");
		
		 mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixE.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
	}
	
	@Test (dependsOnMethods="moveUncheckedEntitlementUnderUncheckedMIX", description ="Move unchecked MIX under Root in branch") 
	public void moveUncheckedMIXUnderRoot () throws IOException, JSONException {
		//adding features under the root that should be visible in the branch but not checked out
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);		
		jsonE.put("name", "dummyE6");
	    entitlementID6 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID6.contains("error"), "Feature1 was not added: " + entitlementID6);
		
		JSONObject dummyE8 = new JSONObject(entitlement) ;
		dummyE8.put("name", "dummyE8");				
		dummyE8Id = purchasesApi.addPurchaseItem(seasonID, dummyE8.toString(), "ROOT", sessionToken);
		
		JSONObject e1 = new JSONObject(entitlement);
		e1.put("name", "dummyF10");
		dummyE10Id = purchasesApi.addPurchaseItem(seasonID, e1.toString(), "ROOT", sessionToken);
		
		String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID4 = purchasesApi.addPurchaseItem(seasonID, featureMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID4.contains("error"), "Feature was not added to the season: " + mixID4);

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		String rootID = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		br.checkoutFeature(branchID, rootID, sessionToken);
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootID, branchID, sessionToken));
		
		int initialChildren = mixE.getJSONArray("entitlements").size();
		
		JSONArray children = root.getJSONArray("entitlements");
		children.put(mixE);
		
		root.put("entitlements", children);
		//mix is under E5 and must be removed from it
		for(int i=0; i<root.getJSONArray("entitlements").size(); i++){
			if (root.getJSONArray("entitlements").getJSONObject(i).getString("uniqueId").equals(entitlementID5)){
				root.getJSONArray("entitlements").getJSONObject(i).put("entitlements", new JSONArray());
			}
		}
		
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't moved unchecked feature under root in branch: " + response);
		
		 mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixE.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
		JSONObject e5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID5, branchID, sessionToken));
		Assert.assertTrue(e5.getJSONArray("branchEntitlementItems").size()==0, "MTX is listed in branchEntitlementItems of the old parent");
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXUnderRoot", description ="Update checked out MIX") 
	public void updateCheckedMIX () throws IOException, JSONException {
		String response = br.checkoutFeature(branchID, mixID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MIX was not checked out to branch: " + response);

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));

		//update mix
		mixE.put("maxFeaturesOn", 3);		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update checked out mix :" + response);
	}
	
	
	@Test (dependsOnMethods="updateCheckedMIX", description ="Change features order in checked out MIX") 
	public void reorderInCheckedMIXInBranch () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = new JSONArray();
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		children.put(e3);
		children.put(e2);
		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered features in mix that is checked out in branch: " + response);
		

	}
	
	@Test (dependsOnMethods="reorderInCheckedMIXInBranch", description ="Moved checked out MIX under unchecked feature") 
	public void moveCheckedMIXUnderUncheckedFeature () throws IOException, JSONException {
		//uncheck E1		
		String response = br.cancelCheckoutFeature(branchID, entitlementID1, sessionToken);
		//Assert.assertFalse(response.contains("error"), "feature1 was not unchecked out to branch: " + response);

		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		
		JSONArray children = e1.getJSONArray("entitlements");
		children.put(mix);
		
		e1.put("entitlements", children);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked out mix to unchecked feature ");
		
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");


	}
	
	@Test (dependsOnMethods="moveCheckedMIXUnderUncheckedFeature", description ="Moved unchecked feature under checked MIX") 
	public void moveUncheckedEntitlementUnderCheckedMIX () throws IOException, JSONException {
		//cancel checkout root, otherwise new master feature is not visible in branch
		String rootID = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		
		JSONObject e6 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID6, branchID, sessionToken));
		Assert.assertFalse(e6.containsKey("error"), "Feature added to master is not found in branch");
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		
		JSONArray children = mix.getJSONArray("entitlements");
		children.put(e6);
		
		mix.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  unchecked feature to checked out mix: " + response);
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren+1, "Incorrect children size in MTX");
	}

	@Test (dependsOnMethods="moveUncheckedEntitlementUnderCheckedMIX", description ="Moved new feature under checked MIX") 
	public void moveNewEntitlementUnderCheckedMIX () throws IOException, JSONException {
		JSONObject e5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID5, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		JSONArray children = mix.getJSONArray("entitlements");
		children.put(e5);
		
		mix.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  new entitlement to checked out mix: " + response);
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren+1, "Incorrect children size in MTX");
	}
	
	@Test (dependsOnMethods="moveNewEntitlementUnderCheckedMIX", description ="Moved checked out mix under unchecked MIX") 
	public void moveCheckedMixUnderUncheckedMix() throws IOException, JSONException{
		//add mix2 to master
		String featureMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID2 = purchasesApi.addPurchaseItem(seasonID, featureMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "entitlement was not added to the season: " + mixID2);
		
		//move MIX under MIX2 - fail
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		JSONArray children = mix2.getJSONArray("entitlements");
		children.put(mix);
		
		mix2.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID2, mix2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move checkout mix under unchecked out mix ");
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderUncheckedMix", description ="Moved unchecked out mix under checked MIX") 
	public void moveUncheckedMixUnderCheckedMix() throws JSONException, IOException{
		//move MIX2 under MIX - ok
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		JSONArray children = mix.getJSONArray("entitlements");
		children.put(mix2);
		
		mix.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move uncheckout mix under checked out mix: " + response);
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren+1, "Incorrect children size in MTX");
	}

	@Test (dependsOnMethods="moveUncheckedMixUnderCheckedMix", description ="Moved checked out mix under new MIX") 
	public void moveCheckedMixUnderNewMix() throws IOException, JSONException{
		//add mix3 to branch
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Entitlement was not added to the season: " + mixID3);

		//move MIX under MIX3 - ok
		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("entitlements").size();
		JSONArray children = mix3.getJSONArray("entitlements");
		children.put(mix);
		
		mix3.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checkout mix under new mix: " + response);
		
		 mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("entitlements").size() == initialChildren, "Incorrect children size in MTX");
	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderNewMix", description ="Move new entitlement under new MIX") 
	public void moveNewEntitlementUnderNewMix() throws JSONException, IOException{
		//move new entitlement under MIX3 (new under new)
		String feature = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject dummyE7 = new JSONObject(feature) ;
		dummyE7.put("name", "dummyE7");
				
		String dummyF7Id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, dummyE7.toString(), "ROOT", sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Entitlement was not added to the season: " + mixID3);

		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		dummyE7 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyF7Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("entitlements");
		children.put(dummyE7);
		
		mix3.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new entitlement under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveNewEntitlementUnderNewMix", description ="Move checked out entitlement under new MIX") 
	public void moveCheckedEntitlementUnderNewMix() throws JSONException, IOException{
		String response = br.checkoutFeature(branchID, dummyE8Id, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checke out feature: " + response);
		
		//move dummyF8Id under MIX3 (checked under new)
		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject dummyE8 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyE8Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("entitlements");
		children.put(dummyE8);
		
		mix3.put("entitlements", children);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out entitlement under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedEntitlementUnderNewMix", description ="Update new MIX") 
	public void updateNewMIX () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));	
		//update mix
		mixE.put("maxFeaturesOn", 3);		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update new mix :" + response);
	}
	
	@Test (dependsOnMethods="updateNewMIX", description ="Change entitlements order in new MIX") 
	public void reorderInNewMIXInBranch () throws IOException, JSONException {
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String MIX4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(MIX4.contains("error"), "Entitlement was not added to the season: " + MIX4);
		
		//add 2 entitlements to new MIX4
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject e1 = new JSONObject(entitlement);
		e1.put("name", "dummy1");
		String child1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, e1.toString(), MIX4, sessionToken);
		Assert.assertFalse(child1.contains("error"), "Entitlement was not added to new mix: " + child1);
		e1.put("name", "dummy2");
		String child2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, e1.toString(), MIX4, sessionToken);
		Assert.assertFalse(child2.contains("error"), "Entitlement was not added to new mix: " + child2);
		
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(MIX4, branchID, sessionToken));
		JSONObject ch1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(child1, branchID, sessionToken));
		JSONObject ch2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(child2, branchID, sessionToken));

		JSONArray children = new JSONArray();
		children.put(ch2);
		children.put(ch1);
		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, MIX4, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered entitlements in mix that is new in branch: " + response);
	}
	

	@Test (dependsOnMethods="reorderInNewMIXInBranch", description ="Move unchecked MIX under unchecked feature") 
	public void moveUncheckedMIXUnderUncheckedEntitlement() throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("entitlements");
		children.put(mixE);		
		e1.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked mix under unchecked entitlement in branch");
	}

	@Test (dependsOnMethods="moveUncheckedMIXUnderUncheckedEntitlement", description ="Move checked MIX under new entitlement") 
	public void moveCheckedMIXUnderNewEntitlement() throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject e1 = new JSONObject(entitlement);
		e1.put("name", "dummyE9");
		String dummyF9Id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, e1.toString(), "ROOT", sessionToken);
		JSONObject dummyE9 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyF9Id, branchID, sessionToken));
		
		JSONArray children = dummyE9.getJSONArray("entitlements");
		children.put(mixE);		
		dummyE9.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, dummyF9Id, dummyE9.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under new entitlement in branch");
	}
	
	//- checked MIX under checked entitlement (mix under E4)
	@Test (dependsOnMethods="moveCheckedMIXUnderNewEntitlement", description ="Move checked MIX under checked out entitlement") 
	public void moveCheckedMIXUnderCheckedEntitlement() throws IOException, JSONException {
		
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("entitlements");
		children.put(mixE);		
		e4.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under checked out entitlement in branch");
	}
	
	//- checked MIX to root
	@Test (dependsOnMethods="moveCheckedMIXUnderCheckedEntitlement", description ="Move checked MIX under root") 
	public void moveCheckedMIXToRoot () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		root = removeChildFromFeature(root,  entitlementID4, mixID);
			
		JSONArray children = root.getJSONArray("entitlements");
		children.put(mixE);		
		root.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix to root in branch");		
	}
	
	//new MIX under unchecked entitlement
	@Test (dependsOnMethods="moveCheckedMIXToRoot", description ="Move new MIX under unchecked entitlement") 
	public void moveNewMIXUnderUncheckedEntitlement() throws IOException, JSONException {

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("entitlements");
		children.put(mixE);		
		e1.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked in branch");
	}
	
	//- new MIX under checked entitlement
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedEntitlement", description ="Move new MIX under checked entitlement") 
	public void moveNewMIXUnderCheckedEntitlement() throws IOException, JSONException {

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		br.checkoutFeature(branchID, dummyE10Id, sessionToken);
		
		JSONObject dummyF10 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyE10Id, branchID, sessionToken));
		
		JSONArray children = dummyF10.getJSONArray("entitlements");
		children.put(mixE);		
		dummyF10.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, dummyE10Id, dummyF10.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked entitlement in branch");
	}
	
	//new MIX under new entitlement
	@Test (dependsOnMethods="moveNewMIXUnderCheckedEntitlement", description ="Move new MIX under new entitlement") 
	public void moveNewMIXUnderNewEntitlement() throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(feature);
		
		jsonE.put("name", "dummy20");
		entitlementID7 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID7.contains("error"), "Entitlement1 was not added: " + entitlementID7);

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject e7 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID7, branchID, sessionToken));
		
		JSONArray children = e7.getJSONArray("entitlements");
		children.put(mixE);		
		e7.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID7, e7.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new entitlement in branch");
	}
	
	//- new MIX to branch root
	@Test (dependsOnMethods="moveNewMIXUnderNewEntitlement", description ="Move new MIX under root") 
	public void moveNewMIXToRoot () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		
		String rootId = purchasesApi.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(purchasesApi.getPurchaseItemFromBranch(rootId, branchID, sessionToken));
		root = removeChildFromFeature(root,  entitlementID7, mixID3);
		
		JSONArray children = root.getJSONArray("entitlements");
		children.put(mixE);		
		root.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new to root in branch");
	}
	
	//new MIX under unchecked mix
	@Test (dependsOnMethods="moveNewMIXToRoot", description ="Move new MIX under unchecked MIX") 
	public void moveNewMIXUnderUncheckedMix () throws IOException, JSONException {
		JSONObject newMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(newMix);		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID4, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked entitlement in branch");
	}
	
	//new MIX under checked mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under checked MIX") 
	public void moveNewMIXUnderCheckedMix () throws IOException, JSONException {
		JSONObject newMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(newMix);		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked entitlement in branch");
	}
	
	//new MIX under new mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under new MIX") 
	public void moveNewMIXUnderNewMix () throws IOException, JSONException {
		String response = br.checkoutFeature(branchID, entitlementID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
			
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String dummyMixID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, entitlementMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Entitlement was not added to the season: " + mixID);

		JSONObject dummyMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyMixID, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(dummyMix);		
		mixE.put("entitlements", children);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new mix in branch");
	}
	
	// checked entitlement to unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewMix", description ="Move checked entitlement under unchecked MIX") 
	public void moveCheckedEntitlementUnderUncheckedMix () throws IOException, JSONException {
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(e2);		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID4, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked feature under unchecked mix in branch");
	}
	
	// checked entitlement to checked mix
	@Test (dependsOnMethods="moveCheckedEntitlementUnderUncheckedMix", description ="Move checked entitlement under checked MIX") 
	public void moveCheckedEntitlementUnderCheckedMix () throws IOException, JSONException {
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("entitlements");
		children.put(e2);		
		mixE.put("entitlements", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't move checked entitlement under checked mix in branch");
	}
	 
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private  JSONObject removeChildFromFeature(JSONObject root,  String parentId, String childId) throws JSONException{
		
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


	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
