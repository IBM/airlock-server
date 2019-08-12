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

public class UpdateConfigurationRulesParentInBranch {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
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
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		m_analyticsUrl = analyticsUrl;
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
	configuration rules:
	- checked CR under checked feature
	- checked CR under unchecked feature
	- checked CR under new feature
	- unchecked CR  under new feature
	- unchecked CR under unchecked feature
	- unchecked CR under checked feature
	- new CR to unchecked feature
	- new CR to checked feature
	- new CR to new feature
	
	- checked CR under new CR
	- checked CR under unchecked CR 
	- checked CR under new CR 	
	- unchecked CR  under new CR
	- unchecked CR under unchecked CR
	- unchecked CR under checked CR	
	- new CR to unchecked CR
	- new CR to checked CR
	- new CR to new CR
		
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

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		
		jsonCR.put("name", "CR1");
		configID1 = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season: " + configID1);

		jsonF.put("name", "F2");
		featureID2 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature2 was not added: " + featureID2);
		
		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season: " + configID3);

		//checkout F2
		String response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature2 was not checked out to branch: " + response);

		
		jsonF.put("name", "F3");
		featureID3 = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature3 was not added: " + featureID2);
		
		jsonCR.put("name", "CR3");
		configID3 = f.addFeature(seasonID, jsonCR.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Configuration rule3 was not added to the season: " + configID3);

		//new feature in branch
		jsonF.put("name", "F4");
		featureID4 = f.addFeatureToBranch(seasonID, branchID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature3 was not added to branch: " + featureID4);
		
		jsonCR.put("name", "CR4");
		configID4 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureID4, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Configuration rule4 was not added to the season: " + configID4);
	
	}
	
	@Test (dependsOnMethods="addComponents", description ="Move unchecked CR to unchecked out feature") 
	public void moveUncheckedConfigToUncheckedFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToUncheckedFeature", description ="Move unchecked CR to checked out feature") 
	public void moveUncheckedConfigToCheckedFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(cr);
		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to checked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToCheckedFeature", description ="Move unchecked CR to checked out feature") 
	public void moveUncheckedConfigToNewFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	
	@Test (dependsOnMethods="moveUncheckedConfigToNewFeature", description ="Move checked CR to unchecked out feature") 
	public void moveCheckedConfigToUncheckedFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToUncheckedFeature", description ="Move checked CR to checked out feature") 
	public void moveCheckedConfigToCheckedFeature () throws IOException, JSONException {
		//checkout F3
		String response = br.checkoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't checkout featue3 to branch: " + response);
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(cr);
		
		f3.put("configurationRules", children);
		
		response = f.updateFeatureInBranch(seasonID, branchID, featureID3, f3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToCheckedFeature", description ="Move checked CR to checked out feature") 
	public void moveCheckedConfigToNewFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	
	@Test (dependsOnMethods="moveCheckedConfigToNewFeature", description ="Move new CR to unchecked out feature") 
	public void moveNewConfigToUncheckedFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new CR to unchecked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToUncheckedFeature", description ="Move new CR to checked out feature") 
	public void moveNewConfigToCheckedFeature () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		JSONObject f3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		
		JSONArray children = f3.getJSONArray("configurationRules");
		children.put(cr);
		
		f3.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID3, f3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToCheckedFeature", description ="Move new CR to checked out feature") 
	public void moveNewConfigToNewFeature () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToNewFeature", description ="Move new CR to unchecked out configuration rule") 
	public void moveNewConfigToUncheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));	
		
		//uncheck featureID3 to make its configuration CR1 unchecked
		String res = br.cancelCheckoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(res.contains("error"), "cannot cancel checkout");
		
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved new CR to unchecked configuration rule in branch: " + response);
		
		//checkout featureID1 for future workflow
		br.checkoutFeature(branchID, featureID3, sessionToken);
	}
	
	@Test (dependsOnMethods="moveNewConfigToUncheckedConfiguration", description ="Move new CR to checked out configuration rule") 
	public void moveNewConfigToCheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(cr);
		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToCheckedConfiguration", description ="Move new CR to new configuration") 
	public void moveNewConfigToNewConfiguration () throws IOException, JSONException {
		//add new configuration		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "CR5");
		configID5 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureID4, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule5 was not added to the season: " + configID1);
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID5, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveNewConfigToNewConfiguration", description ="Move unchecked CR to unchecked out configuration rule") 
	public void moveUncheckedConfigToUncheckedConfiguration () throws IOException, JSONException {
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "dummyCR1");
		String dummyCR1Id = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule was not added to the season: " + configID1);

		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(dummyCR1Id, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved unchecked CR to unchecked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToUncheckedConfiguration", description ="Move unchecked CR to checked out configuration rule") 
	public void moveUncheckedConfigToCheckedConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(cr);
		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToCheckedConfiguration", description ="Move unchecked CR to new configuration") 
	public void moveUncheckedConfigToNewConfiguration () throws IOException, JSONException {
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID4, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move unchecked CR to new feature in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveUncheckedConfigToNewConfiguration", description ="Move checked CR to unchecked out configuration rule") 
	public void moveCheckedConfigToUncheckedConfiguration () throws IOException, JSONException {
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		
		JSONArray children = f1.getJSONArray("configurationRules");
		children.put(cr);
		
		f1.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, f1.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Moved checked CR to unchecked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToUncheckedConfiguration", description ="Move checked CR to checked out configuration rule") 
	public void moveCheckedConfigToCheckedConfiguration () throws IOException, JSONException {
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject jsonF = new JSONObject(feature);
		
		jsonF.put("name", "checkedF");
		String checkedF = f.addFeature(seasonID, jsonF.toString(), "ROOT", sessionToken);
		Assert.assertFalse(checkedF.contains("error"), "checkedF was not added: " + checkedF);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "checkedCR1");
		String checkedCR1 = f.addFeature(seasonID, jsonCR.toString(), featureID1, sessionToken);
		Assert.assertFalse(checkedCR1.contains("error"), "Configuration rule checkedCR1 was not added to the season: " + checkedCR1);

		br.checkoutFeature(branchID, checkedF, sessionToken);
		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(checkedCR1, branchID, sessionToken));
		JSONObject f2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		
		JSONArray children = f2.getJSONArray("configurationRules");
		children.put(cr);
		
		f2.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, configID2, f2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to checked configuration rule in branch: " + response);
	}
	
	@Test (dependsOnMethods="moveCheckedConfigToCheckedConfiguration", description ="Move checked CR to new configuration") 
	public void moveCheckedConfigToNewConfiguration () throws IOException, JSONException {
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);		
		jsonCR.put("name", "checkedCR2");
		String checkedCR2 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), featureID4, sessionToken);
		Assert.assertFalse(checkedCR2.contains("error"), "Configuration rule checkedCR1 was not added to the season: " + checkedCR2);

		
		JSONObject cr = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		JSONObject f4 = new JSONObject(f.getFeatureFromBranch(checkedCR2, branchID, sessionToken));
		
		JSONArray children = f4.getJSONArray("configurationRules");
		children.put(cr);
		
		f4.put("configurationRules", children);
		
		String response = f.updateFeatureInBranch(seasonID, branchID, checkedCR2, f4.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't move checked CR to new feature in branch: " + response);
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
