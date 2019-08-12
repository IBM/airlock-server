package tests.restapi.copy_import.import_purchases;

import java.io.IOException;
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

public class ImportWithMissingSchema {
	private String seasonID;
	private String seasonID2;
	private String productID;
	private String productID2;
	private String entitlementID1;
	private String entitlementID2;
	private String purchaseOptionsID1;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private BranchesRestApi br ;		
	private InputSchemaRestApi schema;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	private InAppPurchasesRestApi purchasesApi;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
	    br.setURL(m_url);
	    purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
	    
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
		this.runOnMaster = runOnMaster;
	}
	
	@Test(description = "Add schema")
	public void addSchema() throws Exception{
		
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String schemaResponse = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(schemaResponse.contains("error"), "Schema was not added to the season" + schemaResponse);	
	}
	
	@Test(dependsOnMethods = "addSchema", description = "Add entitlement with rule using schema field")
	public void addEntitlement() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(entitlement);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.weatherSummary.closestLightning < 2");
		fJson.put("rule", rule);
		fJson.put("minAppVersion", "2.0");
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not created: " + entitlementID1 );
	}
	
	@Test (dependsOnMethods = "addEntitlement", description="Create new product and season without schema")
	public void createNewProduct() throws Exception{
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
	
	@Test (dependsOnMethods="createNewProduct", description="Copy entitlement to the new season with missing schema")
	public void simulateCopyEntitlementToRoot() throws IOException{	
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID1, srcBranchID, sessionToken);
		
		String rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "VALIDATE", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("missingFields") && response.contains("context.weatherSummary.closestLightning"), "Missing assests were not recognized ");
	}

	
	@Test(dependsOnMethods = "simulateCopyEntitlementToRoot", description = "Add purchase options with rule using schema field")
	public void addPurcahseOptions() throws IOException, JSONException{
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("name", "E2");
		entitlementID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(entitlementID2.contains("error"), "Entitlement was not created: " + entitlementID2 );
		
		String purchaseOptions = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		JSONObject poJson = new JSONObject(purchaseOptions);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "context.weatherSummary.closestLightning < 2");
		poJson.put("rule", rule);
		poJson.put("minAppVersion", "2.0");
		purchaseOptionsID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, poJson.toString(), entitlementID2, sessionToken);
		Assert.assertFalse(purchaseOptionsID1.contains("error"), "purchaseOptions was not created: " + purchaseOptionsID1 );
	}
	
	@Test (dependsOnMethods="addPurcahseOptions", description="Copy entitlement with purchaseOptions to the new season with missing schema")
	public void simulateCopyEntitlementWithPurchaseOptionsToRoot() throws IOException{	
		String entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID2, srcBranchID, sessionToken);
		
		String rootId = purchasesApi.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.importFeatureToBranch(entitlementToImport, rootId, "VALIDATE", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("missingFields") && response.contains("context.weatherSummary.closestLightning"), "Missing assests were not recognized ");
		
		int code = purchasesApi.deletePurchaseItemFromBranch(purchaseOptionsID1, srcBranchID, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete purchaseOptionsID1");
		
		entitlementToImport = purchasesApi.getPurchaseItemFromBranch(entitlementID2, srcBranchID, sessionToken);
		response = f.importFeatureToBranch(entitlementToImport, rootId, "VALIDATE", null, null, true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "no missing assests were not recognized");
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}

}