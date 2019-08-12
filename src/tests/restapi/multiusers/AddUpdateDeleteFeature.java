package tests.restapi.multiusers;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
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

public class AddUpdateDeleteFeature {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected List<Integer> actualResult = new ArrayList<Integer>();
	protected int iteration=0;
	private String sessionToken = "";
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
	}

	   
	   @Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously add features")
	    public void addFeature() throws IOException, InterruptedException {
			Thread.sleep(1000);
	        feature = JSONUtils.generateUniqueString(feature, 5, "name");
	        f.addFeature(seasonID, feature, "ROOT", sessionToken);

	    }
	   
	   @Test (dependsOnMethods= "addFeature", description = "Validate features")
	   public void getFeatureID() throws JSONException{
		   JSONArray rootFeatures = f.getFeaturesBySeason(seasonID, sessionToken);
		   if (rootFeatures.size()==2){
			   featureID1 = rootFeatures.getJSONObject(0).getString("uniqueId");
			   featureID2 = rootFeatures.getJSONObject(1).getString("uniqueId");			   		   
		   }else{
			   Assert.fail("Invalid number of features");
		   }
		   
	   }
	   
	   @Test(dependsOnMethods= "getFeatureID" ,threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously update features")
	   public void updateFeatures() throws InterruptedException, IOException, JSONException{
		  // Long id = Thread.currentThread().getId();
		   iteration = iteration+1;
		   //Thread.currentThread().sleep(1000);
		  // System.out.println("Started thead: " + id + " iteration=" + iteration);	  
		   if (iteration%2 == 0) {
			   String feature1 = changeDescription(featureID1, "description1");
			   f.updateFeature(seasonID, featureID1, feature1, sessionToken);
		   } else  {
			   String feature2 = changeDescription(featureID2, "description2");
			   f.updateFeature(seasonID, featureID2, feature2, sessionToken);
		   }
	   }
	   
	   @Test (dependsOnMethods= "updateFeatures", description = "Validate updated features")
	   public void validateChanges() throws JSONException{
		   String updatedFeature1 = f.getFeature(featureID1, sessionToken);
		   JSONObject updatedJson1 = new JSONObject(updatedFeature1);
		   String updatedFeature2 = f.getFeature(featureID2, sessionToken);
		   JSONObject updatedJson2 = new JSONObject(updatedFeature2);
		   Assert.assertTrue(updatedJson1.getString("description").equals("description1"), "Description1 was not updated");
		   Assert.assertTrue(updatedJson2.getString("description").equals("description2"), "Description2 was not updated");		   
	   }
	   
	   
	   @Test (dependsOnMethods= "validateChanges" ,threadPoolSize = 2, invocationCount = 2, timeOut = 10000,  description = "Simultaneously delete features")
	   public void deleteFeatures(){
		   iteration = iteration+1;
		   int response = 0;		  
		   if (iteration%2 == 0){
			   response = f.deleteFeature(featureID1, sessionToken);
		   } else {
			   response = f.deleteFeature(featureID2, sessionToken);
		   }   
		   
		   actualResult.add(response);
	   }
	   
	   @Test (dependsOnMethods= "deleteFeatures")
	   public void validate(){
		   List<Integer> expectedResult= new ArrayList<Integer>(Arrays.asList(200,200));		  
		   Assert.assertEqualsNoOrder(actualResult.toArray(), expectedResult.toArray(), "Expected response codes were not received.");
		   p.deleteProduct(productID, sessionToken);
	   }
	   
	   private String changeDescription(String id, String description) throws JSONException{
		   String featureString = "";
		   featureString = f.getFeature(id, sessionToken);
		   JSONObject json = new JSONObject(featureString);
		   json.put("description", description);
		   return json.toString();		   
	   }
	   
	   @AfterTest 
	   public void reset(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
