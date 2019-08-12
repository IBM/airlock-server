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

public class UpdateMIXParentInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureID5;
	private String featureID6;
	private String featureID7;
	private String dummyF8Id;
	private String dummyF10Id ;
	private String mixID;
	private String mixID2;
	private String mixID3;
	private String mixID4;
	
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
	in master add F1-> MIX -> F2&F3, add F4 under root in master, checkout F4
	- add MIX2 to master, MIX2 unchecked
	in branch: 		
 	- add MIX3 to root (status new MIX)
	unchecked MIX:	
				- update MIX maxFeaturesOn - fails
				- change order of MIX features - fails	
				- unchecked MIX under new feature (mix under F5)
				- unchecked MIX under unchecked feature (mix under F1)
				- unchecked MIX under checked feature (mix under F4 - ok)
				- unchecked MIX to root
				- unchecked MIX under new mix (move MIX2 under MIX3 - ok)
				- unchecked MIX under unchecked mix (move MIX under MIX2 - fails) 
				- unchecked MIX under checked mix (mix2 under mix)
	checked MIX:
				- checkout MIX
				- update MIX maxFeatureOn - ok
				- change order of MIX features - ok			
	  			- checked MIX under new feature  (mix to F5)
				- checked MIX under unchecked feature (mix under F1)
				- checked MIX under checked	feature (mix under F4)
				- checked MIX to root
				- checked MIX under new mix (move MIX under MIX3 - ok)
				- checked MIX under unchecked mix (mix under mix2)
				- checked MIX under checked mix (move MIX2 under MIX - ok)	
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
				- unchecked feature to unchecked mix (move F1 under MIX - fails)
				- unchecked feature to checked mix (F1 to mix)
				- new feature to unchecked mix (move F5 under MIX)
				- checked feature to unchecked mix
				- checked feature to new mix ( move F5 under MIX3 (new under new))
				- checked feature to checked mix
				- new feature to new mix ( move F5 under MIX3 (new under new))
				- new feature to checked mix (move F5 to mix)


		
 */
	
	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "F1");
		featureID1 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);

		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);

		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		jsonF.put("name", "F3");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), mixID, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature3 was not added: " + featureID3);

		jsonF.put("name", "F4");
		featureID4 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature4 was not added: " + featureID4);
		
		//checkout F4
		String response = br.checkoutFeature(branchID, featureID4, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature4 was not checked out to branch");

		//new feature in branch
		jsonF.put("name", "F5");
		featureID5 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID5.contains("error"), "Feature5 was not added to branch: " + featureID5);
	
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
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		children.put(f3);
		children.put(f2);
		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Reordered features in mix that is not checked out in branch");
	}

	@Test (dependsOnMethods="reorderInUncheckedMIXInBranch", description ="Move unchecked MIX to checked out feature") 
	public void moveUncheckedMIXToCheckedFeature () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		int initialChildren = mixF.getJSONArray("features").size();
		
		JSONArray children = f4.getJSONArray("features");
		children.put(mixF);
		
		f4.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIX to checked out feature in branch: " + response);
		
		 mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixF.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");
		f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(f4.getJSONArray("branchFeaturesItems").getString(0).contains(mixF.getString("uniqueId")), "MTX is not listed in branchFeaturesItems");
	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToCheckedFeature", description ="Move unchecked MIX to new feature") 
	public void moveUncheckedMIXToNewFeature () throws IOException, JSONException {
		JSONObject mixF = new JSONObject (f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		int initialChildren = mixF.getJSONArray("features").size();
		
		JSONArray children = f5.getJSONArray("features");
		children.put(mixF);
		
		f5.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID5, f5.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked MIX to new feature in branch: " + response);
		
		 mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixF.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");
		f5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		Assert.assertTrue(f5.getJSONArray("branchFeaturesItems").getString(0).contains(mixF.getString("uniqueId")), "MTX is not listed in branchFeaturesItems of the new parent");
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		Assert.assertTrue(f4.getJSONArray("branchFeaturesItems").size()==0, "MTX is listed in branchFeaturesItems of the old parent");

	}
	
	@Test (dependsOnMethods="moveUncheckedMIXToNewFeature", description ="Move unchecked feature under unchecked MIX") 
	public void moveUncheckedFeatureUnderUncheckedMIX () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		int initialChildren = mixF.getJSONArray("features").size();
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(f1);
		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked feature under unchecked MIX in branch");
		
		 mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixF.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");
	
	}
	
	@Test (dependsOnMethods="moveUncheckedFeatureUnderUncheckedMIX", description ="Move unchecked MIX under Root in branch") 
	public void moveUncheckedMIXUnderRoot () throws IOException, JSONException {
		
		////
		//adding features under the root that should be visible in the branch but not checked out
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "dummyF6");
	    featureID6 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID6.contains("error"), "Feature1 was not added: " + featureID6);
		
		JSONObject dummyF8 = new JSONObject(feature) ;
		dummyF8.put("name", "dummyF8");				
		dummyF8Id = f.addFeature(seasonID, dummyF8.toString(), "ROOT", sessionToken);
		
		JSONObject f1 = new JSONObject(feature);
		f1.put("name", "dummyF10");
		dummyF10Id = f.addFeature(seasonID, f1.toString(), "ROOT", sessionToken);
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID4 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);

		
		////
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		String rootID = f.getBranchRootId(seasonID, branchID, sessionToken);
		br.checkoutFeature(branchID, rootID, sessionToken);
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootID, branchID, sessionToken));
		
		int initialChildren = mixF.getJSONArray("features").size();
		
		JSONArray children = root.getJSONArray("features");
		children.put(mixF);
		
		root.put("features", children);
		//mix is under F5 and must be removed from it
		for(int i=0; i<root.getJSONArray("features").size(); i++){
			if (root.getJSONArray("features").getJSONObject(i).getString("uniqueId").equals(featureID5)){
				root.getJSONArray("features").getJSONObject(i).put("features", new JSONArray());
			}
		}
		
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootID, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't moved unchecked feature under root in branch: " + response);
		
		 mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mixF.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");
		JSONObject f5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		Assert.assertTrue(f5.getJSONArray("branchFeaturesItems").size()==0, "MTX is listed in branchFeaturesItems of the old parent");

	}
	
	@Test (dependsOnMethods="moveUncheckedMIXUnderRoot", description ="Update checked out MIX") 
	public void updateCheckedMIX () throws IOException, JSONException {
		String response = br.checkoutFeature(branchID, mixID, sessionToken);
		Assert.assertFalse(response.contains("error"), "MIX was not checked out to branch: " + response);

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));

		//update mix
		mixF.put("maxFeaturesOn", 3);		
		response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update checked out mix :" + response);
	}
	
	
	@Test (dependsOnMethods="updateCheckedMIX", description ="Change features order in checked out MIX") 
	public void reorderInCheckedMIXInBranch () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = new JSONArray();
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		children.put(f3);
		children.put(f2);
		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered features in mix that is checked out in branch: " + response);
		

	}
	
	@Test (dependsOnMethods="reorderInCheckedMIXInBranch", description ="Moved checked out MIX under unchecked feature") 
	public void moveCheckedMIXUnderUncheckedFeature () throws IOException, JSONException {
		//uncheck F1		
		String response = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		//Assert.assertFalse(response.contains("error"), "feature1 was not unchecked out to branch: " + response);

		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		
		JSONArray children = f1.getJSONArray("features");
		children.put(mix);
		
		f1.put("features", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked out mix to unchecked feature ");
		
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");


	}
	
	@Test (dependsOnMethods="moveCheckedMIXUnderUncheckedFeature", description ="Moved unchecked feature under checked MIX") 
	public void moveUncheckedFeatureUnderCheckedMIX () throws IOException, JSONException {
		//cancel checkout root, otherwise new master feature is not visible in branch
		String rootID = f.getBranchRootId(seasonID, branchID, sessionToken);
		//br.cancelCheckoutFeature(branchID, rootID, sessionToken);
		/*
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);		
		jsonF.put("name", "dummyF6");
		String featureID6 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID6.contains("error"), "Feature1 was not added: " + featureID6);
*/
		JSONObject f6 = new JSONObject(f.getFeatureFromBranch(featureID6, branchID, sessionToken));
		Assert.assertFalse(f6.containsKey("error"), "Feature added to master is not found in branch");
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		
		JSONArray children = mix.getJSONArray("features");
		children.put(f6);
		
		mix.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  unchecked feature to checked out mix: " + response);
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren+1, "Incorrect children size in MTX");

	}

	@Test (dependsOnMethods="moveUncheckedFeatureUnderCheckedMIX", description ="Moved new feature under checked MIX") 
	public void moveNewFeatureUnderCheckedMIX () throws IOException, JSONException {

		JSONObject f5 = new JSONObject(f.getFeatureFromBranch(featureID5, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		JSONArray children = mix.getJSONArray("features");
		children.put(f5);
		
		mix.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move  new feature to checked out mix: " + response);
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren+1, "Incorrect children size in MTX");

	}
	
	@Test (dependsOnMethods="moveNewFeatureUnderCheckedMIX", description ="Moved checked out mix under unchecked MIX") 
	public void moveCheckedMixUnderUncheckedMix() throws IOException, JSONException{
		//add mix2 to master
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID2 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);
		
		//move MIX under MIX2 - fail
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		JSONArray children = mix2.getJSONArray("features");
		children.put(mix);
		
		mix2.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID2, mix2.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Move checkout mix under unchecked out mix ");
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");

	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderUncheckedMix", description ="Moved unchecked out mix under checked MIX") 
	public void moveUncheckedMixUnderCheckedMix() throws JSONException, IOException{


		//move MIX2 under MIX - ok
		JSONObject mix2 = new JSONObject(f.getFeatureFromBranch(mixID2, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		JSONArray children = mix.getJSONArray("features");
		children.put(mix2);
		
		mix.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mix.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move uncheckout mix under checked out mix: " + response);
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren+1, "Incorrect children size in MTX");

	}

	@Test (dependsOnMethods="moveUncheckedMixUnderCheckedMix", description ="Moved checked out mix under new MIX") 
	public void moveCheckedMixUnderNewMix() throws IOException, JSONException{
		//add mix3 to branch
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID3 = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Feature was not added to the season: " + mixID3);

		//move MIX under MIX3 - ok
		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		int initialChildren = mix.getJSONArray("features").size();
		JSONArray children = mix3.getJSONArray("features");
		children.put(mix);
		
		mix3.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checkout mix under new mix: " + response);
		
		 mix = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").size() == initialChildren, "Incorrect children size in MTX");
	}
	
	@Test (dependsOnMethods="moveCheckedMixUnderNewMix", description ="Move new feature under new MIX") 
	public void moveNewFeatureUnderNewMix() throws JSONException, IOException{

		//move new feature under MIX3 (new under new)
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject dummyF7 = new JSONObject(feature) ;
		dummyF7.put("name", "dummyF7");
				
		String dummyF7Id = f.addFeatureToBranch(seasonID, branchID, dummyF7.toString(), "ROOT", sessionToken);
		Assert.assertFalse(mixID3.contains("error"), "Feature was not added to the season: " + mixID3);

		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		dummyF7 = new JSONObject(f.getFeatureFromBranch(dummyF7Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("features");
		children.put(dummyF7);
		
		mix3.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new feature under new mix: " + response);

	}
	@Test (dependsOnMethods="moveNewFeatureUnderNewMix", description ="Move checked out feature under new MIX") 
	public void moveCheckedFeatureUnderNewMix() throws JSONException, IOException{
	/*	String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject dummyF8 = new JSONObject(feature) ;
		dummyF8.put("name", "dummyF8");				
		String dummyF8Id = f.addFeature(seasonID, dummyF8.toString(), "ROOT", sessionToken);
*/
		String response = br.checkoutFeature(branchID, dummyF8Id, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checke out feature: " + response);
		
		//move dummyF8Id under MIX3 (checked under new)
		JSONObject mix3 = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject dummyF8 = new JSONObject(f.getFeatureFromBranch(dummyF8Id, branchID, sessionToken));
		JSONArray children = mix3.getJSONArray("features");
		children.put(dummyF8);
		
		mix3.put("features", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mix3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked out feature under new mix: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedFeatureUnderNewMix", description ="Update new MIX") 
	public void updateNewMIX () throws IOException, JSONException {
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));	
		//update mix
		mixF.put("maxFeaturesOn", 3);		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Coudn't update new mix :" + response);
	}
	
	
	@Test (dependsOnMethods="updateNewMIX", description ="Change features order in new MIX") 
	public void reorderInNewMIXInBranch () throws IOException, JSONException {
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String MIX4 = f.addFeatureToBranch(seasonID, branchID, featureMix, "ROOT", sessionToken);
		Assert.assertFalse(MIX4.contains("error"), "Feature was not added to the season: " + MIX4);
		
		//add 2 features to new MIX4
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject f1 = new JSONObject(feature);
		f1.put("name", "dummy1");
		String child1 = f.addFeatureToBranch(seasonID, branchID, f1.toString(), MIX4, sessionToken);
		Assert.assertFalse(child1.contains("error"), "Feature was not added to new mix: " + child1);
		f1.put("name", "dummy2");
		String child2 = f.addFeatureToBranch(seasonID, branchID, f1.toString(), MIX4, sessionToken);
		Assert.assertFalse(child2.contains("error"), "Feature was not added to new mix: " + child2);
		
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(MIX4, branchID, sessionToken));
		JSONObject ch1 = new JSONObject(f.getFeatureFromBranch(child1, branchID, sessionToken));
		JSONObject ch2 = new JSONObject(f.getFeatureFromBranch(child2, branchID, sessionToken));

		JSONArray children = new JSONArray();
		children.put(ch2);
		children.put(ch1);
		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, MIX4, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't reordered features in mix that is new in branch: " + response);
	}
	

	@Test (dependsOnMethods="reorderInNewMIXInBranch", description ="Move unchecked MIX under unchecked feature") 
	public void moveUncheckedMIXUnderUncheckedFeature () throws IOException, JSONException {
		//make sure that MIX is unchecked
		//String response = br.cancelCheckoutFeature(branchID, mixID, sessionToken);
		//Assert.assertFalse(response.contains("error"), "cannot checkout");
			
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("features");
		children.put(mixF);		
		f1.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked mix under unchecked feature in branch");
	}
	

	@Test (dependsOnMethods="moveUncheckedMIXUnderUncheckedFeature", description ="Move checked MIX under new feature") 
	public void moveCheckedMIXUnderNewFeature () throws IOException, JSONException {
		//checkout MIX
		//String response = br.checkoutFeature(branchID, mixID, sessionToken);
		//Assert.assertFalse(response.contains("error"), "cannot checkout");
			
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject f1 = new JSONObject(feature);
		f1.put("name", "dummyF9");
		String dummyF9Id = f.addFeatureToBranch(seasonID, branchID, f1.toString(), "ROOT", sessionToken);
		JSONObject dummyF9 = new JSONObject(f.getFeatureFromBranch(dummyF9Id, branchID, sessionToken));
		
		JSONArray children = dummyF9.getJSONArray("features");
		children.put(mixF);		
		dummyF9.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, dummyF9Id, dummyF9.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under new feature in branch");
	}
	
	//- checked MIX under checked	feature (mix under F4)
	@Test (dependsOnMethods="moveCheckedMIXUnderNewFeature", description ="Move checked MIX under checked out feature") 
	public void moveCheckedMIXUnderCheckedFeature () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("features");
		children.put(mixF);		
		f4.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix under checked out feature in branch");
	}
	
	//- checked MIX to root
	@Test (dependsOnMethods="moveCheckedMIXUnderCheckedFeature", description ="Move checked MIX under root") 
	public void moveCheckedMIXToRoot () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		root = removeChildFromFeature(root,  featureID4, mixID);
		
		
		JSONArray children = root.getJSONArray("features");
		children.put(mixF);		
		root.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked mix to root in branch");
		
	}
	
	//new MIX under unchecked feature
	@Test (dependsOnMethods="moveCheckedMIXToRoot", description ="Move new MIX under unchecked feature") 
	public void moveNewMIXUnderUncheckedFeature () throws IOException, JSONException {

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("features");
		children.put(mixF);		
		f1.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked in branch");
	}
	
	//- new MIX under checked feature
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedFeature", description ="Move new MIX under checked feature") 
	public void moveNewMIXUnderCheckedFeature () throws IOException, JSONException {

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		/*String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject f1 = new JSONObject(feature);
		f1.put("name", "dummyF10");
		String dummyF10Id = f.addFeature(seasonID, f1.toString(), "ROOT", sessionToken);
		*/
		br.checkoutFeature(branchID, dummyF10Id, sessionToken);
		
		JSONObject dummyF10 = new JSONObject(f.getFeatureFromBranch(dummyF10Id, branchID, sessionToken));
		
		JSONArray children = dummyF10.getJSONArray("features");
		children.put(mixF);		
		dummyF10.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, dummyF10Id, dummyF10.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked feature in branch");
	}
	
	//new MIX under new feature
	@Test (dependsOnMethods="moveNewMIXUnderCheckedFeature", description ="Move new MIX under new feature") 
	public void moveNewMIXUnderNewFeature () throws IOException, JSONException {
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "dummy20");
		featureID7 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature1 was not added: " + featureID1);

		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject f7 = new JSONObject(f.getFeatureFromBranch(featureID7, branchID, sessionToken));
		
		JSONArray children = f7.getJSONArray("features");
		children.put(mixF);		
		f7.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID7, f7.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new feature in branch");
	}
	
	//- new MIX to branch root
	@Test (dependsOnMethods="moveNewMIXUnderNewFeature", description ="Move new MIX under root") 
	public void moveNewMIXToRoot () throws IOException, JSONException {
		
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		
		String rootId = f.getBranchRootId(seasonID, branchID, sessionToken);
		JSONObject root = new JSONObject(f.getFeatureFromBranch(rootId, branchID, sessionToken));
		root = removeChildFromFeature(root,  featureID7, mixID3);
		
		
		JSONArray children = root.getJSONArray("features");
		children.put(mixF);		
		root.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, rootId, root.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new to root in branch:" + response);
	}
	
	//new MIX under unchecked mix
	@Test (dependsOnMethods="moveNewMIXToRoot", description ="Move new MIX under unchecked MIX") 
	public void moveNewMIXUnderUncheckedMix () throws IOException, JSONException {

		//String response = br.cancelCheckoutFeature(branchID, mixID, sessionToken);
		//Assert.assertTrue(response.contains("error"), "cannot cancel checkout");
		
		JSONObject newMix = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(newMix);		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID4, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new mix under unchecked feature in branch");
	}
	
	//new MIX under checked mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under checked MIX") 
	public void moveNewMIXUnderCheckedMix () throws IOException, JSONException {

	//	String response = br.checkoutFeature(branchID, mixID, sessionToken);
	//	Assert.assertFalse(response.contains("error"), "cannot checkout");
		
		JSONObject newMix = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(newMix);		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under checked feature in branch");
	}
	
	//new MIX under new mix
	@Test (dependsOnMethods="moveNewMIXUnderUncheckedMix", description ="Move new MIX under new MIX") 
	public void moveNewMIXUnderNewMix () throws IOException, JSONException {
		String response = br.checkoutFeature(branchID, featureID1, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout");
			
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String dummyMixID = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID.contains("error"), "Feature was not added to the season: " + mixID);

		
		JSONObject dummyMix = new JSONObject(f.getFeatureFromBranch(dummyMixID, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID3, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(dummyMix);		
		mixF.put("features", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, mixID3, mixF.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move new mix under new mix in branch");
	}
	
	// checked feature to unchecked mix
	@Test (dependsOnMethods="moveNewMIXUnderNewMix", description ="Move checked feature under unchecked MIX") 
	public void moveCheckedFeatureUnderUncheckedMix () throws IOException, JSONException {

		//String response = br.cancelCheckoutFeature(branchID, mixID, sessionToken);
		//Assert.assertFalse(response.contains("error"), "cannot cancel checkout");
			
		
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID4, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(f2);		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID4, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked feature under unchecked mix in branch");
	}
	
	// checked feature to checked mix
	@Test (dependsOnMethods="moveCheckedFeatureUnderUncheckedMix", description ="Move checked feature under checked MIX") 
	public void moveCheckedFeatureUnderCheckedMix () throws IOException, JSONException {

		//br.checkoutFeature(branchID, mixID, sessionToken);
		
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject mixF = new JSONObject(f.getFeatureFromBranch(mixID, branchID, sessionToken));
		
		JSONArray children = mixF.getJSONArray("features");
		children.put(f2);		
		mixF.put("features", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, mixID, mixF.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Can't move checked feature under checked mix in branch");
	}
	 
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private  JSONObject removeChildFromFeature(JSONObject root,  String parentId, String childId) throws JSONException{
		
		JSONArray features = root.getJSONArray("features");
		
		for (int i=0; i<features.size(); i++){
			JSONObject feature = features.getJSONObject(i);
			if (feature.getString("uniqueId").equals(parentId)){	//find old parent
				JSONArray children = feature.getJSONArray("features");
				for (int j=0; j< children.size(); j++){
					if (children.getJSONObject(j).getString("uniqueId").equals(childId)){
						children.remove(j);
					}
				}
			}
	
		}
		return root;
		
	}


	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
