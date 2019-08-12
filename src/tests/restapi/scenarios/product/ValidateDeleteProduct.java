package tests.restapi.scenarios.product;


import java.io.IOException;

import java.util.Properties;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.UtilitiesRestApi;

public class ValidateDeleteProduct {
	
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	protected InputSchemaRestApi schema;
	protected UtilitiesRestApi utilitiesApi;
	protected StringsRestApi stringsApi;
	private FeaturesRestApi f;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		productID = "";
		filePath = configPath;
		p = new ProductsRestApi();
 		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		f = new FeaturesRestApi();
		f.setURL(url);
		
        schema = new InputSchemaRestApi();
        schema.setURL(url);
		utilitiesApi = new UtilitiesRestApi();
		utilitiesApi.setURL(url);
		stringsApi = new StringsRestApi();
		stringsApi.setURL(translationsUrl);

	}
	
	//added: test bug, when product was deleted strings and utilities were not deleted.
	@Test (description = "Test that product is deleted")
	public void deleteProduct() throws Exception{
		try {
			String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
			product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
			product = JSONUtils.generateUniqueString(product, 8, "name");
			productID = p.addProduct(product, sessionToken);			
			baseUtils.printProductToFile(productID);
			
			String seasonID = baseUtils.createSeason(productID);
			
			//add schema
			String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android__AirlockInputShema_profile_turbo_NoTestData_WithSettings.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);

			//add utility
			Properties utilProps = new Properties();
			utilProps.setProperty("stage", "PRODUCTION");
			utilProps.setProperty("utility", "function A(){return true;}");
			String utilityID = utilitiesApi.addUtility(seasonID, utilProps, sessionToken);

			//add string
			
			String str = FileUtils.fileToString(filePath + "/strings/string1.txt", "UTF-8", false);
			String stringID = stringsApi.addString(seasonID, str, sessionToken);

			//add feature
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			
			p.deleteProduct(productID, sessionToken);
			productID = p.getProduct(productID, sessionToken);
			
			Assert.assertEquals("404", "404", "Got already deleted product");
			response = schema.getInputSchema(seasonID, sessionToken);
			Assert.assertTrue(response.contains("not found"), "Schema was not deleted");
			response = utilitiesApi.getUtility(utilityID, sessionToken);
			Assert.assertTrue(response.contains("not found"), "Utility was not deleted");
			response = stringsApi.getString(stringID, sessionToken);
			Assert.assertTrue(response.contains("not found"), "String was not deleted");
			response = f.getFeature(featureID, sessionToken);
			Assert.assertTrue(response.contains("not found"), "Feature was not deleted");

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 			
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}


}
