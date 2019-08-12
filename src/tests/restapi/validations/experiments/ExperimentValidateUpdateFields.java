package tests.restapi.validations.experiments;


import java.util.ArrayList;


import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UserGroupsRestApi;

public class ExperimentValidateUpdateFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private UserGroupsRestApi ug;
	private String m_analyticsUrl;
	private BranchesRestApi br ;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		m_analyticsUrl = analyticsUrl;
		ug = new UserGroupsRestApi();
		ug.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		br = new BranchesRestApi();
		br.setURL(m_url);

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		experimentID = baseUtils.addExperiment(analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "experiment  was not added " + experimentID );

	}
	
	@Test 
	public void updateName() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		String experimentName = RandomStringUtils.randomAlphabetic(5);
		json.put("name", experimentName);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("name").equals(experimentName), "Name was not udated");
		
	}
	
	@Test 
	public void updateStage() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("stage", "PRODUCTION");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("stage").equals("PRODUCTION"), "Stage was not udated");
			
	}
	
	@Test 
	public void updateEnabled() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		//add variant so the exp can be enabled
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		String variantID1 = exp.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);

		experiment = exp.getExperiment(experimentID, sessionToken);
		
		JSONObject json = new JSONObject(experiment);
		json.put("enabled", true);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getBoolean("enabled"), "enabled was not udated");
	}
	
	@Test 
	public void updateMinVersion() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("minVersion", "2.0");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("minVersion").equals("2.0"), "minVersion was not udated");
		}

	@Test 
	public void updateMaxVersion() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("maxVersion", "5.0");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("maxVersion").equals("5.0"), "maxVersion was not udated");
	}
	
	@Test 
	public void updateDescription() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("description", "new description");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("description").equals("new description"), "description was not udated");
	}
	
	@Test 
	public void updateHypothesis () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("hypothesis", "new hypothesis");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		String updatedExperiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(updatedExperiment);
		Assert.assertTrue(updatedJson.getString("hypothesis").equals("new hypothesis"), "hypothesis was not udated");
	}
	
	@Test 
	public void updateMeasurements () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("measurements", "new measurements");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("measurements").equals("new measurements"), "measurments was not udated");
	}
	
	@Test 
	public void updateDisplayName () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("displayName", "new display name");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getString("displayName").equals("new display name"), "displayName was not udated");
	}

	
	@Test 
	public void updateRolloutPercentage() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("rolloutPercentage", 50.5);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getDouble("rolloutPercentage")==50.5, "rolloutPercentage was not udated");
	
	}
	
	@Test 
	public void updateRule() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		JSONObject ruleString = new JSONObject(); 
		ruleString.put("ruleString", "false");
		json.put("rule", ruleString);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject updatedJson = new JSONObject(experiment);
		Assert.assertTrue(updatedJson.getJSONObject("rule").getString("ruleString").equals("false"), "rule was not udated");	
	}
	
	@Test 
	public void updateLastModified() throws Exception{
			long timestamp = System.currentTimeMillis();
			String experiment = exp.getExperiment(experimentID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("lastModified", timestamp - 10000000);
			String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "experiment  was not updated " + response );
	}
	
	@Test 
	public void updateCreationDate() throws Exception{
			long timestamp = System.currentTimeMillis();
			String experiment = exp.getExperiment(experimentID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("creationDate", timestamp - 10000000);
			String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "experiment  was not updated " + response );
	}
	
	@Test 
	public void updateCreator() throws Exception{
			String experiment = exp.getExperiment(experimentID, sessionToken);
			JSONObject json = new JSONObject(experiment);
			json.put("creator", "vicky");
			String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "experiment  was not updated " + response );
	}

	
	@Test 
	public void updateUniqueID() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		String experiment2 = FileUtils.fileToString(filePath + "experiments/experiment2.txt", "UTF-8", false);
		String experimentID2 = exp.createExperiment(productID, experiment2, sessionToken);
		experimentID2 = baseUtils.addExperiment(m_analyticsUrl, false, true);
		json.put("uniqueId", experimentID2);
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was not updated " + response );
	}
	
	@Test 
	public void updateInternalUserGroups() throws Exception{
		//update with non-existing group
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		@SuppressWarnings("unchecked")
		ArrayList<String>  internalUserGroupsBefore = (ArrayList<String> )json.get("internalUserGroups");
		internalUserGroupsBefore.add(RandomStringUtils.randomAlphabetic(3).toUpperCase());
		json.put("internalUserGroups", internalUserGroupsBefore);

		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was not updated " + response );
		
		
		//add a new group to the list of all available groups
		String userGroups = ug.getUserGroups(productID, sessionToken);;
		JSONObject jsonGroups = new JSONObject(userGroups);
		JSONArray groups = jsonGroups.getJSONArray("internalUserGroups");
		
		String newGroup = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		groups.put(newGroup);
		JSONObject groupsJson = new JSONObject(userGroups);
		groupsJson.remove("internalUserGroups");
		groupsJson.put("internalUserGroups", groups);
		ug.setUserGroups(productID, groupsJson.toString(), sessionToken);
		
		//add the new group to the feature's list of user groups
		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		@SuppressWarnings("unchecked")
		ArrayList<String> internalUserGroups = (ArrayList<String> )json.get("internalUserGroups");
		internalUserGroups.add(newGroup);
		json.put("internalUserGroups", internalUserGroups);

		response = exp.updateExperiment(experimentID, json.toString(), sessionToken);

		experiment = exp.getExperiment(experimentID, sessionToken);
		json = new JSONObject(experiment);
		@SuppressWarnings("unchecked")
		ArrayList<String>  internalUserGroupsAfter = (ArrayList<String> )json.get("internalUserGroups");
		Assert.assertEqualsNoOrder(internalUserGroups.toArray(), internalUserGroupsAfter.toArray(), "Parameter \"internalUserGroups\" was not updated.");
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
