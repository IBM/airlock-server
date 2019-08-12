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

public class BooleanRuleValidationInPurchaseItems {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String filePath;
	private String entitlement;
	private String purchaseOptions;
	private String purchaseOptionsID;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		
		purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purchaseOptionsID = purchasesApi.addPurchaseItem(seasonID, purchaseOptions, entitlementID, sessionToken);
	}
	
	@Test (description = "Add single line comment in rule")
	public void singleLineCommentInRuleEntitlement() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "//");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test ( description = "Add single line comment and boolean expression in rule ")
	public void singleLineCommentAndExpressionInRuleEntitlement() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "//context.somefield\r\nfalse;");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	
	@Test (description = "Add multi line comment in rule")
	public void ruleWithoutBoolean1Entitlement() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "1+1");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Add non-boolean expression in rule")
	public void ruleWithoutBoolean2Entitlement() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "a=1");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test ( description = "Add boolean expression in rule")
	public void ruleWithBooleanEntitlement() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "a=1; a==true");

		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@Test (description = "Add single line comment in rule")
	public void singleLineCommentInRulePurchaseOptions() throws JSONException, IOException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "//");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test ( description = "Add single line comment and boolean expression in rule ")
	public void singleLineCommentAndExpressionInRulePurchaseOptions() throws JSONException, IOException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "//context.somefield\r\nfalse;");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (description = "Add multi line comment in rule")
	public void ruleWithoutBoolean1PurchaseOptions() throws JSONException, IOException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "1+1");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Add non-boolean expression in rule")
	public void ruleWithoutBoolean2PurchaseOptions() throws JSONException, IOException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "a=1");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test ( description = "Add boolean expression in rule")
	public void ruleWithBooleanPurchaseOptions() throws JSONException, IOException{
		purchaseOptions = purchasesApi.getPurchaseItem(purchaseOptionsID, sessionToken);
		JSONObject json = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		
		rule.put("ruleString", "a=1; a==true");

		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, purchaseOptionsID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
