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

public class ProductUpdateNonLatinFields {
	protected String m_url;
	protected JSONObject json;
	protected ProductsRestApi p;
	protected String filePath;
	protected String product;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 5, "name");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
	}
	
	@Test 
		public void frenchName() throws JSONException, IOException{
		product = p.getProduct(productID, sessionToken);
		json = new JSONObject(product);
		json.put("name", "néergie");
		String response =p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test 
	public void frenchCodeIdentifier() throws JSONException, IOException{
		product = p.getProduct(productID, sessionToken);
		json = new JSONObject(product);
		json.put("codeIdentifier", "nérgie");
		String response =p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	

		
	@AfterTest
	public void reset(){		
		baseUtils.reset(productID, sessionToken);
	}

	


}
