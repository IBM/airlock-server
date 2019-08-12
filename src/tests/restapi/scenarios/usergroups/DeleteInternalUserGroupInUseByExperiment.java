package tests.restapi.scenarios.usergroups;



import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.UserGroupsRestApi;

public class DeleteInternalUserGroupInUseByExperiment {
	
	protected String usergroups_url;
	protected String newGroup;
	protected String seasonID;
	protected String featureID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private UserGroupsRestApi ug;
	private ExperimentsRestApi exp; 
	private String m_analyticsUrl;
	private BranchesRestApi br;
	private String branchID;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_analyticsUrl = analyticsUrl;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		ug = new UserGroupsRestApi();
		ug.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(url);
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
		groups.put("QA");	
		groups.put("DEV");	
		
		List<JSONArray> allGroups = Arrays.asList(groups);
		if (!allGroups.contains("TestGroupExperiment"))
			groups.put("TestGroupExperiment");
		if (!allGroups.contains("TestGroupVariant"))
			groups.put("TestGroupVariant");
		if (!allGroups.contains("TestGroupBranch"))
			groups.put("TestGroupBranch");
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Couldn't add usergroup");
	}
	
	@Test (dependsOnMethods="addUserGroups", description="Add experiment & variant")
	public void addExperiment() throws IOException, JSONException{
		JSONObject experiment =  new JSONObject(FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false));
		JSONArray groups = new JSONArray();
		groups.put("TestGroupExperiment");
		experiment.put("internalUserGroups", groups);
		experiment.put("enabled", false);
		experiment.put("name", "experiment."+RandomStringUtils.randomAlphabetic(3));
		String experimentID = exp.createExperiment(productID, experiment.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		
		JSONObject variant =  new JSONObject(FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false));
		JSONArray groupsVar = new JSONArray();
		groupsVar.put("TestGroupVariant");
		variant.put("internalUserGroups", groupsVar);
		variant.put("branchName", "branch1");
		String variantID = exp.createVariant(experimentID, variant.toString(), sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Experiment was not created: " + variantID);
	}
	

	
	@Test (dependsOnMethods = "addExperiment", description = "Delete usergroup in use by experiment")
	public void deleteUserGroupsInExperiment() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		JSONArray newGroups = new JSONArray();
		for (int i=0; i<groups.size(); i++){
			if (!groups.get(i).equals("TestGroupExperiment")){
				newGroups.put(groups.get(i));
			}
		}
		json.put("internalUserGroups", newGroups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertTrue(res.contains("cannot be deleted"), "Removed usergroup in use by experiment");

	}
	
	@Test (dependsOnMethods = "deleteUserGroupsInExperiment", description = "Delete usergroup in use by variant")
	public void deleteUserGroupsInVariant() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		JSONArray newGroups = new JSONArray();
		for (int i=0; i<groups.size(); i++){
			if (!groups.get(i).equals("TestGroupVariant")){
				newGroups.put(groups.get(i));
			}
		}
		json.put("internalUserGroups", newGroups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertTrue(res.contains("cannot be deleted"), "Removed usergroup in use by variant");
	}
	
	@Test (dependsOnMethods = "deleteUserGroupsInVariant", description = "Add feature in Branch with new usergroup")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONArray internalUserGroup  = json.getJSONArray("internalUserGroups");
		internalUserGroup.add("TestGroupBranch");
		json.put("internalUserGroups", internalUserGroup);
		String response = f.addFeatureToBranch(seasonID, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't add usergroup");
	}
	
	@Test (dependsOnMethods = "addFeature", description = "Delete usergroup in use by feature in branch")
	public void deleteUserGroupsInBranch() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		JSONArray newGroups = new JSONArray();
		for (int i=0; i<groups.size(); i++){
			if (!groups.get(i).equals("TestGroupBranch")){
				newGroups.put(groups.get(i));
			}
		}
		json.put("internalUserGroups", newGroups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertTrue(res.contains("cannot be deleted"), "Removed usergroup in use by experiment");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
