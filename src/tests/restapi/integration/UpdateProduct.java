package tests.restapi.integration;

import java.io.IOException;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;

public class UpdateProduct {
	
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected String product;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}
	
	
	@Test (description = "Update product name")
	public void updateName() throws IOException, JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		String newName = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		json.put("name", newName);
		json.remove("seasons");
		p.updateProduct(productID, json.toString(), sessionToken);
		product = p.getProduct(productID, sessionToken);
		json = new JSONObject(product);
		Assert.assertTrue(json.getString("name").equals(newName), "Product name was not updated");
	}
	
	@Test (description = "Update CodeIdentifier")
	public void updateCodeIdentifier() throws IOException, JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		String newCodeIdentifier = RandomStringUtils.randomAlphabetic(3);
		json.put("codeIdentifier", newCodeIdentifier);
		json.remove("seasons");
		p.updateProduct(productID, json.toString(), sessionToken);
		product = p.getProduct(productID, sessionToken);
		json = new JSONObject(product);
		Assert.assertTrue(json.getString("codeIdentifier").equals(newCodeIdentifier), "Product codeIdentifier was not updated");

	}
	
	@Test (description = "Update uniqueId")
	public void updateUniqueId() throws IOException, JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.put("uniqueId", UUID.randomUUID());
		json.remove("seasons");
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test (description = "Update description")
	public void updateDescrption() throws IOException, JSONException{
		JSONObject json = new JSONObject(product);
		json.put("description", "new product description");
		json.remove("seasons");
		p.updateProduct(productID, json.toString(), sessionToken);
		product = p.getProduct(productID, sessionToken);
		json = new JSONObject(product);
		Assert.assertTrue(json.getString("description").equals("new product description"), "Product description was not updated");

	}
	
	@Test (description = "If nothing is changed in json, lastModified shouldn't change after update")
	public void noChangeInUpdate() throws JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		long currentDate = json.getLong("lastModified");
		json.remove("seasons");
		
		p.updateProduct(productID, json.toString(), sessionToken);
		product = p.getProduct(productID, sessionToken);
		long newDate = json.getLong("lastModified");
		Assert.assertTrue(currentDate == newDate, "lastModified date was updated");
		
	}
	
	@AfterTest 
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
