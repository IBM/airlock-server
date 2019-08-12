package tests.restapi.validations.product;

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
import tests.restapi.ProductsRestApi;

public class ProductCreationValidateNullFields {
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected String product;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		p = new ProductsRestApi();
		p.setURL(url);
		product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);

	}
	
	@Test 
	public void validateMissingDescription() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		JSONObject json = new JSONObject(product);
		json.put("description", JSONObject.NULL);
		String productID = p.addProduct(json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );
		baseUtils.printProductToFile(productID);
	}

	@Test 
	public void validateMissingName() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		JSONObject json = new JSONObject(product);
		json.put("name", JSONObject.NULL);
		String response = p.addProduct(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void validateMissingCodeIdentifier() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		JSONObject json = new JSONObject(product);
		json.put("codeIdentifier", JSONObject.NULL);
		String response = p.addProduct(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test
	public void validateMissingUniqueId() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		JSONObject json = new JSONObject(product);
		json.put("uniqueId", JSONObject.NULL);
		String productID = p.addProduct(json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );
		baseUtils.printProductToFile(productID);
	}
	
	@Test 
	public void validateLastModified() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		JSONObject json = new JSONObject(product);
		json.put("lastModified", JSONObject.NULL);
		String productID = p.addProduct(json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );
		baseUtils.printProductToFile(productID);
	}

	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
