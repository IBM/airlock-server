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

public class UpdateConfigurationMIXParentInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
	private String entitlementID11;
	private String mixID;
	private String mixID2;
	private String mixID3;
	private String mixID4;
	private String mixID5;
	private String mixID10;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	private String configID5;
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
	in master add in master add E1-> MIXCR -> CR1&CR2, add F2 with CR3 & E3 under root in master, checkout E2
	- add MIX2 to master, MIX2 unchecked
	in branch: 		
 	- in branch add E4 + CR4 - new
	unchecked MIX:	
				- update MIX maxFeaturesOn - fails
				- change order of MIX entitlements - fails	
				- unchecked MIX under new entitlement 
				- unchecked MIX under unchecked entitlement 
				- unchecked MIX under checked entitlement
				- unchecked MIX to root
				- unchecked MIX under new mix 
				- unchecked MIX under unchecked mix 
				- unchecked MIX under checked mix 
	checked MIX:
				- checkout MIX
				- update MIX maxFeatureOn - ok
				- change order of MIX entitlements - ok			
	  			- checked MIX under new entitlement  
				- checked MIX under unchecked entitlement 
				- checked MIX under checked	entitlement 
				- checked MIX to root
				- checked MIX under new mix 
				- checked MIX under unchecked mix 
				- checked MIX under checked mix 
	new MIX:
				- update MIX maxFeatureOn - ok
				- change order of MIX entitlements - ok				
				- new MIX under unchecked entitlement
				- new MIX under checked entitlement
				- new MIX under new entitlement
				- new MIX to branch root		
				- new MIX under unchecked mix
				- new MIX under checked mix
				- new MIX under new mix

	move entitlement to mix:
				- unchecked entitlement to unchecked mix 
				- unchecked entitlement to checked mix 
				- new entitlement to unchecked mix 
				- checked entitlement to unchecked mix
				- checked entitlement to new mix 
				- checked entitlement to checked mix
				- new entitlement to new mix 
				- new entitlement to checked mix 		
 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		//in master add E1-> MIXCR -> CR1&CR2, add F2 with CR3 & E3 under root in master, checkout E2
		//in branch add E4 + CR4 - new
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID = purchasesApi.addPurchaseItem(seasonID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Configuration mix was not added to the season: " + mixID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		jsonCR.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		jsonCR.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season: " + configID2);
		
		
		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		jsonCR.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule3 was not added to the season: " + configID3);
		
		jsonE.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID3.contains("error"), "entitlement3 was not added: " + entitlementID3);
		
		jsonCR.put("name", "CR5");
		configID5 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID3, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule5 was not added to the season: " + configID3);
		
		//checkout E2
		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement2 was not checked out to branch: " + response);

		//new entitlement in branch
		jsonE.put("name", "E4");
		entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement4 was not added to branch: " + entitlementID4);
		
		jsonCR.put("name", "CR4");
		configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season: " + configID4);	
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update maxFeaturesOn in unchecked out MIX") 
	public void updateUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject json = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		json.put("maxFeaturesOn", 3);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated mix that is not checked out in branch");
	}
	

	@Test (dependsOnMethods="updateUncheckedMIXInBranch", description ="Change entitlements order in unchecked out MIX") 
	public void reorderInUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));

		JSONArray children = new JSONArray();
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		children.put(e3);
		children.put(e2);
		
		mixE.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Reordered entitlements in mix that is not checked out in branch");
	}

	@Test (dependsOnMethods="reorderInUncheckedMIXInBranch", description ="Move unchecked MIX to checked out entitlement") 
	public void moveUncheckedMIXToCheckedEntitlement () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(mixE);
		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIXCR to checked out entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToCheckedEntitlement", description ="Move unchecked MIX to new entitlement") 
	public void moveUncheckedMIXToNewEntitlement () throws IOException, JSONException {
		JSONObject mixE = new JSONObject (purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(mixE);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIXCR to new entitlement in branch: " + response);
		
		mixE = new JSONObject (purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixE.getString("branchStatus").equals("CHECKED_OUT"), "MTXCR status was not changed to checked_out");	
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToNewEntitlement", description ="Move unchecked entitlement under unchecked MIX") 
	public void moveUncheckedEntitlementUnderUncheckedMIX () throws IOException, JSONException {
		//add new MTXCR in master under E1
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID10 = purchasesApi.addPurchaseItem(seasonID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Configuration mix was not added to the season: " + mixID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "CR10");
		String configID10 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), mixID10, sessionToken);
		Assert.assertFalse(configID10.contains("error"), "Configuration rule10 was not added to the season: " + configID10);

		JSONObject mixE10 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID10, branchID, sessionToken));
		JSONObject f5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID5, branchID, sessionToken));
		
		JSONArray children = mixE10.getJSONArray("configurationRules");
		children.put(f5);
		
		mixE10.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID10, mixE10.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked entitlement under unchecked MIX in branch");
	}
	
	@Test (dependsOnMethods="moveUncheckedEntitlementUnderUncheckedMIX", description ="Update checked out MIX") 
	public void updateCheckedMIX () throws IOException, JSONException {
		//mixE is under E4(new entitlement in branch)
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));

		//update mix
		mixE.put("maxFeaturesOn", 3);		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update checked out mix :" + response);		
	}
	
	
	@Test (dependsOnMethods="updateCheckedMIX", description ="Change entitlements order in checked out MIX") 
	public void reorderInCheckedMIXInBranch () throws IOException, JSONException {
		
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));

		JSONArray children = mixE.getJSONArray("configurationRules");

		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID3, branchID, sessionToken));
		children.remove(e2);
		children.remove(e3);
		children.put(e3);
		children.put(e2);
		
		mixE.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered entitlements in mix that is checked out in branch: " + response);
	}
	
	@Test (dependsOnMethods="reorderInCheckedMIXInBranch", description ="Moved checked out MIX under unchecked entitlement") 
	public void moveCheckedMIXUnderUncheckedEntitlement () throws IOException, JSONException {

		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = e3.getJSONArray("configurationRules");
		children.put(mix);
		
		e3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, e3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked out mix to unchecked entitlement");
	}
	
	
	@Test (dependsOnMethods="moveCheckedMIXUnderUncheckedEntitlement", description ="Moved unchecked entitlement under checked MIX") 
	public void moveUncheckedEntitlementUnderCheckedMIX () throws IOException, JSONException {

		JSONObject e5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID5, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(e5);
		
		mix.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  unchecked entitlement to checked out mix: " + response);
	}

	@Test (dependsOnMethods="moveUncheckedEntitlementUnderCheckedMIX", description ="Moved new entitlement under checked MIX") 
	public void moveNewEntitlementUnderCheckedMIX () throws IOException, JSONException {
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(e4);
		
		mix.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new entitlement to checked out mix: " + response);
	}

	@Test (dependsOnMethods="moveNewEntitlementUnderCheckedMIX", description ="Moved checked out mix under unchecked MIX") 
	public void moveCheckedMixUnderUncheckedMix() throws IOException, JSONException{
		//add mix2 to master
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);		
		jsonE.put("name", "E10");
		String featureID10 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement10 was not added: " + featureID10);

		String crMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID2 = purchasesApi.addPurchaseItem(seasonID, crMix, featureID10, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Configuration mix was not added to the season: " + mixID2);
		
		//move MIX (checked) under MIX2 (unchecked) - fail
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix2.getJSONArray("configurationRules");
		children.put(mix);
		
		mix2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID2, mix2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move checkout mix under unchecked out mix ");
	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderUncheckedMix", description ="Moved unchecked out mix under checked MIX") 
	public void moveUncheckedMixUnderCheckedMix() throws JSONException, IOException{
		//move MIX2 under MIX - ok
		JSONObject mix2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(mix2);
		
		mix.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move uncheckout mix under checked out mix: " + response);
	}

	@Test (dependsOnMethods="moveUncheckedMixUnderCheckedMix", description ="Moved checked out mix under new MIX") 
	public void moveCheckedMixUnderNewMix() throws IOException, JSONException{
		//add mix3 to branch
		String crMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID3 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, crMix, entitlementID4, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "cr mix was not added to the season: " + mixID3);

		//move MIX under MIX3 - ok
		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("configurationRules");
		children.put(mix);
		
		mix3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checkout mix under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderNewMix", description ="Move new entitlement under new MIX") 
	public void moveNewEntitlementUnderNewMix() throws JSONException, IOException{

		//move new configuraiton rule under MIX3 (new under new)
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject dummyCR1 = new JSONObject(feature) ;
		dummyCR1.put("name", "dummyCR1");
				
		String dummyCR1Id = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, dummyCR1.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "cr was not added to the season: " + mixID3);

		JSONObject mix3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		dummyCR1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyCR1Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("configurationRules");
		children.put(dummyCR1);
		
		mix3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new cr under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveNewEntitlementUnderNewMix", description ="Move checked out entitlement under new MIX") 
	public void moveCheckedFeatureUnderNewMix() throws JSONException, IOException{
		br.checkoutFeature(branchID, entitlementID2, sessionToken);
		 
		//add new MIXCR to branch
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID5 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, feature, entitlementID2, sessionToken);

		//move config2 under MIX5 (checked under new)
		JSONObject mix5 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID5, branchID, sessionToken));
		JSONObject f2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONArray children = mix5.getJSONArray("configurationRules");
		children.put(f2);
		
		mix5.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID5, mix5.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out cr under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedFeatureUnderNewMix", description ="Update new MIX") 
	public void updateNewMIX () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID5, branchID, sessionToken));	
		//update mix
		mixE.put("maxFeaturesOn", 3);		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID5, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update new mix :" + response);
	}
	
	@Test (dependsOnMethods="updateNewMIX", description ="Change features order in new MIX") 
	public void reorderInNewMIXInBranch () throws IOException, JSONException {
		//add 2 entitlements to new MIX3
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID5, branchID, sessionToken));
		JSONObject prevChild = mixE.getJSONArray("configurationRules").getJSONObject(0);
		
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject e1 = new JSONObject(feature);
		e1.put("name", "dummy1");
		String childID1 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, e1.toString(), mixID5, sessionToken);
		Assert.assertFalse(childID1.contains("error"), "entitlement was not added to new mix: " + childID1);
		e1.put("name", "dummy2");
		String childID2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, e1.toString(), mixID5, sessionToken);
		Assert.assertFalse(childID2.contains("error"), "entitlement was not added to new mix: " + childID2);
		
		JSONObject ch1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(childID1, branchID, sessionToken));
		JSONObject ch2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(childID2, branchID, sessionToken));

		JSONArray children = new JSONArray();
		children.put(ch2);
		children.put(ch1);
		children.put(prevChild);
		
		mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID5, branchID, sessionToken));
		mixE.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID5, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered entitlements in mix that is new in branch: " + response);
		
		mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID5, branchID, sessionToken));
		Assert.assertTrue(mixE.getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(ch2.getString("uniqueId")), "Incorrect child in the first place");
		Assert.assertTrue(mixE.getJSONArray("configurationRules").getJSONObject(1).getString("uniqueId").equals(ch1.getString("uniqueId")), "Incorrect child in the second place");
		Assert.assertTrue(mixE.getJSONArray("configurationRules").getJSONObject(2).getString("uniqueId").equals(prevChild.getString("uniqueId")), "Incorrect child in the third place");
	}	

	@Test (dependsOnMethods="reorderInNewMIXInBranch", description ="Move unchecked MIX under unchecked entitlement") 
	public void moveUncheckedMIXUnderUncheckedEntitlement () throws IOException, JSONException {
		
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = e3.getJSONArray("configurationRules");
		children.put(mixE);		
		e3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, e3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked mix under unchecked entitlement in branch");
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXUnderUncheckedEntitlement", description ="Move checked MIX under new entitlement") 
	public void moveCheckedMIXUnderNewEntitlement () throws IOException, JSONException {
		//checkout MIX
		br.checkoutFeature(branchID, mixID, sessionToken);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);		
		jsonE.put("name", "E11");
		entitlementID11 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID11.contains("error"), "entitlement1 was not added: " + entitlementID11);
	
		JSONObject mixF = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject f11 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID11, branchID, sessionToken));
		
		JSONArray children = f11.getJSONArray("configurationRules");
		children.put(mixF);		
		f11.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID11, f11.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under new entitlement in branch: " + response);
	}
	
	//- checked MIX under checked entitlement 
	@Test (dependsOnMethods="moveCheckedMIXUnderNewEntitlement", description ="Move checked MIX under checked out entitlement") 
	public void moveCheckedMIXUnderCheckedEntitlement () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(mixE);		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under checked out entitlement in branch: " + response);
	}

	
	//new MIX under unchecked entitlement
	@Test (dependsOnMethods="moveCheckedMIXUnderCheckedEntitlement", description ="Move new MIX under unchecked entitlement") 
	public void moveNewMIXUnderUncheckedEntitlement () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject f3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(mixF);		
		f3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, f3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked in branch");
	}
	
	//- new MIX under checked entitlement
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedEntitlement", description ="Move new MIX under checked entitlement") 
	public void moveNewMIXUnderCheckedEntitlement () throws IOException, JSONException {
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(mixE);		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked entitlement in branch: " + response);
	}
	
	//new MIX under new entitlement
	@Test (dependsOnMethods="moveNewMIXUnderCheckedEntitlement", description ="Move new MIX under new entitlement") 
	public void moveNewMIXUnderNewEntitlement () throws IOException, JSONException {

		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(mixE);		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new entitlement in branch: " + response);
	}
		
	//new MIX under unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewEntitlement", description ="Move new MIX under unchecked MIX") 
	public void moveNewMIXUnderUncheckedMix () throws IOException, JSONException {
		//add new MIXCR to master under unchecked feature
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID4 = purchasesApi.addPurchaseItem(seasonID, feature, entitlementID3, sessionToken);
			
		JSONObject newMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixF = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(newMix);		
		mixF.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked entitlement in branch");
	}
	
	//new MIX under checked mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under checked MIX") 
	public void moveNewMIXUnderCheckedMix () throws IOException, JSONException {

		br.checkoutFeature(branchID, mixID, sessionToken);
		
		JSONObject newMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("configurationRules");
		children.put(newMix);		
		mixE.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked entitlement in branch: " + response);
	}
	
	//new MIX under new mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under new MIX") 
	public void moveNewMIXUnderNewMix () throws IOException, JSONException {
		String featureMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String dummyMixID = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, featureMix, entitlementID4, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "mix was not added to the season: " + mixID);

		
		JSONObject dummyMix = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyMixID, branchID, sessionToken));
		JSONObject mixF = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID3, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(dummyMix);		
		mixF.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID3, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new mix in branch: " + response);
	}
	
	// checked feature to unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewMix", description ="Move checked entitlement under unchecked MIX") 
	public void moveCheckedEntitlementUnderUncheckedMix () throws IOException, JSONException {
		
		JSONObject f2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject mixF = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(f2);		
		mixF.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID4, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked entitlement under unchecked mix in branch: " + response);
	}
	
	// checked entitlement to checked mix
	@Test (dependsOnMethods="moveCheckedEntitlementUnderUncheckedMix", description ="Move checked entitlement under checked MIX") 
	public void moveCheckedFeatureUnderCheckedMix () throws IOException, JSONException {
		br.checkoutFeature(branchID, mixID, sessionToken);
		
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject mixE = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixE.getJSONArray("configurationRules");
		children.put(e2);		
		mixE.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, mixID, mixE.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked entitlement under checked mix in branch");
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
