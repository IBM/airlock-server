package tests.restapi.multiusers;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class AddUpdateDeleteSeason {
		   
	protected String productID1;
	protected String productID2;
	protected String seasonID1;
	protected String seasonID2;
	protected String filePath;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	protected List<Integer> actualResult = new ArrayList<Integer>();
	protected String season1;
	protected String season2;
	protected int iteration;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		s = new SeasonsRestApi();
		s.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID1);
		
		productID2 = baseUtils.createProduct();
		baseUtils.printProductToFile(productID2);

	}

	   
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add seasons to different products")
	    public void addSeason() throws IOException, InterruptedException, JSONException {

	        iteration = iteration+1;
			if (iteration%2 == 0) {
				String season1 = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
				seasonID1 = s.addSeason(productID1, season1, sessionToken);
			} else {
				String season2 = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
				seasonID2 = s.addSeason(productID2, season2, sessionToken);
			}
	        

	    }
	   
	   @Test (dependsOnMethods = "addSeason", description = "Validate 2 seasons")
	   public void validateSeasons() throws Exception{
		   JSONArray seasons1 = s.getSeasonsPerProduct(productID1, sessionToken);
		   if (seasons1.size()!=1){
			   Assert.fail("Invalid number of seasons in the first product");
		   }
		   
		   JSONArray seasons2 = s.getSeasonsPerProduct(productID2, sessionToken);
		   if (seasons2.size()!=1){
			   Assert.fail("Invalid number of seasons in the second product");
		   }

	   }

	   
	   @Test (dependsOnMethods= "validateSeasons" ,threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously delete 2 seasons")
	   public void deleteSeason(){
		   iteration = iteration+1;
		   int response = 0;		  
		   if (iteration%2 == 0){
			   response = s.deleteSeason(seasonID1, sessionToken);
		   } else 
			   response = s.deleteSeason(seasonID2, sessionToken);
		   
		   actualResult.add(response);
	   }
	   
	   @Test (dependsOnMethods= "deleteSeason")
	   public void validate(){
		   List<Integer> expectedResult= new ArrayList<Integer>(Arrays.asList(200,200));
		   Assert.assertEqualsNoOrder(actualResult.toArray(), expectedResult.toArray(), "Expected response codes were not received.");
		   
	   }
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID1, sessionToken);
		   baseUtils.reset(productID2, sessionToken);
	   }
	   

}
