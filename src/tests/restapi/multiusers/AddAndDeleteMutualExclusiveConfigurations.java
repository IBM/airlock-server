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

public class AddAndDeleteMutualExclusiveConfigurations {
		   
	protected String productID;
	protected String seasonID;
	protected String parentID;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected String filePath;
	protected List<Integer> actualResult;
	protected String childID1;
	protected String childID2;
	protected int iteration = 0;
	private String sessionToken = "";
	protected String configuration;
	private AirlockUtils baseUtils;

	/*
	 * This test validates that 2 configurations can be simultaneous added to a mutual exclusive feature and then deleted from the parent
	 */
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		seasonID = baseUtils.createSeason(productID);
		
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID = baseUtils.createFeature(seasonID);	//parent feature
		String miConfiguration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		parentID = f.addFeature(seasonID, miConfiguration, featureID, sessionToken);
		configuration = FileUtils.fileToString(configPath + "configuration_rule1.txt", "UTF-8", false);
	}

	   //add 2 configurations to mutual exclusion
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000)
	    public void addConfigurations() throws IOException, InterruptedException {
	      
	        configuration = JSONUtils.generateUniqueString(configuration, 5, "name");	   	        	
	        f.addFeature(seasonID, configuration, parentID, sessionToken);
	    }
	   
	   @Test (dependsOnMethods = "addConfigurations", description = "Validate that 2 configurations were added to mutually exclusive group")
	   public void validateParent() throws JSONException{
		   String parent = f.getFeature(parentID, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("configurationRules");
		   Assert.assertEquals(children.size(), 2, "Not all children were added to mutually exclusive group");
		   if (children.size() == 2) {
			   JSONObject child = children.getJSONObject(0);
			   childID1 = child.getString("uniqueId");
			   child = children.getJSONObject(1);
			   childID2 = child.getString("uniqueId");
		   } 
	   }
	   
	   @Test (dependsOnMethods = "validateParent", threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously delete 2 features from mutually exclusive group")
	   public void deleteChildren() throws JSONException{
		   iteration = iteration+1;
		   if (iteration%2 == 0) {
			   f.deleteFeature(childID1, sessionToken);
		   } else {
			   f.deleteFeature(childID2, sessionToken);
		   }
	   }
	   
	   	   
	   @Test (dependsOnMethods = "deleteChildren", description = "Validate mutually exclusive group")
	   public void validateParentAfterDelete() throws JSONException{
		   String parent = f.getFeature(parentID, sessionToken);
		   JSONObject json = new JSONObject(parent);
		   JSONArray children = json.getJSONArray("configurationRules");
		   Assert.assertEquals(children.size(), 0, "Not all children were removed from mutually exclusive group");
	   }
	   
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
