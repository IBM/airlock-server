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


public class WebhooksUpdateFields {
	protected String seasonID;
	protected String filePath;
	protected String m_url;
	private OperationRestApi opsApi;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	private String webhookID;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "operationsUrl"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String operationsUrl) throws Exception{
		filePath = configPath;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

		m_url = url;

		
		opsApi = new OperationRestApi();
		opsApi.setURL(operationsUrl);
		
		String webhook = FileUtils.fileToString(filePath + "webhooks/webhook1.txt", "UTF-8", false);
		JSONObject webhookJSON = new JSONObject(webhook);
		webhookJSON.put("name", RandomStringUtils.randomAlphabetic(5));
		String response = opsApi.addWebhook(webhookJSON.toString(), sessionToken);
		JSONObject hookObj = new JSONObject(response);
		webhookID = hookObj.getString("uniqueId");
		Assert.assertFalse(webhookID.contains("error"), "Can't create webhook: " + webhookID);

		
	}
	
	@Test
	private void lastModifiedField() throws Exception{

		JSONObject json = getWebhook();
		json.put("lastModified", "");
		updateWebhook(json, "lastModified", true);
		
		json = getWebhook();
		json.put("lastModified", JSON.NULL);
		updateWebhook(json, "lastModified", true);
		
		json = getWebhook();
		json.remove("lastModified");
		updateWebhook(json, "lastModified", true);
	}
	
	@Test
	private void minStageField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("minStage");
		updateWebhook(json, "minStage", true);
		
		json = getWebhook();
		json.put("minStage", "");
		updateWebhook(json, "minStage", true);
		
		json = getWebhook();
		json.put("minStage", JSON.NULL);
		updateWebhook(json, "minStage", true);
	}

	@Test
	private void nameField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("name");
		updateWebhook(json, "name", true);
		
		json = getWebhook();
		json.put("name", "");
		updateWebhook(json, "name", true);
		
		json = getWebhook();
		json.put("name", JSON.NULL);
		updateWebhook(json, "name", true);
	}
	
	@Test
	private void urlField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("url");
		updateWebhook(json, "url", true);
		
		json = getWebhook();
		json.put("url", "");
		updateWebhook(json, "url", true);
		
		json = getWebhook();
		json.put("url", JSON.NULL);
		updateWebhook(json, "url", true);
		
		json = getWebhook();
		json.put("url", "this is not a valid url");
		updateWebhook(json, "url", true);
		
		json = getWebhook();
		json.put("url", "http://localhost:8080/callback");
		updateWebhook(json, "url", false);
	}
	
	@Test
	private void productsField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("products");
		updateWebhook(json, "products", false);
		
		json = getWebhook();
		json.put("products", "");
		updateWebhook(json, "products", true);
		
		json = getWebhook();
		json.put("products", JSON.NULL);
		updateWebhook(json, "products", false);
		
		json = getWebhook();
		json.put("products", "[]");
		updateWebhook(json, "products", true);
		
		json = getWebhook();
		json.put("products", "[this-is-not-a-product-id]");
		updateWebhook(json, "products", true);
		
		json = getWebhook();
		JSONArray arr = new JSONArray();
		arr.add(productID);
		json.put("products", arr);
		updateWebhook(json, "products", false);
	}
	
	@Test
	private void sendRuntimeField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("sendRuntime");
		updateWebhook(json, "sendRuntime", true);
		
		json = getWebhook();
		json.put("sendRuntime", "");
		updateWebhook(json, "sendRuntime", true);
		
		json = getWebhook();
		json.put("sendRuntime", JSON.NULL);
		updateWebhook(json, "sendRuntime", true);
		
		json = getWebhook();
		json.put("sendRuntime", "not-a-boolean");
		updateWebhook(json, "sendRuntime", true);
		
		json = getWebhook();
		json.put("sendRuntime", true);
		updateWebhook(json, "sendRuntime", false);
		
		json = getWebhook();
		json.put("sendRuntime", false);
		updateWebhook(json, "sendRuntime", false);
	}
	
	@Test
	private void sendAdminField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("sendAdmin");
		updateWebhook(json, "sendAdmin", true);
		
		json = getWebhook();
		json.put("sendAdmin", "");
		updateWebhook(json, "sendAdmin", true);
		
		json = getWebhook();
		json.put("sendAdmin", JSON.NULL);
		updateWebhook(json, "sendAdmin", true);
		
		json = getWebhook();
		json.put("sendAdmin", "not-a-boolean");
		updateWebhook(json, "sendAdmin", true);
		
		json = getWebhook();
		json.put("sendAdmin", true);
		updateWebhook(json, "sendAdmin", false);
		
		json = getWebhook();
		json.put("sendAdmin", false);
		updateWebhook(json, "sendAdmin", false);
	}
	
	@Test
	private void uniqueIdField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.put("uniqueId", "0");
		updateWebhook(json, "uniqueId", true);
		
		json = getWebhook();
		json.put("uniqueId", JSON.NULL);
		updateWebhook(json, "uniqueId", false);
		
		json = getWebhook();
		json.put("uniqueId", UUID.randomUUID());
		updateWebhook(json, "uniqueId", true);
	}
	
	
	@Test
	private void creationDateField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json = getWebhook();
		json.put("creationDate", System.currentTimeMillis());
		updateWebhook(json, "creationDate", true);

		json = getWebhook();
		json.put("creationDate", "");
		updateWebhook(json, "creationDate", true);
		
		json = getWebhook();
		json.put("creationDate", JSON.NULL);
		updateWebhook(json, "creationDate", true);
	}
	
	@Test
	private void creatorField() throws IOException, JSONException{
		JSONObject json = getWebhook();
		json.remove("creator");
		updateWebhook(json, "creator", true);
		
		json = getWebhook();
		json.put("creator", "");
		updateWebhook(json, "creator", true);
		
		json = getWebhook();
		json.put("creator", JSON.NULL);
		updateWebhook(json, "creator", true);
	}
	
	
	private void updateWebhook(JSONObject json, String field, boolean expectedResult){

			String response;
			try {
				response = opsApi.updateWebhook(webhookID, json.toString(), sessionToken);
				Assert.assertEquals(response.contains("error"), expectedResult,  "Test failed for field: " + field);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.assertFalse(true, "Test failed for field: " + field);
			}
			
	}
	
	private JSONObject getWebhook(){
		String hooks;
		try {
			hooks = opsApi.getWebhooks(sessionToken);
			JSONArray webhooks;
			JSONObject webhooksObj = new JSONObject(hooks);
			webhooks = webhooksObj.getJSONArray("webhooks");
			for (Object obj : webhooks) {
				JSONObject json = new JSONObject(obj);
				if (json.getString("uniqueId").equals(webhookID)) {
					return json;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "could not get webhook: " + e.getMessage());
		}
		return null;
	}
	


	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
		try {
			opsApi.deleteWebhook(webhookID, sessionToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertFalse(true, "could not delete webhook");
		}
	}

	

}
