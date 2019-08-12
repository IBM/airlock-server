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
import tests.restapi.StreamsRestApi;

public class WebhookTriggersScenarios {
	private String sessionToken = "";
	private String adminToken = "";
	private String productID;
	private String seasonID;
	protected String filePath;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private AirlockUtils baseUtils;
	private OperationRestApi opsApi;
	private StreamsRestApi streamApi;
	protected String m_url;
	protected String webhookID;
	protected String webhook;
	private String analyticsURL;
	//private String translationsURL;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify, String operationsUrl) throws Exception{
		m_url = url;
		filePath = configPath;
		analyticsURL = analyticsUrl;
		//translationsURL = translationsUrl;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
		sessionToken = baseUtils.setNewJWTToken(userName, userPassword, appName);
		baseUtils.setSessionToken(adminToken);
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		streamApi = new StreamsRestApi();
		streamApi.setURL(url);
		webhook = FileUtils.fileToString(configPath + "webhooks/webhook1.txt", "UTF-8", false);
	}

	@Test (description="create simple webhook and initiate it", enabled=true)
	public void createBranch() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4340);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4340");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String response = baseUtils.createBranch(seasonID);
		Assert.assertFalse(response.contains("error"), "cannot create branch: " + response);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (description="create simple webhook and initiate it", enabled=true)
	public void createSeason() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4341);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4341");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String response = baseUtils.createSeason(productID,"1.6");
		Assert.assertFalse(response.contains("error"), "cannot create season: " + response);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (description="create simple webhook and initiate it", enabled=true)
	public void createExperiment() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4342);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4342");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "DEVELOPMENT");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String response = baseUtils.addExperiment(analyticsURL, false, false);
		Assert.assertFalse(response.contains("error"), "cannot add experiment: " + response);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (description="create simple webhook and don't initiate it", enabled=true)
	public void createExperimentDev() throws JSONException, InterruptedException, IOException {
		WebhookListenerRunner runner = new WebhookListenerRunner(4343);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4343");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "PRODUCTION");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String response = baseUtils.addExperiment(analyticsURL, false, false);
		Assert.assertFalse(response.contains("error"), "cannot add experiment: " + response);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result == null, "webhook did call the url altough it shouldn't have");
		deleteWebhook(hookID);
	}

	@Test (description="create simple webhook and initiate it", enabled=true)
	public void createVariant() throws JSONException, InterruptedException, IOException {
		String response = baseUtils.addExperiment(analyticsURL, false, false);
		WebhookListenerRunner runner = new WebhookListenerRunner(4344);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4344");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "DEVELOPMENT");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String res = baseUtils.addVariant(response, "variant", "MASTER", analyticsURL, false);
		Assert.assertFalse(res.contains("error"), "cannot add variant: " + response);
		thread.join(4000);
		String result = runner.content;
		Assert.assertTrue(result != null, "webhook did not call the url altough it should have");
		deleteWebhook(hookID);
	}

	@Test (description="create simple webhook and initiate it", enabled=true)
	public void createStream() throws Exception {
		WebhookListenerRunner runner = new WebhookListenerRunner(4345);
		Thread thread = new Thread(runner);
		thread.start();
		JSONObject json = new JSONObject(webhook);
		json.put("url", "http://localhost:4345");
		json.put("sendAdmin", false);
		json.put("sendRuntime", true);
		json.put("minStage", "DEVELOPMENT");
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		String hookID = addWebhook(json, false);
		//make runtime change
		String res = addStream(seasonID);
		Assert.assertFalse(res.contains("error"), "cannot add stream: " + res);
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

	private String addStream(String seasonId) throws Exception{

		String stream = FileUtils.fileToString(filePath + "streams/stream1.txt", "UTF-8", false);
		JSONObject streamJson = new JSONObject(stream);
		streamJson.put("name", "video played");
		streamJson.put("minAppVersion", "1.1");
		String streamID = streamApi.createStream(seasonId, streamJson.toString(), sessionToken);
		return streamID;
		//    	Assert.assertFalse(streamID.contains("error"), "Stream was not created: " + streamID);
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
