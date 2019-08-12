package tests.restapi.multiusers;

import java.io.IOException;

import java.util.List;

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

public class AddChildAndDeleteParent {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected List<Integer> actualResult;
	protected int iteration = 0;
	private String sessionToken = "";
	private AirlockUtils baseUtils;

	/*
	 * This test validates that 2 features can be simultaneous added to a parent feature and then deleted from the parent
	 */
	
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
		featureID = baseUtils.createFeature(seasonID);	//parent feature
	}

	   //add 1 child to featureID and delete featureID
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add sub-feature to parent and delete parent")
	    public void addFeature() throws IOException, InterruptedException {
	       // Long id = Thread.currentThread().getId();
        	iteration = iteration+1;
	        Thread.currentThread();
			Thread.sleep(1000);
	        if (iteration%2 == 0) {
	        	feature = JSONUtils.generateUniqueString(feature, 5, "name");
	        	//System.out.println("Started thead: " + id);	   	        	
	        	f.addFeature(seasonID, feature, featureID, sessionToken);

	        }	else {
	        	int response = f.deleteFeature(featureID, sessionToken);
	        	Assert.assertEquals(response, 200, "The feature could not be deleted");
	        }
	    }
	   

	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
