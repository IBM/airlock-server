package tests.restapi.scenarios.rules;

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
import tests.restapi.StringsRestApi;

public class InvalidFieldTypeInRuleInEntitlement {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String filePath;
	private AirlockUtils baseUtils;
	private String entitlement;
	private ProductsRestApi p;
	private String sessionToken = "";
	private StringsRestApi t;
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
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
	
	@Test (description = "Apply action allowed for strings to the field that is defined as numeric in schema in create entitlement")	
	public void createEntitlementWithRule() throws Exception{
		//add entitlement with rule
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);				
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "var a = context.viewedLocation.lon.split() ; a.length > 1;");	
		json.put("rule", rule);
		
		entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(entitlementID.contains("error"), "Test should fail, but instead passed: " + entitlementID );
	}
	
	@Test (description = "Apply action allowed for strings to the field that is defined as numeric in schema in update entitlement")	
	public void updateEntitlementWithRule() throws Exception{
		//add entitlement with rule
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		
		entitlement = purchasesApi.getPurchaseItem(entitlementID, sessionToken);		
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "var a = context.viewedLocation.lon.split() ; a.length > 1;");	
		json.put("rule", rule);
		
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
