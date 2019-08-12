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

public class VariantValidateUpdateFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private String variantID;
	private String experimentID;
	private UserGroupsRestApi ug;

	
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
		ug = new UserGroupsRestApi();
		ug.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createBranch(seasonID);
		experimentID = baseUtils.addExperiment(analyticsUrl, false, false);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		variantID = exp.createVariant(experimentID, variant, sessionToken);

	}

	@Test 
	public void updateName() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("name", "new variant name");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );		
	}

	/*
	 * tested in scenarios, as experiment must also be in production stage
	@Test 
	public void updateStage() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", "PRODUCTION");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "stage  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getString("stage").equals("PRODUCTION"), "Stage was not udated");
			
	}
	*/
	
	@Test 
	public void updateDescription() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("description", "new variant description");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "description  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getString("description").equals("new variant description"), "description was not udated");
			
	}
	
	@Test 
	public void updateDisplayName() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("displayName", "new displayName");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "description  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getString("displayName").equals("new displayName"), "displayName was not udated");
			
	}
	
	@Test 
	public void updateBranchName() throws Exception{
		//create branch to assign to variant
		BranchesRestApi br = new BranchesRestApi();
		br.setURL(m_url);
		String branch = FileUtils.fileToString(filePath + "experiments/branch2.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, "MASTER", sessionToken);
		JSONObject branchJson = new JSONObject(branch);
				
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("branchName", branchJson.getString("name"));
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "stage  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getString("branchName").equals("branch2"), "branchName was not udated");
			
	}
	
	@Test 
	public void updateEnabled() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("enabled", true);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "variant  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getBoolean("enabled"), "enabled was not udated");
	}
	
	@Test 
	public void updateRule() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		JSONObject ruleString = new JSONObject(); 
		ruleString.put("ruleString", "false");
		json.put("rule", ruleString);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment  was not updated " + response );
		
		variant = exp.getVariant(variantID, sessionToken);
		JSONObject updatedJson = new JSONObject(variant);
		Assert.assertTrue(updatedJson.getJSONObject("rule").getString("ruleString").equals("false"), "rule was not udated");	
	}
	
	@Test 
	public void updateLastModified() throws Exception{
			long timestamp = System.currentTimeMillis();
			String variant = exp.getVariant(variantID, sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("lastModified", timestamp - 10000000);
			String response = exp.updateVariant(variantID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );
	}
	
	@Test 
	public void updateCreationDate() throws Exception{
			long timestamp = System.currentTimeMillis();
			String variant = exp.getVariant(variantID, sessionToken);
			JSONObject json = new JSONObject(variant);
			json.put("creationDate", timestamp - 10000000);
			String response = exp.updateVariant(variantID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );
	}
	
	@Test 
	public void updateCreator() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("creator", "vicky");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );
	}

	
	@Test 
	public void updateUniqueID() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		String variant2 = FileUtils.fileToString(filePath + "experiments/variant2.txt", "UTF-8", false);
		String variantID2 = exp.createVariant(experimentID, variant, sessionToken);

		json.put("uniqueId", variantID2);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );
	}
	
	@Test 
	public void updateExperimentID() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("experimentId", "780cd507-1b86-56c3-88b8-1f44910c0f94");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  was updated " + response );
	}
	
	@Test 
	public void updateInternalUserGroups() throws Exception{
		//update with non-existing group
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		@SuppressWarnings("unchecked")
		ArrayList<String>  internalUserGroupsBefore = (ArrayList<String> )json.get("internalUserGroups");
		internalUserGroupsBefore.add(RandomStringUtils.randomAlphabetic(3).toUpperCase());
		json.put("internalUserGroups", internalUserGroupsBefore);

		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "variant  was not updated " + response );
		
		
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
		variant = exp.getVariant(variantID, sessionToken);
		json = new JSONObject(variant);
		@SuppressWarnings("unchecked")
		ArrayList<String> internalUserGroups = (ArrayList<String> )json.get("internalUserGroups");
		internalUserGroups.add(newGroup);
		json.put("internalUserGroups", internalUserGroups);

		response = exp.updateVariant(variantID, json.toString(), sessionToken);

		variant = exp.getVariant(variantID, sessionToken);
		json = new JSONObject(variant);
		@SuppressWarnings("unchecked")
		ArrayList<String>  internalUserGroupsAfter = (ArrayList<String> )json.get("internalUserGroups");
		Assert.assertEqualsNoOrder(internalUserGroups.toArray(), internalUserGroupsAfter.toArray(), "Parameter \"internalUserGroups\" was not updated.");
	}

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
