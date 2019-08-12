package tests.restapi.scenarios.webhooks;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;

public class WebhookPurchasesSimpleScenarios {
	private String sessionToken = "";
	private String adminToken = "";
	private String productID;
	private String seasonID;
	protected String filePath;
	private ProductsRestApi p;
	protected InAppPurchasesRestApi purchasesApi;
	private AirlockUtils baseUtils;
	private OperationRestApi opsApi;
	protected String m_url;
	protected String webhookID;
	protected String webhook;
	private Thread thread1;
	private WebhookListenerRunner runner1;
	private Thread thread2;
	private WebhookListenerRunner runner2;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appNameSimple", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appNameSimple, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify, String operationsUrl) throws Exception{
		m_url = url;
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appNameSimple, productsToDeleteFile);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appNameSimple);
		sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appNameSimple);
		baseUtils.setSessionToken(adminToken);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		p = new ProductsRestApi();
		p.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		webhook = FileUtils.fileToString(configPath + "webhooks/webhook1.txt", "UTF-8", false);
	}



	@Test (description="create simple webhook and call it", enabled=true)
	public void sendAdminTrue() throws JSONException, InterruptedException {
		runner1 = new WebhookListenerRunner(4321);
		thread1 = new Thread(runner1);
		thread1.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4321");
		json.put("sendAdmin", true);
		String hookID = addWebhook(json, false);
		thread1.join(4000);
		String result = runner1.content;
		Assert.assertTrue(result != null, "webhook did not call the url successfully");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="sendAdminTrue", description="create simple that shouldnt accept admin change", enabled=true)
	public void sendAdminFalse() throws JSONException, InterruptedException {
		runner2 = new WebhookListenerRunner(4322);
		thread2 = new Thread(runner2);
		thread2.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4322");
		json.put("sendAdmin", false);
		String hookID = addWebhook(json, false);
		thread2.join(4000);
		String result = runner2.content;
		Assert.assertTrue(result == null, "webhook did call the url altough it shouldn't");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="sendAdminFalse", description="create simple webhook that should accept runtime change", enabled=true)
	public void sendRuntimeTrue() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner3 = new WebhookListenerRunner(4323);
		Thread thread3 = new Thread(runner3);
		thread3.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4323");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "PRODUCTION");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread3.join(4000);
		String result = runner3.content;
		Assert.assertTrue(result != null, "webhook did not call the url where it should have");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="sendRuntimeTrue", description="create simple webhook that should not accept runtime change", enabled=true)
	public void sendRuntimeFalse() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4324);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4324");
		json.put("sendAdmin", false);
		json.put("sendRuntime", false);
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "PRODUCTION");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result == null, "webhook did call the url altough it shouldn't");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="sendRuntimeFalse", description="create simple webhook that should not accept runtime change", enabled=true)
	public void minStageFalse() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4325);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4325");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "DEVELOPMENT");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result == null, "webhook did call the url altough it shouldn't");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="minStageFalse", description="create simple webhook that should accept runtime change", enabled=true)
	public void minStageDev() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4326);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4326");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "DEVELOPMENT");
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "DEVELOPMENT");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="minStageDev", description="create simple webhook that should accept runtime change", enabled=true)
	public void minStageProdDev() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4327);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4327");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "DEVELOPMENT");
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "PRODUCTION");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (dependsOnMethods="minStageProdDev", description="create simple webhook that should accept runtime change", enabled=true )
	public void minStageDevProd() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4328);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4328");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		String hookID = addWebhook(json, false);
		//make runtime change
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject eJson = new JSONObject(entitlement);
		eJson.put("stage", "PRODUCTION");
		eJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = purchasesApi.addPurchaseItem(seasonID, eJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add entitlement: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);

	}

	private String addWebhook(JSONObject json, boolean expectedResult) throws JSONException{
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		try {
			String response = opsApi.addWebhook(json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "failed adding webhook: "+ response);
			JSONObject hookObj = new JSONObject(response);
			String webhookID = hookObj.getString("uniqueId");
			return webhookID;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "failed adding webhook");
			return null;
		}	
	}

	private void deleteWebhook(String webhookID) {
		try {
			opsApi.deleteWebhook(webhookID, sessionToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//    private class WebhookListenerRunner implements Runnable {
	//
	//    	private int port;
	//	    private String content;
	//	    
	//	    public WebhookListenerRunner(int port) {
	//	    	this.port = port;
	//	    	this.content = null;
	//	    }
	//
	//	    public void run() {
	//	        try {
	//				listenOnPort(this.port);
	//			} catch (IOException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//	    }
	//	    
	//	    private void listenOnPort(int port) throws IOException {
	//	    	ServerSocket serverSocket = new ServerSocket(port);
	//	    	Socket socket = serverSocket.accept();
	//	    	//read message
	//	    	BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	//	    	String content = br.readLine();
	//	    	System.out.println("webhook recieved! - "+port+ ":"+ content);
	//	    	this.content = content;
	//	    	serverSocket.close();
	//	    }
	//	}
}
