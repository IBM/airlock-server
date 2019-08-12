package tests.restapi.scenarios.multiServer;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;

public class ModifyMultiServer {
	private String sessionToken = "";
	private String operationsUrl;
	JSONObject originalJson;
	protected AirlockUtils baseUtils;

	@BeforeClass
	@Parameters({"operationsUrl", "url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String oUrl, String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
	 
		operationsUrl = oUrl;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
 	}


	@Test (description = "test multiple servers")
	public void testMultiServer() throws Exception{
		RestClientUtils.RestCallResults results = RestClientUtils.sendGet(operationsUrl + "/airlockservers", sessionToken);
		JSONObject json = new JSONObject(results.message);
		originalJson = new JSONObject(results.message);
		JSONArray allServers = json.getJSONArray("servers");
		int originalNumberOfServers = allServers.size();
		Assert.assertTrue(originalNumberOfServers>0,"there should be at least one server");

		//add server without name check fail
		JSONObject newServer = new JSONObject();
		newServer.put("cdnOverride","https://s3.amazonaws.com/airlockdev/TEST3/");
		newServer.put("url","https://s3-eu-west-1.amazonaws.com/airlockdev/TEST3/");
		allServers.add(newServer);
		json.put("servers",allServers);
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==400,"same server name should fail");
		Assert.assertTrue(results.message.equals("A displayName is missing."));

		//add server with same name check fail
		String existingName = ((JSONObject)allServers.get(0)).getString("displayName");
		((JSONObject)allServers.get(originalNumberOfServers)).put("displayName",existingName);
		json.put("servers",allServers);
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==400,"same server name should fail");
		Assert.assertTrue(results.message.contains("apears more than once in the airlock servers JSON."));

		//change name and remove url. should fail
		((JSONObject)allServers.get(originalNumberOfServers)).put("displayName","NEW_SERVER");
		((JSONObject)allServers.get(originalNumberOfServers)).remove("url");
		json.put("servers",allServers);
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==400,"missing url server name should fail");
		Assert.assertTrue(results.message.equals("A url is missing."));

		// change url and change default to missing see fail
		((JSONObject)allServers.get(originalNumberOfServers)).put("url","https://s3-eu-west-1.amazonaws.com/airlockdev/TEST3/");
		results = RestClientUtils.sendGet(operationsUrl + "/airlockservers", sessionToken);
		json = new JSONObject(results.message);
		json.put("servers",allServers);
		json.put("defaultServer","ANOTHER_SERVER");
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==400,"missing default server name should fail");
		Assert.assertTrue(results.message.contains("The defaultServer was not found"));

		//change default and get again and compare
		json.put("defaultServer","NEW_SERVER");
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==200,"could not update servers list");
		results = RestClientUtils.sendGet(operationsUrl + "/airlockservers", sessionToken);
		json = new JSONObject(results.message);
		allServers = json.getJSONArray("servers");
		Assert.assertTrue(originalNumberOfServers+1 == allServers.size(),"server was not added");
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers)).getString("cdnOverride").equals("https://s3.amazonaws.com/airlockdev/TEST3/"));
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers)).getString("displayName").equals("NEW_SERVER"));
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers)).getString("url").equals("https://s3-eu-west-1.amazonaws.com/airlockdev/TEST3/"));

		//add another without cdn
		JSONObject newServer2 = new JSONObject();
		newServer2.put("displayName","NO_CDN");
		newServer2.put("url","https://s3-eu-west-1.amazonaws.com/airlockdev/TEST4/");
		allServers.add(newServer2);
		json.put("servers",allServers);
		results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",json.toString() ,sessionToken);
		Assert.assertTrue(results.code==200,"could not update servers list");
		results = RestClientUtils.sendGet(operationsUrl + "/airlockservers", sessionToken);
		json = new JSONObject(results.message);
		allServers = json.getJSONArray("servers");
		Assert.assertTrue(originalNumberOfServers+2 == allServers.size(),"server was not added");
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers+1)).get("cdnOverride") == null);
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers+1)).getString("displayName").equals("NO_CDN"));
		Assert.assertTrue(((JSONObject) allServers.get(originalNumberOfServers+1)).getString("url").equals("https://s3-eu-west-1.amazonaws.com/airlockdev/TEST4/"));
	}


	@AfterTest
	private void reset(){
		if(originalJson == null) // nothing was done
			return;
		try {//reset to original
			RestClientUtils.RestCallResults results = RestClientUtils.sendGet(operationsUrl + "/airlockservers", sessionToken);
			JSONObject json = new JSONObject(results.message);
			originalJson.put("lastModified",json.get("lastModified"));
			results = RestClientUtils.sendPut(operationsUrl + "/airlockservers",originalJson.toString() ,sessionToken);
			Assert.assertTrue(results.code==200,"could not reset");
		} catch (Exception e) {
			Assert.fail("could not reset");
		}
	}
}
