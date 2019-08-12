package tests.restapi.scenarios.rules;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
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
import tests.restapi.UtilitiesRestApi;


public class CreateVariableInRuleInEntitlement {
	private String seasonID;
	private String productID;
	private String filePath;
	private AirlockUtils baseUtils;
	private ProductsRestApi p;
	private String sessionToken = "";
	private String entitlement;
	private String configuration;
	private UtilitiesRestApi u;
	private InAppPurchasesRestApi purchasesApi;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
	}
		
	@Test(description = "Create global variable in rule and use it in configuration")
	public void addGlobalVar() throws JSONException, IOException{
		String ruleString = "myvar='stam';true";
		String confString = "{\"text\":myvar}";
		
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "E1");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("configuration", confString);
		jsonCR.put("name", "CR1");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );
	}

	
	@Test(description = "Create locale variable in rule and use it in configuration")
	public void addLocalVar() throws JSONException, IOException{
		String ruleString = "var myvar='stam';true";
		String confString = "{\"text\":myvar}";
		
		JSONObject json = new JSONObject(entitlement);
			json.put("name", "E2");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR2");
		jsonCR.put("configuration", confString);
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);

		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );
	}
	
	@Test(description = "Create locale variable in rule and use it in configuration")
	public void addUtility() throws JSONException, IOException{
		String ruleString = "function myfunc(){return \"stam\";}; true";
		String confString = "{\"text\":myfunc()}";
		
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "E3");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR3");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		jsonCR.put("rule", ruleObj);
	
		jsonCR.put("configuration", confString);
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID );		
	}
	
	//trying to define variable/utility in another entitlement rule and use it in configuration
	@Test(description = "Create utility in entitlement rule and use it in configuration")
	public void addUtilityInEntitlement() throws JSONException, IOException{
		String ruleString = "var myvar='stam';true";
		String confString = "{\"text\":myvar}";
		
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "E4a");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		json.put("rule", ruleObj);
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR4a");	
		jsonCR.put("configuration", confString);
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuraton should not access utility crated in a entitlement rule");
	}
	
	//trying to define variable/utility in another entitlement rule and use it in configuration
	@Test(description = "Create variable in entitlement rule and use it in configuration")
	public void addVariableInEntitlement() throws JSONException, IOException{
		String ruleString = "function myfunc(){return \"stam\";}; true";
		String confString = "{\"text\":myfunc}";
		
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "E4");
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", ruleString);
		json.put("rule", ruleObj);
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR4");	
		jsonCR.put("configuration", confString);
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertTrue(configID.contains("error"), "Configuraton should not access utility created in a a entitlement rule");
	}
	
	@Test(description = "Add valid utility")
	public void addVarInUtility() throws IOException, JSONException{
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "var varInUtil={\"first\":\"one\", \"second\":\"two\"}; function isTrue(){return true;}");
		utilProps.setProperty("minAppVersion", "1.0");
		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Utility was not added: " + utilityID );
		
		String confString = "{\"text\":varInUtil.first}";
		
		JSONObject json = new JSONObject(entitlement);
		json.put("name", "E5");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "Can't create rule with local variable");
		
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("name", "CR5");	
		jsonCR.put("configuration", confString);
		String configID = purchasesApi.addPurchaseItem(seasonID, jsonCR.toString(), entitlementID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Configuraton was not created: " + configID);
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
