package tests.restapi.scenarios.capabilities;
/*
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.OperationRestApi;
*/
public class VerifyCapabilities {
	
	// Test commented out as it changes the server capabilities and it in turn changes all products capabilites
	/*
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private OperationRestApi operApi;
	private AirlockUtils baseUtils;
	private TestAllApi allApis;
	
	@BeforeClass
	@Parameters({"url", "operationsUrl", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String operationsUrl, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		operApi = new OperationRestApi();
		operApi.setURL(operationsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		allApis = new  TestAllApi(url,operationsUrl,translationsUrl,analyticsUrl, configPath);
		allApis.resetServerCapabilities(sessionToken);
		
	}
	

	
	@Test (description = "Set empty capabilities")
	public void setEmptyCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();
		
		capabilities.put("capabilities", new JSONArray()); 
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set empty capabilities");
	}
	
	@Test (description = "Duplicate capabilities")
	public void duplicateCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();
		JSONArray newCapabilities = new JSONArray();
		newCapabilities.add("FEATURES");
		newCapabilities.add("FEATURES");
		capabilities.put("capabilities", new JSONArray()); 
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set duplicate capabilities");
	}
	
	@Test (description = "invalid capabilities")
	public void invalidCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();
		JSONArray newCapabilities = new JSONArray();
		newCapabilities.add("FEATURES");
		newCapabilities.add("FEAT_URES");
		capabilities.put("capabilities", new JSONArray()); 
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set invalid capabilities");
	}
	
	@Test (description = "No features capabilities")
	public void noFeaturesCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();
		
		capabilities.getJSONArray("capabilities").remove("FEATURES");
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Removed required FEATURES from capabilities");
	}
	
	@Test (description = "All except features capabilities")
	public void onlyFeaturesCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();		
		
		JSONArray newCapabilities = new JSONArray();
		newCapabilities.add("FEATURES");
		capabilities.put("capabilities",newCapabilities);
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set only FEATURES  capabilities: " + response);
		
		capabilities = getCapabilities();
		Assert.assertTrue(capabilities.getJSONArray("capabilities").size()==1, "Incorrect number of capabilites");
		Assert.assertTrue(capabilities.getJSONArray("capabilities").getString(0).equals("FEATURES"), "FEATURES not found in capabilites");
	}
	
	@Test (description = "Experiments in capabilities")
	public void experimentsInCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();		
		
		//add EXPERIMENTS without BRANCHES
		JSONArray newCapabilities = new JSONArray();
		newCapabilities.add("FEATURES");
		newCapabilities.add("EXPERIMENTS");
		capabilities.put("capabilities",newCapabilities);
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Set EXPERIMENTS without  BRANCHES capabilities");
		
		newCapabilities.add("BRANCHES");
		response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't set  capabilities: " + response);
		
		capabilities = getCapabilities();
		Assert.assertTrue(capabilities.getJSONArray("capabilities").size()==3, "Incorrect number of capabilites");
		
		//remove branches, leave experiments
		newCapabilities.remove("BRANCHES");
		response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Incorrect  capabilities ");

	}
	
	@Test (description = "lastModified in capabilities")
	public void lastModifiedInCapabilities() throws Exception{

		JSONObject capabilities = getCapabilities();				
		capabilities.remove("lastModified");
		String response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated capabilities without lastModified");
		
		capabilities = getCapabilities();				
		capabilities.put("lastModified", "");
		response = operApi.setCapabilities(capabilities.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated capabilities with empty string lastModified");
	}
	
	private JSONObject getCapabilities() throws Exception{
		String capabilities = operApi.getCapabilities(sessionToken);
		
		if (capabilities.contains("error")){
			Assert.fail("Can't get capabilities " + capabilities);
		}
		
		return new JSONObject(capabilities);
	}
	
	@AfterTest(alwaysRun = true)
	public void reset() throws Exception{
		allApis.resetServerCapabilities(sessionToken);
	}
	*/

}
