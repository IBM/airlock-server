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

public class TranslateInRuleInEntitlement {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String filePath;
	private InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	private String entitlement;
	private ProductsRestApi p;
	private String sessionToken = "";
	private StringsRestApi t;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		p = new ProductsRestApi();
		p.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
	}
	
	@Test (description = "Use translate() in rule - not allowed")
	
	public void useTranslateInRule() throws Exception{
		//add string
		String str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
		t.addString(seasonID, str, sessionToken);
		
		//add entitlement with rule
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);				
		JSONObject json = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "translate(\"app.hello\", context.userPreferences.unitsOfMeasure)");	
		json.put("rule", rule);
		
		entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(entitlementID.contains("error"), "Test should fail, but instead passed: " + entitlementID );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
