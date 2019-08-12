package tests.restapi.scenarios.rules;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;

public class TestRuleInEntitlement {
	private String seasonID;
	private String productID;
	private String filePath;
	private InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	private ProductsRestApi p;
	private String sessionToken = "";
	private int countEntitlement=0;
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
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

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


	@Test(dataProvider = "rulesReader", description = "Use context in comment in entitlement with rules")
	public void addRules(String rule) throws JSONException, IOException{
		countEntitlement++;
		String entitlementName  = "entitlementName" + countEntitlement;
		JSONObject json = new JSONObject(entitlement);
		JSONObject ruleObj = new JSONObject();
		rule = rule.replaceAll("\"", "'");
		ruleObj.put("ruleString", rule);
		json.put("rule", ruleObj);
		json.put("name", entitlementName);
		json.put("minAppVersion", "7.8");
		String entitlementID = purchasesApi.addPurchaseItem(seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID.contains("error"), "entitlement with rule " + rule + " was not added");
	}



	@DataProvider (name="rulesReader")
	private Iterator<Object[]> positiveScenarios() throws IOException{

		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
		BufferedReader br = new BufferedReader(new FileReader(filePath + "commentsInRules.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			if (line != "") {
				dataToBeReturned.add(new Object[] {line });
			}	
		}
		br.close();
		return dataToBeReturned.iterator(); 
	}	


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
