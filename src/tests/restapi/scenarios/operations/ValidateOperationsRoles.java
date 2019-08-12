package tests.restapi.scenarios.operations;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.RestClientUtils;
import tests.restapi.AirlockUtils;

public class ValidateOperationsRoles {
	
	protected String m_url;
	protected JSONArray groups;
	protected JSONObject roles;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"operationsUrl", "url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String operationsUrl, String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{

 		m_url = operationsUrl + "/roles"; 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
	}
	
	@Test
	public void getRoles() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url, sessionToken);
		String response = res.message;
		roles = new JSONObject(response);
		groups = roles.getJSONArray("roles");
		Assert.assertTrue(groups.size()>0, "The roles list is empty");
	}	

	
	@Test (dependsOnMethods = "getRoles")
	public void addExistingRoles() throws Exception{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(m_url, sessionToken);
		String response = res.message;
		roles = new JSONObject(response);
		groups = roles.getJSONArray("roles");
		
		JSONObject newRole = new JSONObject();
		newRole.put("role", "Viewer");
		JSONArray actions = new JSONArray();
		actions.put("getFeatures");
		actions.put("editFeatures");
		newRole.put("actions", actions);
		groups.put(newRole);
		roles.put("roles", groups);
		res = RestClientUtils.sendPut(m_url, roles.toString(), sessionToken);
		response = res.message;
		Assert.assertTrue(!response.equals(""), "Failed to update roles.");
	}

}
