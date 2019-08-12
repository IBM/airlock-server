package tests.restapi.unitest;

import java.io.IOException;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;


public class ProductBasicTest {
	
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected String product;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		productID = "";
		filePath = configPath + "product1.txt";
		p = new ProductsRestApi();
		p.setURL(url);

		product = FileUtils.fileToString(filePath, "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");	
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}
	
	@Test
	public void testGetAllProducts(){
		p.getAllProducts(sessionToken);		
	}
	
	@Test
	public void testAddProduct(){
		try {
			productID = p.addProduct(product, sessionToken);
			baseUtils.printProductToFile(productID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 			
	}
	
	@Test (dependsOnMethods="testAddProduct")
	public void testGetProduct(){
		p.getProduct(productID, sessionToken);
 			
	}
		
	
	
	@Test (dependsOnMethods="testGetProduct")
	public void testDeleteProduct(){
		baseUtils.reset(productID, sessionToken);
 			
	}


}
