package tests.restapi.scenarios.product;

import java.io.IOException;












import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;

public class UniqueProductName {
	
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String product;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		productID = "";
		p = new ProductsRestApi();
 		p.setURL(url); 	
 		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);

		
	}
	
	
	@Test (description = "Add existing product name")
	public void testUniqueProductNameInCreation() throws IOException, JSONException{
		JSONObject productJson = new JSONObject(product);
		String productName = RandomStringUtils.randomAlphabetic(5);
		productJson.put("name", productName);
		productJson.put("codeIdentifier", productName);
		
		productID = p.addProduct(productJson.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Product was not created: " + productID );
		
		String response = p.addProduct(productJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Product was created twice ");

		
	}
	
	@Test (dependsOnMethods="testUniqueProductNameInCreation", description = "Update feature name to an existing name")
	public void testUniqueProductNameInUpdate() throws IOException, JSONException{

		//create the second product
		
		JSONObject secondProduct = new JSONObject();
		secondProduct.put("name", RandomStringUtils.randomAlphabetic(5)); 
		secondProduct.put("codeIdentifier", RandomStringUtils.randomAlphabetic(5)); 
		String productID2 = p.addProduct(secondProduct.toString(), sessionToken);
		
		//get the name of the first product and put it in the second product
		product = p.getProduct(productID, sessionToken);
		JSONObject firstProductJson = new JSONObject(product);
		JSONObject secondProductJson = new JSONObject(secondProduct);
		secondProductJson.put("name", firstProductJson.getString("name"));
		//update the second product - it's name is not unique
		String response = p.updateProduct(productID2, secondProductJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="testUniqueProductNameInUpdate", description = "Add feature with existing name upper case")
	public void testUniqueProductNameToUpper() throws JSONException, IOException{
		JSONObject productJson = new JSONObject(product);
		String name = productJson.getString("name");
		productJson.put("name", name.toUpperCase());
		String response = p.addProduct(productJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (dependsOnMethods="testUniqueProductNameToUpper", description = "Add feature with existing name low case")
	public void testUniqueProductNameToLower() throws JSONException, IOException{
		JSONObject productJson = new JSONObject(product);
		String name = productJson.getString("name");
		productJson.put("name", name.toLowerCase());
		String response = p.addProduct(productJson.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
