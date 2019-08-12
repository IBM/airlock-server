package tests.restapi.scenarios.rules;

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
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class NonExistingSchemaFieldInRuleInEntitlement {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private AirlockUtils baseUtils;
	private String filePath;
	private InAppPurchasesRestApi purchasesApi;
	private String entitlement;
	private ProductsRestApi p;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
	}
	
	@Test (description = "Create a entitlement with a rule that uses a field that doesn't exist in inputSchema")
	
	public void nonExistingFieldInCreate() throws JSONException, IOException{
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
				
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("rule", rule);
		
		entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(entitlementID.contains("error"), "Test should fail, but instead passed: " + entitlementID );
	}
	
	
	@Test (description = "Update a entitlement with a rule that uses a field that doesn't exist in inputSchema")
	public void nonExistingFieldInUpdate() throws JSONException, IOException{
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
						JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.newField == 1");
		json.put("rule", rule);
		
		entitlementID = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(entitlementID.contains("error"), "Test should fail, but instead passed: " + entitlementID );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
