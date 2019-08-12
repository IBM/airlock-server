package tests.restapi.unitest;


import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.TranslationsRestApi;

public class TranslationsBasicTest {
	
	protected String seasonID;
	protected String productID;
	protected String stringID;
	protected String filePath;
	protected String json;
	protected TranslationsRestApi t;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		t = new TranslationsRestApi();
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
	public void testGetTranslation() throws Exception{
		t.getTranslation(seasonID, "en", "DEVELOPMENT", sessionToken);
	}
	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
 			
	}
	
		
}
