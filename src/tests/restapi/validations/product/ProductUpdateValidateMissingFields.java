package tests.restapi.validations.product;




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

public class ProductUpdateValidateMissingFields {
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected String product;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_url = url;
		product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		//generate random codeIdentifier
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");	
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
	}
	
	@Test 
	public void validateMissingDescription() throws JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		removeKey("description", json);
		removeKey("seasons", json);
		productID = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );
	}

	@Test 
	public void validateMissingName() throws JSONException{
		JSONObject json = new JSONObject(product);
		removeKey("name", json);
		removeKey("seasons", json);
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void validateMissingCodeIdentifier() throws JSONException{
		JSONObject json = new JSONObject(product);
		removeKey("codeIdentifier", json);
		removeKey("seasons", json);
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test
	public void validateMissingUniqueId() throws JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		removeKey("uniqueId", json);
		removeKey("seasons", json);
		productID = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );

	}
	
	@Test 
	public void validateMissingLastModified() throws JSONException{
		JSONObject json = new JSONObject(product);
		removeKey("lastModified", json);
		removeKey("seasons", json);
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	private void removeKey(String key, JSONObject json ){
		json.remove(key);
	}

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
