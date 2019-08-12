package tests.restapi.validations.inputschema;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.InputSchemaRestApi;;

public class ValidInputSchema {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected InputSchemaRestApi is;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
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
	
	@Test(dependsOnMethods = "addValidInputSchema", description = "Add valid input schema")
	public void getInputSample() throws Exception{
		String result = is.getInputSample(seasonID, "DEVELOPMENT", "1.1.1", sessionToken, "MAXIMAL", 0.7);

		Assert.assertFalse(result.contains("error"), "Couldn't get input sample" + result);
				
	}
	

	



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
