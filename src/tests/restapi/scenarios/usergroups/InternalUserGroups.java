package tests.restapi.scenarios.usergroups;

import org.apache.commons.lang3.RandomStringUtils;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.UserGroupsRestApi;

public class InternalUserGroups {
	
	protected String m_url;
	protected JSONArray groups;
	protected String userGroups;
	private String sessionToken = "";
	private UserGroupsRestApi ug;
	private AirlockUtils baseUtils;
	String productID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProductWithoutAddingUserGroups();
		
	}
	
	@Test (description = "Get user groups")
	public void getUserGroups() throws Exception{

		userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==0, "The internalUserGroups list is not empty");
	}
	
	@Test (dependsOnMethods = "getUserGroups", description = "Update user groups")
	public void updateUserGroups() throws Exception{
		int previousSize = groups.size();
		groups.put(RandomStringUtils.randomAlphabetic(3).toUpperCase());
		groups.put("QA");	
		groups.put("DEV");	
		
		JSONObject json = new JSONObject(userGroups);
		json.remove("internalUserGroups");
		json.put("internalUserGroups", groups);
		ug.setUserGroups(productID, json.toString(), sessionToken);
		String userGroups = ug.getUserGroups(productID, sessionToken);
		json = new JSONObject(userGroups);
		JSONArray groupsAfter = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsAfter.size()==previousSize+3, "The internalUserGroups list was not updated");

	}
	
	@Test (dependsOnMethods = "updateUserGroups", description = "Add the same group twice")
	public void addGroupTwice() throws Exception{
		userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		if (groups.size()>0){
			String newGroups = groups.getString(0);
			groups.add(newGroups);
			json.remove("internalUserGroups");
			json.put("internalUserGroups", groups);
			
			String response = ug.setUserGroups(productID, json.toString(), sessionToken);
			Assert.assertTrue(!response.equals(""), "The same group was added twice.");
		}

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
