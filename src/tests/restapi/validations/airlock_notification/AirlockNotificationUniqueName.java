package tests.restapi.validations.airlock_notification;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AirlocklNotificationRestApi;


public class AirlockNotificationUniqueName {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private String notificationID;
	private AirlocklNotificationRestApi notifApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	
	
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

		
	}
	
	@Test (description="Create the same notification name twice")
	private void createNotification() throws Exception{
		String notification = FileUtils.fileToString(filePath + "notifications/notification1.txt", "UTF-8", false);			
		notificationID = notifApi.createNotification(seasonID, notification, sessionToken);
		Assert.assertFalse(notificationID.contains("error"), "Airlock notification was not created: " + notificationID);

		String response = notifApi.createNotification(seasonID, notification, sessionToken);
		Assert.assertTrue(response.contains("already exists"), "notification created twice" );
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
