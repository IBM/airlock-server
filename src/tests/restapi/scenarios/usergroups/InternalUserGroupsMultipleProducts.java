package tests.restapi.scenarios.usergroups;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.ibm.airlock.admin.Utilities;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UserGroupsRestApi;

public class InternalUserGroupsMultipleProducts {
	
	protected String m_url;
	//protected JSONArray groups;
	//protected String userGroups;
	private String sessionToken = "";
	private UserGroupsRestApi ug;
	private SeasonsRestApi s;
	private FeaturesRestApi f;
	private AirlockUtils baseUtils;
	private String productID1;
	private String productID2;
	private String seasonID11;
	private String seasonID12;
	private String seasonID21;
	private String seasonID22;
	private String m_configPath;
	private String featureID11;
	private String featureID12;
	private String featureID21;
	private String featureID22;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		f = new FeaturesRestApi();
		f.setURL(url);
		
		m_url = url;
		m_configPath = configPath;
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID1 = baseUtils.createProductWithoutAddingUserGroups();
		productID2 = baseUtils.createProductWithoutAddingUserGroups();
		
		String season = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		JSONObject seasonObj = new JSONObject(season);
		seasonObj.put("minVersion", "0.2");
		seasonID11 = s.addSeason(productID1, seasonObj.toString(), sessionToken);
		seasonID21 = s.addSeason(productID2, seasonObj.toString(), sessionToken);
		
		seasonObj.put("minVersion", "0.4");
		seasonID12 = s.addSeason(productID1, seasonObj.toString(), sessionToken);
		seasonID22 = s.addSeason(productID2, seasonObj.toString(), sessionToken);
	}
	
	@Test (description = "Get user groups")
	public void getUserGroups() throws Exception{

		String userGroups = ug.getUserGroups(productID1, sessionToken);
		JSONObject json = new JSONObject(userGroups);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==0, "The internalUserGroups list of product 1 is not empty");
		
		userGroups = ug.getUserGroups(productID2, sessionToken);
		json = new JSONObject(userGroups);
		groups = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groups.size()==0, "The internalUserGroups list of product 2 is not empty");
	}
	
	@Test (dependsOnMethods = "getUserGroups", description = "check user groups runtime file existance for all 4 seasons")
	public void checkRuntimeFileExistanceInSeasons() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults response = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID11, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==304, "internal user groups runtime file does not exists for season1 in product1");
		
		response = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID12, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==304, "internal user groups runtime file does not exists for season2 in product1");
		
		response = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID21, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==304, "internal user groups runtime file does not exists for season1 in product2");
		
		response = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID22, dateFormat, sessionToken);		
		Assert.assertTrue(response.code ==304, "internal user groups runtime file does not exists for season2 in product2");		
	}
	
	@Test (dependsOnMethods = "checkRuntimeFileExistanceInSeasons", description = "Update user groups for product 1")
	public void updateUserGroupsProduct1() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String userGroups = ug.getUserGroups(productID1, sessionToken);
		Assert.assertFalse(userGroups.contains("error"), "Fail getting user groups");
		
		JSONObject json = new JSONObject(userGroups);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		
		groups.put("TEST_PROD1");
		groups.put("QA_PROD1");	
		groups.put("DEV_PROD1");	
		groups.put("QA");	
		groups.put("DEV");	
		
		json.put("internalUserGroups", groups);
		String response = ug.setUserGroups(productID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail adding user groups");
		
		userGroups = ug.getUserGroups(productID1, sessionToken);
		json = new JSONObject(userGroups);
		JSONArray groupsAfter = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsAfter.size()==5, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsAfter, Arrays.asList("TEST_PROD1",  "QA_PROD1", "DEV_PROD1", "QA","DEV" )));
		
		userGroups = ug.getUserGroups(productID2, sessionToken);
		json = new JSONObject(userGroups);
		groupsAfter = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsAfter.size()==0, "The internalUserGroups list was not updated");
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID11, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==200, "internal user groups runtime file does not exists for season1 in product1");
		json = new JSONObject(res.message);
		JSONArray groupsInRT = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsInRT.size()==5, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsInRT, Arrays.asList("TEST_PROD1",  "QA_PROD1", "DEV_PROD1", "QA","DEV" )));
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID12, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==200, "internal user groups runtime file does not exists for season2 in product1");
		json = new JSONObject(res.message);
		groupsInRT = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsInRT.size()==5, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsInRT, Arrays.asList("TEST_PROD1",  "QA_PROD1", "DEV_PROD1", "QA","DEV" )));
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID21, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==304, "internal user groups runtime file does not exists for season1 in product2");
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID22, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==304, "internal user groups runtime file does not exists for season2 in product2");
	
	}
	
	@Test (dependsOnMethods = "updateUserGroupsProduct1", description = "Update user groups for product 2")
	public void updateUserGroupsProduct2() throws Exception{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		String userGroups = ug.getUserGroups(productID2, sessionToken);
		Assert.assertFalse(userGroups.contains("error"), "Fail getting user groups");
		
		JSONObject json = new JSONObject(userGroups);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		
		groups.put("TEST_PROD2");
		groups.put("QA_PROD2");	
		groups.put("QA");	
		groups.put("DEV");	
		
		json.put("internalUserGroups", groups);
		String response = ug.setUserGroups(productID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Fail adding user groups");
		
		userGroups = ug.getUserGroups(productID2, sessionToken);
		json = new JSONObject(userGroups);
		JSONArray groupsAfter = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsAfter.size()==4, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsAfter, Arrays.asList("TEST_PROD2",  "QA_PROD2", "QA","DEV" )));
		
		userGroups = ug.getUserGroups(productID1, sessionToken);
		json = new JSONObject(userGroups);
		groupsAfter = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsAfter.size()==5, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsAfter, Arrays.asList("TEST_PROD1",  "QA_PROD1", "DEV_PROD1", "QA","DEV" )));
		
		//check if files were changed
		RuntimeDateUtilities.setSleep();
		RuntimeRestApi.DateModificationResults res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID21, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==200, "internal user groups runtime file does not exists for season1 in product2");
		json = new JSONObject(res.message);
		JSONArray groupsInRT = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsInRT.size()==4, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsInRT, Arrays.asList("TEST_PROD2", "QA_PROD2", "QA","DEV")));
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID2, seasonID22, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==200, "internal user groups runtime file does not exists for season2 in product2");
		json = new JSONObject(res.message);
		groupsInRT = json.getJSONArray("internalUserGroups");
		Assert.assertTrue(groupsInRT.size()==4, "The internalUserGroups list was not updated");
		Assert.assertTrue(areArraysEqual(groupsInRT, Arrays.asList("TEST_PROD2", "QA_PROD2", "QA","DEV")));
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID11, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==304, "internal user groups runtime file does not exists for season1 in product2");
		
		res = RuntimeDateUtilities.getInternalUserGroupsRuntimeDateModification(m_url, productID1, seasonID12, dateFormat, sessionToken);		
		Assert.assertTrue(res.code ==304, "internal user groups runtime file does not exists for season2 in product2");
	}
	
	@Test (dependsOnMethods = "updateUserGroupsProduct2", description = "Add feature with new usergroup")
	public void addFeatureWithSpecificUserGroup() throws Exception{
		//prod1
		String feature = FileUtils.fileToString(m_configPath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONArray internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD2");
		json.put("internalUserGroups", internalUserGroup);
		featureID11 = f.addFeature(seasonID11, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID11.contains("error"), "can add feature with non existing user group");
		
		json = new JSONObject(feature);		
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD1");
		json.put("internalUserGroups", internalUserGroup);
		featureID11 = f.addFeature(seasonID11, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID11.contains("error"), "Couldn't add feature");
		
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD2");
		json.put("internalUserGroups", internalUserGroup);
		featureID12 = f.addFeature(seasonID12, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID12.contains("error"), "can add feature with non existing user group");
		
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD1");
		json.put("internalUserGroups", internalUserGroup);
		featureID12 = f.addFeature(seasonID12, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID12.contains("error"), "Couldn't add feature");
		
		//prod2
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD1");
		json.put("internalUserGroups", internalUserGroup);
		featureID21 = f.addFeature(seasonID21, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID21.contains("error"), "can add feature with non existing user group");
		
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD2");
		json.put("internalUserGroups", internalUserGroup);
		featureID21 = f.addFeature(seasonID21, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID21.contains("error"), "Couldn't add feature");
		
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD1");
		json.put("internalUserGroups", internalUserGroup);
		featureID22 = f.addFeature(seasonID22, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(featureID22.contains("error"), "can add feature with non existing user group");
		
		json = new JSONObject(feature);
		internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TEST_PROD2");
		json.put("internalUserGroups", internalUserGroup);
		featureID22 = f.addFeature(seasonID22, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID22.contains("error"), "Couldn't add feature");
	}
	
	@Test (dependsOnMethods = "addFeatureWithSpecificUserGroup", description = "Add feature with new usergroup")
	public void deleteUserGroupsInUse() throws Exception{
		//remove from prod1 - first season
		int code = f.deleteFeature(featureID11, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete feature");
		
		//remove user group in use by season12
		String userGroups = ug.getUserGroups(productID1, sessionToken);
		Assert.assertFalse(userGroups.contains("error"), "Fail getting user groups");
		JSONObject json = new JSONObject(userGroups);
		JSONArray groups = new JSONArray();
		
		groups.put("TEST_PROD1");
		groups.put("QA_PROD1");	
		groups.put("DEV");	
		
		json.put("internalUserGroups", groups);
		String response = ug.setUserGroups(productID1, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("cannot be deleted"), "Succeed removing user groups that is being used");
		
		//remove from prod1 - second season
		code = f.deleteFeature(featureID12, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete feature");
		
		response = ug.setUserGroups(productID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("cannot be deleted"), "Fail removing user groups that is not being used");
		
	}
	
	private boolean areArraysEqual(JSONArray existingUserGroups, List<String> expectedUserGroups) throws JSONException {
		HashSet<String> h1 = new HashSet<String>( Arrays.asList( Utilities.jsonArrToStringArr(existingUserGroups) ));
		HashSet<String> h2 = new HashSet<String>(expectedUserGroups);
		return h1.equals(h2);
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID1, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}
}
