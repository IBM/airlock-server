package tests.restapi.authentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class ProdUnderDevAuthentication {
	protected String productID;
	protected String seasonID;
	private String branchID;
	protected ProductsRestApi productApi;
	protected SeasonsRestApi seasonApi;
	private FeaturesRestApi featureApi;
	private BranchesRestApi branchesRestApi;
	protected String sessionToken;
	protected String adminToken;
	protected String m_url;
	protected String operationsUrl;
	protected String translationUrl;
	protected String analyticsUrl;
	protected String testServicesUrl;
	protected String adminUser;
	protected String productLeadUser;
	protected String editorUser;
	protected String translatorUser;
	protected String viewerUser;
	protected String adminPassword;
	protected String productLeadPassword;
	protected String editorPassword;
	protected String translatorPassword;
	protected String viewerPassword;
	protected String m_appName = "backend_dev";
	protected String m_ssoConfigPath;
	protected String m_stage;
	protected List<String> rolesToRun = new ArrayList<String>();
	protected AirlockUtils baseUtils;
	protected String config;
	private ExperimentsRestApi experimentsApi;
	
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","testServicesUrl","configPath", "operationsUrl","admin","productLead","editor","translator","viewer","adminPass","productLeadPass","editorPass","translatorPass","viewerPass","appName","ssoConfigPath","stage", "expectedServerVersion", "productsToDeleteFile", "runRoles"})
	public void init(String url,String t_url,String a_url,String ts_url, String configPath, String c_operationsUrl,String admin,String productLead, String editor,String translator,String viewer,String adminPass,String productleadPass,String editorPass,String translatorPass,String viewerPass,@Optional String appName,@Optional String ssoConfigPath,@Optional String stage, @Optional String expectedServerVersion, String productsToDeleteFile, String runRoles) throws IOException{
		m_url = url;		
		operationsUrl = c_operationsUrl;
		translationUrl = t_url;
		analyticsUrl = a_url;
		testServicesUrl =ts_url;
		config = configPath;
		adminUser = admin;
		adminPassword = adminPass;
		productLeadUser = productLead;
		productLeadPassword = productleadPass;
		editorUser = editor;
		editorPassword = editorPass;
		translatorUser = translator;
		translatorPassword = translatorPass;
		viewerUser = viewer;
		viewerPassword = viewerPass;
		m_ssoConfigPath = ssoConfigPath;
		m_stage = stage;
		
		productApi = new ProductsRestApi();
		seasonApi = new SeasonsRestApi();
		featureApi = new FeaturesRestApi();
		branchesRestApi = new BranchesRestApi();
		experimentsApi = new ExperimentsRestApi();
		
		
		productApi.setURL(url);
		seasonApi.setURL(url);
		featureApi.setURL(url);
		branchesRestApi.setURL(url);
		experimentsApi.setURL(analyticsUrl); 

		
		baseUtils = new AirlockUtils(m_url, a_url, t_url, configPath, "", adminUser, adminPassword, m_appName, productsToDeleteFile);
		if(appName != null) {
			m_appName = appName;
		}
		if(m_ssoConfigPath != null && m_stage != null){
			adminToken = baseUtils.setNewJWTTokenUsingBluemix(adminUser, adminPassword, m_ssoConfigPath,m_stage);
		} else {
			adminToken = baseUtils.setNewJWTToken(adminUser, adminPassword, m_appName);
		}
		
		if (adminToken == null){
			Assert.fail("Can't set adminToken");
		}
			
		
		rolesToRun = Arrays.asList(runRoles.split(","));
		

	}
	
	
	@BeforeMethod
	public void prepareTests(){
		try {
			String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
			product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
			product = JSONUtils.generateUniqueString(product, 8, "name");
			productID = productApi.addProduct(product, adminToken);
			Assert.assertFalse(productID.contains("SecurityPolicyException"),  "createProduct failed: " + productID);
			baseUtils.printProductToFile(productID);
			
			String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
			seasonID = seasonApi.addSeason(productID, season, adminToken);
			
			String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
			branchID = branchesRestApi.createBranch(seasonID,branch,BranchesRestApi.MASTER,adminToken);
			Assert.assertFalse(branchID.contains("SecurityPolicyException") || branchID.contains("error"),  "createBranch failed: " + branchID);

			String experiment = FileUtils.fileToString(config + "experiments/experiment1.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experiment);
			expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
			expJson.put("enabled", false);
			expJson.put("stage", "PRODUCTION");
			String experimentID = experimentsApi.createExperiment(productID, expJson.toString(), adminToken);
			String variant = FileUtils.fileToString(config + "experiments/variant1.txt", "UTF-8", false);
			JSONObject variantJson = new JSONObject(variant);
			variantJson.put("name", "variant1");
			variantJson.put("stage", "PRODUCTION");
			String variantID = experimentsApi.createVariant(experimentID, variantJson.toString(), sessionToken);


		} catch (Exception e) {

				Assert.fail("createExperiment failed with exception:\n" +e.getLocalizedMessage());
			
		}
	}
	
	@AfterMethod
	public void deleteData(){
		try {
			productApi.deleteProduct(productID, adminToken);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	@Test
	public void run(){
		if (rolesToRun.contains("all")) {
			testAdmin();
			testProductLead();
			testEditor();
			//testViewer();
		}
		
		if (rolesToRun.contains("admin"))
			testAdmin();
		
		if (rolesToRun.contains("productLead"))
			testProductLead();
			
		if (rolesToRun.contains("editor"))
			testEditor();
		
		
		//translator and viewer cannot even create a development feature therefore these tests is irrelevant
		
		/*
		if (rolesToRun.contains("viewer"))
			testViewer();
		*/
		/*if (rolesToRun.contains("translator"))
			testTranslator();*/
	}
	
	
	public void testAdmin(){
		System.out.println("Starting role: admin");
		sessionToken = adminToken;
		ArrayList<String> featureIds = createFeature(true, false);
		ArrayList<String> configRuleIds = createConfigurationRule(true, false);
		ArrayList<String> featureUnderMTXIds = createMTXFeatures(true, false);
		ArrayList<String> configRuleUnderMTXIds = createCRMTXConfigurationRules(true, false);
		
		updateFeature(featureIds.get(1), false);	//update prod feature under dev feature
		updateFeature(configRuleIds.get(1), false); //update prod config rule under dev feature
		updateFeature(featureUnderMTXIds.get(1), false);	//update prod feature of MTX under dev feature 
		updateFeature(configRuleUnderMTXIds.get(1), false); //update prod config rule of MTX under dev feature
		
		checkoutFeature(featureIds.get(1), true, false);
		checkoutFeature(configRuleIds.get(0), false, false);			//checkout parent feature as config rules can't be checked out	
		checkoutFeature(featureUnderMTXIds.get(1), false, false);
		checkoutFeature(configRuleUnderMTXIds.get(0), false, false); //checkout parent feature as config rules can't be checked out
		
		cancelCheckoutFeature(featureIds.get(1), false);
		cancelCheckoutFeature(configRuleIds.get(0), false);		//cancel checkout parent feature as config rules can't be checked out				
		cancelCheckoutFeature(featureUnderMTXIds.get(1), false);
		cancelCheckoutFeature(configRuleUnderMTXIds.get(0), false); 	//cancel checkout parent feature as config rules can't be checked out	
		
		deleteFeature(featureIds.get(0), true);
		deleteFeature(configRuleIds.get(0), true);
		deleteFeature(featureUnderMTXIds.get(0), true);
		deleteFeature(configRuleUnderMTXIds.get(0), true);


	}
	
	public void testProductLead(){
		System.out.println("Starting role: productLead");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(productLeadUser,productLeadPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(productLeadUser,productLeadPassword,m_appName);
		}
		
		ArrayList<String> featureIds = createFeature(false, false);
		ArrayList<String> configRuleIds = createConfigurationRule(false, false);
		ArrayList<String> featureUnderMTXIds = createMTXFeatures(false, false);
		ArrayList<String> configRuleUnderMTXIds = createCRMTXConfigurationRules(false, false);
		
		updateFeature(featureIds.get(1), false);	//update prod feature under dev feature
		updateFeature(configRuleIds.get(1), false); //update prod config rule under dev feature
		updateFeature(featureUnderMTXIds.get(1), false);	//update prod feature of MTX under dev feature 
		updateFeature(configRuleUnderMTXIds.get(1), false); //update prod config rule of MTX under dev feature
		
		checkoutFeature(featureIds.get(1), false, false);
		checkoutFeature(configRuleIds.get(0), false, false);			//checkout parent feature as config rules can't be checked out	
		checkoutFeature(featureUnderMTXIds.get(1), false, false);
		checkoutFeature(configRuleUnderMTXIds.get(0), false, false); //checkout parent feature as config rules can't be checked out
		
		cancelCheckoutFeature(featureIds.get(1), false);
		cancelCheckoutFeature(configRuleIds.get(0), false);		//cancel checkout parent feature as config rules can't be checked out				
		cancelCheckoutFeature(featureUnderMTXIds.get(1), false);
		cancelCheckoutFeature(configRuleUnderMTXIds.get(0), false); 	//cancel checkout parent feature as config rules can't be checked out	
		
		deleteFeature(featureIds.get(0), true);
		deleteFeature(configRuleIds.get(0), true);
		deleteFeature(featureUnderMTXIds.get(0), true);
		deleteFeature(configRuleUnderMTXIds.get(0), true);



	}
	
	public void testEditor(){
		System.out.println("Starting role: editor");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(editorUser,editorPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(editorUser,editorPassword,m_appName);
		}
		
		ArrayList<String> featureIds = createFeature(false ,true);
		ArrayList<String> configRuleIds = createConfigurationRule(false, true);
		ArrayList<String> featureUnderMTXIds = createMTXFeatures(false, true);
		ArrayList<String> configRuleUnderMTXIds = createCRMTXConfigurationRules(false, true);
		
		updateFeature(featureIds.get(1), true);	//update prod feature under dev feature
		updateFeature(configRuleIds.get(1), true); //update prod config rule under dev feature
		updateFeature(featureUnderMTXIds.get(1), true);	//update prod feature of MTX under dev feature 
		updateFeature(configRuleUnderMTXIds.get(1), true); //update prod config rule of MTX under dev feature
		
		checkoutFeature(featureIds.get(1), false, false); //should not fail - prod under dev is considered dev
 		checkoutFeature(configRuleIds.get(0), false, false);			//checkout parent feature as config rules can't be checked out (should not fail - prod under dev is considered dev)	
		checkoutFeature(featureUnderMTXIds.get(1), false, false);
		checkoutFeature(configRuleUnderMTXIds.get(0), false, false); //checkout parent feature as config rules can't be checked out
		
		cancelCheckoutFeature(featureIds.get(1), false);
		cancelCheckoutFeature(configRuleIds.get(0), false);		//cancel checkout parent feature as config rules can't be checked out				
		cancelCheckoutFeature(featureUnderMTXIds.get(1), false);
		cancelCheckoutFeature(configRuleUnderMTXIds.get(0), false); 	//cancel checkout parent feature as config rules can't be checked out	
		
		deleteFeature(featureIds.get(0), true);
		deleteFeature(configRuleIds.get(0), true);
		deleteFeature(featureUnderMTXIds.get(0), true);
		deleteFeature(configRuleUnderMTXIds.get(0), true);



	}
	
	/*
	//viewer cannot even create a development feature therefore this test is irrelevant	
	public void testViewer(){
		System.out.println("Starting role: viewer");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(viewerUser,viewerPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(viewerUser, viewerPassword, m_appName);
		}

		ArrayList<String> featureIds = createFeature(false, true);
		ArrayList<String> configRuleIds = createConfigurationRule(false, true);
		ArrayList<String> featureUnderMTXIds = createMTXFeatures(false, true);
		ArrayList<String> configRuleUnderMTXIds = createCRMTXConfigurationRules(false, true);
		
		updateFeature(featureIds.get(1), true);	//update prod feature under dev feature
		updateFeature(configRuleIds.get(1), true); //update prod config rule under dev feature
		updateFeature(featureUnderMTXIds.get(1), true);	//update prod feature of MTX under dev feature 
		updateFeature(configRuleUnderMTXIds.get(1), true); //update prod config rule of MTX under dev feature
		
		checkoutFeature(featureIds.get(1), false, true);
		checkoutFeature(configRuleIds.get(0), false, true);			//checkout parent feature as config rules can't be checked out	
		checkoutFeature(featureUnderMTXIds.get(1), false, true);
		checkoutFeature(configRuleUnderMTXIds.get(0), false, true); //checkout parent feature as config rules can't be checked out
		
		cancelCheckoutFeature(featureIds.get(1), true);
		cancelCheckoutFeature(configRuleIds.get(0), true);		//cancel checkout parent feature as config rules can't be checked out				
		cancelCheckoutFeature(featureUnderMTXIds.get(1), true);
		cancelCheckoutFeature(configRuleUnderMTXIds.get(0), true); 	//cancel checkout parent feature as config rules can't be checked out	
		
		deleteFeature(featureIds.get(0), true);
		deleteFeature(configRuleIds.get(0), true);
		deleteFeature(featureUnderMTXIds.get(0), true);
		deleteFeature(configRuleUnderMTXIds.get(0), true);
	}
	*/
	/*
	//translator cannot even create a development feature therefore this test is irrelevant
	public void testTranslator(){
		System.out.println("Starting role: translator");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(translatorUser,translatorPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(translatorUser, translatorPassword, m_appName);
		}

		ArrayList<String> featureIds = createFeature(false, true);
		ArrayList<String> configRuleIds = createConfigurationRule(false, true);
		ArrayList<String> featureUnderMTXIds = createMTXFeatures(false, true);
		ArrayList<String> configRuleUnderMTXIds = createCRMTXConfigurationRules(false, true);
		
		updateFeature(featureIds.get(1), true);	//update prod feature under dev feature
		updateFeature(configRuleIds.get(1), true); //update prod config rule under dev feature
		updateFeature(featureUnderMTXIds.get(1), true);	//update prod feature of MTX under dev feature 
		updateFeature(configRuleUnderMTXIds.get(1), true); //update prod config rule of MTX under dev feature
		
		checkoutFeature(featureIds.get(1), false, true);
		checkoutFeature(configRuleIds.get(0), false, true);			//checkout parent feature as config rules can't be checked out	
		checkoutFeature(featureUnderMTXIds.get(1), false, true);
		checkoutFeature(configRuleUnderMTXIds.get(0), false, true); //checkout parent feature as config rules can't be checked out
		
		cancelCheckoutFeature(featureIds.get(1), true);
		cancelCheckoutFeature(configRuleIds.get(0), true);		//cancel checkout parent feature as config rules can't be checked out				
		cancelCheckoutFeature(featureUnderMTXIds.get(1), true);
		cancelCheckoutFeature(configRuleUnderMTXIds.get(0), true); 	//cancel checkout parent feature as config rules can't be checked out	
		
		deleteFeature(featureIds.get(0), true);
		deleteFeature(configRuleIds.get(0), true);
		deleteFeature(featureUnderMTXIds.get(0), true);
		deleteFeature(configRuleUnderMTXIds.get(0), true);
	}*/
	
	public ArrayList<String> createFeature(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running createFeature");
		ArrayList<String> ids = new ArrayList<String>();
		try {
			String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = featureApi.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
	
			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID = featureApi.addFeature(seasonID, json.toString(), parentID, sessionToken);
			Assert.assertEquals(childID.contains("SecurityPolicyException") ||  childID.contains("Only a user with the Administrator"), expectedFailure, "createFeature failed: " + childID);
			
			
			if(!asAdmin) {
				json.put("name", RandomStringUtils.randomAlphabetic(5));				
				childID = featureApi.addFeature(seasonID, json.toString(), parentID, adminToken);
			}
			
			
			ids.add(parentID);
			ids.add(childID);
			
		} catch (Exception e){
			if (expectedFailure == false) {
				Assert.fail("createFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
		
		return ids;

	}
	
	public ArrayList<String> createConfigurationRule(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running createConfigurationRule");
		ArrayList<String> ids = new ArrayList<String>();
		try {
			String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = featureApi.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
	
			String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = featureApi.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertEquals(configID1.contains("SecurityPolicyException") || configID1.contains("Only a user with the Administrator"), expectedFailure, "createFeature failed: " + configID1);
			

			if(!asAdmin) {
				jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));				
				configID1 = featureApi.addFeature(seasonID, jsonCR.toString(), parentID, adminToken);
			}
			
			ids.add(parentID);
			ids.add(configID1);
		} catch (Exception e){
			if (expectedFailure == false) {
				Assert.fail("createConfigurationRule failed with exception:\n" +e.getLocalizedMessage());
			}
		}
		return ids;

	}
	
	public ArrayList<String> createMTXFeatures(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running createMTXFeatures");
		ArrayList<String> ids = new ArrayList<String>();
		try {
			String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = featureApi.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
				
			String featureMix = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
			String mtxID = featureApi.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);
			
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = featureApi.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);

			json.put("stage", "PRODUCTION");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = featureApi.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			
			Assert.assertEquals(childID1.contains("SecurityPolicyException") || childID1.contains("Only a user with the Administrator"), expectedFailure, "createFeature failed: " + childID1);
			
			if(!asAdmin) {
				json.put("name", RandomStringUtils.randomAlphabetic(5));				
				childID1 = featureApi.addFeature(seasonID, json.toString(), mtxID, adminToken);
			}
			
			ids.add(parentID);
			ids.add(childID1);
		} catch (Exception e){
			if (expectedFailure == false) {
				Assert.fail("createMTXFeatures failed with exception:\n" +e.getLocalizedMessage());
			}
		}
		return ids;
	}
	
	
	public ArrayList<String> createCRMTXConfigurationRules(boolean asAdmin, boolean expectedFailure){
		System.out.println("Running createCRMTXConfigurationRules");
		ArrayList<String> ids = new ArrayList<String>();
		try {
			String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = featureApi.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
				
			String configMTX = FileUtils.fileToString(config + "configuration_feature-mutual.txt", "UTF-8", false);
			String mtxID = featureApi.addFeature(seasonID, configMTX.toString(), parentID, sessionToken);
			
			String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			featureApi.addFeature(seasonID, jsonCR.toString(), mtxID, sessionToken);

			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = featureApi.addFeature(seasonID, jsonCR.toString(), mtxID, sessionToken);
			
			Assert.assertEquals(configID1.contains("SecurityPolicyException") || configID1.contains("Only a user with the Administrator"), expectedFailure, "createFeature failed: " + configID1);
			
			if(!asAdmin) {
				jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));				
				configID1 = featureApi.addFeature(seasonID, jsonCR.toString(), mtxID, adminToken);
			}
			
			ids.add(parentID);
			ids.add(configID1);
		} catch (Exception e){
			if (expectedFailure == false) {
				Assert.fail("createCRMTXConfigurationRules failed with exception:\n" +e.getLocalizedMessage());
			}
		}
		return ids;

	}
	
	public void updateFeature(String featureID, boolean expectedFailure){
		System.out.println("Running update feature");
		try {
			String feature = featureApi.getFeature(featureID, sessionToken);
			JSONObject json = new JSONObject(feature);
			//update description
			json.put("description", "new feature descpription");
			String response = featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("Only a user with the Administrator"), expectedFailure, "updateFeature update description failed: " + response);
			
			feature = featureApi.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			json.put("minAppVersion", "1.2");
			response = featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("SecurityPolicyException")|| response.contains("Only a user with the Administrator"), expectedFailure, "updateFeature update minAppVersion failed: " + response);
			
			//name of production feature can't be changed including by Admin/ProdLead
			feature = featureApi.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			json.put("name", json.getString("name") + "123");
			response = featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error") || response.contains("SecurityPolicyException"), "updateFeature update name failed: " + response);
			
			feature = featureApi.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			response = featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
			Assert.assertEquals(response.contains("SecurityPolicyException")|| response.contains("Only a user with the Administrator"), expectedFailure, "updateFeature update stage to development failed: " + response);
			
			feature = featureApi.getFeature(featureID, sessionToken);
			json = new JSONObject(feature);
			if (json.getString("stage").equals("DEVELOPMENT")) {
				json.put("stage", "PRODUCTION");
				response = featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
				Assert.assertEquals(response.contains("SecurityPolicyException")|| response.contains("Only a user with the Administrator"), expectedFailure, "updateFeature update stage to production failed: " + response);
			}	

			
		}catch (Exception e){
			if (expectedFailure == false) {
				Assert.fail("updateFeature failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
		
		public void checkoutFeature(String featureID, boolean asAdmin, boolean expectedFailure){
			System.out.println("Running checkoutFeature");
			try {
				String response = branchesRestApi.checkoutFeature(branchID, featureID, sessionToken);
				Assert.assertEquals(response.contains("SecurityPolicyException")|| response.contains("Only a user with the Administrator"), expectedFailure, "checkout feature to branch failed: " + response);

				if(!asAdmin) {
					branchesRestApi.checkoutFeature(branchID, featureID, adminToken);
				}
				
			}catch (Exception e){
				if (expectedFailure == false) {
					Assert.fail("checkoutFeature failed with exception:\n" +e.getLocalizedMessage());
				}
			}
		}
		
		public void cancelCheckoutFeature(String featureID, boolean expectedFailure){
			System.out.println("Running cancelCheckoutFeature");
			try {
				
				String response = branchesRestApi.cancelCheckoutFeature(branchID, featureID, sessionToken);
				Assert.assertEquals(response.contains("SecurityPolicyException")|| response.contains("Only a user with the Administrator"), expectedFailure, "cancel checkout failed: " + response);

				
			}catch (Exception e){
				if (expectedFailure == false) {
					Assert.fail("checkoutFeature failed with exception:\n" +e.getLocalizedMessage());
				}
			}
		}
		
		
		
		public void deleteFeature(String featureID, boolean expectedFailure){
			System.out.println("Running deleteFeature");
			try {
				
				int response = featureApi.deleteFeature(featureID, sessionToken);
				Assert.assertEquals(response != 200, expectedFailure, "deleteFeature failed: code " + response);
				
			} catch (Exception e){
				if (expectedFailure == false) {
					Assert.fail("createFeature failed with exception:\n" +e.getLocalizedMessage());
				}
			}

		}
		
		
	    @AfterTest
	    private void reset(){
	        baseUtils.reset(productID, sessionToken);
	    }

}
