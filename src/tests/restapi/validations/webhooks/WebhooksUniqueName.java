package tests.restapi.validations.webhooks;


import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;


public class WebhooksUniqueName {
	protected String filePath;
	protected String m_url;
	private OperationRestApi opsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String webhookID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		m_url = url;
			
//		opsApi = new OperationRestApi();
//		opsApi.setURL(url);

		
	}
	
	@Test (description="Create the same webhook name twice")
	private void createWebhook() throws Exception{
//		String webhook = FileUtils.fileToString(filePath + "webhooks/webhook1.txt", "UTF-8", false);			
//		webhookID = opsApi.addWebhook(webhook, sessionToken);
//		Assert.assertFalse(webhookID.contains("error"), "Webhook was not created: " + webhookID);
//
//		String response = opsApi.addWebhook(webhook, sessionToken);
//		Assert.assertTrue(response.contains("already exists"), "webhook created twice" );
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}

	

}
