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

public class AddAndDeleteChildrenFeatures2 {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID;
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
	 * This test validates that 1 feature can be added to a parent feature while the other is simultaneously  deleted from the parent
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
		
		feature = JSONUtils.generateUniqueString(feature, 5, "name");
		childID1 = f.addFeature(seasonID, feature, featureID, sessionToken); // child1 feature
	}

	   //add 1 newchild to featureID
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add one feature and delete another")
	    public void addFeature() throws IOException, InterruptedException {
       		iteration = iteration+1;
	        if (iteration%2 == 0) {
	        	feature = JSONUtils.generateUniqueString(feature, 5, "name");	   	        	
	        	childID2 = f.addFeature(seasonID, feature, featureID, sessionToken);
	        }	else {	        		   	 
	        	int response = f.deleteFeature(childID1, sessionToken);
	        	Assert.assertEquals(response, 200, "The feature could not be deleted");
	        }
	    }
	   
	   @Test (dependsOnMethods = "addFeature", description = "Validate parent feature")
	   public void validateParent() throws JSONException{
		   String parent = f.getFeature(featureID, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("features");
		   Assert.assertEquals(children.size(), 1, "Incorrect number of children");  
	   }
	   

	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
