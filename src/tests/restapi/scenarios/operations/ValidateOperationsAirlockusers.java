package tests.restapi.scenarios.operations;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.UserGroupsRestApi;

public class ValidateOperationsAirlockusers {
	
	protected String m_url;
	protected JSONArray groups;
	protected JSONObject roles;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String productID;
	private UserGroupsRestApi ug;
	
	//create product, validate that user groups list is empty, add user group, try to add the same user group twice, delete user group
	
	@BeforeClass
	@Parameters({"operationsUrl", "url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String operationsUrl, String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{

 		m_url = operationsUrl + "/airlockusers"; 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		
	}
	
	@Test
	public void getUserGroups() throws Exception{
		String userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==2, "The internalUserGroups list is not empty");
		
		Assert.assertTrue(groupExists("QA", groups), "The internalUserGroups list does not contain new user group");
		Assert.assertTrue(groupExists("DEV", groups), "The internalUserGroups list does not contain new user group");
	}
	
	@Test (dependsOnMethods = "getUserGroups")
	public void addUserGroups() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		json.remove("internalUserGroups");

		String newGroup = "TestGroup";
		groups.put(newGroup);	
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Couldn't add usergroup");
		
		String userGroups = ug.getUserGroups(productID, sessionToken);
		json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==3, "The internalUserGroups list does not contain new user group");
		Assert.assertTrue(groupExists("QA", groups), "The internalUserGroups list does not contain QA user group");
		Assert.assertTrue(groupExists("DEV", groups), "The internalUserGroups list does not contain DEV user group");
		Assert.assertTrue(groupExists(newGroup, groups), "The internalUserGroups list does not contain new user group");
	}
	
	private boolean groupExists(String group, JSONArray groups) throws JSONException {
		for (int i=0; i<groups.size(); i++) {
			if (groups.getString(i).equals(group))
				return true;
		}
		return false;
	}
	
	@Test (dependsOnMethods = "addUserGroups")
	public void addExistingUserGroups() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		
		String newGroup = "TestGroup";
		groups.put(newGroup);	
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertTrue(res.contains("error"), "Can add usergroup twice");
	}
	
	@Test (dependsOnMethods = "addExistingUserGroups")
	public void removeUserGroup() throws Exception{
		String userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		
		String newGroup = "TestGroup";
		Assert.assertTrue(groups.size()==3, "The internalUserGroups list does not contain new user group");
		Assert.assertTrue(groupExists("QA", groups), "The internalUserGroups list does not contain QA user group");
		Assert.assertTrue(groupExists("DEV", groups), "The internalUserGroups list does not contain DEV user group");
		Assert.assertTrue(groupExists(newGroup, groups), "The internalUserGroups list does not contain new user group");
		
		groups.remove(2);
		
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Cannot remove usergroup");
	
		userGroups = ug.getUserGroups(productID, sessionToken);
		json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==2, "The internalUserGroups list is not empty");
		
		Assert.assertTrue(groupExists("QA", groups), "The internalUserGroups list does not contain QA user group");
		Assert.assertTrue(groupExists("DEV", groups), "The internalUserGroups list does not contain DEV user group");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
