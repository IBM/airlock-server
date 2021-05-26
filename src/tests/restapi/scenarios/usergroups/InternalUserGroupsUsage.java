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
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UserGroupsRestApi;

public class InternalUserGroupsUsage {
	public static final String USER_GROUP_A = "userGroupA";
	public static final String USER_GROUP_B = "userGroupB";
	public static final String USER_GROUP_C = "userGroupC";
	
	protected String usergroups_url;
	protected String seasonID1;
	protected String seasonID2;
	protected String featureID;
	protected String featureInBranchID;
	protected String productID;
	protected String branchID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private UserGroupsRestApi ug;
	private SeasonsRestApi s;
	private ExperimentsRestApi exp;
	private BranchesRestApi br;
	private String varID1;
	private String varID2;
	private String expID1;
	private String expID2;
	private String configIDInMaster;
	private String orderingRuleID1;
	
	
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
		s = new SeasonsRestApi();
		s.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProductWithoutAddingUserGroups();
		
		String season = FileUtils.fileToString(configPath + "season1.txt", "UTF-8", false);
		JSONObject seasonObj = new JSONObject(season);
		seasonObj.put("minVersion", "0.2");
		seasonID1 = s.addSeason(productID, seasonObj.toString(), sessionToken);
		seasonObj.put("minVersion", "1.2");
		seasonID2 = s.addSeason(productID, seasonObj.toString(), sessionToken);
		
		
		usergroups_url = url + "/usergroups";
	}
	
	
	@Test (description = "Add new usergroup")
	public void addUserGroups() throws Exception{
		String response = ug.getUserGroups(productID, sessionToken);
		JSONObject json = new JSONObject(response);
		JSONArray groups = json.getJSONArray("internalUserGroups");
		groups.put(USER_GROUP_A);
		groups.put(USER_GROUP_B);
		groups.put(USER_GROUP_C);
		json.put("internalUserGroups", groups);
		String res = ug.setUserGroups(productID, json.toString(), sessionToken);
		Assert.assertFalse(res.contains("error"), "Couldn't add usergroup");
	}
	
	@Test (dependsOnMethods = "addUserGroups", description = "Add feature with new usergroup")
	public void addFeature() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONArray internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_A);
		json.put("internalUserGroups", internalUserGroupArray);
		featureID = f.addFeature(seasonID1, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID.contains("error"), "Couldn't add feature");
		
		String response = ug.getUserGroupsUsage(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't get usergroups usage");
		
		JSONObject usageObj = new JSONObject(response);
		String resProdutId = usageObj.getString("productId");
		Assert.assertEquals(resProdutId, productID, "product ids are not the same");
		
		JSONArray userGroups = usageObj.getJSONArray("internalUserGroups");
		Assert.assertTrue(userGroups.size()== 3, "wrong number of user groups");
		
		JSONObject userGruop1 = userGroups.getJSONObject(0);
		JSONObject userGruop2 = userGroups.getJSONObject(1);
		JSONObject userGruop3 = userGroups.getJSONObject(2);
		
		Assert.assertTrue(userGruop1.getString("internalUserGroup").equals(USER_GROUP_A), "wrong user group name");
		Assert.assertTrue(userGruop2.getString("internalUserGroup").equals(USER_GROUP_B), "wrong user group name");
		Assert.assertTrue(userGruop3.getString("internalUserGroup").equals(USER_GROUP_C), "wrong user group name");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==0, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==0, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==0, "wrong number of experiments");
		
		Assert.assertTrue(userGruop1.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop2.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop3.getJSONArray("seasons").size()==2, "wrong number of seasons");
		
		JSONObject season1Obj = userGruop1.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 1, "wrong number of branches");
		JSONObject featureObj = season1Obj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureObj.getString("name").equals("Feature1"), "wrong user group name");
		Assert.assertTrue(featureObj.getString("namespace").equals("ns1"), "wrong user group name");
		Assert.assertTrue(featureObj.getString("type").equals("FEATURE"), "wrong user group name");
		Assert.assertTrue(featureObj.getString("uniqueId").equals(featureID), "wrong user group name");
		
		
		JSONObject season2Obj = userGruop1.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season1Obj = userGruop2.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop2.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season1Obj = userGruop3.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop3.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
	}
	
	@Test (dependsOnMethods = "addFeature", description = "Add feature with new usergroup to branch")
	public void addFeatureToBranch() throws Exception{
		branchID = baseUtils.createBranch(seasonID2);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONArray internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_B);
		json.put("internalUserGroups", internalUserGroupArray);
		json.put("name","Feature2");
		featureInBranchID = f.addFeatureToBranch(seasonID2, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureInBranchID.contains("error"), "Couldn't add feature to branch");
		
		String response = ug.getUserGroupsUsage(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't get usergroups usage");
		
		JSONObject usageObj = new JSONObject(response);
		String resProdutId = usageObj.getString("productId");
		Assert.assertEquals(resProdutId, productID, "product ids are not the same");
		
		JSONArray userGroups = usageObj.getJSONArray("internalUserGroups");
		Assert.assertTrue(userGroups.size()== 3, "wrong number of user groups");
		
		JSONObject userGruop1 = userGroups.getJSONObject(0);
		JSONObject userGruop2 = userGroups.getJSONObject(1);
		JSONObject userGruop3 = userGroups.getJSONObject(2);
		
		Assert.assertTrue(userGruop1.getString("internalUserGroup").equals(USER_GROUP_A), "wrong user group name");
		Assert.assertTrue(userGruop2.getString("internalUserGroup").equals(USER_GROUP_B), "wrong user group name");
		Assert.assertTrue(userGruop3.getString("internalUserGroup").equals(USER_GROUP_C), "wrong user group name");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==0, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==0, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==0, "wrong number of experiments");
		
		Assert.assertTrue(userGruop1.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop2.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop3.getJSONArray("seasons").size()==2, "wrong number of seasons");
		
		JSONObject season1Obj = userGruop1.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 1, "wrong number of branches");
		JSONObject featureObj = season1Obj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureObj.getString("name").equals("Feature1"), "wrong feature name");
		Assert.assertTrue(featureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(featureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(featureObj.getString("uniqueId").equals(featureID), "wrong feature id");
		
		
		JSONObject season2Obj = userGruop1.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");

		season1Obj = userGruop2.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop2.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 1, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		JSONObject branchObj = season2Obj.getJSONArray("branches").getJSONObject(0);
		Assert.assertTrue(branchObj.getString("name").equals("branch1"), "wrong branch name");
		Assert.assertTrue(branchObj.getString("uniqueId").equals(branchID), "wrong branch id");
		Assert.assertTrue(branchObj.getJSONArray("features").size() == 1, "wrong branch features");
		JSONObject branchFeatureObj = branchObj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(branchFeatureObj.getString("name").equals("Feature2"), "wrong feature name");
		Assert.assertTrue(branchFeatureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(branchFeatureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(branchFeatureObj.getString("uniqueId").equals(featureInBranchID), "wrong feature id");
		
		
		season1Obj = userGruop3.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop3.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
	}
	
	@Test (dependsOnMethods = "addFeatureToBranch", description = "Add experiment new usergroup to branch")
	public void addExperiments() throws Exception{
		/*
		  exp1(ug_c)
		  	var1(ug_a)
		  	
		  exp2 (ug_a, ug_b, ug_c)
		  	var2 (ug_a, ug_b)
		 */
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		JSONArray internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_C);
		expJson.put("internalUserGroups", internalUserGroupArray);
		expJson.put("name","Experiment1");
		expJson.put("enabled", false);
		expJson.put("minVersion", "2.0");
		
		expID1 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(expID1.contains("error"), "Couldn't add experiment");
		internalUserGroupArray  = new JSONArray();
		
		internalUserGroupArray.add(USER_GROUP_A);
		internalUserGroupArray.add(USER_GROUP_B);
		internalUserGroupArray.add(USER_GROUP_C);
		expJson.put("internalUserGroups", internalUserGroupArray);
		expJson.put("name","Experiment2");
		expID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(expID2.contains("error"), "Couldn't add experiment");
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantJson.put("branchName", "branch1");
		internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_A);
		variantJson.put("internalUserGroups", internalUserGroupArray);
		
		varID1 = exp.createVariant(expID1, variantJson.toString(), sessionToken);
		Assert.assertFalse(varID1.contains("error"), "Couldn't add variant");
		
		variantJson.put("name", "variant2");
		variantJson.put("branchName", "branch1");
		internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_B);
		internalUserGroupArray.add(USER_GROUP_A);		
		variantJson.put("internalUserGroups", internalUserGroupArray);
		
		varID2 = exp.createVariant(expID2, variantJson.toString(), sessionToken);
		Assert.assertFalse(varID2.contains("error"), "Couldn't add variant");
		
		String response = ug.getUserGroupsUsage(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't get usergroups usage");
		
		JSONObject usageObj = new JSONObject(response);
		String resProdutId = usageObj.getString("productId");
		Assert.assertEquals(resProdutId, productID, "product ids are not the same");
		
		JSONArray userGroups = usageObj.getJSONArray("internalUserGroups");
		Assert.assertTrue(userGroups.size()== 3, "wrong number of user groups");
		
		JSONObject userGruop1 = userGroups.getJSONObject(0);
		JSONObject userGruop2 = userGroups.getJSONObject(1);
		JSONObject userGruop3 = userGroups.getJSONObject(2);
		
		Assert.assertTrue(userGruop1.getString("internalUserGroup").equals(USER_GROUP_A), "wrong user group name");
		Assert.assertTrue(userGruop2.getString("internalUserGroup").equals(USER_GROUP_B), "wrong user group name");
		Assert.assertTrue(userGruop3.getString("internalUserGroup").equals(USER_GROUP_C), "wrong user group name");
		
		//seasons section as above
		Assert.assertTrue(userGruop1.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop2.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop3.getJSONArray("seasons").size()==2, "wrong number of seasons");
		
		JSONObject season1Obj = userGruop1.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 1, "wrong number of branches");
		JSONObject featureObj = season1Obj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureObj.getString("name").equals("Feature1"), "wrong feature name");
		Assert.assertTrue(featureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(featureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(featureObj.getString("uniqueId").equals(featureID), "wrong feature id");
		
		
		JSONObject season2Obj = userGruop1.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		
		season1Obj = userGruop2.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop2.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 1, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		JSONObject branchObj = season2Obj.getJSONArray("branches").getJSONObject(0);
		Assert.assertTrue(branchObj.getString("name").equals("branch1"), "wrong branch name");
		Assert.assertTrue(branchObj.getString("uniqueId").equals(branchID), "wrong branch id");
		Assert.assertTrue(branchObj.getJSONArray("features").size() == 1, "wrong branch features");
		JSONObject branchFeatureObj = branchObj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(branchFeatureObj.getString("name").equals("Feature2"), "wrong feature name");
		Assert.assertTrue(branchFeatureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(branchFeatureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(branchFeatureObj.getString("uniqueId").equals(featureInBranchID), "wrong feature id");
		
		
		season1Obj = userGruop3.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		season2Obj = userGruop3.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		//experiments section
		/*
		  exp1(ug_c)
		  	var1(ug_a)
		  	
		  exp2 (ug_a, ug_b, ug_c)
		  	var2 (ug_a, ug_b)
		 */
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==2, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==1, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==2, "wrong number of experiments");

		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID2), "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(expID1), "wrong experiment id");
		Assert.assertTrue(!userGruop1.getJSONArray("experiments").getJSONObject(1).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID1), "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID2), "wrong variants number in experiment");

		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID1), "wrong experiment id");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 0, "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").size() == 0, "wrong variants number in experiment");
		
	}
	
	@Test (dependsOnMethods = "addExperiments", description = "Add feature with new usergroup")
	public void addConfigurationAndOrderingRules() throws Exception{
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		JSONArray internalUserGroupArray  = new JSONArray();
		internalUserGroupArray.add(USER_GROUP_B);
		internalUserGroupArray.add(USER_GROUP_C);
		jsonOR.put("internalUserGroups", internalUserGroupArray);
		jsonOR.put("minAppVersion", "0.1");
		orderingRuleID1 = f.addFeature(seasonID1, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID1.contains("error"), "Can't create orderingRule  " + orderingRuleID1);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("internalUserGroups", internalUserGroupArray);
		jsonCR.put("minAppVersion", "0.1");
		configIDInMaster = f.addFeatureToBranch(seasonID1, "MASTER", jsonCR.toString(), featureID, sessionToken);
		Assert.assertFalse(configIDInMaster.contains("error"), "Configuration rule was not added to the master: " + configIDInMaster);
	
		String response = ug.getUserGroupsUsage(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't get usergroups usage");
		
		JSONObject usageObj = new JSONObject(response);
		String resProdutId = usageObj.getString("productId");
		Assert.assertEquals(resProdutId, productID, "product ids are not the same");
		
		JSONArray userGroups = usageObj.getJSONArray("internalUserGroups");
		Assert.assertTrue(userGroups.size()== 3, "wrong number of user groups");
		
		JSONObject userGruop1 = userGroups.getJSONObject(0);
		JSONObject userGruop2 = userGroups.getJSONObject(1);
		JSONObject userGruop3 = userGroups.getJSONObject(2);
		
		Assert.assertTrue(userGruop1.getString("internalUserGroup").equals(USER_GROUP_A), "wrong user group name");
		Assert.assertTrue(userGruop2.getString("internalUserGroup").equals(USER_GROUP_B), "wrong user group name");
		Assert.assertTrue(userGruop3.getString("internalUserGroup").equals(USER_GROUP_C), "wrong user group name");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==2, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==1, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==2, "wrong number of experiments");
		
		Assert.assertTrue(userGruop1.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop2.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop3.getJSONArray("seasons").size()==2, "wrong number of seasons");
		
		JSONObject season1Obj = userGruop1.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 1, "wrong number of branches");
		JSONObject featureObj = season1Obj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureObj.getString("name").equals("Feature1"), "wrong feature name");
		Assert.assertTrue(featureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(featureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(featureObj.getString("uniqueId").equals(featureID), "wrong feature id");
		
		
		JSONObject season2Obj = userGruop1.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		
		season1Obj = userGruop2.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 2, "wrong number of branches");
		
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(configIDInMaster), "wrong config rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("type").equals("CONFIGURATION_RULE"), "wrong config rule type");

		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(1).getString("uniqueId").equals(orderingRuleID1), "wrong ordering rule rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(1).getString("type").equals("ORDERING_RULE"), "wrong ordering rule type");

		season2Obj = userGruop2.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 1, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		JSONObject branchObj = season2Obj.getJSONArray("branches").getJSONObject(0);
		Assert.assertTrue(branchObj.getString("name").equals("branch1"), "wrong branch name");
		Assert.assertTrue(branchObj.getString("uniqueId").equals(branchID), "wrong branch id");
		Assert.assertTrue(branchObj.getJSONArray("features").size() == 1, "wrong branch features");
		JSONObject branchFeatureObj = branchObj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(branchFeatureObj.getString("name").equals("Feature2"), "wrong feature name");
		Assert.assertTrue(branchFeatureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(branchFeatureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(branchFeatureObj.getString("uniqueId").equals(featureInBranchID), "wrong feature id");
		
		
		season1Obj = userGruop3.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 2, "wrong number of branches");
		
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(configIDInMaster), "wrong config rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("type").equals("CONFIGURATION_RULE"), "wrong config rule type");

		season2Obj = userGruop3.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
	}
	
	@Test (dependsOnMethods = "addConfigurationAndOrderingRules", description = "Add items with null usergroup")
	public void addItemsWithNullUserGroups() throws Exception{
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		JSONArray internalUserGroupArray  = null;
		json.put("internalUserGroups", internalUserGroupArray);
		String featureWthNoUGID = f.addFeature(seasonID2, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureWthNoUGID.contains("error"), "Couldn't add feature");
		
		String orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("internalUserGroups", internalUserGroupArray);
		jsonOR.put("minAppVersion", "0.1");
		jsonOR.put("name", "OR4");
		
		String orderingRuleID2 = f.addFeature(seasonID2, jsonOR.toString(), featureWthNoUGID, sessionToken);
		Assert.assertFalse(orderingRuleID2.contains("error"), "Can't create orderingRule  " + orderingRuleID2);
		
		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration);
		jsonCR.put("internalUserGroups", internalUserGroupArray);
		jsonCR.put("minAppVersion", "0.1");
		jsonCR.put("name", "CR4");
		String configIDInMaster1 = f.addFeatureToBranch(seasonID2, "MASTER", jsonCR.toString(), featureWthNoUGID, sessionToken);
		Assert.assertFalse(configIDInMaster1.contains("error"), "Configuration rule was not added to the master: " + configIDInMaster1);
	
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject brObj = new JSONObject(branch);
		brObj.put("name", "b11");
		String branchWithNoFeaturesID = br.createBranch(seasonID2, brObj.toString(), "MASTER", sessionToken);
		Assert.assertFalse(branchWithNoFeaturesID.contains("error"), "Can't create branch  " + branchWithNoFeaturesID);
		
		String branchID2 = br.createBranch(seasonID1, brObj.toString(), "MASTER", sessionToken);
		Assert.assertFalse(branchID2.contains("error"), "Can't create branch  " + branchID2);
		
		json.put("internalUserGroups", internalUserGroupArray);
		json.put("name","Feature2");
		String featureInBranchID1 = f.addFeatureToBranch(seasonID1, branchID2, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureInBranchID1.contains("error"), "Couldn't add feature to branch: " + featureInBranchID1);
		
		jsonOR.put("name", "OR5");		
		String orderingRuleID3 = f.addFeatureToBranch(seasonID1, branchID2, jsonOR.toString(), featureInBranchID1, sessionToken);
		Assert.assertFalse(orderingRuleID3.contains("error"), "Can't create orderingRule in bracnh  " + orderingRuleID3);
	
		jsonCR.put("name", "CR5");		
		String configID1 = f.addFeatureToBranch(seasonID1, branchID2, jsonCR.toString(), featureInBranchID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the branch: " + configID1);
	
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("internalUserGroups", internalUserGroupArray);
		expJson.put("name","Experiment5");
		expJson.put("enabled", false);
		expJson.put("minVersion", "2.0");
		
		String expID4 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(expID4.contains("error"), "Couldn't add experiment");
		expJson.put("internalUserGroups", internalUserGroupArray);
		expJson.put("name","Experiment3");
		String expID3 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertFalse(expID3.contains("error"), "Couldn't add experiment");
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant11");
		variantJson.put("branchName", "branch1");
		variantJson.put("internalUserGroups", internalUserGroupArray);
		
		String varID3 = exp.createVariant(expID4, variantJson.toString(), sessionToken);
		Assert.assertFalse(varID3.contains("error"), "Couldn't add variant");
		
		variantJson.put("name", "variant3");
		variantJson.put("branchName", "branch1");
		variantJson.put("internalUserGroups", internalUserGroupArray);
		
		String varID4 = exp.createVariant(expID4, variantJson.toString(), sessionToken);
		Assert.assertFalse(varID4.contains("error"), "Couldn't add variant");
		
		
		//verify no exception is thrown
		String response = ug.getUserGroupsUsage(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Couldn't get usergroups usage");
		
		JSONObject usageObj = new JSONObject(response);
		String resProdutId = usageObj.getString("productId");
		Assert.assertEquals(resProdutId, productID, "product ids are not the same");
		
		JSONArray userGroups = usageObj.getJSONArray("internalUserGroups");
		Assert.assertTrue(userGroups.size()== 3, "wrong number of user groups");
		
		JSONObject userGruop1 = userGroups.getJSONObject(0);
		JSONObject userGruop2 = userGroups.getJSONObject(1);
		JSONObject userGruop3 = userGroups.getJSONObject(2);
		
		Assert.assertTrue(userGruop1.getString("internalUserGroup").equals(USER_GROUP_A), "wrong user group name");
		Assert.assertTrue(userGruop2.getString("internalUserGroup").equals(USER_GROUP_B), "wrong user group name");
		Assert.assertTrue(userGruop3.getString("internalUserGroup").equals(USER_GROUP_C), "wrong user group name");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==2, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==1, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==2, "wrong number of experiments");
		
		Assert.assertTrue(userGruop1.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop2.getJSONArray("seasons").size()==2, "wrong number of seasons");
		Assert.assertTrue(userGruop3.getJSONArray("seasons").size()==2, "wrong number of seasons");
		
		JSONObject season1Obj = userGruop1.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		//Assert.assertTrue(season1Obj.getJSONArray("branches").getJSONObject(0).getJSONArray("features").size() == 0, "wrong number of features in branche");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 1, "wrong number of branches");
		JSONObject featureObj = season1Obj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(featureObj.getString("name").equals("Feature1"), "wrong feature name");
		Assert.assertTrue(featureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(featureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(featureObj.getString("uniqueId").equals(featureID), "wrong feature id");
		
		JSONObject season2Obj = userGruop1.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");

		season1Obj = userGruop2.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 2, "wrong number of branches");
		
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(configIDInMaster), "wrong config rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("type").equals("CONFIGURATION_RULE"), "wrong config rule type");

		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(1).getString("uniqueId").equals(orderingRuleID1), "wrong ordering rule rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(1).getString("type").equals("ORDERING_RULE"), "wrong ordering rule type");

		season2Obj = userGruop2.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 1, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
		
		JSONObject branchObj = season2Obj.getJSONArray("branches").getJSONObject(0);
		Assert.assertTrue(branchObj.getString("name").equals("branch1"), "wrong branch name");
		Assert.assertTrue(branchObj.getString("uniqueId").equals(branchID), "wrong branch id");
		Assert.assertTrue(branchObj.getJSONArray("features").size() == 1, "wrong branch features");
		JSONObject branchFeatureObj = branchObj.getJSONArray("features").getJSONObject(0);
		Assert.assertTrue(branchFeatureObj.getString("name").equals("Feature2"), "wrong feature name");
		Assert.assertTrue(branchFeatureObj.getString("namespace").equals("ns1"), "wrong feature namespace");
		Assert.assertTrue(branchFeatureObj.getString("type").equals("FEATURE"), "wrong feature type");
		Assert.assertTrue(branchFeatureObj.getString("uniqueId").equals(featureInBranchID), "wrong feature id");
		
		season1Obj = userGruop3.getJSONArray("seasons").getJSONObject(0);
		Assert.assertTrue(season1Obj.getString("seasonId").equals(seasonID1), "wrong season id");
		Assert.assertTrue(season1Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season1Obj.getJSONArray("features").size() == 2, "wrong number of branches");
		
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("uniqueId").equals(configIDInMaster), "wrong config rule id");
		Assert.assertTrue(season1Obj.getJSONArray("features").getJSONObject(0).getString("type").equals("CONFIGURATION_RULE"), "wrong config rule type");

		season2Obj = userGruop3.getJSONArray("seasons").getJSONObject(1);
		Assert.assertTrue(season2Obj.getString("seasonId").equals(seasonID2), "wrong season id");		
		Assert.assertTrue(season2Obj.getJSONArray("branches").size() == 0, "wrong number of branches");
		Assert.assertTrue(season2Obj.getJSONArray("features").size() == 0, "wrong number of branches");
	
		Assert.assertTrue(userGruop1.getJSONArray("experiments").size()==2, "wrong number of experiments");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").size()==1, "wrong number of experiments");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").size()==2, "wrong number of experiments");

		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID2), "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(expID1), "wrong experiment id");
		Assert.assertTrue(!userGruop1.getJSONArray("experiments").getJSONObject(1).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop1.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID1), "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 1, "wrong variants number in experiment");
		Assert.assertTrue(userGruop2.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").getJSONObject(0).getString("uniqueId").equals(varID2), "wrong variants number in experiment");

		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getString("uniqueId").equals(expID1), "wrong experiment id");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(0).getJSONArray("variants").size() == 0, "wrong variants number in experiment");
		
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getString("uniqueId").equals(expID2), "wrong experiment id");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getBoolean("inUse"), "wrong inUse param in experiment");
		Assert.assertTrue(userGruop3.getJSONArray("experiments").getJSONObject(1).getJSONArray("variants").size() == 0, "wrong variants number in experiment");

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);		
	}

}
