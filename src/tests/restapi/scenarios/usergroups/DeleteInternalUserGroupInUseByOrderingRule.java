package tests.restapi.scenarios.usergroups;


import org.apache.wink.json4j.JSONArray;
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

public class DeleteInternalUserGroupInUseByOrderingRule {
	
	protected String usergroups_url;
	protected String newGroup;
	protected String seasonID;
	//protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private UserGroupsRestApi ug;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProductWithoutAddingUserGroups();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		
		usergroups_url = url + "/usergroups";
	}
	
	
	@Test (description = "Add new usergroup")
	public void addUserGroups() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		json.remove("internalUserGroups");

		newGroup = "TestGroup";
		groups.put(newGroup);	
		groups.put("QA");	
		groups.put("DEV");	
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Couldn't add usergroup");
	}
	
	@Test (dependsOnMethods = "addUserGroups", description = "Add feature with new usergroup")
	public void addOrderingRule() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureId = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureId.contains("error"), "Couldn't add feature");
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		JSONArray internalUserGroup  = jsonOR.getJSONArray("internalUserGroups");
		internalUserGroup.add(newGroup);
		jsonOR.put("internalUserGroups", internalUserGroup);
		
		String orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureId, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
	}
	
	@Test (dependsOnMethods = "addOrderingRule", description = "Delete usergroup in use by ordering rule")
	public void deleteUserGroups() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		JSONArray newGroups = new JSONArray();
		for (int i=0; i<groups.size(); i++){
			if (!groups.get(i).equals(newGroup)){
				newGroups.put(groups.get(i));
			}
		}
		
		json.remove("internalUserGroups");
		
		json.put("internalUserGroups", newGroups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertTrue(res.contains("cannot be deleted") && res.contains("OR1.OR1"), "Removed usergroup in use");
	}
	
	@AfterTest
	private void reset(){
		//f.reset();
		p.deleteProduct(productID, sessionToken);
	}

}
