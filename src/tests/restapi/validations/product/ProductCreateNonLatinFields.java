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

public class ProductCreateNonLatinFields {
	protected String productID;
	protected String m_url;
	protected JSONObject json;
	protected ProductsRestApi p;
	protected String filePath;
	protected String product;
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
		public void frenchName() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		json = new JSONObject(product);
		json.put("name", "neérgie");
		String response = p.addProduct(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		baseUtils.printProductToFile(response);
	}
	
	@Test 
	public void frenchCodeIdentifier() throws JSONException, IOException{
		product = JSONUtils.generateUniqueString(product, 5, "name");
		json = new JSONObject(product);
		json.put("codeIdentifier", "nérgie");
		String response = p.addProduct(json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		baseUtils.printProductToFile(response);
	}
	
	@AfterTest
	private void reset(){
		//baseUtils.reset(p, productID, sessionToken); //products are not created
	}

}
