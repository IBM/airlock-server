package tests.restapi.scenarios.inputSchema;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;


public class inputSchemaValidate {
    protected String seasonID;
    protected String productID;
    protected String featureID;
    protected String configID;
    protected String filePath;
    protected ProductsRestApi p;
    protected FeaturesRestApi f;
    protected InputSchemaRestApi schema;
    private String sessionToken = "";
    protected AirlockUtils baseUtils;
    protected String murl;

    @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
        filePath = configPath;
        murl=url;
       p = new ProductsRestApi();
        p.setURL(url);
        schema = new InputSchemaRestApi();
        schema.setURL(url);
        f = new FeaturesRestApi();
        f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
          productID = baseUtils.createProduct();
        baseUtils.printProductToFile(productID);
        seasonID = baseUtils.createSeason(productID);

    }


    @Test(description = "invalid params")
    private void validateSchemaNoFeature() {
        try {
            String sch = schema.getInputSchema(seasonID, sessionToken);
            JSONObject jsonSchema = new JSONObject(sch);
            String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
            JSONObject result = new JSONObject(schema.validateSchema(seasonID,schemaBody,sessionToken));
            Assert.assertTrue(result.getJSONArray("brokenConfigurations").size()==0 && result.getJSONArray("brokenExperiments").size()==0 && result.getJSONArray("brokenRules").size()==0 && result.getJSONArray("brokenVariants").size()==0, "Incorrect response");
            //Assert.assertTrue(result.replace("\n","").replace("\t","").equals("{\"brokenConfigurations\": [],\"brokenExperiments\": [],\"brokenRules\": [],\"brokenVariants\": []}"));
            jsonSchema.put("inputSchema", new JSONObject(schemaBody));
            schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }
    @Test(dependsOnMethods = "validateSchemaNoFeature",description = "unprotected feature and config")
    private void validateSchemaUnprotectedFeatureAndConfig() throws Exception {

            String feature = FileUtils.fileToString(filePath + "featureWithRule.txt", "UTF-8", false);
            featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
            Assert.assertFalse(featureID.contains("error"), "Feature was not added " + featureID);
            
            //add config with rule and field
            String configuration = FileUtils.fileToString(filePath + "configuration_rule3.txt", "UTF-8", false);
            JSONObject json = new JSONObject(configuration);
            configID = f.addFeature(seasonID, json.toString(), featureID, sessionToken );
            Assert.assertFalse(configID.contains("error"), "Configuration was not added " + configID);
           
            //without required
            String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilities.txt", "UTF-8", false);
            schemaBody= schemaBody.replace("teaserTitle","newfield");
            String result = schema.validateSchema(seasonID,schemaBody,sessionToken);
            JSONObject resultJson = new JSONObject(result);
            JSONArray brokenRules = (JSONArray) resultJson.get("brokenRules");
            Assert.assertTrue(brokenRules.size() == 2);
            Assert.assertTrue(((JSONObject)brokenRules.get(0)).getString("name").equals("ns1.FeatureWithRule"));
            Assert.assertTrue(((JSONObject)brokenRules.get(1)).getString("name").equals("ns1.CR3"));
            JSONArray brokenConfigurations = (JSONArray) resultJson.get("brokenConfigurations");
            Assert.assertTrue(brokenConfigurations.size() == 1);
            Assert.assertTrue(((JSONObject)brokenConfigurations.get(0)).getString("name").equals("ns1.CR3"));
            // without field
            schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaWithoutTeaserTitle.txt", "UTF-8", false);
            result = schema.validateSchema(seasonID,schemaBody,sessionToken);
            resultJson = new JSONObject(result);
            brokenRules = (JSONArray) resultJson.get("brokenRules");
            Assert.assertTrue(brokenRules.size() == 2);
            Assert.assertTrue(((JSONObject)brokenRules.get(0)).getString("name").equals("ns1.FeatureWithRule"));
            Assert.assertTrue(((JSONObject)brokenRules.get(1)).getString("name").equals("ns1.CR3"));
            brokenConfigurations = (JSONArray) resultJson.get("brokenConfigurations");
            Assert.assertTrue(brokenConfigurations.size() == 1);
            Assert.assertTrue(((JSONObject)brokenConfigurations.get(0)).getString("name").equals("ns1.CR3"));

            
            f.deleteFeature(featureID,sessionToken);
            f.deleteFeature(configID,sessionToken);

            resultJson = new JSONObject(schema.validateSchema(seasonID,schemaBody,sessionToken));
            
            //Assert.assertTrue(result.replace("\n","").replace("\t","").equals("{\"brokenConfigurations\": [],\"brokenExperiments\": [],\"brokenRules\": [],\"brokenVariants\": []}"));
            Assert.assertTrue(resultJson.getJSONArray("brokenConfigurations").size()==0 && resultJson.getJSONArray("brokenExperiments").size()==0 && resultJson.getJSONArray("brokenRules").size()==0 && resultJson.getJSONArray("brokenVariants").size()==0, "Incorrect response");

 
    }

    @Test(dependsOnMethods = "validateSchemaUnprotectedFeatureAndConfig",description = "protected feature and config")
    private void validateSchemaProtectedFeatureAndConfig() {
        try {
            String feature = FileUtils.fileToString(filePath + "featureWithRuleProtected.txt", "UTF-8", false);
            featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
            //add config with rule and field
            String configuration = FileUtils.fileToString(filePath + "configuration_ruleProtected.txt", "UTF-8", false);
            JSONObject json = new JSONObject(configuration);
            configID = f.addFeature(seasonID, json.toString(), featureID, sessionToken );
            Assert.assertFalse(configID.contains("error"));
           //without required
            String schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaForUtilitiesTeaserTitleNotRequired.txt", "UTF-8", false);
            
            JSONObject resultJson = new JSONObject(schema.validateSchema(seasonID,schemaBody,sessionToken));
            Assert.assertTrue(resultJson.getJSONArray("brokenConfigurations").size()==0 && resultJson.getJSONArray("brokenExperiments").size()==0 && resultJson.getJSONArray("brokenRules").size()==0 && resultJson.getJSONArray("brokenVariants").size()==0, "Incorrect response");

            // without field
            schemaBody = FileUtils.fileToString(filePath + "validInputSchema/inputSchemaWithoutTeaserTitle.txt", "UTF-8", false);
            String result = schema.validateSchema(seasonID,schemaBody,sessionToken);
            //Assert.assertTrue(result.replace("\n","").replace("\t","").equals("{\"brokenConfigurations\": [],\"brokenRules\": []}"));
            resultJson = new JSONObject(result);
            JSONArray brokenRules = (JSONArray) resultJson.get("brokenRules");
 
            Assert.assertTrue(brokenRules.size() == 2);
         }catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @AfterTest
    private void reset(){
    	baseUtils.reset(productID, sessionToken);
    }
}
