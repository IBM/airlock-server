package tests.restapi.multiusers;

import java.io.IOException;

import java.util.Date;
import java.util.List;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class AddAndDeleteInvalidFeatures {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected List<Integer> actualResult;
	private String sessionToken = "";
	protected int iteration=0;
	private AirlockUtils baseUtils;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		seasonID = baseUtils.createSeason(productID);
		
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
		 feature = JSONUtils.generateUniqueString(feature, 5, "name");
		JSONObject json = new JSONObject(feature);
		//add illegal field "lastModified" - features should not be created
		json.put("lastModified",new Date(System.currentTimeMillis()));
		feature = json.toString();
	}

	   
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add the same feature twice")
	    public void addFeature() throws IOException, InterruptedException {
	       // Long id = Thread.currentThread().getId();	
		   
		   if (iteration%2 == 0) {
			   featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
			   Assert.assertTrue(featureID.contains("error"), "Test should pass, but instead failed: " + featureID );
		   }   
		   else {
		        featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		        Assert.assertTrue(featureID.contains("error"), "Test should fail, but instead passed: " + featureID );

		   }
		   iteration = iteration+1;	  
	    }
	   
   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
