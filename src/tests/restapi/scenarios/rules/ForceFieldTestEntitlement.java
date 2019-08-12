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

public class ForceFieldTestEntitlement {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String filePath;
	private AirlockUtils baseUtils;
	private String entitlement;
	private ProductsRestApi p;
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
	}
	
	@Test (description = "When force=false create invalid rule")
	public void invalidRuleWithFalseForce() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("force", false);
		rule.put("ruleString", "test.viewedLocation.country");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@Test (dependsOnMethods="invalidRuleWithFalseForce", description = "When force=true create invalid rule")
	
	public void invalidRuleWithTrueForce() throws JSONException, IOException{
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "test.viewedLocation.country");
		json.put("rule", rule);
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
