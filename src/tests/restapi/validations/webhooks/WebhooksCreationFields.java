package tests.restapi.validations.webhooks;

import java.io.IOException;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;


public class WebhooksCreationFields {
	protected String filePath;
	protected String m_url;
	protected String webhook;
	private OperationRestApi opsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String operationsUrl) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

		m_url = url;

		webhook = FileUtils.fileToString(filePath + "webhooks/webhook1.txt", "UTF-8", false);		
		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		
	}

	
	
	
	@Test
	private void minStageField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("minStage");
		addWebhook(json, "minStage", true);
		
		json = new JSONObject(webhook);
		json.put("minStage", "");
		addWebhook(json, "minStage", true);
		
		json = new JSONObject(webhook);
		json.put("minStage", JSON.NULL);
		addWebhook(json, "minStage", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("name");
		addWebhook(json, "name", true);
		
		json = new JSONObject(webhook);
		json.put("name", "");
		addWebhook(json, "name", true);
		
		json = new JSONObject(webhook);
		json.put("name", JSON.NULL);
		addWebhook(json, "name", true);
	}
	
	@Test
	private void urlField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("url");
		addWebhook(json, "url", true);
		
		json = new JSONObject(webhook);
		json.put("url", "");
		addWebhook(json, "url", true);
		
		json = new JSONObject(webhook);
		json.put("url", JSON.NULL);
		addWebhook(json, "url", true);
		
		json = new JSONObject(webhook);
		json.put("url", "this is not a valid url");
		addWebhook(json, "url", true);
		
		json = new JSONObject(webhook);
		json.put("url", "http://localhost:8080/callback");
		addWebhook(json, "url", false);
	}
	
	@Test
	private void productsField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("products");
		addWebhook(json, "products", false);
		
		json = new JSONObject(webhook);
		json.put("products", "");
		addWebhook(json, "products", true);
		
		json = new JSONObject(webhook);
		json.put("products", JSON.NULL);
		addWebhook(json, "products", false);
		
		json = new JSONObject(webhook);
		json.put("products", "[]");
		addWebhook(json, "products", true);
		
		json = new JSONObject(webhook);
		json.put("products", "[this-is-not-a-product-id]");
		addWebhook(json, "products", true);
		
		json = new JSONObject(webhook);
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		addWebhook(json, "products", false);
	}
	
	@Test
	private void sendRuntimeField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("sendRuntime");
		addWebhook(json, "sendRuntime", true);
		
		json = new JSONObject(webhook);
		json.put("sendRuntime", "");
		addWebhook(json, "sendRuntime", true);
		
		json = new JSONObject(webhook);
		json.put("sendRuntime", JSON.NULL);
		addWebhook(json, "sendRuntime", true);
		
		json = new JSONObject(webhook);
		json.put("sendRuntime", "not-a-boolean");
		addWebhook(json, "sendRuntime", true);
		
		json = new JSONObject(webhook);
		json.put("sendRuntime", true);
		addWebhook(json, "sendRuntime", false);
		
		json = new JSONObject(webhook);
		json.put("sendRuntime", false);
		addWebhook(json, "sendRuntime", false);
	}
	
	@Test
	private void sendAdminField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("sendAdmin");
		addWebhook(json, "sendAdmin", true);
		
		json = new JSONObject(webhook);
		json.put("sendAdmin", "");
		addWebhook(json, "sendAdmin", true);
		
		json = new JSONObject(webhook);
		json.put("sendAdmin", JSON.NULL);
		addWebhook(json, "sendAdmin", true);
		
		json = new JSONObject(webhook);
		json.put("sendAdmin", "not-a-boolean");
		addWebhook(json, "sendAdmin", true);
		
		json = new JSONObject(webhook);
		json.put("sendAdmin", true);
		addWebhook(json, "sendAdmin", false);
		
		json = new JSONObject(webhook);
		json.put("sendAdmin", false);
		addWebhook(json, "sendAdmin", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json = new JSONObject(webhook);
		json.put("uniqueId", "");
		addWebhook(json, "uniqueId", true);
		
		json = new JSONObject(webhook);
		json.put("uniqueId", JSON.NULL);
		addWebhook(json, "uniqueId", false);
		
		json = new JSONObject(webhook);
		json.put("uniqueId", UUID.randomUUID());
		addWebhook(json, "uniqueId", true);
	}
	
	
	@Test
	private void creationDateField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json = new JSONObject(webhook);
		json.put("creationDate", System.currentTimeMillis());
		addWebhook(json, "creationDate", true);

		json = new JSONObject(webhook);
		json.put("creationDate", "");
		addWebhook(json, "creationDate", true);
		
		json = new JSONObject(webhook);
		json.put("creationDate", JSON.NULL);
		addWebhook(json, "creationDate", false);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json.remove("creator");
		addWebhook(json, "creator", true);
		
		json = new JSONObject(webhook);
		json.put("creator", "");
		addWebhook(json, "creator", true);
		
		json = new JSONObject(webhook);
		json.put("creator", JSON.NULL);
		addWebhook(json, "creator", true);
	}
	
	
	@Test
	private void lastModifiedField() throws IOException, JSONException{
		JSONObject json = new JSONObject(webhook);
		json = new JSONObject(webhook);
		json.put("lastModified", System.currentTimeMillis()-10000);
		addWebhook(json, "lastModified", true);

		json = new JSONObject(webhook);
		json.put("lastModified", "");
		addWebhook(json, "lastModified", true);
		
		json = new JSONObject(webhook);
		json.put("lastModified", JSON.NULL);
		addWebhook(json, "lastModified", false);
	}
	
	@Test (description="Create the same webhook name twice")
	private void createWebhook() throws Exception{
		JSONObject json = new JSONObject(webhook);
		
		json = new JSONObject(webhook);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		addWebhook(json, "name", false);
		
		addWebhook(json, "name", true);
	}
	
	
	
	
	private void addWebhook(JSONObject json, String field, boolean expectedResult) throws JSONException{
		if (!field.equals("name"))
			json.put("name", RandomStringUtils.randomAlphabetic(5));

		try {
			String response = opsApi.addWebhook(json.toString(), sessionToken);
			Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "Test failed for field: " + field);
		}	
	}
	


	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
//		deleteAllWebhooks();
	}

	private void deleteAllWebhooks() {
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
	

}
