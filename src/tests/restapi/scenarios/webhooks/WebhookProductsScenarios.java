package tests.restapi.scenarios.webhooks;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;

public class WebhookProductsScenarios {
	private String sessionToken = "";
	private String adminToken = "";
	private String sessionToken2 = "";
	private String adminToken2 = "";
	private String productID;
	private String productID2;
	private String seasonID;
	private String seasonID2;
	protected String filePath;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private AirlockUtils baseUtils;
	private AirlockUtils baseUtils2;
	private OperationRestApi opsApi;
	protected String m_url;
	protected String webhookID;
	protected String webhook;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "appName2", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String appName2, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify, String operationsUrl) throws Exception{
		m_url = url;
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		baseUtils2 = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName2, productsToDeleteFile);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
		sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName);
		adminToken2 = baseUtils.setNewJWTToken(adminUser, adminPassword,appName2);
		sessionToken2 = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName2);
		baseUtils.setSessionToken(adminToken);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		productID2 = baseUtils2.createProduct();
		baseUtils2.printProductToFile(productID2);
		seasonID2 = baseUtils2.createSeason(productID2);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		webhook = FileUtils.fileToString(configPath + "webhooks/webhook1.txt", "UTF-8", false);
	}

	@Test (description="create simple webhook that should not be fired because of products", enabled=true)
	public void differentProduct() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4330);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4330");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		JSONArray arr = new JSONArray();
		arr.add(productID2);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("stage", "PRODUCTION");
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result == null, "webhook did call the url altough it shouldn't have");
		deleteWebhook(hookID);
		f.deleteFeature(response, sessionToken);
	}

	@Test (description="create simple webhook that should be fired because of products", enabled=true)
	public void sameProduct() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4331);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4331");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("stage", "PRODUCTION");
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		String response  = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot add feature: " + response);
		
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID2, sessionToken2);
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
