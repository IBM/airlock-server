package tests.restapi.bvt;

 import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.*;

 import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

 /**
  * This is end to end test that verifies next scenario:
      Add product
      Add season
      Add input schema in development stage
      Add utility in development stage
      Add string
      Mark string for translations
      Mark string as Send to translation
      Add translation
      Add feature in development stage with a rule that uses input schema and utility
      Add configuration with attributes
      Add in-app purchase in development stage with a rule that uses input schema and utility
      Add purchase configuration with attributes
      Add purchase options in development stage with a rule that uses input schema and utility
          
      Add to analytics: input fields, feature, attributes
      Add global user
      Add product user
      Create second season
      Validate features in the second season
      Copy feature from season1 to season2
      Import feature from season1 to season2
      In season2 move feature and configuration to Production – should fail as schema field and utility are in development stage
      Update input schema to production
      Update utility to production
      In season2 move feature and configuration to Production – should pass correctly
      Delete product – should fail as there are features and assets in production stage
      Move all features to development
      Add branch
      Add variant
      Add experiment
      Create stream
      Update stream
      Update global user
      Update product user
      Delete stream
      Delete feature
      Delete purchase
      Delete season1
      Delete product
      Delete global user
      Delete product user
  */

 public class EndToEndBasicTest {
     protected String seasonID1;
     protected String seasonID2;
     protected String featureID1;
     protected String featureID2;
     protected String purchaseID1;
     protected String purchaseID2;
     protected String purchaseOptionsID1;
     protected String purchaseOptionsID2;
     protected String productID;
     protected String productUserID;
     protected String globalUserID;
     protected String schemaID;
     protected String config;
     protected String utilityID;
     protected String stringID;
     protected String streamID;
     protected FeaturesRestApi featureApi;
     protected ProductsRestApi productApi;
     protected SeasonsRestApi seasonApi;
     protected InputSchemaRestApi schemaApi;
     protected UtilitiesRestApi utilitiesApi;
     protected TranslationsRestApi translationApi;
     protected AnalyticsRestApi analyticsApi;
     protected StringsRestApi stringApi;
     protected StreamsRestApi streamApi;
     protected BranchesRestApi branchApi;
     protected ExperimentsRestApi experimentApi;
     protected OperationRestApi operationsApi ;
     protected InAppPurchasesRestApi purchasesApi;
     protected AirlockUtils baseUtils;
     protected String sessionToken = "";
     
     //protected String adminToken;

     @BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "adminUser", "adminPassword"})
 	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String adminUser, String adminPassword) throws Exception{

    	    String operUrl = url.replace("airlock/api/admin", "airlock/api/ops");
    	    //String purcahsesUrl = url.replace("features", "purchases");
     		
         config = configPath;
         productApi = new ProductsRestApi();
         seasonApi = new SeasonsRestApi();
         featureApi = new FeaturesRestApi();
         schemaApi = new InputSchemaRestApi();
         utilitiesApi = new UtilitiesRestApi();
         stringApi = new StringsRestApi();
         translationApi = new TranslationsRestApi();
         analyticsApi = new AnalyticsRestApi();
         streamApi = new StreamsRestApi();
         branchApi = new BranchesRestApi();
         experimentApi = new ExperimentsRestApi();
         operationsApi = new OperationRestApi();
         purchasesApi = new InAppPurchasesRestApi();
  		

         productApi.setURL(url);
         seasonApi.setURL(url);
         featureApi.setURL(url);
         schemaApi.setURL(url);
         utilitiesApi.setURL(url);
         stringApi.setURL(translationsUrl);
         translationApi.setURL(translationsUrl);
         analyticsApi.setURL(analyticsUrl);
         streamApi.setURL(url);
         branchApi.setURL(url);
         experimentApi.setURL(analyticsUrl);
         operationsApi.setURL(operUrl);
         purchasesApi.setURL(url);

         baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
       	 if (adminUser!= null && !adminUser.equals("") && adminPassword != null && !adminPassword.equals("") && appName != null && !appName.equals(""))
    		 sessionToken = baseUtils.getJWTToken(adminUser, adminPassword, appName);
    	 else
             sessionToken = baseUtils.sessionToken;

     }


     @Test(description = "Add product")
     public void addProduct() throws IOException, JSONException {

         String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
         product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
         product = JSONUtils.generateUniqueString(product, 8, "name");
         productID = productApi.addProduct(product, sessionToken);
         baseUtils.printProductToFile(productID);
         Assert.assertNotNull(productID);
         Assert.assertFalse(productID.contains("error"), "Product was not created: " + productID);

         String productId = productApi.getProduct(productID, sessionToken);
         Assert.assertNotNull(productId);
         Assert.assertFalse(productId.contains("error"), "Product was not retrieved: " + productId);

     }

     @Test(dependsOnMethods = "addProduct",  description = "Add product user")
     public void addProductUser() throws Exception {

         String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);
         JSONObject userObj = new JSONObject(user);
         userObj.put("identifier", RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com");
         productUserID = operationsApi.addProductAirlockUser(userObj.toString(), productID, sessionToken);
         Assert.assertNotNull(productUserID);
         Assert.assertFalse(productUserID.contains("error"), "Product user was not created: " + productID);

         String tmpProductUser = operationsApi.getAirlockUser(productUserID, sessionToken);
         Assert.assertNotNull(tmpProductUser);
         Assert.assertFalse(tmpProductUser.contains("error"), "Product user was not retrieved: " + tmpProductUser);
         String tmpProductUserID = getUniqueId(tmpProductUser);
         Assert.assertEquals(tmpProductUserID, productUserID, "wrong user id.");
     }

     private String getUniqueId(String src) throws JSONException {
    	 	JSONObject obj = new JSONObject(src);
    	 	return obj.getString("uniqueId");
     }
     
     @Test(dependsOnMethods = "addProductUser",  description = "update product user")
     public void updateProductUser() throws Exception {
         String response = operationsApi.getAirlockUser(productUserID, sessionToken);
         Assert.assertFalse(response.contains("error"), "Product user was not retrieved: " + response);
         
         JSONObject user = new JSONObject(response);
         Assert.assertTrue(user.getJSONArray("roles").size() == 2, "wrong number of roles before update");
         
         JSONArray newRoles = new JSONArray();
         newRoles.add("Viewer");
         //user.put("lastName", "kuku");
         user.put("roles", newRoles);
         
         response = operationsApi.updateAirlockUser(productUserID, user.toString(), sessionToken);
         Assert.assertFalse(response.contains("error"), "Product user was not updated: " + response);
         
         response = operationsApi.getAirlockUser(productUserID, sessionToken);
         Assert.assertFalse(response.contains("error"), "Product user was not retrieved: " + response);
         
         JSONObject updatedUser = new JSONObject(response);
        // Assert.assertTrue(updatedUser.getString("lastName").equals("kuku"), "Product user was not updated: " + response);
         Assert.assertTrue(updatedUser.getJSONArray("roles").size() == 1, "wrong number of roles before update");
         
     }
     
     @Test(dependsOnMethods = "updateProductUser",  description = "delete product user")
     public void deleteProductUser() throws Exception {
         int code = operationsApi.deleteAirlockUser(productUserID, sessionToken);
         Assert.assertTrue(code == 200, "Product user was not deleted: " + code);
         
         String response = operationsApi.getAirlockUser(productUserID, sessionToken);
         Assert.assertTrue(response.contains("error"), "Product user was not deleted.");
     }
    
     @Test(dependsOnMethods = "deleteProductUser",  description = "Add global user")
     public void addGlobalUser() throws Exception {

         String user = FileUtils.fileToString(config + "airlockUser.txt", "UTF-8", false);
         JSONObject userObj = new JSONObject(user);
         userObj.put("identifier", RandomStringUtils.randomAlphabetic(5) + "@il.ibm.com");
         
         globalUserID = operationsApi.addGlobalAirlockUser(userObj.toString(), sessionToken);
         Assert.assertNotNull(globalUserID);
         Assert.assertFalse(globalUserID.contains("error"), "Global was not created: " + productID);

         String tmpUser = operationsApi.getAirlockUser(globalUserID, sessionToken);
         Assert.assertNotNull(tmpUser);
         Assert.assertFalse(tmpUser.contains("error"), "Global user was not retrieved: " + tmpUser);
         String tmpUserID = getUniqueId(tmpUser); 
         Assert.assertEquals(tmpUserID, globalUserID, "wrong user id.");
     }

     @Test(dependsOnMethods = "addGlobalUser",  description = "delete global user")
     public void deleteGlobalUser() throws Exception {
         int code = operationsApi.deleteAirlockUser(globalUserID, sessionToken);
         Assert.assertTrue(code == 200, "Global user was not deleted: " + code);
         
         String response = operationsApi.getAirlockUser(globalUserID, sessionToken);
         Assert.assertTrue(response.contains("error"), "Global user was not deleted.");
     }
     
     
     @Test(dependsOnMethods = "addProduct", description = "Add season")
     public void addSeason() throws Exception {

         String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
         seasonID1 = seasonApi.addSeason(productID, season, sessionToken);
         Assert.assertNotNull(seasonID1);
         Assert.assertFalse(seasonID1.contains("error"), "Season was not created: " + seasonID1);

         JSONArray seasonsInProduct = seasonApi.getSeasonsPerProduct(productID, sessionToken);
         Assert.assertEquals(seasonsInProduct.size(), 1, "The number of seasons is incorrect. " + seasonsInProduct.size());

     }

     @Test(dependsOnMethods = "addSeason", description = "Add input schema in development stage")
     public void addInputSchemaDev() throws Exception {

         String schema = schemaApi.getInputSchema(seasonID1, sessionToken);
         String file = FileUtils.fileToString(config + "inputSchema.txt", "UTF-8", false);
         JSONObject is = new JSONObject(file);
         JSONObject jsonSchema = new JSONObject(schema);
         jsonSchema.put("inputSchema", is);
         schemaID = schemaApi.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
         Assert.assertNotNull(schemaID);
         Assert.assertFalse(schemaID.contains("error"), "Schema was not added: " + schemaID);

         String inputSchema = schemaApi.getInputSchema(seasonID1, sessionToken);
         Assert.assertNotNull(inputSchema);
         Assert.assertFalse(inputSchema.contains("error"), "Input Schema was not retrieved: "+ inputSchema);
     }

     @Test(dependsOnMethods = "addInputSchemaDev", description = "Add utility in development stage")
     public void addUtility() throws IOException, JSONException {

         String utility = FileUtils.fileToString(config + "/utilities/utility1.txt", "UTF-8", false);
         Properties utilProps1 = new Properties();
         utilProps1.load(new StringReader(utility));
         utilityID = utilitiesApi.addUtility(seasonID1, utilProps1, sessionToken);
         Assert.assertNotNull(utilityID);
         Assert.assertFalse(utilityID.contains("error"), "Utility was not added: " + utilityID);

         String utilityDev = utilitiesApi.getAllUtilites(seasonID1, sessionToken, "DEVELOPMENT");
         Assert.assertTrue(utilityDev.contains("isTrue()"));

         String utilityProd = utilitiesApi.getAllUtilites(seasonID1, sessionToken, "PRODUCTION");
         Assert.assertFalse(utilityProd.contains("isTrue()"));

     }

     @Test(dependsOnMethods = "addUtility", description = "Add string")
     public void addString() throws Exception {

         String str = FileUtils.fileToString(config + "/strings/string2.txt", "UTF-8", false);
         stringID = stringApi.addString(seasonID1, str, sessionToken);
         Assert.assertNotNull(stringID);
         Assert.assertFalse(stringID.contains("error"), "String was not added: " + stringID);

     }

     @Test(dependsOnMethods = "addString", description = "Mark string for translations")
     public void markForTranslation() throws Exception {

         String responseMarkTrans = translationApi.markForTranslation(seasonID1, new String[]{stringID}, sessionToken);
         Assert.assertNotNull(responseMarkTrans);
         Assert.assertFalse(responseMarkTrans.contains("error"), "String was not marked for translation: "  + responseMarkTrans);

     }

     @Test(dependsOnMethods = "markForTranslation", description = "Mark string as complete review")
     public void reviewTranslation() throws Exception {

         String responseSendTrans = translationApi.reviewForTranslation(seasonID1, new String[]{stringID}, sessionToken);
         Assert.assertNotNull(responseSendTrans);
         Assert.assertFalse(responseSendTrans.contains("error"), "String was reviewed to translation: "  + responseSendTrans);

     }
     @Test(dependsOnMethods = "reviewTranslation", description = "Mark string as Send to translation")
     public void markSendToTranslation() throws Exception {

         String responseSendTrans = translationApi.sendToTranslation(seasonID1, new String[]{stringID}, sessionToken);
         Assert.assertNotNull(responseSendTrans);
         Assert.assertFalse(responseSendTrans.contains("error"), "String was sent to translation: "  + responseSendTrans);

     }

     @Test(dependsOnMethods = "markSendToTranslation", description = "Add translation")
     public void addTranslation() throws Exception {

         String frTranslation = FileUtils.fileToString(config + "strings/translationFR.txt", "UTF-8", false);
         String responseAddTrans = translationApi.addTranslation(seasonID1, "fr", frTranslation, sessionToken);
         Assert.assertNotNull(responseAddTrans);
         Assert.assertFalse(responseAddTrans.contains("error"), "String was added to translation: "  + responseAddTrans);

     }

     @Test(dependsOnMethods = "addTranslation", description = "Add feature in development stage with a rule that uses input schema and utility")
     public void addFeature1() throws Exception {

         String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);

         JSONObject fJson = new JSONObject(feature);
         fJson.put("minAppVersion", "1.1.1");
         JSONObject rule = new JSONObject();
         rule.put("ruleString", "context.viewedLocation.country == isTrue()");
         fJson.put("rule", rule);

         featureID1 = featureApi.addFeature(seasonID1, fJson.toString(), "ROOT", sessionToken);
         Assert.assertNotNull(featureID1);
         Assert.assertFalse(featureID1.contains("error"), "Feature was not added: " + featureID1);

         JSONArray features = featureApi.getFeaturesBySeason(seasonID1, sessionToken);
         Assert.assertEquals(features.size(), 1, "The number of features is incorrect. " + features.size());
     }

     @Test(dependsOnMethods = "addFeature1", description = "Add configuration with attributes")
     public void addConfiguration() throws Exception {

         String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
         JSONObject jsonConfig = new JSONObject(configuration);
         JSONObject newConfiguration = new JSONObject();
         newConfiguration.put("color", "red");
         newConfiguration.put("minAppVersion", "0.1");
         jsonConfig.put("configuration", newConfiguration);
         jsonConfig.put("name", "CR2");       
         String response = featureApi.addFeature(seasonID1, jsonConfig.toString(), featureID1, sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Configuration was not added: "  + response);

         String feature1 = featureApi.getFeature(featureID1, sessionToken);
         Assert.assertTrue(feature1.contains("color") && feature1.contains("red"), "Feature doesn't contain configuration color:red " + feature1);

     }

     @Test(dependsOnMethods = "addConfiguration", description = "Add to analytics: input fields, feature, attributes")
     public void addAnalytics() throws Exception {

         String response1 = analyticsApi.addFeatureToAnalytics(featureID1, BranchesRestApi.MASTER, sessionToken);
         Assert.assertNotNull(response1);
         Assert.assertFalse(response1.contains("error"), "Feature was not added to analytics: "  + response1);

         String response2 = analyticsApi.addAttributesToAnalytics(featureID1, BranchesRestApi.MASTER,"[]", sessionToken);
         Assert.assertNotNull(response2);
         Assert.assertFalse(response2.contains("error"), "Feature attributes were not added to analytics: "  + response2);

         JSONArray inputFields = new JSONArray();
         //field in DEVELOPMENT stage
         inputFields.put("context.device.locale");
         String response3 = analyticsApi.updateInputFieldToAnalytics(seasonID1, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
         Assert.assertNotNull(response3);
         Assert.assertFalse(response3.contains("error"), "Input fields were not added to analytics: "  + response3);

     }

     
     @Test(dependsOnMethods = "addAnalytics", description = "Add purchase in development stage with a rule that uses input schema and utility")
     public void addPurchase1() throws Exception {

         String purcahse = FileUtils.fileToString(config + "purchases/inAppPurchase1.txt", "UTF-8", false);

         JSONObject pJson = new JSONObject(purcahse);
         pJson.put("minAppVersion", "1.1.1");
         JSONObject rule = new JSONObject();
         rule.put("ruleString", "context.viewedLocation.country == isTrue()");
         pJson.put("rule", rule);

         purchaseID1 = purchasesApi.addPurchaseItem(seasonID1, pJson.toString(), "ROOT", sessionToken);
         Assert.assertNotNull(purchaseID1);
         Assert.assertFalse(purchaseID1.contains("error"), "Purchase item was not added: " + purchaseID1);

         JSONArray purchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
         Assert.assertEquals(purchases.size(), 1, "The number of purchases is incorrect. " + purchases.size());
     }

     @Test(dependsOnMethods = "addPurchase1", description = "Add configuration with attributes to purcahse")
     public void addConfigurationToPurchase() throws Exception {

         String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
         JSONObject jsonConfig = new JSONObject(configuration);
         JSONObject newConfiguration = new JSONObject();
         newConfiguration.put("color", "red");
         newConfiguration.put("minAppVersion", "0.1");
         jsonConfig.put("configuration", newConfiguration);
         jsonConfig.put("name", "CR1");
         String response = purchasesApi.addPurchaseItem(seasonID1, jsonConfig.toString(), purchaseID1, sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Configuration was not added: "  + response);

         String feature1 = purchasesApi.getPurchaseItem(purchaseID1, sessionToken);
         Assert.assertTrue(feature1.contains("color") && feature1.contains("red"), "Purchase doesn't contain configuration color:red " + feature1);

     }

     @Test(dependsOnMethods = "addConfigurationToPurchase", description = "Add to analytics: entitlement, attributes")
     public void addPurcahsesToAnalytics() throws Exception {

         String response1 = analyticsApi.addFeatureToAnalytics(purchaseID1, BranchesRestApi.MASTER, sessionToken);
         Assert.assertNotNull(response1);
         Assert.assertFalse(response1.contains("error"), "entitlement was not added to analytics: "  + response1);

         String response2 = analyticsApi.addAttributesToAnalytics(purchaseID1, BranchesRestApi.MASTER,"[]", sessionToken);
         Assert.assertNotNull(response2);
         Assert.assertFalse(response2.contains("error"), "entitlement attributes were not added to analytics: "  + response2);
     }

     ///
     @Test(dependsOnMethods = "addPurcahsesToAnalytics", description = "Add purchase options in development stage with a rule that uses input schema and utility")
     public void addPurchaseOptions1() throws Exception {

         String purcahseOpt = FileUtils.fileToString(config + "purchases/purchaseOptions1.txt", "UTF-8", false);

         JSONObject poJson = new JSONObject(purcahseOpt);
         poJson.put("minAppVersion", "1.1.1");
         JSONObject rule = new JSONObject();
         rule.put("ruleString", "context.viewedLocation.country == isTrue()");
         poJson.put("rule", rule);

         purchaseOptionsID1 = purchasesApi.addPurchaseItem(seasonID1, poJson.toString(), purchaseID1, sessionToken);
         Assert.assertNotNull(purchaseOptionsID1);
         Assert.assertFalse(purchaseOptionsID1.contains("error"), "Purchase options was not added: " + featureID1);

         JSONArray purchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
         Assert.assertEquals(purchases.size(), 1, "The number of in-app purchases is incorrect. " + purchases.size());
         
         JSONArray purchaseOptions = purchases.getJSONObject(0).getJSONArray("purchaseOptions");
         Assert.assertEquals(purchaseOptions.size(), 1, "The number of purchase options is incorrect. " + purchaseOptions.size()); 
     }

     @Test(dependsOnMethods = "addPurchaseOptions1", description = "Add configuration with attributes to purcahse")
     public void addConfigurationToPurchaseOptions() throws Exception {

         String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
         JSONObject jsonConfig = new JSONObject(configuration);
         JSONObject newConfiguration = new JSONObject();
         newConfiguration.put("color", "green");
         newConfiguration.put("minAppVersion", "0.1");
         jsonConfig.put("configuration", newConfiguration);
         jsonConfig.put("name","cr"+RandomStringUtils.randomAlphabetic(5));
         String response = purchasesApi.addPurchaseItem(seasonID1, jsonConfig.toString(), purchaseOptionsID1, sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Configuration was not added: "  + response);

         String pi = purchasesApi.getPurchaseItem(purchaseOptionsID1, sessionToken);
         Assert.assertTrue(pi.contains("color") && pi.contains("green"), "Purchase doesn't contain configuration color:green " + pi);

     }
     
     @Test(dependsOnMethods = "addConfigurationToPurchaseOptions", description = "Add branch")
     public void addBranch() throws IOException{
	    	 String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
	    	 String response = branchApi.createBranch(seasonID1, branch, BranchesRestApi.MASTER, sessionToken);
	    	 Assert.assertFalse(response.contains("error"), "Branch was not created: "  + response);	    	 
     }
     
     @Test(dependsOnMethods = "addBranch", description = "Add experiment and variant")
     public void addExperimentAndVariant() throws IOException, JSONException{
    	String experiment = FileUtils.fileToString(config + "experiments/experiment1.txt", "UTF-8", false);
 		JSONObject expJson = new JSONObject(experiment);
 		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
 		expJson.put("enabled", false);
 		String experimentID =  experimentApi.createExperiment(productID, expJson.toString(), sessionToken);
    	 	Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: "  + experimentID);
    	 
     	String variant = FileUtils.fileToString(config + "experiments/variant1.txt", "UTF-8", false);
  		String response =  experimentApi.createVariant(experimentID, variant.toString(), sessionToken);
     	 Assert.assertFalse(response.contains("error"), "Variant was not created: "  + response);
     }

     @Test(dependsOnMethods = "addExperimentAndVariant", description = "Create second season")
     public void addSeason2() throws Exception {

         String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
         seasonID2 = seasonApi.addSeason(productID, season, sessionToken);
         Assert.assertNotNull(seasonID2);
         Assert.assertFalse(seasonID2.contains("error"), "Season was not created: " + seasonID2);

         JSONArray seasonsInProduct = seasonApi.getSeasonsPerProduct(productID, sessionToken);
         Assert.assertEquals(seasonsInProduct.size(), 2, "The number of seasons is incorrect. " + seasonsInProduct.size());

     }

     @Test(dependsOnMethods = "addSeason2", description = "Validate features in the second season")
     public void validateFeatures() throws Exception {

         JSONArray features = featureApi.getFeaturesBySeason(seasonID2, sessionToken);
         Assert.assertEquals(features.size(), 1, "The number of features is incorrect. " + features.size());
         JSONObject feature1 = features.getJSONObject(0);

         features = featureApi.getFeaturesBySeason(seasonID1, sessionToken);
         Assert.assertEquals(features.size(), 1, "The number of features is incorrect. " + features.size());
         JSONObject feature2 = features.getJSONObject(0);
         
         // min app version will be different
         //Assert.assertTrue(featureApi.jsonObjsAreEqual(new JSONObject(feature1), new JSONObject(feature2)), "\nfeature1: "  feature1  "\nfeature2: "  feature2  "\n");

     }

     @Test(dependsOnMethods = "validateFeatures", description = "Validate purchases in the second season")
     public void validatePurchases() throws Exception {

         JSONArray purchases = purchasesApi.getPurchasesBySeason(seasonID2, sessionToken);
         Assert.assertEquals(purchases.size(), 1, "The number of purchases is incorrect. " + purchases.size());
         JSONObject purchase1 = purchases.getJSONObject(0);
         String purchase1ID = purchase1.getString("uniqueId");

         purchases = purchasesApi.getPurchasesBySeason(seasonID1, sessionToken);
         Assert.assertEquals(purchases.size(), 1, "The number of features is incorrect. " + purchases.size());
         JSONObject purchase2 = purchases.getJSONObject(0);
         String purchase2ID = purchase2.getString("uniqueId");

         Assert.assertFalse(purchase1ID.equals(purchase2ID), "purchase is created with the same id in the new season");
         
         JSONArray poArr = purchase1.getJSONArray("purchaseOptions");
         Assert.assertTrue(poArr.size() == 1, "wrong purchase options number");
         Assert.assertFalse(purchaseOptionsID1.equals(poArr.getJSONObject(0).getString("uniqueId")), "wrong purchase options id after season duplication");    
     }

     @Test(dependsOnMethods = "validatePurchases", description = "Add second feature")
     public void addFeature2() throws Exception {
         String feature = FileUtils.fileToString(config + "feature2.txt", "UTF-8", false);
         featureID2 = featureApi.addFeature(seasonID1, feature, "ROOT", sessionToken);
         Assert.assertNotNull(featureID2);
         Assert.assertFalse(featureID2.contains("error"), "Feature 2 was not created: " + featureID2);
     }
     
     @Test(dependsOnMethods = "addFeature2", description = "Add second feature")
     public void addPurcahse2() throws Exception {
    	 	String purcahse = FileUtils.fileToString(config + "purchases/inAppPurchase2.txt", "UTF-8", false);

         JSONObject pJson = new JSONObject(purcahse);
         pJson.put("minAppVersion", "1.1.1");
         JSONObject rule = new JSONObject();
         rule.put("ruleString", "context.viewedLocation.country == isTrue()");
         pJson.put("rule", rule);

         purchaseID2 = purchasesApi.addPurchaseItem(seasonID1, pJson.toString(), "ROOT", sessionToken);

         Assert.assertNotNull(purchaseID2);
         Assert.assertFalse(purchaseID2.contains("error"), "Purcahse 2 was not created: " + purchaseID2);
     }

     @Test(dependsOnMethods = "addPurcahse2", description = "Copy feature from season1 to season2")
     public void copyFeatures() throws Exception {

         String rootId = featureApi.getRootId(seasonID2, sessionToken);

         String response = featureApi.copyFeature(featureID1, rootId, "ACT", null, "suffix3", sessionToken);
         Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: "  + response);

        JSONObject result = new JSONObject(response);
        String newFeature = featureApi.getFeature(result.getString("newSubTreeId"), sessionToken);
        String oldFeature = featureApi.getFeature(featureID1, sessionToken);
        JSONObject newFeatureJson = new JSONObject(newFeature);
        JSONObject oldFeatureJson = new JSONObject(oldFeature);
        // sendToAnalytics flag by design is not copied to new feature. So we just setting it to make sure comparison doesn't fail
        newFeatureJson.put("sendToAnalytics", oldFeatureJson.get("sendToAnalytics"));
        Assert.assertTrue(featureApi.jsonObjsAreEqual(newFeatureJson, oldFeatureJson), "\nnew: " + newFeature + "\nold: " + oldFeature + "\n");

     }

     @Test(dependsOnMethods = "copyFeatures", description = "Import feature from season1 to season2")
     public void importFeature() throws Exception {

         JSONArray features = featureApi.getFeaturesBySeason(seasonID2, sessionToken);
         int currentSize = features.size();

         String rootId = featureApi.getRootId(seasonID2, sessionToken);
         String featureToImport = featureApi.getFeature(featureID2, sessionToken);
         String response = featureApi.importFeature(featureToImport, rootId, "ACT", null, "suffix1", true, sessionToken);
         Assert.assertNotNull(response);
         Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not imported: "  + response);

         JSONObject result = new JSONObject(response);
         String newFeature = featureApi.getFeature(result.getString("newSubTreeId"), sessionToken);
         Assert.assertTrue(featureApi.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(featureToImport)));

         features = featureApi.getFeaturesBySeason(seasonID2, sessionToken);
         Assert.assertEquals(features.size(), currentSize + 1, "The number of features is incorrect. " + features.size());

     }

     @Test(dependsOnMethods = "importFeature", description = "In season2 move feature and configuration to Production – should fail as schema field and utility are in development stage")
     public void moveFeatureToProdFail() throws Exception {

         String feature = featureApi.getFeature(featureID1, sessionToken);
         JSONObject json = new JSONObject(feature);
         json.put("stage", "PRODUCTION");
         String response = featureApi.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);
         Assert.assertTrue(response.contains("error"), "Move feature to production should fail: "  + response);

     }

     @Test(dependsOnMethods = "moveFeatureToProdFail", description = "Update input schema to production")
     public void updateSchemeProd() throws Exception {

         String inputSchema = schemaApi.getInputSchema(seasonID1, sessionToken);
         JSONObject jsonSchema = new JSONObject(inputSchema);
         JSONObject viewedLocation = jsonSchema.getJSONObject("inputSchema").getJSONObject("properties")
                 .getJSONObject("context").getJSONObject("properties").getJSONObject("viewedLocation");
         viewedLocation.put("stage", "PRODUCTION");
         String response = schemaApi.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
         Assert.assertFalse(response.contains("error"), "Schema was not updated: "  + response);
     }

     @Test(dependsOnMethods = "updateSchemeProd", description = "Update utility to production")
     public void updateUtilityProd() throws Exception {

         String utility = utilitiesApi.getUtility(utilityID, sessionToken);
         JSONObject jsonUtil = new JSONObject(utility);
         jsonUtil.put("stage", "PRODUCTION");
         String response = utilitiesApi.updateUtility(utilityID, jsonUtil, sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Utility was not updated to PRODUCTION: "  + response);

     }

     @Test(dependsOnMethods = "updateUtilityProd", description = "In season2 move feature and configuration to Production – should pass correctly")
     public void moveFeatureToProd() throws Exception {

         String feature = featureApi.getFeature(featureID1, sessionToken);
         JSONObject json = new JSONObject(feature);
         json.put("stage", "PRODUCTION");
         String response = featureApi.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Feature was not moved to prod: "  + response);

     }

     @Test(dependsOnMethods = "moveFeatureToProd", description = "Delete product – should fail as there are features and assets in production stage")
     public void deleteProductFail() throws Exception {

         int response = productApi.deleteProduct(productID, sessionToken);
         Assert.assertNotEquals(response, 200, "deleteProduct should fail: code "  + response);

     }

     @Test(dependsOnMethods = "deleteProductFail", description = "Move all features to development")
     public void moveFeaturesToDev() throws Exception {

         String feature = featureApi.getFeature(featureID1, sessionToken);
         JSONObject json = new JSONObject(feature);
         json.put("stage", "DEVELOPMENT");
         String response = featureApi.updateFeature(seasonID1, featureID1, json.toString(), sessionToken);
         Assert.assertNotNull(response);
         Assert.assertFalse(response.contains("error"), "Feature was not moved to dev: "  + response);

     }

     @Test(dependsOnMethods = "moveFeaturesToDev", description = "Delete feature")
     public void deleteFeatures() throws Exception {

         int response1 = featureApi.deleteFeature(featureID1, sessionToken);
         Assert.assertEquals(response1, 200, "deleteFeature failed: code "  + response1);

         int response2 = featureApi.deleteFeature(featureID2, sessionToken);
         Assert.assertEquals(response2, 200, "deleteFeature failed: code "  + response2);

     }
     
     @Test(dependsOnMethods = "deleteFeatures", description = "Delete purcahses")
     public void deletePurchases() throws Exception {
    	 	int response0 = purchasesApi.deletePurchaseItem(purchaseOptionsID1, sessionToken);
         Assert.assertEquals(response0, 200, "deleteFeature failed: code "  + response0);

         int response1 = purchasesApi.deletePurchaseItem(purchaseID1, sessionToken);
         Assert.assertEquals(response1, 200, "deleteFeature failed: code "  + response1);

         int response2 = purchasesApi.deletePurchaseItem(purchaseID2, sessionToken);
         Assert.assertEquals(response2, 200, "deleteFeature failed: code "  + response2);
     }
     
     @Test(dependsOnMethods = "deleteFeatures", description = "Create stream")
 	public void createStream() throws IOException, JSONException{

		String stream = FileUtils.fileToString(config + "streams/stream1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(stream);
		json.put("minAppVersion", "0.5");
		streamID = streamApi.createStream(seasonID1, json.toString(), sessionToken);
		Assert.assertFalse(streamID.contains("error"), "createStream failed: "+ streamID);

	}
     
     @Test(dependsOnMethods = "createStream", description = "Update stream")
 	public void updateStream() throws Exception{

			String response = streamApi.getStream(streamID, sessionToken);
			JSONObject json = new JSONObject(response);
			json.put("description", "new stream description");
			response = streamApi.updateStream(streamID, json.toString(), sessionToken);

			Assert.assertFalse(response.contains("error"), "updateStream failed: "+ response);

	}
     
    @Test(dependsOnMethods = "updateStream", description = "Delete stream")
 	public void deleteStream() throws Exception{

		int respCode = streamApi.deleteStream(streamID, sessionToken);	
    	Assert.assertTrue(respCode == 200, "deleteStream failed ");

	}

     @Test(dependsOnMethods = "deleteStream", description = "Delete season1")
     public void deleteSeason1() throws Exception {

         int response = seasonApi.deleteSeason(seasonID1, sessionToken);
         Assert.assertEquals(response, 200, "deleteSeason failed: code "  + response);

     }

     @Test(dependsOnMethods = "deleteSeason1", description = "Delete product")
     public void deleteProduct() throws Exception {

         int response = productApi.deleteProduct(productID, sessionToken);
         Assert.assertEquals(response, 200, "deleteProduct failed: code "  + response);

     }

 }
