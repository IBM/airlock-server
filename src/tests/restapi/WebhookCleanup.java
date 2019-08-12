package tests.restapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.OperationRestApi;
import tests.restapi.ProductsRestApi;

public class WebhookCleanup {
	private String sessionToken = "";
    private String adminToken = "";
    private String productID;
    private String seasonID;
    private ProductsRestApi p;
    private FeaturesRestApi f;
    private AirlockUtils baseUtils;
    private OperationRestApi opsApi;
    protected String m_url;
    protected String webhookID;
    protected String webhook;
    private Thread thread1;
    private WebhookListenerRunner runner1;
    
    @BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "adminUser", "adminPassword", "productLeadName", "productLeadPassword", "productsToDeleteFile", "notify", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String adminUser, String adminPassword, String productLeadName, String productLeadPassword, String productsToDeleteFile, String notify, String operationsUrl) throws Exception{
        m_url = url;
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
        adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword,appName);
        sessionToken = baseUtils.setNewJWTToken(productLeadName, productLeadPassword,appName);
        baseUtils.setSessionToken(adminToken);
        p = new ProductsRestApi();
        p.setURL(m_url);
        f = new FeaturesRestApi();
        f.setURL(m_url);
        opsApi = new OperationRestApi();
        opsApi.setURL(operationsUrl);
        
    }
    
    
    
    
    @Test (description="clean all webhooks")
    public void deleteAllWebhooks() {
		String hooks;
		try {
			hooks = opsApi.getWebhooks(sessionToken);
			JSONArray webhooks;
			JSONObject webhooksObj = new JSONObject(hooks);
			webhooks = webhooksObj.getJSONArray("webhooks");
			for (Object obj : webhooks) {
				JSONObject json = new JSONObject(obj);
				opsApi.deleteWebhook(json.getString("uniqueId"), sessionToken);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "could not get webhook: " + e.getMessage());
		}
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
    
    private class WebhookListenerRunner implements Runnable {

    	private int port;
	    private String content;
	    
	    public WebhookListenerRunner(int port) {
	    	this.port = port;
	    	this.content = null;
	    }

	    public void run() {
	        try {
				listenOnPort(this.port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    private void listenOnPort(int port) throws IOException {
	    	ServerSocket serverSocket = new ServerSocket(port);
	    	Socket socket = serverSocket.accept();
	    	//read message
	    	BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    	String content = br.readLine();
	    	System.out.println(content);
	    	this.content = content;
	    	serverSocket.close();
	    }
	}
}
