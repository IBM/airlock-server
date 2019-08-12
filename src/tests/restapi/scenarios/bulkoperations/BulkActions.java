package tests.restapi.scenarios.bulkoperations;


import java.io.IOException;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UserGroupsRestApi;


public class BulkActions {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected FeaturesRestApi f;
	protected String sessionToken = "";
	protected String m_url;
	protected String m_testUrl;
	protected String m_fromVersion;
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private String featureID;
	private String subId;
	private String airlockControlFeatureId;
	private String branchID;
	private UserGroupsRestApi ug;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_testUrl = url.replace("/api/admin", "/api/test/import");
		filePath = configPath;
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		
		productID = "384a580c-3228-46f2-95cc-fd9a3c7c4953";
		seasonID = "23c8ee4a-3736-4ad5-a20e-344303a0bf39";
		
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
		featureID = "acd73728-78c7-43eb-adac-453b007c92c4";	//HeadsUp

		subId = "48bec8b2-86fc-4eb3-8ad5-77fac118577b";
		

		airlockControlFeatureId = "8e7aa874-1dbb-4580-84bd-535da1d8e8f1";
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

	}
	
	/*
	@Test (description = "copy product to server")
	public void copy() throws Exception{
		JSONObject body = new JSONObject();
		body.put("path", "vicky/PRODUCTION_DATA/384a580c-3228-46f2-95cc-fd9a3c7c4953/23c8ee4a-3736-4ad5-a20e-344303a0bf39");		
		body.put("productName", "AndroidBulk");
		body.put("productId", productID);
		body.put("seasonId", seasonID);
		body.put("minVersion", "7.15");
		

		//http://localhost:8080/airlock/api/test/import
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(m_testUrl, body.toString(), sessionToken);
		Assert.assertTrue(res.code==200, "Product was not copied");;		
		
	}
	

	@Test (dependsOnMethods="copy", description = "move to development in master")
	public void moveToDevelopmentInMaster() throws IOException{
		String headsup = f.getFeature(featureID, sessionToken);
		headsup = headsup.replaceAll("DEVELOPMENT", "PRODUCTION");
		String response = f.updateFeature(seasonID, featureID, headsup, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk moving features to production stage: " + response);
	}
	
	@Test (dependsOnMethods="moveToDevelopmentInMaster", description = "move to production in branch ")
	public void moveToProductionInBranch() throws IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID= br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		br.checkoutFeature(branchID, subId, sessionToken);
		
		//add new feature and configuration in branch
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String newFeatureId = f.addFeatureToBranch(seasonID, branchID, feature1, subId, sessionToken);
		Assert.assertFalse(newFeatureId.contains("error"), "Feature was not added to the branch: " + newFeatureId);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String newConfigId = f.addFeatureToBranch(seasonID, branchID, configuration1, newFeatureId, sessionToken);
		Assert.assertFalse(newConfigId.contains("error"), "Configuration rule was not added to the branch: " + newConfigId);
		
		String radar = f.getFeatureFromBranch(subId, branchID, sessionToken);
		radar = radar.replaceAll("PRODUCTION", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, subId, radar, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk moving features to production stage");
	}
	
	@Test (dependsOnMethods="moveToProductionInBranch", description = "move to production in master")
	public void moveToProductionInMaster() throws IOException{
		String headsup = f.getFeature(featureID, sessionToken);
		headsup = headsup.replaceAll("PRODUCTION", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, featureID, headsup, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk moving features to production stage: " + response);
	}
	
	
	@Test (dependsOnMethods="moveToProductionInMaster", description = "move to production in branch with error")
	public void moveToProductionInBranchWithError() throws IOException{
		//checkout headsupb, its subfeatures are in status NONE
		br.checkoutFeature(branchID, airlockControlFeatureId, sessionToken);
		
		//add new feature and configuration in branch
		String feature1 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String newFeatureId = f.addFeatureToBranch(seasonID, branchID, feature1, airlockControlFeatureId, sessionToken);
		Assert.assertFalse(newFeatureId.contains("error"), "Feature was not added to the branch: " + newFeatureId);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String newConfigId = f.addFeatureToBranch(seasonID, branchID, configuration1, newFeatureId, sessionToken);
		Assert.assertFalse(newConfigId.contains("error"), "Configuration rule was not added to the branch: " + newConfigId);
		
		String headsup = f.getFeatureFromBranch(airlockControlFeatureId, branchID, sessionToken);
		headsup = headsup.replaceAll("PRODUCTION", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, airlockControlFeatureId, headsup, sessionToken);
		Assert.assertTrue(response.contains("error"), "Bulk stage update success in branch when some features are in NONE status");
	}

	//rename
	@Test (dependsOnMethods="moveToProductionInBranchWithError", description = "rename in master")
	public void renameInMaster() throws IOException{
		String headsup = f.getFeature(featureID, sessionToken);
		headsup = headsup.replaceAll("headsupv2Hero", "headsupv2NewHero");
		String response = f.updateFeature(seasonID, featureID, headsup, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk renaming features: " + response);
		
		headsup = f.getFeature(featureID, sessionToken);
		Assert.assertTrue(headsup.contains("headsupv2NewHero"), "headsupv2NewHero not found");
	}
	
	@Test (dependsOnMethods="renameInMaster",description = "rename in branch")
	public void renameInBranch() throws IOException, JSONException{
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("name", "Feature1a");
		String featureID1 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
	
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeatureToBranch(seasonID, branchID,  featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
				
		fJson.put("name", "Feature1b");
		String featureID2 = f.addFeatureToBranch(seasonID, branchID,  fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		fJson.put("name", "Feature1c");
		String featureID3 = f.addFeatureToBranch(seasonID, branchID,  fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);

		feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject allFeatures = new JSONObject(feature);
		allFeatures.put("name", "NewFeature1");
		allFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(0).put("name", "NewFeature1");
		allFeatures.getJSONArray("features").getJSONObject(0).getJSONArray("features").getJSONObject(1).put("name", "NewFeature1");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, allFeatures.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't update features names in branch: " + response);
	}
	
	//usergroups
	//add new group to server
	@Test (dependsOnMethods = "renameInBranch", description = "Add new usergroups to servers")
	public void addUserGroups() throws Exception{
		String response = ug.getUserGroups(sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		
		List<String> currGroupsList = new ArrayList<String>();
		for (int i=0; i<groups.length(); i++){
			currGroupsList.add(groups.getString(i));
		}
		
		if (!currGroupsList.contains("newUserGroup1")) {
			currGroupsList.add("newUserGroup1");
		}
		if (!currGroupsList.contains("newUserGroup2")) {
			currGroupsList.add("newUserGroup2");
		}		

		json.put("internalUserGroups", currGroupsList);
		String res = ug.setUserGroups(json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Usergroup was not added on server");

	}
	
	@Test (dependsOnMethods="addUserGroups", description = "add new usergroup in master")
	public void usergroupInMaster() throws IOException{
		String headsup = f.getFeature(featureID, sessionToken);
		headsup = headsup.replaceAll("HeadsUp V2", "newUserGroup1");
		String response = f.updateFeature(seasonID, featureID, headsup, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk changing usergroups features: " + response);
	}
	

	@Test (dependsOnMethods="usergroupInMaster", description = "add new usergroup in branch")
	public void usergroupInBranch() throws IOException{
		
		br.cancelCheckoutFeature(branchID, subId, sessionToken);
		br.checkoutFeature(branchID, subId, sessionToken);
		
		
		String radar = f.getFeatureFromBranch(subId, branchID, sessionToken);
		radar = radar.replaceAll("newUserGroup1", "newUserGroup2");
		String response = f.updateFeatureInBranch(seasonID, branchID, subId, radar, sessionToken);
		Assert.assertFalse(response.contains("error"), "Error when bulk changing usergroups features: " + response);
	}
	*/
	@Test (description ="reorder in master")
	public void reorderFeaturesInMaster() throws JSONException, IOException{
		String response = "";
		JSONObject json = new JSONObject(f.getFeature(airlockControlFeatureId, sessionToken));
		for (int i=0; i<json.getJSONArray("features").size(); i++){
			if (json.getJSONArray("features").getJSONObject(i).getJSONArray("features").size()>0){	//there is an empty MTX group under the parent feature
				JSONObject feature1 = json.getJSONArray("features").getJSONObject(i).getJSONArray("features").getJSONObject(0);
				JSONObject feature2 = json.getJSONArray("features").getJSONObject(i).getJSONArray("features").getJSONObject(1);
				json.getJSONArray("features").getJSONObject(i).put("features", new JSONArray());	//empty existing JSONArray
				json.getJSONArray("features").getJSONObject(i).getJSONArray("features").put(feature2);
				json.getJSONArray("features").getJSONObject(i).getJSONArray("features").put(feature1);
				response = f.updateFeature(seasonID, airlockControlFeatureId, json.toString(), sessionToken);
				
			}
		}
		Assert.assertFalse(response.equals("")&& response.contains("error"), "Can't reorder subfeatures");
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
	

}
