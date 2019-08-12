package tests.restapi.scenarios.feature_with_configuration;

import java.io.IOException;

import java.util.ArrayList;

import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.EmailNotification;
import tests.restapi.ProductsRestApi;
import tests.restapi.UserGroupsRestApi;

public class UpdateConfigurationAndCompareParameters {
	protected String seasonID;
	protected String configurationID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected EmailNotification notification;
	private UserGroupsRestApi ug;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "notify"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String notify) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		ug = new UserGroupsRestApi();
		ug.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String feature = FileUtils.fileToString(filePath  + "feature1.txt", "UTF-8", false);
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configurationID = f.addFeature(seasonID, configuration, featureID, sessionToken);
		
		notification = baseUtils.setNotification(notify, url, sessionToken);
		notification.followFeature(configurationID);
	}


	/**
	 * Test updating, retrieving, and comparing feature parameters
	 * @throws IOException 
	 */
	@Test 
	public void testUpdateOwner() throws JSONException, IOException{
			String field = updateStringField("owner", "New Owner");
			Assert.assertTrue(field.equals("New Owner"), "Failed to updated parameter \"owner\"."); 

	}

	
	@Test 
	public void testUpdateDescription() throws JSONException, IOException{

			String field = updateStringField("description", "New description");
			Assert.assertTrue(field.equals("New description"), "Failed to updated parameter \"description\".");
	}	
	
	
	@Test 
	public void testUpdateCreator() throws JSONException, IOException{
			String feature = f.getFeature(configurationID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("creator", "New creator");
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test 
	public void testUpdateNamespace() throws JSONException, IOException{
			String field = updateStringField("namespace", "NewNamespace");
			Assert.assertTrue(field.equals("NewNamespace"), "Failed to updated parameter \"namespace\".");
	}
	
	@Test 
	public void testUpdateMinVersion() throws JSONException, IOException{
			String field = updateStringField("minAppVersion", "New minAppVersion");
			Assert.assertTrue(field.equals("New minAppVersion"), "Failed to updated parameter \"minAppVersion\".");
	}
	
	
	@Test 
	public void testName() throws JSONException, IOException{
			String field = updateStringField("name", "New feature name");
			Assert.assertTrue(field.equals("New feature name"), "Failed to updated parameter \"name\".");
	}

	@Test 
	public void testEnabled() throws JSONException, IOException{
			boolean field = updateBooleanField("enabled", false);
			Assert.assertTrue(!field, "Failed to updated parameter \"enabled\".");
	}

	
	@Test 
	public void testLastModified() throws JSONException, IOException{
			long timestamp = System.currentTimeMillis();
			updateDateField("lastModified", timestamp);

	}
	
	@Test 
	public void testCreationDate() throws JSONException, IOException{
			long timestamp = System.currentTimeMillis();
			String feature = f.getFeature(configurationID, sessionToken);
			JSONObject json = new JSONObject(feature);
			long currentDate = json.getLong("creationDate");
			Assert.assertNotNull(currentDate, "The field creationDate was not initialized.");
			json.put("creationDate", timestamp);
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@Test 
	public void testRolloutPercentage() throws JSONException, IOException{
			
			String feature = f.getFeature(configurationID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("rolloutPercentage", 30);
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
			feature = f.getFeature(configurationID, sessionToken);
			json = new JSONObject(feature);
			Assert.assertTrue(json.getDouble("rolloutPercentage")==30, "Failed to updated parameter \"rolloutPercentage\".");
	}

	@Test 
	public void testUniqueID() throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("uniqueId", "780cd507-1b86-56c3-88b8-1f44910c0f94");
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
			
	}
	
	@Test
	public void testType() throws JSONException, IOException{
			String feature = f.getFeature(configurationID, sessionToken);
			JSONObject json = new JSONObject(feature);
			json.put("type", "FEATURE");
			String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

			
	}
	
	@Test 
	public void testInternalUserGroups() throws Exception{
		
		//add a new group to the list of all available groups
		String userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject jsonGroups = new JSONObject(userGroups);
		JSONArray groups = jsonGroups.getJSONArray("internalUserGroups");
		
		String newGroup = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		groups.put(newGroup);
		JSONObject json = new JSONObject(userGroups);
		json.remove("internalUserGroups");
		json.put("internalUserGroups", groups);
		ug.setUserGroups(productID, json.toString(), sessionToken);
		
		//add the new group to the feature's list of user groups
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject obj = new JSONObject(feature);
		ArrayList<String>  internalUserGroupsBefore = (ArrayList<String> )obj.get("internalUserGroups");
		internalUserGroupsBefore.add(newGroup);
		obj.put("internalUserGroups", internalUserGroupsBefore);

		f.updateFeature(seasonID, configurationID, obj.toString(), sessionToken);
		feature = f.getFeature(configurationID, sessionToken);
		obj = new JSONObject(feature);
		ArrayList<String>  internalUserGroupsAfter = (ArrayList<String> )obj.get("internalUserGroups");
		Assert.assertEqualsNoOrder(internalUserGroupsBefore.toArray(), internalUserGroupsAfter.toArray(), "Parameter \"internalUserGroups\" was not updated.");
	}
	

	
	@Test
	public void updateRule() throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject obj = new JSONObject(feature);
		JSONObject rule = new JSONObject();
		rule.put("ruleString", "true");
		rule.put("force", true);
		obj.put("rule", rule);
		
		f.updateFeature(seasonID, configurationID, obj.toString(), sessionToken);
		
		feature = f.getFeature(configurationID, sessionToken);
		JSONObject newObj = new JSONObject(feature);
		//Assert.assertTrue(rule.equals(newRule), "Rule was not updated");
		Assert.assertTrue(newObj.getJSONObject("rule").getString("ruleString").equals(obj.getJSONObject("rule").getString("ruleString")),"Rule was not updated" );
		Assert.assertFalse(newObj.getJSONObject("rule").has("force"), "The server returned rule parameter \"force\".");

	}
	

	
	@Test (description = "If nothing is changed in json, lastModified shouldn't change after update")
	private void noChangeInUpdate() throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		long currentDate = json.getLong("lastModified");
		json.remove("seasons");
		
		f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		feature = f.getFeature(configurationID, sessionToken);
		long newDate = json.getLong("lastModified");
		Assert.assertTrue(currentDate == newDate, "lastModified date was updated");
		
	}
	

	
	private String updateStringField(String name, String value) throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put(name, value);
		f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		feature = f.getFeature(configurationID, sessionToken);
		json = new JSONObject(feature);
		return (String)json.get(name);
		
	}
	
	private boolean updateBooleanField(String name, boolean value) throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put(name, value);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		feature = f.getFeature(configurationID, sessionToken);
		json = new JSONObject(feature);
		return (boolean)json.get(name);
		
	}
	
	private long updateDateField(String name, long value) throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		long currentDate = json.getLong(name);
		Assert.assertNotNull(currentDate, "The field " + name + " was not initialized.");
		json.put(name, value);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		feature = f.getFeature(configurationID, sessionToken);
		json = new JSONObject(feature);
		return (long)json.get(name);
		
	}

	/*
	private int updateIntField(String name, int value) throws JSONException, IOException{
		String feature = f.getFeature(configurationID, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put(name, value);
		String response = f.updateFeature(seasonID, configurationID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
		feature = f.getFeature(configurationID, sessionToken);
		json = new JSONObject(feature);
		return (int)json.get(name);
		
	}

	*/

	@AfterTest
	private void reset(){
		f.unfollowFeature(configurationID, sessionToken);
		baseUtils.getNotificationResult(notification);
		baseUtils.reset(productID, sessionToken);
	}
}
