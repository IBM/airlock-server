package tests.restapi.unitest;


import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class StringsBasicTest {
	
	protected String seasonID;
	protected String productID;
	protected String stringID;
	protected String filePath;
	protected String json;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
 		
	}
	
	@Test 
	public void testGetAllString() throws Exception{
		t.getAllStrings(seasonID, sessionToken);			
	}
	
	@Test
	public void testAddString() throws Exception{
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}
	
	@Test(dependsOnMethods = "testAddString")
	public void testGetString() throws Exception{
		
		t.getString(stringID, sessionToken);
	}
	
	@Test(dependsOnMethods = "testAddString")
	public void testUpdateString() throws Exception{
		
		String str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("owner", "John");
		t.updateString(stringID, json.toString(), sessionToken);
	}
	
	@Test(dependsOnMethods = "testUpdateString")
	public void testDeleteString() throws Exception{
		
		t.deleteString(stringID, sessionToken);
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
 			
	}
	
		
}
