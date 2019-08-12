package tests.restapi.scenarios.feature_with_configuration;

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
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;

public class DefaultConfigurationNoContext {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected InputSchemaRestApi is;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		is = new InputSchemaRestApi();
		is.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createSchema(seasonID);
	}

	@Test(description = "Add valid input schema")
	public void addValidInputSchema() throws Exception{
		String schema = is.getInputSchema(seasonID, sessionToken);
		JSONObject schemaJson = new JSONObject(schema);
		String inputSchema = FileUtils.fileToString(filePath + "inputSchema.txt", "UTF-8", false);

		schemaJson.put("inputSchema", new JSONObject(inputSchema));
		String result = is.updateInputSchema(seasonID, schemaJson.toString(), sessionToken);

		Assert.assertFalse(result.contains("error"), "Input schema was not added to the season");
				
	}	
	
	@Test (dependsOnMethods ="addValidInputSchema" , description = "defaultSchema can't contain fields that depend on 'context.'")
	public void createFeatureIncludingDefaultSchema() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature_defaultSchema.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken );
		 Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
