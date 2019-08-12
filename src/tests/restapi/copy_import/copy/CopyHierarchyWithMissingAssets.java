package tests.restapi.copy_import.copy;

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
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class CopyHierarchyWithMissingAssets {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	private String productID2;
	protected String featureID1;
	protected String targetFeatureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	protected InputSchemaRestApi schema;
	protected StringsRestApi t;
	protected UtilitiesRestApi u;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
		u = new UtilitiesRestApi();
		u.setURL(url);
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		
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
	
	/*
		F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
					->F3 -> CR3 -> CR4
	 */
	
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
		
		String utility2 = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility2.txt", "UTF-8", false);
		Properties utilProps2 = new Properties();
		utilProps2.load(new StringReader(utility2));
		utilProps2.setProperty("utility", "function isFalse(){return false;}");
		utilProps2.setProperty("minAppVersion", "1.0");
		
		String utilityID2 = u.addUtility(seasonID, utilProps2, sessionToken);
		Assert.assertFalse(utilityID2.contains("error"), "Utility was not added: " + utilityID2 );

	}
	

	
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{

		


		
		//create feature tree in season1
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature2);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "isTrue()");
		fJson.put("rule", rule);
		fJson.put("minAppVersion", "2.0");
		String featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		JSONObject fJson3 = new JSONObject(feature3);
		JSONObject rule3 = new JSONObject();
		rule3.put("ruleString", "isFalse()");
		fJson3.put("rule", rule3);
		fJson3.put("minAppVersion", "2.0");
		String featureID3 = f.addFeatureToBranch(seasonID, srcBranchID, fJson3.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");
		
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		crJson.put("name", "CR1");
		crJson.put("minAppVersion", "2.0");
		String configuration1 =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration1);
		String configID1 = f.addFeatureToBranch(seasonID, srcBranchID, crJson.toString(), mixConfigID, sessionToken );
		Assert.assertFalse(configID1.contains("error"), "Configuration was not created: " + configID1 );
				
		crJson.put("name", "CR2");
		String configuration2 =  "{ \"text\" :  context.viewedLocation.country	}" ;
		crJson.put("configuration", configuration2);
		String configID2 = f.addFeatureToBranch(seasonID, srcBranchID, crJson.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		crJson.put("name", "CR3");
		String configuration3 =  "{ \"text\" :  context.device.connectionType	}" ;
		crJson.put("configuration", configuration3);
		String configID3 = f.addFeatureToBranch(seasonID, srcBranchID, crJson.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		crJson.put("name", "CR4");
		String configID4 = f.addFeatureToBranch(seasonID, srcBranchID, crJson.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
		


	}
	
	@Test (dependsOnMethods="addComponents", description="Add product2 and season2")
	public void addProduct() throws IOException, JSONException{
		
		//create product2 with season2
		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);

		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");
		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The second season was not created: " + seasonID2);
	}
	
	@Test (dependsOnMethods="addProduct", description="Copy feature to the new product with missing assests")
	public void simulateCopyFeatureToRoot() throws IOException, JSONException{
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
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(featureID1, rootId, "VALIDATE", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("isFalse") && response.contains("isTrue"), "Incorrect number of missingUtilities");
		Assert.assertTrue(response.contains("context.viewedLocation.country") && response.contains("context.device.connectionType"), "Incorrect number of missingFields");
	}
	
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}