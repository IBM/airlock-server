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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;


public class OverrrideContextRuleInEntitlement {
	private String seasonID;
	private String entitlementID;
	private String productID;
	private String filePath;
	private InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	private ProductsRestApi p;
	private String sessionToken = "";
	private String entitlement;
	private InputSchemaRestApi schema;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(url);
        schema = new InputSchemaRestApi();
        schema.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		p = new ProductsRestApi();
		p.setURL(url);
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		
		//add schema
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	
	@Test(description = "Override context add entitlement rule")
	public void addRule() throws JSONException, IOException{
		JSONObject json = new JSONObject(entitlement);
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", "context.viewedLocation.country = \"US\"");
		json.put("rule", ruleObj);
		json.put("minAppVersion", "7.8");
		String response = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("trying to modify"), "Incorrect error on rule with context override");
	}
	
	@Test(dependsOnMethods="addRule", description = "Override context update entitlement rule")
	public void updateRule() throws JSONException, IOException{
		entitlementID = purchasesApi.addPurchaseItem(seasonID, entitlement, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement was not added");
		
		JSONObject json = new JSONObject(entitlement);
		JSONObject ruleObj = new JSONObject();
		ruleObj.put("ruleString", "context.viewedLocation.country = \"US\"");
		json.put("rule", ruleObj);
		json.put("minAppVersion", "7.8");
		String response = purchasesApi.updatePurchaseItem(seasonID, entitlementID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("trying to modify"), "Incorrect error on rule with context override");
	}
	
	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
