package tests.restapi.integration;

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
import tests.restapi.ProductsRestApi;
import tests.restapi.UserGroupsRestApi;

public class ChangeInternalUserGroupsInFeature {
	protected String m_seasonID;
	protected String featureID;
	protected String feature;
	protected String config;
	protected FeaturesRestApi f;
	protected String m_url;
	protected JSONObject json;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private UserGroupsRestApi ug;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{

		m_url = url;
		p = new ProductsRestApi();
		p.setURL(url);
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		m_seasonID = baseUtils.createSeason(productID);
		f = new FeaturesRestApi();
		f.setURL(m_url);
 		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
	}
		
	@Test (description = "Add a feature with a non-existing user group")
	public void addFeatureWithInvalidGroup() throws JSONException, IOException{
		json = new JSONObject(feature);
		JSONArray internalUserGroups = json.getJSONArray("internalUserGroups");
		String newGroup = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		internalUserGroups.put(newGroup);
		json.remove("internalUserGroups");
		json.put("internalUserGroups", internalUserGroups);
		String response = f.addFeature(m_seasonID, json.toString(), "ROOT", sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@SuppressWarnings("unchecked")
	@Test (dependsOnMethods = "addFeatureWithInvalidGroup", description = "Add a feature with a valid user group")
	public void addFeatureWithValidGroup() throws Exception{
		//add a new group to the list of all available groups
		String userGroups = ug.getUserGroups(productID, sessionToken);
		JSONObject jsonGroups = new JSONObject(userGroups);
		JSONArray groups = jsonGroups.getJSONArray("internalUserGroups");
		
		String newGroup = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		groups.put(newGroup);
		JSONObject json = new JSONObject(userGroups);
		json.remove("internalUserGroups");
		json.put("internalUserGroups", groups);
		String response = ug.setUserGroups(productID, json.toString(), sessionToken);
		
		//add the new group to the feature's list of user groups
		JSONObject obj = new JSONObject(feature);
		ArrayList<String>  internalUserGroupsBefore = (ArrayList<String> )obj.get("internalUserGroups");
		internalUserGroupsBefore.add(newGroup);
		obj.put("internalUserGroups", internalUserGroupsBefore);
		
		featureID = f.addFeature(m_seasonID, obj.toString(), "ROOT", sessionToken);
		feature = f.getFeature(featureID, sessionToken);
		obj = new JSONObject(feature);
		ArrayList<String>  internalUserGroupsAfter = (ArrayList<String> )obj.get("internalUserGroups");
		Assert.assertEqualsNoOrder(internalUserGroupsBefore.toArray(), internalUserGroupsAfter.toArray(), "Parameter \"internalUserGroups\" was not updated.");

	}
	
	@AfterTest 
	public void reset(){
		baseUtils.reset(productID, sessionToken);
		
	}
	
}
