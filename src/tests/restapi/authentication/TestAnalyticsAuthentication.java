package tests.restapi.authentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.com.ibm.qautils.RestClientUtils.RestCallResults;
import tests.restapi.AirlockUtils;
import tests.restapi.ExperimentAnalyticsApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class TestAnalyticsAuthentication {
	protected String productID;
	protected String seasonID;
	protected String experimentID;
	protected String analyticsExperimentID;
	protected String experimentID4Test;
	protected ProductsRestApi productApi;
	protected SeasonsRestApi seasonApi;
	protected ExperimentsRestApi experimentsRestApi;
	private ExperimentAnalyticsApi expAnalyticsApi;
	protected String sessionToken;
	protected String adminToken;
	protected String m_url;
	private String m_analyticsServerUrl;
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
	
	@BeforeClass
	@Parameters({"url","translationsUrl","analyticsUrl","testServicesUrl","analyticsServerUrl","configPath", "operationsUrl","admin","productLead","editor","translator","viewer","adminPass","productLeadPass","editorPass","translatorPass","viewerPass","appName","ssoConfigPath","stage", "expectedServerVersion", "productsToDeleteFile", "runRoles"})
	public void init(String url,String t_url,String a_url,String ts_url, String analyticsServerUrl, String configPath, String c_operationsUrl,String admin,String productLead, String editor,String translator,String viewer,String adminPass,String productleadPass,String editorPass,String translatorPass,String viewerPass,@Optional String appName,@Optional String ssoConfigPath,@Optional String stage, @Optional String expectedServerVersion, String productsToDeleteFile, String runRoles) throws IOException{
		m_url = url;		
		m_analyticsServerUrl = analyticsServerUrl;
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
		experimentsRestApi = new ExperimentsRestApi();
		expAnalyticsApi = new ExperimentAnalyticsApi();
		productApi.setURL(url);
		seasonApi.setURL(url);
		experimentsRestApi.setURL(analyticsUrl);
		expAnalyticsApi.setURL(m_analyticsServerUrl);
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

			String season = FileUtils.fileToString(config + "season1.txt", "UTF-8", false);
			seasonID = seasonApi.addSeason(productID, season, adminToken);

			
			String experiment = FileUtils.fileToString(config + "experiments/experiment1.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experiment);
			expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
			expJson.put("enabled", "false");
			experimentID = experimentsRestApi.createExperiment(productID,expJson.toString(),adminToken);
			Assert.assertFalse(experimentID.contains("SecurityPolicyException") || experimentID.contains("error"), "createExperiment failed: " + experimentID);

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
			testViewer();
		}
		
		if (rolesToRun.contains("admin"))
			testAdmin();
		
		if (rolesToRun.contains("productLead"))
			testProductLead();
		
		if (rolesToRun.contains("editor"))
			testEditor();		
		
		if (rolesToRun.contains("viewer"))
			testViewer();
	}
	
	
	public void testAdmin(){
		System.out.println("Starting role: admin");
		sessionToken = adminToken;
		healthcheck(false);
		addExperiment(false);
		getExperiment(false);
		updateExperiment(false);
		reindexExperiment(false);
		resetExperimentDashboard(false);
		getStatus(false);
		getConfiguration(false);
		setConfiguration(false);
		getExperiments(false);
		deleteExperiment(false);
	}
	
	public void testProductLead(){
		System.out.println("Starting role: productLead");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(productLeadUser,productLeadPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(productLeadUser,productLeadPassword,m_appName);
		}
		healthcheck(false);
		addExperiment(true);
		getExperiment(false);
		updateExperiment(true);
		reindexExperiment(true);
		resetExperimentDashboard(true);
		getStatus(false);
		getConfiguration(true);
		setConfiguration(true);
		getExperiments(false);
		deleteExperiment(true);
	}
	
	public void testEditor(){
		System.out.println("Starting role: editor");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(editorUser,editorPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(editorUser,editorPassword,m_appName);
		}
		healthcheck(false);
		addExperiment(true);
		getExperiment(false);
		updateExperiment(true);
		reindexExperiment(true);
		resetExperimentDashboard(true);
		getStatus(false);
		getConfiguration(true);
		setConfiguration(true);
		getExperiments(false);
		deleteExperiment(true);
	}
	
	public void testViewer(){
		System.out.println("Starting role: viewer");
		if(m_ssoConfigPath != null && m_stage != null){
			sessionToken = baseUtils.setNewJWTTokenUsingBluemix(viewerUser,viewerPassword,m_ssoConfigPath,m_stage);
		} else {
			sessionToken = baseUtils.setNewJWTToken(viewerUser, viewerPassword, m_appName);
		}
		healthcheck(false);
		addExperiment(true);
		getExperiment(false);
		updateExperiment(true);
		reindexExperiment(true);
		resetExperimentDashboard(true);
		getStatus(false);
		getConfiguration(true);
		setConfiguration(true);
		getExperiments(false);
		deleteExperiment(true);
	}
	
	//only admin
	public void addExperiment(boolean expectedFailure){
		System.out.println("Running: addExperiment");
		try {
			String experiment = FileUtils.fileToString(config + "experiments/experimentForAnalyticsServer.txt", "UTF-8", false);
			JSONObject json = new JSONObject(experiment);
			json.put("name", "experiment."+RandomStringUtils.randomAlphabetic(3));
	        analyticsExperimentID = expAnalyticsApi.addExperiment(json.getString("experimentId"), json.toString(), sessionToken);
	        Assert.assertEquals(analyticsExperimentID.contains("SecurityPolicyException") || analyticsExperimentID.contains("User does not have permission"), expectedFailure, "addExperiment failed: " + analyticsExperimentID);
			if(expectedFailure){
				experimentID4Test = experimentID;
			} else {
				experimentID4Test = analyticsExperimentID;
			}
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("addExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//only admin
	public void deleteExperiment(boolean expectedFailure){
		System.out.println("Running: deleteExperiment");
		try {
	        int respCode = expAnalyticsApi.deleteExperiment(experimentID4Test, sessionToken);
	        Assert.assertEquals(respCode != 200, expectedFailure, "deleteExperiment failed: code " + respCode);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("deleteExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//only admin
	public void updateExperiment(boolean expectedFailure){
		System.out.println("Running: updateExperiment");
		
		try {
			String airlockExperiment = expAnalyticsApi.getExperiment(experimentID4Test, sessionToken);
			JSONObject expJson = new JSONObject(airlockExperiment);
			expJson.put("enabled", true);
			RestCallResults response = expAnalyticsApi.updateExperiment(experimentID4Test, expJson.toString(), sessionToken);
			Assert.assertEquals(response.message.contains("SecurityPolicyException")  || response.message.contains("User does not have permission"), expectedFailure, "updateExperiment failed: " + response.message);

		} catch (Exception e) {
			if(expectedFailure == false){
				Assert.fail("updateExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}

	}
	
	//only admin
	public void reindexExperiment(boolean expectedFailure){
		System.out.println("Running: reindexExperiment");
		try {
	        String response = expAnalyticsApi.reindexExperiment(experimentID4Test, sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "reindexExperiment failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("reindexExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//only admin
	public void resetExperimentDashboard(boolean expectedFailure){
		System.out.println("Running: resetExperimentDashboard");
		try {
	        String response = expAnalyticsApi.resetExperimentDashboard(experimentID4Test, sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "resetExperimentDashboard failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("resetExperimentDashboard failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//only admin
	public void getConfiguration(boolean expectedFailure){
		System.out.println("Running: getConfiguration");
		try {
	        String response = expAnalyticsApi.getConfiguration(sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "getConfiguration failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getConfiguration failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	//only admin
	public void setConfiguration(boolean expectedFailure){
		System.out.println("Running: setConfiguration");
		try {
	        String configuration = expAnalyticsApi.getConfiguration(sessionToken);
	        String response = expAnalyticsApi.setConfiguration(configuration, sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "setConfiguration failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("setConfiguration failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	
	
	
	public void getStatus(boolean expectedFailure){
		System.out.println("Running: getStatus");
		try {
	        
	        String response = expAnalyticsApi.getStatus(sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "getStatus failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getStatus failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getExperiments(boolean expectedFailure){
		System.out.println("Running: getExperiments");
		try {
	        
	        String response = expAnalyticsApi.getAllExperiments(sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "getAllExperiments failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getAllExperiments failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void getExperiment(boolean expectedFailure){
		System.out.println("Running: getExperiment");
		try {
	        
	        String response = expAnalyticsApi.getExperiment(experimentID4Test, sessionToken);
	        Assert.assertEquals(response.contains("SecurityPolicyException") || response.contains("User does not have permission"), expectedFailure, "getExperiment failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("getExperiment failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}
	
	public void healthcheck(boolean expectedFailure){
		System.out.println("Running: healthcheck");
		try {
	        
	        int response = expAnalyticsApi.healthcheck();
	        Assert.assertEquals(response != 200, expectedFailure, "healthcheck failed: " + response);
		}catch (Exception e){
			if(expectedFailure == false){
				Assert.fail("healthcheck failed with exception:\n" +e.getLocalizedMessage());
			}
		}
	}

	
}
