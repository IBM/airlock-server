package tests.restapi.copy_import.copy_purchases;

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
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CopyWithMissingAssets {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String mixID1;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private InputSchemaRestApi schema;
	private StringsRestApi t;
	private UtilitiesRestApi u;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String m_translationsUrl;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
	}
	
	@Test(description = "Add schema, utility and string")
	public void addAssests() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String schemaResponse = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(schemaResponse.contains("error"), "Schema was not added to the season" + schemaResponse);
		
        String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		t.addString(seasonID, str, sessionToken);
		
		String utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false);
		Properties utilProps = new Properties();
		utilProps.load(new StringReader(utility));
		utilProps.setProperty("utility", "function isTrue(){return true;}");
		utilProps.setProperty("minAppVersion", "1.0");
		
		String utilityID = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertFalse(utilityID.contains("error"), "Utility was not added: " + utilityID );
	}
	
	@Test(dependsOnMethods = "addAssests", description = "Add entitlements with rule using assets")
	public void addEntitlement() throws IOException, JSONException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "entitlement mix was not added to the season: " + mixID1);

		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		eJson.put("rule", rule);
		eJson.put("minAppVersion", "1.0");
		String entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "entitlement1 was not created: " + entitlementID1 );
		
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject eJson2 = new JSONObject(entitlement2);
		JSONObject rule2 = new JSONObject();
		rule2.put("ruleString", "isTrue()");
		eJson2.put("rule", rule2);
		eJson2.put("minAppVersion", "1.0");
		String entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eJson2.toString(), mixID1, sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "entitlement2 was not created: " + entitlementID2 );
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		String response = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, crJson.toString(), entitlementID1, sessionToken );
		Assert.assertFalse(response.contains("error"), "Configuration was not created: " + response );
	}
	
	@Test (dependsOnMethods = "addEntitlement", description="Create new product and season without schema")
	public void createNewProduct() throws IOException, JSONException{
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);
		
		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Copy entitlement to the new season with missing assests")
	public void simulateCopyEntitlementToRoot() throws IOException{	
		String rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(mixID1, rootId, "VALIDATE", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("\"missingUtilities\":[\"isTrue\"]"), "Entitlement was copied with missing assets");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}

}