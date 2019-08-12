package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;


public class AttributeToAnalyticsChangeParentDevelopmentStage {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	
	/*Test flow:
	 * ---in runtime file this feature has a new field "configAttributesToAnalytics"---
	 
		1. add feature1->config1, report to analytics add feature2, move config1 to feature2
		2. add feature1->config1->config2, report to analytics config2,  add config3 under feature1, move config2 to config3
		3. add feature1->config1->config2, report to analytics config2,  add feature2->config3, move config2 to config3
		4. add feature1->mixconfig1->config1, report to analytics config1,  add feautre1->mixconfig2,   move config1 to mixconfig2
		5. add feature1->mixconfig1->config1, report to analytics config1,  add feautre2->mixconfig2,   move config1 to mixconfig2
		6. add feature1->mixconfig1->config1, report to analytics config1,  add feature2->config2->mixconfig2,   move config1 to mixconfig2
		7. add feature1->config1->config2, report to analytics both config1 & config2,  add feature2->config3, move config2 to config3
	
	 */


	@Test (description="add feature1->config1, report to analytics add feature2, move config1 to feature2, delete feature2")
	public void changeParent1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add feature2 and change parent for config1
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		feature2 = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(feature2);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, featureID2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID2).size()==0, "Incorrect number of attributes in analytics");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Attributes were not deleted from the first feature");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID2), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID2).size()==0, "Incorrect number of attributes in runtime development file ");

		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
	}
	

	@Test (description="add feature1->config1->config2, report to analytics config2,  add config3 under feature1, move config2 to config3")
	public void changeParent2() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeatureToBranch(seasonID, branchID, config, featureID1, sessionToken);
		
		//add first configuration
		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config2);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), configID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add config3 under the same feature and change parent for config2 to config3
		
		JSONObject jsonConfig3 = new JSONObject(config2);
		jsonConfig3.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, branchID, jsonConfig3.toString(), featureID1, sessionToken);


		String target = f.getFeatureFromBranch(configID3, branchID, sessionToken);
		config = f.getFeatureFromBranch(configID2, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, configID3, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
	}
	
	
	@Test (description="add feature1->config1->config2, report to analytics config2,  add feature2->config3, move config2 to config3")
	public void changeParent3() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = f.addFeatureToBranch(seasonID, branchID, config, featureID1, sessionToken);
		
		//add first configuration
		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config2);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), configID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add config3 under the feature2->config3 and change parent for config2 to config3
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		JSONObject jsonConfig3 = new JSONObject(config2);
		jsonConfig3.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, branchID, jsonConfig3.toString(), featureID2, sessionToken);


		String target = f.getFeatureFromBranch(configID3, branchID, sessionToken);
		config = f.getFeatureFromBranch(configID2, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, configID3, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID2).size()==0, "Incorrect number of attributes in analytics");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Attributes were not deleted from the first feature");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID2), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID2).size()==0, "Incorrect number of attributes in runtime development file ");
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
	}
	
	@Test (description=" add feature1->mixconfig1->config1, report to analytics config1,  add feautre1->mixconfig2,   move config1 to mixconfig2")
	public void changeParent4() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String mix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, branchID, mix, featureID1, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Configuration rule was not added to the season" + mixId);

		//add first configuration
		String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config1);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), mixId, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add feautre1->mixconfig2,   move config1 to mixconfig2
		
		String mixId2 = f.addFeatureToBranch(seasonID, branchID, mix, featureID1, sessionToken);
		Assert.assertFalse(mixId2.contains("error"), "Configuration rule was not added to the season" + mixId2);


		String target = f.getFeatureFromBranch(mixId2, branchID, sessionToken);
		String config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, mixId2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
	}
	
	
	@Test (description=" add feature1->mixconfig1->config1, report to analytics config1,  add feature2->config2->mixconfig2,   move config1 to mixconfig2")
	public void changeParent6() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String mix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, branchID, mix, featureID1, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Configuration rule was not added to the season" + mixId);

		//add first configuration
		String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config1);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), mixId, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");
		

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add feature2->config2->mixconfig2,   move config1 to mixconfig2
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = f.addFeatureToBranch(seasonID, branchID, config2, featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID2);
		
		String mixId2 = f.addFeatureToBranch(seasonID, branchID, mix, configID2, sessionToken);
		Assert.assertFalse(mixId2.contains("error"), "Configuration rule was not added to the season" + mixId2);


		String target = f.getFeatureFromBranch(mixId2, branchID, sessionToken);
		String config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, mixId2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID2).size()==0, "Incorrect number of attributes in analytics");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Attributes were not deleted from the first feature");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID2), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID2).size()==0, "Incorrect number of attributes in runtime development file ");
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
	}
	
	@Test (description=" add feature1->mixconfig1->config1, report to analytics config1,  add feautre2->mixconfig2,   move config1 to mixconfig2")
	public void changeParent5() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String mix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, branchID, mix, featureID1, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Configuration rule was not added to the season" + mixId);

		//add first configuration
		String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config1);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), mixId, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Incorrect number of attributes in analytics");

		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add feautre2->mixconfig2,   move config1 to mixconfig2
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		String mixId2 = f.addFeatureToBranch(seasonID, branchID, mix, featureID2, sessionToken);
		Assert.assertFalse(mixId2.contains("error"), "Configuration rule was not added to the season" + mixId2);


		String target = f.getFeatureFromBranch(mixId2, branchID, sessionToken);
		String config = f.getFeatureFromBranch(configID1, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, mixId2, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID2).size()==0, "Incorrect number of attributes in analytics");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==2, "Attributes were not deleted from the first feature");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID2), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID2).size()==0, "Incorrect number of attributes in runtime development file ");
		Assert.assertTrue(validateFeatureInRuntime(root, featureID1), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==2, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
	}
	
	
	@Test (description="add feature1->config1->config2, report to analytics both config1 & config2,  add feature2->config3, move config2 to config3")
	public void changeParent7() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		String config1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig1 = new JSONObject(config1);
		JSONObject newConfiguration1 = new JSONObject();
		newConfiguration1.put("title", "my title");
		newConfiguration1.put("icon", "a.gif");
		jsonConfig1.put("configuration", newConfiguration1);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig1.toString(), featureID1, sessionToken);
		
		//add first configuration
		String config2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		JSONObject jsonConfig2 = new JSONObject(config2);
		JSONObject newConfiguration2 = new JSONObject();
		newConfiguration2.put("color", "red");
		newConfiguration2.put("size", "small");
		jsonConfig2.put("configuration", newConfiguration2);
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonConfig2.toString(), configID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration1 was not added to the season" + configID2);
			
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		JSONObject attr3 = new JSONObject();
		attr3.put("name", "title");
		attr3.put("type", "REGULAR");
		attributes.add(attr3);
		JSONObject attr4 = new JSONObject();
		attr4.put("name", "icon");
		attr4.put("type", "REGULAR");
		attributes.add(attr4);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==4, "Incorrect number of attributes in analytics");


		String display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));
		
		
		//add config3 under the feature2->config3 and change parent for config2 to config3
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "feature was not added to the season" + featureID2);

		JSONObject jsonConfig3 = new JSONObject(config2);
		jsonConfig3.put("name", "CR3");
		String configID3 = f.addFeatureToBranch(seasonID, branchID, jsonConfig3.toString(), featureID2, sessionToken);


		String target = f.getFeatureFromBranch(configID3, branchID, sessionToken);
		String config = f.getFeatureFromBranch(configID2, branchID, sessionToken);
		JSONObject feature2Json = new JSONObject(target);
		JSONArray configurationRules = new JSONArray();
		configurationRules.add(new JSONObject(config));
		feature2Json.put("configurationRules", configurationRules);
		response = f.updateFeatureInBranch(seasonID, branchID, configID3, feature2Json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not updated " + response);
		
		anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID2).size()==0, "Incorrect number of attributes in analytics for feature2");
		Assert.assertTrue(validateAttributeInAnalytics(anResponse, featureID1).size()==4, "Incorrect number of attributes in analytics for feature1");

		display = an.getGlobalDataCollection(seasonID, branchID, "DISPLAY", sessionToken);
		Assert.assertTrue(an.validateDataCollectionByFeatureNames(display, anResponse).equals("true"));

		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		Assert.assertFalse(validateFeatureInRuntime(root, featureID2), "Feature's configAttributesToAnalytics was not updated in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID2).size()==0, "Incorrect number of attributes in runtime development file ");
		Assert.assertTrue(getAttributesInRuntime(root, featureID1).size()==4, "Incorrect number of attributes in runtime development file ");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
		f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
	}
	private boolean validateFeatureInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics"))
					return true;
			}
		}
		return false;
	}
	
	private JSONArray getAttributesInRuntime(JSONObject root, String id ) throws JSONException{
		JSONArray features = root.getJSONArray("features");
		for (Object f : features){
			JSONObject feature = new JSONObject(f);
			//find feature
			if (feature.getString("uniqueId").equals(id)){
				if (feature.containsKey("configAttributesForAnalytics")){
					return feature.getJSONArray("configAttributesForAnalytics");
				}
					
			}
		}
		return new JSONArray(); //if no configAttributesToAnalytics
	}

	private JSONArray validateAttributeInAnalytics(String analytics, String featureId) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesToAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");
		for (int i=0; i<featuresAttributesToAnalytics.size(); i++){
			if(featuresAttributesToAnalytics.getJSONObject(i).getString("id").equals(featureId)) 
				return featuresAttributesToAnalytics.getJSONObject(i).getJSONArray("attributes");
		}	
		
		return new JSONArray();

	}
	



	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
