package tests.restapi.validations.product;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
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


public class ProductUpdateSpecialCharacters {
	protected String m_url;
	protected JSONObject json;
	protected List<String> illegalCharacters;
	protected List<String> legalCharacters;
	protected ProductsRestApi p;
	protected String productID;
	protected String product;
	protected String filePath;
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
		product = JSONUtils.generateUniqueString(product, 7, "name");
		productID = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID);
		illegalCharacters= new ArrayList<String>(Arrays.asList("(", ")", "[", "]", "{", "}", "|", "/", "\\", "\"", ">", "<", 
				",", "!", "?", "@", "#", "$", "%", "^", "&", "*", "~", ";", "'", "-","_"));
		legalCharacters= new ArrayList<String>(Arrays.asList(" ", "."));

	}
	
	//in name we allow literals, numbers and spaces
	//in namespace we allow literals and numbers
	
	@Test
	public void legalCharactersInName() throws IOException, JSONException{
		String product1 = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product1);
		String newName = RandomStringUtils.randomAlphabetic(4) + " 123";
		json.put("name", newName );
		json.remove("seasons");
		String response =p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );
		
		String product2 = p.getProduct(productID, sessionToken);
		json = new JSONObject(product2);
		newName = RandomStringUtils.randomAlphabetic(4) + ".123";
		json.put("name", newName);
		json.remove("seasons");
		response =p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertFalse(productID.contains("error"), "Test should succeed, but instead failed: " + productID );

	}
	
	@Test (dependsOnMethods = "legalCharactersInName")
	public void dotInName() throws IOException, JSONException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		//should fail as space (above) and dot are both translated to the same character, so this feature name already exists
		json.put("name", "name.123");	
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
		
	@Test 
	public void spaceInCodeIdentifier() throws JSONException, IOException{
			product = p.getProduct(productID, sessionToken);
			JSONObject json = new JSONObject(product);
			json.put("codeIdentifier", "name 123");
			String response = p.updateProduct(productID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );	
	}
	
	@Test 
	public void illegalCharactersInName() throws JSONException, IOException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		for (String character : illegalCharacters) {
			json.put("name", "name" + character + "123");
			String response = p.updateProduct(productID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		}		
	}
	
	@Test 
	public void illegalCharactersInCodeIdentifier() throws JSONException, IOException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		for (String character : illegalCharacters) {
			json.put("codeIdentifier", "name" + character + "123");
			String response = p.updateProduct(productID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
		}		
	}
	
	@Test 
	public void codeIdentifierStartWithNumber() throws JSONException, IOException{
		product = p.getProduct(productID, sessionToken);
		JSONObject json = new JSONObject(product);
		json.put("codeIdentifier", "123name");
		String response = p.updateProduct(productID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
				
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
