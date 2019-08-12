package tests.restapi.scenarios.experiments;

import java.io.IOException;


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
import tests.restapi.SeasonsRestApi;

public class UpdateConfigurationMIXParentInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID11;
	private String mixID;
	private String mixID2;
	private String mixID3;
	private String mixID4;
	private String mixID5;
	private String mixID10;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	private String configID5;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		

	}

	/* 
	in master add in master add F1-> MIXCR -> CR1&CR2, add F2 with CR3 & F3 under root in master, checkout F2
	- add MIX2 to master, MIX2 unchecked
	in branch: 		
 	- in branch add F4 + CR4 - new
	unchecked MIX:	
				- update MIX maxFeaturesOn - fails
				- change order of MIX features - fails	
				- unchecked MIX under new feature 
				- unchecked MIX under unchecked feature 
				- unchecked MIX under checked feature
				- unchecked MIX to root
				- unchecked MIX under new mix 
				- unchecked MIX under unchecked mix 
				- unchecked MIX under checked mix 
	checked MIX:
				- checkout MIX
				- update MIX maxFeatureOn - ok
				- change order of MIX features - ok			
	  			- checked MIX under new feature  
				- checked MIX under unchecked feature 
				- checked MIX under checked	feature 
				- checked MIX to root
				- checked MIX under new mix 
				- checked MIX under unchecked mix 
				- checked MIX under checked mix 
	new MIX:
				- update MIX maxFeatureOn - ok
				- change order of MIX features - ok				
				- new MIX under unchecked feature
				- new MIX under checked feature
				- new MIX under new feature
				- new MIX to branch root		
				- new MIX under unchecked mix
				- new MIX under checked mix
				- new MIX under new mix

	
	move feature to mix:
				- unchecked feature to unchecked mix 
				- unchecked feature to checked mix 
				- new feature to unchecked mix 
				- checked feature to unchecked mix
				- checked feature to new mix 
				- checked feature to checked mix
				- new feature to new mix 
				- new feature to checked mix 


		
 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		//in master add F1-> MIXCR -> CR1&CR2, add F2 with CR3 & F3 under root in master, checkout F2
		//in branch add F4 + CR4 - new
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Configuration mix was not added to the season: " + mixID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		jsonCR.put("name", "CR1");
		configID1 = f.addFeature(seasonID, jsonCR.toString(), mixID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), mixID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season: " + configID2);
		
		
		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		jsonCR.put("name", "CR3");
		configID3 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule3 was not added to the season: " + configID3);

		
		jsonF.put("name", "F3");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID3);
		
		jsonCR.put("name", "CR5");
		configID5 = f.addFeature(seasonID, jsonCR.toString(), featureID3, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule5 was not added to the season: " + configID3);

		
		//checkout F2
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature2 was not checked out to branch: " + response);

		//new feature in branch
		jsonF.put("name", "F4");
		featureID4 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added to branch: " + featureID4);
		
		jsonCR.put("name", "CR4");
		configID4 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season: " + configID4);

	
	}
	
	@Test (dependsOnMethods="addComponents", description ="Update maxFeaturesOn in unchecked out MIX") 
	public void updateUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject jsonF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		jsonF.put("maxFeaturesOn", 3);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, jsonF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Updated mix that is not checked out in branch");
	}
	

	@Test (dependsOnMethods="updateUncheckedMIXInBranch", description ="Change features order in unchecked out MIX") 
	public void reorderInUncheckedMIXInBranch () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));

		JSONArray children = new JSONArray();
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		children.put(f3);
		children.put(f2);
		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Reordered features in mix that is not checked out in branch");
	}

	@Test (dependsOnMethods="reorderInUncheckedMIXInBranch", description ="Move unchecked MIX to checked out feature") 
	public void moveUncheckedMIXToCheckedFeature () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(mixF);
		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIXCR to checked out feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToCheckedFeature", description ="Move unchecked MIX to new feature") 
	public void moveUncheckedMIXToNewFeature () throws IOException, JSONException {
		JSONObject mixF = new JSONObject (f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(mixF);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIXCR to new feature in branch: " + response);
		
		mixF = new JSONObject (f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixF.getString("branchStatus").equals("CHECKED_OUT"), "MTXCR status was not changed to checked_out");
		
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToNewFeature", description ="Move unchecked feature under unchecked MIX") 
	public void moveUncheckedFeatureUnderUncheckedMIX () throws IOException, JSONException {
		//add new MTXCR in master under F1
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID10 = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Configuration mix was not added to the season: " + mixID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "CR10");
		String configID10 = f.addFeature(seasonID, jsonCR.toString(), mixID10, sessionToken);
		Assert.assertFalse(configID10.contains("error"), "Configuration rule10 was not added to the season: " + configID10);

		
		JSONObject mixF10 = new JSONObject(f.getFeatureFromBranch(mixID10, branchID, sessionToken));
		JSONObject f5 = new JSONObject(f.getFeatureFromBranch(configID5, branchID, sessionToken));
		
		JSONArray children = mixF10.getJSONArray("configurationRules");
		children.put(f5);
		
		mixF10.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID10, mixF10.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked feature under unchecked MIX in branch");
	}
	
	
	@Test (dependsOnMethods="moveUncheckedFeatureUnderUncheckedMIX", description ="Update checked out MIX") 
	public void updateCheckedMIX () throws IOException, JSONException {
		//mixF is under F4(new feature in branch)
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));

		//update mix
		mixF.put("maxFeaturesOn", 3);		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update checked out mix :" + response);
		
	}
	
	
	@Test (dependsOnMethods="updateCheckedMIX", description ="Change features order in checked out MIX") 
	public void reorderInCheckedMIXInBranch () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));

		JSONArray children = mixF.getJSONArray("configurationRules");

		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		children.remove(f2);
		children.remove(f3);
		children.put(f3);
		children.put(f2);
		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered features in mix that is checked out in branch: " + response);
		
	}
	
	@Test (dependsOnMethods="reorderInCheckedMIXInBranch", description ="Moved checked out MIX under unchecked feature") 
	public void moveCheckedMIXUnderUncheckedFeature () throws IOException, JSONException {

		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(mix);
		
		f3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, f3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked out mix to unchecked feature ");

	}
	
	
	@Test (dependsOnMethods="moveCheckedMIXUnderUncheckedFeature", description ="Moved unchecked feature under checked MIX") 
	public void moveUncheckedFeatureUnderCheckedMIX () throws IOException, JSONException {

		JSONObject f5 = new JSONObject(f.getFeatureFromBranch(configID5, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(f5);
		
		mix.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  unchecked feature to checked out mix: " + response);

	}

	@Test (dependsOnMethods="moveUncheckedFeatureUnderCheckedMIX", description ="Moved new feature under checked MIX") 
	public void moveNewFeatureUnderCheckedMIX () throws IOException, JSONException {

		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(f4);
		
		mix.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  new feature to checked out mix: " + response);

	}

	@Test (dependsOnMethods="moveNewFeatureUnderCheckedMIX", description ="Moved checked out mix under unchecked MIX") 
	public void moveCheckedMixUnderUncheckedMix() throws IOException, JSONException{
		//add mix2 to master
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "F10");
		String featureID10 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature10 was not added: " + featureID10);

		String featureMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID2 = f.addFeature(seasonID, featureMix, featureID10, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Configuration mix was not added to the season: " + mixID2);
		
		//move MIX (checked) under MIX2 (unchecked) - fail
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix2.getJSONArray("configurationRules");
		children.put(mix);
		
		mix2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID2, mix2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move checkout mix under unchecked out mix ");

	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderUncheckedMix", description ="Moved unchecked out mix under checked MIX") 
	public void moveUncheckedMixUnderCheckedMix() throws JSONException, IOException{

		//move MIX2 under MIX - ok
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix.getJSONArray("configurationRules");
		children.put(mix2);
		
		mix.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move uncheckout mix under checked out mix: " + response);

	}

	@Test (dependsOnMethods="moveUncheckedMixUnderCheckedMix", description ="Moved checked out mix under new MIX") 
	public void moveCheckedMixUnderNewMix() throws IOException, JSONException{
		//add mix3 to branch
		String featureMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID3 = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID4, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Feature was not added to the season: " + mixID3);

		//move MIX under MIX3 - ok
		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("configurationRules");
		children.put(mix);
		
		mix3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checkout mix under new mix: " + response);

	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderNewMix", description ="Move new feature under new MIX") 
	public void moveNewFeatureUnderNewMix() throws JSONException, IOException{

		//move new configuraiton rule under MIX3 (new under new)
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject dummyCR1 = new JSONObject(feature) ;
		dummyCR1.put("name", "dummyCR1");
				
		String dummyCR1Id = f.addFeatureToBranch(seasonID, branchID, dummyCR1.toString(), featureID4, sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Feature was not added to the season: " + mixID3);

		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		dummyCR1 = new JSONObject(f.getFeatureFromBranch(dummyCR1Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("configurationRules");
		children.put(dummyCR1);
		
		mix3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new feature under new mix: " + response);

	}
	@Test (dependsOnMethods="moveNewFeatureUnderNewMix", description ="Move checked out feature under new MIX") 
	public void moveCheckedFeatureUnderNewMix() throws JSONException, IOException{
		br.checkoutFeature(branchID, featureID2, sessionToken);
		 
		//add new MIXCR to branch
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID5 = f.addFeatureToBranch(seasonID, branchID, feature, featureID2, sessionToken);

		//move config2 under MIX5 (checked under new)
		JSONObject mix5 = new JSONObject(f.getFeatureFromBranch(mixID5, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONArray children = mix5.getJSONArray("configurationRules");
		children.put(f2);
		
		mix5.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID5, mix5.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out feature under new mix: " + response);

	}
	
	@Test (dependsOnMethods="moveCheckedFeatureUnderNewMix", description ="Update new MIX") 
	public void updateNewMIX () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID5, branchID, sessionToken));	
		//update mix
		mixF.put("maxFeaturesOn", 3);		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID5, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update new mix :" + response);
	}
	
	
	@Test (dependsOnMethods="updateNewMIX", description ="Change features order in new MIX") 
	public void reorderInNewMIXInBranch () throws IOException, JSONException {
		//add 2 features to new MIX3
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID5, branchID, sessionToken));
		JSONObject prevChild = mixF.getJSONArray("configurationRules").getJSONObject(0);
		
		String feature = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject f1 = new JSONObject(feature);
		f1.put("name", "dummy1");
		String childID1 = f.addFeatureToBranch(seasonID, branchID, f1.toString(), mixID5, sessionToken);
		Assert.assertFalse(childID1.contains("error"), "Feature was not added to new mix: " + childID1);
		f1.put("name", "dummy2");
		String childID2 = f.addFeatureToBranch(seasonID, branchID, f1.toString(), mixID5, sessionToken);
		Assert.assertFalse(childID2.contains("error"), "Feature was not added to new mix: " + childID2);
		
		JSONObject ch1 = new JSONObject(f.getFeatureFromBranch(childID1, branchID, sessionToken));
		JSONObject ch2 = new JSONObject(f.getFeatureFromBranch(childID2, branchID, sessionToken));

		JSONArray children = new JSONArray();
		children.put(ch2);
		children.put(ch1);
		children.put(prevChild);
		
		mixF = new JSONObject(f.getFeatureFromBranch(mixID5, branchID, sessionToken));
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID5, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered features in mix that is new in branch: " + response);
		
		mixF = new JSONObject(f.getFeatureFromBranch(mixID5, branchID, sessionToken));
		Assert.assertTrue(mixF.getJSONArray("configurationRules").getJSONObject(0).getString("uniqueId").equals(ch2.getString("uniqueId")), "Incorrect child in the first place");
		Assert.assertTrue(mixF.getJSONArray("configurationRules").getJSONObject(1).getString("uniqueId").equals(ch1.getString("uniqueId")), "Incorrect child in the second place");
		Assert.assertTrue(mixF.getJSONArray("configurationRules").getJSONObject(2).getString("uniqueId").equals(prevChild.getString("uniqueId")), "Incorrect child in the third place");
	}
	

	@Test (dependsOnMethods="reorderInNewMIXInBranch", description ="Move unchecked MIX under unchecked feature") 
	public void moveUncheckedMIXUnderUncheckedFeature () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(mixF);		
		f3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, f3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked mix under unchecked feature in branch");
	}
	

	@Test (dependsOnMethods="moveUncheckedMIXUnderUncheckedFeature", description ="Move checked MIX under new feature") 
	public void moveCheckedMIXUnderNewFeature () throws IOException, JSONException {
		//checkout MIX
		br.checkoutFeature(branchID, mixID, sessionToken);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "F11");
		featureID11 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID11.contains("error"), "Feature1 was not added: " + featureID11);

		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f11 = new JSONObject(f.getFeatureFromBranch(featureID11, branchID, sessionToken));
		
		JSONArray children = f11.getJSONArray("configurationRules");
		children.put(mixF);		
		f11.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID11, f11.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under new feature in branch: " + response);
	}
	
	//- checked MIX under checked	feature 
	@Test (dependsOnMethods="moveCheckedMIXUnderNewFeature", description ="Move checked MIX under checked out feature") 
	public void moveCheckedMIXUnderCheckedFeature () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(mixF);		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under checked out feature in branch: " + response);
	}

	
	//new MIX under unchecked feature
	@Test (dependsOnMethods="moveCheckedMIXUnderCheckedFeature", description ="Move new MIX under unchecked feature") 
	public void moveNewMIXUnderUncheckedFeature () throws IOException, JSONException {

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(mixF);		
		f3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, f3.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked in branch");
	}
	
	//- new MIX under checked feature
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedFeature", description ="Move new MIX under checked feature") 
	public void moveNewMIXUnderCheckedFeature () throws IOException, JSONException {

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(mixF);		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked feature in branch: " + response);
	}
	
	//new MIX under new feature
	@Test (dependsOnMethods="moveNewMIXUnderCheckedFeature", description ="Move new MIX under new feature") 
	public void moveNewMIXUnderNewFeature () throws IOException, JSONException {

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(mixF);		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new feature in branch: " + response);
	}
		
	//new MIX under unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewFeature", description ="Move new MIX under unchecked MIX") 
	public void moveNewMIXUnderUncheckedMix () throws IOException, JSONException {
		//add new MIXCR to master under unchecked feature
		String feature = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixID4 = f.addFeature(seasonID, feature, featureID3, sessionToken);
			
		JSONObject newMix = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(newMix);		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked feature in branch");
	}
	
	//new MIX under checked mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under checked MIX") 
	public void moveNewMIXUnderCheckedMix () throws IOException, JSONException {

		br.checkoutFeature(branchID, mixID, sessionToken);
		
		JSONObject newMix = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(newMix);		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked feature in branch: " + response);
	}
	
	//new MIX under new mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under new MIX") 
	public void moveNewMIXUnderNewMix () throws IOException, JSONException {
		String featureMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String dummyMixID = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID4, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);

		
		JSONObject dummyMix = new JSONObject(f.getFeatureFromBranch(dummyMixID, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(dummyMix);		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new mix in branch: " + response);
	}
	
	// checked feature to unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewMix", description ="Move checked feature under unchecked MIX") 
	public void moveCheckedFeatureUnderUncheckedMix () throws IOException, JSONException {
		
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(f2);		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID4, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked feature under unchecked mix in branch: " + response);
	}
	
	// checked feature to checked mix
	@Test (dependsOnMethods="moveCheckedFeatureUnderUncheckedMix", description ="Move checked feature under checked MIX") 
	public void moveCheckedFeatureUnderCheckedMix () throws IOException, JSONException {

		br.checkoutFeature(branchID, mixID, sessionToken);
		
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("configurationRules");
		children.put(f2);		
		mixF.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked feature under checked mix in branch");
	}
	 
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
