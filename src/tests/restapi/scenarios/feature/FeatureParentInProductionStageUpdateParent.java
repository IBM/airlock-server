package tests.restapi.scenarios.feature;

import java.io.IOException;












import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class FeatureParentInProductionStageUpdateParent {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	protected String parentID;
	protected String childID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	/**
	 * DESIGN CHANGE:
		production is allowed under development
	 */
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}



	/**
	 * 
	 * Parent in PROD, add child in PROD
	 */
	@Test (description = "Parent in PROD, add child in PROD")
	public void testAddChildInProdStage() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			JSONObject json = new JSONObject(parent);
			json.put("stage", "PRODUCTION");
			parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			JSONObject childJson = new JSONObject(child);
			childJson.put("stage", "PRODUCTION");			
			childID = f.addFeature(seasonID, childJson.toString(), parentID, sessionToken);
			Assert.assertFalse(childID.contains("error"), "Test should pass, but instead failed: " + childID );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//Update parent feature from PROD to DEV - not allowed as its child is in PROD stage
//	@Test (dependsOnMethods = "testAddChildInProdStage", description = "Update parent feature from PROD to DEV")
	public void updateParentFromProdToDevStage() throws JSONException{
		try {			
			String parent  = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			json.put("stage", "DEVELOPMENT");
			String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//Update parent & child features from PROD to DEV simultaneously
	@Test(dependsOnMethods = "testAddChildInProdStage", description = "Update parent & child features from PROD to DEV simultaneously")
	public void updateParentAndChildFromProdToDevStage() throws JSONException{
		try {
			//change parent to DEV
			String parent  = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			json.put("stage", "DEVELOPMENT");
			
			//change child to DEV
			String child  = f.getFeature(childID, sessionToken);
			JSONObject childJson = new JSONObject(child);
			childJson.put("stage", "DEVELOPMENT");
			
			//in parent list of features change the child feature
			json.remove("features");
			JSONArray children = new JSONArray();
			children.put(childJson);
			json.put("features", children);
			
			//update parent
			String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}	
	
	//Update parent & child features from DEV to PROD simultaneously
	@Test(dependsOnMethods = "updateParentAndChildFromProdToDevStage", description = "Update parent & child features from DEV to PROD simultaneously")
	public void updateParentAndChildFromDevToProdStage() throws JSONException{
		try {
			//change parent to PROD
			String parent  = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			json.put("stage", "PRODUCTION");
			
			//change child to PROD
			String child  = f.getFeature(childID, sessionToken);
			JSONObject childJson = new JSONObject(child);
			childJson.put("stage", "PRODUCTION");
			
			//in parent list of features change the child feature
			json.remove("features");
			JSONArray children = new JSONArray();
			children.put(childJson);
			json.put("features", children);
			
			//update parent
			String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}	

//	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
