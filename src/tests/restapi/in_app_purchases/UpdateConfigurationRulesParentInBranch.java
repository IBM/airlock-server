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

public class UpdateConfigurationRulesParentInBranch {
	private String productID;
	private String seasonID;
	private String branchID;
	private String entitlementID1;
	private String entitlementID2;
	private String entitlementID3;
	private String entitlementID4;
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
	configuration rules:
	- checked CR under checked entitlement
	- checked CR under unchecked entitlement
	- checked CR under new entitlement
	- unchecked CR  under new entitlement
	- unchecked CR under unchecked entitlement
	- unchecked CR under checked entitlement
	- new CR to unchecked entitlement
	- new CR to checked entitlement
	- new CR to new entitlement
	
	- checked CR under new CR
	- checked CR under unchecked CR 
	- checked CR under new CR 	
	- unchecked CR  under new CR
	- unchecked CR under unchecked CR
	- unchecked CR under checked CR	
	- new CR to unchecked CR
	- new CR to checked CR
	- new CR to new CR
		
 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		//in master add E1-> MIXCR -> CR1&CR2, add E2 with CR3 & E3 under root in master, checkout E2
		//in branch add E4 + CR4 - new
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		
		jsonE.put("name", "E1");
		entitlementID1 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not added: " + entitlementID1);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		jsonCR.put("name", "CR1");
		configID1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		jsonE.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not added: " + entitlementID2);
		
		jsonCR.put("name", "CR2");
		configID2 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season: " + configID3);

		//checkout E2
		String response = br.checkoutFeature(branchID, entitlementID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "entitlement2 was not checked out to branch: " + response);

		jsonE.put("name", "E3");
		entitlementID3 = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement3 was not added: " + entitlementID2);
		
		jsonCR.put("name", "CR3");
		configID3 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season: " + configID3);

		//new entitlement in branch
		jsonE.put("name", "E4");
		entitlementID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID4.contains("error"), "entitlement3 was not added to branch: " + entitlementID4);
		
		jsonCR.put("name", "CR4");
		configID4 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season: " + configID4);
	}
	
	@Test (dependsOnMethods="addComponents", description ="Move unchecked CR to unchecked out entitlement") 
	public void moveUncheckedConfigToUncheckedEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("configurationRules");
		children.put(cr);
		
		e1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToUncheckedEntitlement", description ="Move unchecked CR to checked out entitlement") 
	public void moveUncheckedConfigToCheckedEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(cr);
		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to checked entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToCheckedEntitlement", description ="Move unchecked CR to checked out entitlement") 
	public void moveUncheckedConfigToNewEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new entitlement in branch: " + response);
	}
	
	
	@Test (dependsOnMethods="moveUncheckedConfigToNewEntitlement", description ="Move checked CR to unchecked out entitlement") 
	public void moveCheckedConfigToUncheckedEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("configurationRules");
		children.put(cr);
		
		e1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToUncheckedEntitlement", description ="Move checked CR to checked out entitlement") 
	public void moveCheckedConfigToCheckedEntitlement () throws IOException, JSONException {
		//checkout E3
		String response = br.checkoutFeature(branchID, entitlementID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout entitlement3 to branch: " + response);
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = e3.getJSONArray("configurationRules");
		children.put(cr);
		
		e3.put("configurationRules", children);
		
		response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, e3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToCheckedEntitlement", description ="Move checked CR to checked out entitlement") 
	public void moveCheckedConfigToNewEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToNewEntitlement", description ="Move new CR to unchecked out entitlement") 
	public void moveNewConfigToUncheckedEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("configurationRules");
		children.put(cr);
		
		e1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new CR to unchecked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToUncheckedEntitlement", description ="Move new CR to checked out entitlement") 
	public void moveNewConfigToCheckedEntitlement () throws IOException, JSONException {	
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		JSONObject e3 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID3, branchID, sessionToken));
		
		JSONArray children = e3.getJSONArray("configurationRules");
		children.put(cr);
		
		e3.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID3, e3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToCheckedEntitlement", description ="Move new CR to checked out entitlement") 
	public void moveNewConfigToNewEntitlement () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(entitlementID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, entitlementID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new entitlement in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToNewEntitlement", description ="Move new CR to unchecked out configuration rule") 
	public void moveNewConfigToUncheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));	
		
		//uncheck featureID3 to make its configuration CR1 unchecked
		String res = br.cancelCheckoutFeature(branchID, entitlementID3, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot cancel checkout");
		
		JSONObject f1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new CR to unchecked configuration rule in branch: " + response);
		
		//checkout entitlementID1 for future workflow
		br.checkoutFeature(branchID, entitlementID3, sessionToken);
	}
	
	@Test (dependsOnMethods="moveNewConfigToUncheckedConfiguration", description ="Move new CR to checked out configuration rule") 
	public void moveNewConfigToCheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(cr);
		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToCheckedConfiguration", description ="Move new CR to new configuration") 
	public void moveNewConfigToNewConfiguration () throws IOException, JSONException {
		//add new configuration		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "CR5");
		configID5 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule5 was not added to the season: " + configID1);
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID5, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToNewConfiguration", description ="Move unchecked CR to unchecked out configuration rule") 
	public void moveUncheckedConfigToUncheckedConfiguration () throws IOException, JSONException {
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "dummyCR1");
		String dummyCR1Id = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);

		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(dummyCR1Id, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("configurationRules");
		children.put(cr);
		
		e1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToUncheckedConfiguration", description ="Move unchecked CR to checked out configuration rule") 
	public void moveUncheckedConfigToCheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(cr);
		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToCheckedConfiguration", description ="Move unchecked CR to new configuration") 
	public void moveUncheckedConfigToNewConfiguration () throws IOException, JSONException {		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID4, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID4, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToNewConfiguration", description ="Move checked CR to unchecked out configuration rule") 
	public void moveCheckedConfigToUncheckedConfiguration () throws IOException, JSONException {
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e1 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = e1.getJSONArray("configurationRules");
		children.put(cr);
		
		e1.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID1, e1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked CR to unchecked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToUncheckedConfiguration", description ="Move checked CR to checked out configuration rule") 
	public void moveCheckedConfigToCheckedConfiguration () throws IOException, JSONException {
		
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject jsonE = new JSONObject(entitlement);
		
		jsonE.put("name", "checkedE");
		String checkedE = purchasesApi.addPurchaseItem(seasonID, jsonE.toString(), "ROOT", sessionToken);
		Assert.assertFalse(checkedE.contains("error"), "checkedE was not added: " + checkedE);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "checkedCR1");
		String checkedCR1 = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID1, sessionToken);
		Assert.assertFalse(checkedCR1.contains("error"), "Configuration rule checkedCR1 was not added to the season: " + checkedCR1);

		br.checkoutFeature(branchID, checkedE, sessionToken);
		
		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(checkedCR1, branchID, sessionToken));
		JSONObject e2 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = e2.getJSONArray("configurationRules");
		children.put(cr);
		
		e2.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, configID2, e2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToCheckedConfiguration", description ="Move checked CR to new configuration") 
	public void moveCheckedConfigToNewConfiguration () throws IOException, JSONException {
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "checkedCR2");
		String checkedCR2 = purchasesApi.addPurchaseItemToBranch(seasonID, branchID, jsonCR.toString(), entitlementID4, sessionToken);
		Assert.assertFalse(checkedCR2.contains("error"), "Configuration rule checkedCR1 was not added to the season: " + checkedCR2);

		JSONObject cr = new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken));
		JSONObject e4 = new JSONObject(purchasesApi.getPurchaseItemFromBranch(checkedCR2, branchID, sessionToken));
		
		JSONArray children = e4.getJSONArray("configurationRules");
		children.put(cr);
		
		e4.put("configurationRules", children);
		
		String response = purchasesApi.updatePurchaseItemInBranch(seasonID, branchID, checkedCR2, e4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to new entitlement in branch: " + response);
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
