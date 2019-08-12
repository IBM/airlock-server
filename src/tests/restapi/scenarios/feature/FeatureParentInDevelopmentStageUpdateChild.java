package tests.restapi.scenarios.feature;

import java.io.IOException;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureParentInDevelopmentStageUpdateChild {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	/**
	 * DESIGN CHANGE:
		production is allowed under development
	 */
	
	//@BeforeClass
	//@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
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
	 * Sub-feature can't be production if its parent is development
	 * Create a child feature in development and try to update it to production
	 * 
	 */
	//@Test (description = "Create a child feature in development and update it to production")
	public void testUpdateToProduction() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			String parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			String childID = f.addFeature(seasonID, child, parentID, sessionToken);
			
			//Update child feature to stage=PRODUCTION - should fail
			JSONObject json = new JSONObject(child);
			json.put("stage", "PRODUCTION");
			String response = f.updateFeature(seasonID, childID, json.toString(), sessionToken);
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	/**
	 * Sub-feature can't be production if its parent is development
	 * Create a child feature in production
	 * 
	 */
	//@Test (description = "Create a child feature in production with parent in dev")
	public void testAddProduction() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			parent = JSONUtils.generateUniqueString(parent, 8, "name");
			String parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			JSONObject json = new JSONObject(child);
			json.put("stage", "PRODUCTION");

			String response = f.addFeature(seasonID, json.toString(), parentID, sessionToken);		
			 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	//@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
