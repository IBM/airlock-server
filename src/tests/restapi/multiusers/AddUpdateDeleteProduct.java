package tests.restapi.multiusers;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class AddUpdateDeleteProduct {
		   
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	protected String product;
	protected String season;
	protected String m_url;
	protected List<Integer> actualResult = new ArrayList<Integer>();
	protected List<String> productIds = new ArrayList<String>();
	protected int iteration=0;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		p = new ProductsRestApi();
		p.setURL(url);
	
		product = FileUtils.fileToString(configPath + "product1.txt", "UTF-8", false);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}

	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add 2 products")
	    public void addProduct() throws InterruptedException, IOException {
	        String productString = randomizeProduct(product);
	        productID = p.addProduct(productString, sessionToken);	  
	        productIds.add(productID);
	        baseUtils.printProductToFile(productID);
	    }
	   
	   
	   @Test (dependsOnMethods = "addProduct",  threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously delete 2 products")
	   public void deleteProduct(){
		   if (productIds.size()!=2)
			   Assert.fail("Incorrect number of product");
		   
		   iteration = iteration+1;
		   int response = 0;		  
		   if (iteration%2 == 0) {
			   response = p.deleteProduct(productIds.get(0), sessionToken);
		   }   
		   else {
			   response = p.deleteProduct(productIds.get(1), sessionToken);
		   }
		   actualResult.add(response);

	   }
	   
	   @Test (dependsOnMethods = "deleteProduct")
	   public void validate(){
		   List<Integer> expectedResult= new ArrayList<Integer>(Arrays.asList(200,200));
		   Assert.assertEqualsNoOrder(actualResult.toArray(), expectedResult.toArray(), "Expected response codes were not received.");
	   }
	   
	   private String randomizeProduct(String product){
		   
			String newProduct = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
			newProduct = JSONUtils.generateUniqueString(newProduct, 8, "name");
			return newProduct;

	   }
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }
	   
}
