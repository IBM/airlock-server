package tests.restapi.multiusers;

import java.io.IOException;

import java.util.List;

import org.apache.wink.json4j.JSONArray;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class AddFeaturesToDifferentParents {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected List<Integer> actualResult;
	protected String childID1;
	protected String childID2;
	protected int iteration = 0;
	private String sessionToken = "";
	private AirlockUtils baseUtils;

	/*
	 * This test validates that 2 features can be simultaneous added to two parent features
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
		featureID1 = baseUtils.createFeature(seasonID);	//parent feature1
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		featureID2 = baseUtils.createFeature(seasonID);	//parent feature1
	}

	   //add 2 children to different parents
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Add 2 children to different parents")
	    public void addFeature() throws IOException, InterruptedException {	  
			   iteration = iteration+1;
	       // Thread.currentThread().sleep(1000);
			   if (iteration%2 == 0) {
				   feature = JSONUtils.generateUniqueString(feature, 5, "name");
				   childID1 = f.addFeature(seasonID, feature, featureID1, sessionToken);
			   } else {
				   feature = JSONUtils.generateUniqueString(feature, 5, "name");
				   childID2 = f.addFeature(seasonID, feature, featureID2, sessionToken);
			   }
	    }
	   
	   @Test (dependsOnMethods = "addFeature", description = "Validate 2 parents")
	   public void validateParent() throws JSONException{
		   String parent = f.getFeature(featureID1, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("features");
		   Assert.assertEquals(children.size(), 1, "A child was not added to parent1");
  
		   parent = f.getFeature(featureID2, sessionToken);
		   json = new JSONObject(parent);
		   children = json.getJSONArray("features");
		   Assert.assertEquals(children.size(), 1, "A child was not added to parent2");
	   }
	   
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
