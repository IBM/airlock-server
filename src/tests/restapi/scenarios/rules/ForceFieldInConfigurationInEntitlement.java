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

public class ForceFieldInConfigurationInEntitlement {
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
	
	//if force is used in configuration rule, its configuration shouldn't be validated
	
	@Test (description = "When force=false create invalid rule")
	public void invalidRuleWithFalseForce() throws JSONException, IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule3.txt", "UTF-8", false);
		String response = purchasesApi.addPurchaseItem(seasonID, configuration, entitlementID, sessionToken);
		Assert.assertTrue(response.contains("error"), "Configuraton should not be created: " + response );
	}
	
	
	@Test (dependsOnMethods="invalidRuleWithFalseForce", description = "When force=true create invalid configuration")
	public void invalidRuleWithTrueForce() throws JSONException, IOException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule3.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		JSONObject rule = new JSONObject();
		rule.put("force", true);
		rule.put("ruleString", "");
		jsonCR.put("rule", rule);
		
		String response = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Configuraton should not be created: " + response );
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
