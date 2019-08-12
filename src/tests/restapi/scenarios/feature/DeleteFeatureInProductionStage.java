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
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;

public class DeleteFeatureInProductionStage {
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected EmailNotification notification;
	private AirlockUtils baseUtils;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String notify) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		boolean isOn = "true".equals(notify);
		notification = new EmailNotification(isOn, url, sToken);
		notification.startTest();
		notification.clearMailFile();

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		notification.followProduct(productID);

	}
	


	/**
	 * Delete a feature that is in "PRODUCTION" - action not allowed
	 */
	@Test (description = "Delete a feature in production stage")
	public void testFeatureDeleteAndValidate() throws JSONException{
		try {			
			String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);	
			JSONObject obj = new JSONObject(feature1);
			obj.put("stage", "PRODUCTION");

			String featureID1 = f.addFeature(seasonID, obj.toString(), "ROOT", sessionToken);
			int response = f.deleteFeature(featureID1, sessionToken);
			Assert.assertNotEquals(response, 200, "Should not allow to delete a feature that is in PRODUCTION stage");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

 			
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);

		if (notification.isOn())
		{
			JSONObject notificationOutput = notification.getMailFile();
			// do something with it (parse or compare to gold)
			notification.stopTest();
		}
	}

}
