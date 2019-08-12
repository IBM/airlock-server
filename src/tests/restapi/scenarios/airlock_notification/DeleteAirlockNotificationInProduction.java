package tests.restapi.scenarios.airlock_notification;

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
import tests.restapi.AirlocklNotificationRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class DeleteAirlockNotificationInProduction {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String notificationID;
	private SeasonsRestApi s;
	private ProductsRestApi p;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;
		
		notifApi = new AirlocklNotificationRestApi();
		notifApi.setUrl(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		
	}
	
	@Test (description="add production notification")
	public void addProdNotification() throws JSONException, IOException, InterruptedException{
		
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(notification);
		json.put("stage", "PRODUCTION");
		notificationID = notifApi.createNotification(seasonID, json.toString(), sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Can't create notification: " + notificationID);

	}
	
	
	@Test (dependsOnMethods="addProdNotification", description="delete production notification")
	public void deleteNotification() throws JSONException, IOException, InterruptedException{
		int respCode = notifApi.deleteNotification(notificationID, sessionToken);
		Assert.assertFalse(respCode == 200, "Production notification was deleted");
	}
	

	@Test (dependsOnMethods="deleteNotification", description="delete season and product")
	public void deleteSeasonAndProduct() throws JSONException, IOException, InterruptedException{
		int respCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertFalse(respCode == 200, "Deleted season with production notification");
		
		respCode = p.deleteProduct(productID, sessionToken);
		Assert.assertFalse(respCode == 200, "Deleted product with production notification");
	}	

	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
