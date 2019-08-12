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

public class AddAndDeleteChildrenFeatures1 {
		   
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

	   //add 2 children to featureID
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add 2 sub-features to the same parent feature")
	    public void addFeature() throws IOException, InterruptedException {
		   //Thread.currentThread().sleep(500);
	        feature = JSONUtils.generateUniqueString(feature, 5, "name"); 	        
	        f.addFeature(seasonID, feature, featureID, sessionToken);
	    }
	   
	   @Test (dependsOnMethods = "addFeature", description = "Validate that 2 featres were added")
	   public void validateParent() throws JSONException{
		   String parent = f.getFeature(featureID, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("features");
		   Assert.assertEquals(children.size(), 2, "Not all children were added to parent");
		   if (children.size() == 2) {
			   JSONObject child = children.getJSONObject(0);
			   childID1 = child.getString("uniqueId");
			   child = children.getJSONObject(1);
			   childID2 = child.getString("uniqueId");
		   }   
	   }
	   
	   @Test (dependsOnMethods = "validateParent", threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously delete 2 sub-features from the same parent feature")
	   public void deleteChildren() throws JSONException{
		   iteration = iteration+1;
		   //Long id = Thread.currentThread().getId();
		   if (iteration % 2 == 0) {
			   f.deleteFeature(childID1, sessionToken);
//			   System.out.println("thread#: " + id + " iteration#: " + iteration + " childID1 response=" + response1);	   
		   } else {
			  f.deleteFeature(childID2, sessionToken);
//			   System.out.println("thread#: " + id + " iteration#: " + iteration + " childID2 response=" + response2);
		   }
		
		   
	   }
	   
	   //delete 2 features from featureID
	   
	   @Test (dependsOnMethods = "deleteChildren", description = "Validate that 2 featres were deleted")
	   public void validateParentAfterDelete() throws JSONException{
		   String parent = f.getFeature(featureID, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("features");
		   Assert.assertEquals(children.size(), 0, "Not all children were removed from parent");
	   }
	   
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
