package tests.restapi.scenarios.feature;

import java.io.IOException;











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
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;


public class FeatureParentInProductionStageUpdateChild {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	protected String parentID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	protected EmailNotification notification;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String notify) throws Exception{
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
		notification = baseUtils.setNotification(notify, url, sessionToken);

	}



	/**
	 * Sub-feature can be development if its parent is production
	 * 
	 * 
	 */
	@Test (description = "Sub-feature can be development if its parent is production")
	public void testAddChildInDevStage() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			JSONObject json = new JSONObject(parent);
			json.put("stage", "PRODUCTION");
			parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			notification.followFeature(parentID);
			
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			String response = f.addFeature(seasonID, child, parentID, sessionToken);
			 Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//Update child feature from PROD to Dev
	@Test (dependsOnMethods = "testAddChildInDevStage", description = "Update child feature from PROD to Dev")
	public void updateChildFromProdToDevStage() throws JSONException{
		try {			
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			JSONObject json = new JSONObject(child);
			json.put("stage", "PRODUCTION");
			String childID = f.addFeature(seasonID, json.toString(), parentID, sessionToken);
			
			child = f.getFeature(childID, sessionToken);
			json = new JSONObject(child);
			json.put("stage", "DEVELOPMENT");
			String response = f.updateFeature(seasonID, childID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	@AfterTest
	private void reset(){
		f.unfollowFeature(parentID, sessionToken);
		baseUtils.getNotificationResult(notification);
		baseUtils.reset(productID, sessionToken);
	}
}
