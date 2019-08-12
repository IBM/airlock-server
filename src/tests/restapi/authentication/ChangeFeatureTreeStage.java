package tests.restapi.authentication;

import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ChangeFeatureTreeStage {

		protected String seasonID;
		protected String productID;
		protected String branchID;
		protected String branchInExperimentID;
		protected FeaturesRestApi featureApi;
		protected ProductsRestApi productApi;
		protected SeasonsRestApi seasonApi;
		protected AnalyticsRestApi analyticsApi;
		protected ExperimentsRestApi experimentsRestApi;
		protected BranchesRestApi branchesRestApi;
		protected AirlockUtils baseUtils;
		protected String sessionToken;
		protected String adminToken;
		protected String m_url;
		protected String analyticsUrl;
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
		protected String config;
		protected JSONObject feature = new JSONObject();
		protected JSONObject configuration = new JSONObject();

		@BeforeClass
		@Parameters({"url","translationsUrl","analyticsUrl","testServicesUrl","configPath", "operationsUrl","admin","productLead","editor","translator","viewer","adminPass","productLeadPass","editorPass","translatorPass","viewerPass","appName", "expectedServerVersion", "sessionToken", "productsToDeleteFile"})
		public void init(String url,String t_url,String a_url,String ts_url, String configPath, String c_operationsUrl,String admin,String productLead, String editor,String translator,String viewer,String adminPass,String productleadPass,String editorPass,String translatorPass,String viewerPass,@Optional String appName, @Optional String expectedServerVersion, String sToken, String productsToDelete) throws IOException, JSONException{
			baseUtils = new AirlockUtils(url, analyticsUrl, t_url, configPath, sToken, admin, adminPass, appName, productsToDelete);
			m_url = url;
			analyticsUrl = a_url;
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
			productApi = new ProductsRestApi();
			seasonApi = new SeasonsRestApi();
			featureApi = new FeaturesRestApi();
			analyticsApi = new AnalyticsRestApi();
			experimentsRestApi = new ExperimentsRestApi();
			branchesRestApi = new BranchesRestApi();
			productApi.setURL(m_url);
			seasonApi.setURL(m_url);
			featureApi.setURL(m_url);
			branchesRestApi.setURL(m_url);
			analyticsApi.setURL(analyticsUrl);
			experimentsRestApi.setURL(analyticsUrl);
			if(appName != null){
				m_appName = appName;
			}
			adminToken = baseUtils.getJWTToken(adminUser,adminPassword,m_appName);
			
			try {
				feature = new JSONObject(FileUtils.fileToString(config + "feature1.txt", "UTF-8", false));
				configuration = new JSONObject(FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false));
			} catch (JSONException e) {
				System.out.println("Can't read input feature1 file " + e.getLocalizedMessage());
			}
			
			productID = createProduct();
			String season = "{\"minVersion\":\"1.5\"}";
			seasonID = seasonApi.addSeason(productID, season, adminToken);
			String branch = FileUtils.fileToString(config + "experiments/branch1.txt", "UTF-8", false);
			branchID = branchesRestApi.createBranch(seasonID,branch,BranchesRestApi.MASTER,adminToken);
			branch = FileUtils.fileToString(config + "experiments/branch2.txt", "UTF-8", false);
			branchInExperimentID = branchesRestApi.createBranch(seasonID,branch,BranchesRestApi.MASTER,adminToken);
			createExperiment();

			
		}

		@AfterTest
		public void deleteData(){
			try {
				productApi.deleteProduct(productID, adminToken);
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		
		@Test
		public void testAdmin() throws IOException, JSONException{
			sessionToken = adminToken;
			//stand alone branch
			featureWithNewSubFeatures(false, branchID);
			featureWithNewMTXSubFeatures(false, branchID);
			featureWithNewMTXConfigurationRules(false, branchID);
			featureWithNewConfigurationRules(false, branchID);
			subFeatureWithNewSubFeatures(false, branchID);
			subFfeatureWithNewMTXSubFeatures(false, branchID);
			subFeatureWithNewMTXConfigurationRules(false, branchID);
			subFeatureWithNewConfigurationRules(false, branchID);
			
			//experiment in development stage
			featureWithNewSubFeatures(false, branchInExperimentID);
			featureWithNewMTXSubFeatures(false, branchInExperimentID);
			featureWithNewMTXConfigurationRules(false, branchInExperimentID);
			featureWithNewConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewSubFeatures(false, branchInExperimentID);
			subFfeatureWithNewMTXSubFeatures(false, branchInExperimentID);
			subFeatureWithNewMTXConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewConfigurationRules(false, branchInExperimentID);
			
			//in master
			featureWithNewSubFeatures(false, BranchesRestApi.MASTER);
			featureWithNewMTXSubFeatures(false, BranchesRestApi.MASTER);
			featureWithNewMTXConfigurationRules(false, BranchesRestApi.MASTER);
			featureWithNewConfigurationRules(false, BranchesRestApi.MASTER);
			subFeatureWithNewSubFeatures(false, BranchesRestApi.MASTER);
			subFfeatureWithNewMTXSubFeatures(false, BranchesRestApi.MASTER);
			subFeatureWithNewMTXConfigurationRules(false, BranchesRestApi.MASTER);
			subFeatureWithNewConfigurationRules(false, BranchesRestApi.MASTER);
		}
		
		@Test(dependsOnMethods="testAdmin")
		public void testProductLead() throws IOException, JSONException{
			sessionToken = baseUtils.getJWTToken(productLeadUser,productLeadPassword,m_appName);
			featureWithNewSubFeatures(false, branchID);
			featureWithNewMTXSubFeatures(false, branchID);
			featureWithNewMTXConfigurationRules(false, branchID);
			featureWithNewConfigurationRules(false, branchID);
			subFeatureWithNewSubFeatures(false, branchID);
			subFfeatureWithNewMTXSubFeatures(false, branchID);
			subFeatureWithNewMTXConfigurationRules(false, branchID);
			subFeatureWithNewConfigurationRules(false, branchID);
			
			//experiment in development stage
			featureWithNewSubFeatures(false, branchInExperimentID);
			featureWithNewMTXSubFeatures(false, branchInExperimentID);
			featureWithNewMTXConfigurationRules(false, branchInExperimentID);
			featureWithNewConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewSubFeatures(false, branchInExperimentID);
			subFfeatureWithNewMTXSubFeatures(false, branchInExperimentID);
			subFeatureWithNewMTXConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewConfigurationRules(false, branchInExperimentID);
			
			//in master
			featureWithNewSubFeatures(false, BranchesRestApi.MASTER);
			featureWithNewMTXSubFeatures(false, BranchesRestApi.MASTER);
			featureWithNewMTXConfigurationRules(false, BranchesRestApi.MASTER);
			featureWithNewConfigurationRules(false, BranchesRestApi.MASTER);
			subFeatureWithNewSubFeatures(false, BranchesRestApi.MASTER);
			subFfeatureWithNewMTXSubFeatures(false, BranchesRestApi.MASTER);
			subFeatureWithNewMTXConfigurationRules(false, BranchesRestApi.MASTER);
			subFeatureWithNewConfigurationRules(false, BranchesRestApi.MASTER);
		}
		
		@Test(dependsOnMethods="testProductLead")
		public void testEditor() throws IOException, JSONException{
			sessionToken = baseUtils.getJWTToken(editorUser,editorPassword,m_appName);
			featureWithNewSubFeatures(false, branchID);
			featureWithNewMTXSubFeatures(false, branchID);
			featureWithNewMTXConfigurationRules(false, branchID);
			featureWithNewConfigurationRules(false, branchID);
			subFeatureWithNewSubFeatures(false, branchID);
			subFfeatureWithNewMTXSubFeatures(false, branchID);
			subFeatureWithNewMTXConfigurationRules(false, branchID);
			subFeatureWithNewConfigurationRules(false, branchID);
			
			//experiment in development stage
			featureWithNewSubFeatures(false, branchInExperimentID);
			featureWithNewMTXSubFeatures(false, branchInExperimentID);
			featureWithNewMTXConfigurationRules(false, branchInExperimentID);
			featureWithNewConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewSubFeatures(false, branchInExperimentID);
			subFfeatureWithNewMTXSubFeatures(false, branchInExperimentID);
			subFeatureWithNewMTXConfigurationRules(false, branchInExperimentID);
			subFeatureWithNewConfigurationRules(false, branchInExperimentID);
			
			//in master
			featureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			featureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			subFfeatureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			subFeatureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
		}
		
		@Test(dependsOnMethods="testEditor")
		public void testTranslator() throws IOException, JSONException{
			sessionToken = baseUtils.getJWTToken(translatorUser,translatorPassword,m_appName);
			featureWithNewSubFeatures(true, branchID);
			featureWithNewMTXSubFeatures(true, branchID);
			featureWithNewMTXConfigurationRules(true, branchID);
			featureWithNewConfigurationRules(true, branchID);
			subFeatureWithNewSubFeatures(true, branchID);
			subFfeatureWithNewMTXSubFeatures(true, branchID);
			subFeatureWithNewMTXConfigurationRules(true, branchID);
			subFeatureWithNewConfigurationRules(true, branchID);
			
			//experiment in development stage
			featureWithNewSubFeatures(true, branchInExperimentID);
			featureWithNewMTXSubFeatures(true, branchInExperimentID);
			featureWithNewMTXConfigurationRules(true, branchInExperimentID);
			featureWithNewConfigurationRules(true, branchInExperimentID);
			subFeatureWithNewSubFeatures(true, branchInExperimentID);
			subFfeatureWithNewMTXSubFeatures(true, branchInExperimentID);
			subFeatureWithNewMTXConfigurationRules(true, branchInExperimentID);
			subFeatureWithNewConfigurationRules(true, branchInExperimentID);
			
			//in master
			featureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			featureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			subFfeatureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			subFeatureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
			
		}
		
		@Test(dependsOnMethods="testTranslator")
		public void testViewer() throws IOException, JSONException{
			sessionToken = baseUtils.getJWTToken(viewerUser,viewerPassword,m_appName);
			featureWithNewSubFeatures(true, branchID);
			featureWithNewMTXSubFeatures(true, branchID);
			featureWithNewMTXConfigurationRules(true, branchID);
			featureWithNewConfigurationRules(true, branchID);
			subFeatureWithNewSubFeatures(true, branchID);
			subFfeatureWithNewMTXSubFeatures(true, branchID);
			subFeatureWithNewMTXConfigurationRules(true, branchID);
			subFeatureWithNewConfigurationRules(true, branchID);
			
			//experiment in development stage
			featureWithNewSubFeatures(true, branchInExperimentID);
			featureWithNewMTXSubFeatures(true, branchInExperimentID);
			featureWithNewMTXConfigurationRules(true, branchInExperimentID);
			featureWithNewConfigurationRules(true, branchInExperimentID);
			subFeatureWithNewSubFeatures(true, branchInExperimentID);
			subFfeatureWithNewMTXSubFeatures(true, branchInExperimentID);
			subFeatureWithNewMTXConfigurationRules(true, branchInExperimentID);
			subFeatureWithNewConfigurationRules(true, branchInExperimentID);
			
			//in master
			featureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			featureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			featureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewSubFeatures(true, BranchesRestApi.MASTER);
			subFfeatureWithNewMTXSubFeatures(true, BranchesRestApi.MASTER);
			subFeatureWithNewMTXConfigurationRules(true, BranchesRestApi.MASTER);
			subFeatureWithNewConfigurationRules(true, BranchesRestApi.MASTER);
		}
		
		public String createProduct(){
			
			String productID4Test = "";
			try {
				String product = FileUtils.fileToString(config + "product1.txt", "UTF-8", false);
				product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
				product = JSONUtils.generateUniqueString(product, 8, "name");
				productID4Test = productApi.addProduct(product, adminToken);
				Assert.assertFalse(productID4Test.contains("SecurityPolicyException"), "createProduct failed: " + productID4Test);
			}catch (Exception e){
					Assert.fail("createProduct failed with exception");
			}
			return productID4Test;
		}
		
		public void featureWithNewSubFeatures(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			branchesRestApi.checkoutFeature(branchId, parentFeatureID, adminToken);

			String featureID2 = createFeatureInBranch("PRODUCTION", parentFeatureID, branchId);
			createFeatureInBranch("PRODUCTION", featureID2, branchId);
			createFeatureInBranch("DEVELOPMENT", featureID2, branchId);
			createFeatureInBranch("PRODUCTION", featureID2, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response);
 
		}
		
		public void featureWithNewMTXSubFeatures(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			branchesRestApi.checkoutFeature(branchId, parentFeatureID, adminToken);

			String featureMix = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
			String mixGroupID = featureApi.addFeatureToBranch(seasonID, branchId, featureMix, parentFeatureID, adminToken);

			createFeatureInBranch("PRODUCTION", mixGroupID, branchId);
			createFeatureInBranch("DEVELOPMENT", mixGroupID, branchId);
			createFeatureInBranch("PRODUCTION", mixGroupID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		
		public void featureWithNewMTXConfigurationRules(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			branchesRestApi.checkoutFeature(branchId, parentFeatureID, adminToken);

			String configurationMix = FileUtils.fileToString(config + "configuration_feature-mutual.txt", "UTF-8", false);
			String mixConfigID = featureApi.addFeatureToBranch(seasonID, branchId, configurationMix, parentFeatureID, adminToken);
			createConfigurationInBranch("PRODUCTION", mixConfigID, branchId);
			createConfigurationInBranch("DEVELOPMENT", mixConfigID, branchId);
			createConfigurationInBranch("PRODUCTION", mixConfigID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		
		public void featureWithNewConfigurationRules(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			branchesRestApi.checkoutFeature(branchId, parentFeatureID, adminToken);

			String configID = createConfigurationInBranch("PRODUCTION", parentFeatureID, branchId);
			createConfigurationInBranch("PRODUCTION", configID, branchId);
			createConfigurationInBranch("DEVELOPMENT", configID, branchId);
			createConfigurationInBranch("PRODUCTION", configID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		

		public void subFeatureWithNewSubFeatures(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			String subFeatureID = createFeature("PRODUCTION", parentFeatureID);
			branchesRestApi.checkoutFeature(branchId, subFeatureID, adminToken);

			String featureID2 = createFeatureInBranch("PRODUCTION", subFeatureID, branchId);
			createFeatureInBranch("PRODUCTION", featureID2, branchId);
			createFeatureInBranch("DEVELOPMENT", featureID2, branchId);
			createFeatureInBranch("PRODUCTION", featureID2, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response);
 
		}
		
		public void subFfeatureWithNewMTXSubFeatures(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			String subFeatureID = createFeature("PRODUCTION", parentFeatureID);
			branchesRestApi.checkoutFeature(branchId, subFeatureID, adminToken);

			String featureMix = FileUtils.fileToString(config + "feature-mutual.txt", "UTF-8", false);
			String mixGroupID = featureApi.addFeatureToBranch(seasonID, branchId, featureMix, subFeatureID, adminToken);

			createFeatureInBranch("PRODUCTION", mixGroupID, branchId);
			createFeatureInBranch("DEVELOPMENT", mixGroupID, branchId);
			createFeatureInBranch("PRODUCTION", mixGroupID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		
		public void subFeatureWithNewMTXConfigurationRules(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			String subFeatureID = createFeature("PRODUCTION", parentFeatureID);
			branchesRestApi.checkoutFeature(branchId, subFeatureID, adminToken);

			String configurationMix = FileUtils.fileToString(config + "configuration_feature-mutual.txt", "UTF-8", false);
			String mixConfigID = featureApi.addFeatureToBranch(seasonID, branchId, configurationMix, subFeatureID, adminToken);
			createConfigurationInBranch("PRODUCTION", mixConfigID, branchId);
			createConfigurationInBranch("DEVELOPMENT", mixConfigID, branchId);
			createConfigurationInBranch("PRODUCTION", mixConfigID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		
		public void subFeatureWithNewConfigurationRules(boolean expectedFailure, String branchId) throws IOException, JSONException{
			
			String parentFeatureID = createFeature("PRODUCTION", "ROOT");
			String subFeatureID = createFeature("PRODUCTION", parentFeatureID);
			branchesRestApi.checkoutFeature(branchId, subFeatureID, adminToken);

			String configID = createConfigurationInBranch("PRODUCTION", subFeatureID, branchId);
			createConfigurationInBranch("PRODUCTION", configID, branchId);
			createConfigurationInBranch("DEVELOPMENT", configID, branchId);
			createConfigurationInBranch("PRODUCTION", configID, branchId);
			
			String featureTree = featureApi.getFeatureFromBranch(parentFeatureID, branchId, sessionToken );
			featureTree = featureTree.replace("PRODUCTION", "DEVELOPMENT");
			String response = featureApi.updateFeatureInBranch(seasonID, branchId, parentFeatureID, featureTree, sessionToken);
			
			Assert.assertEquals(response.contains("error"), expectedFailure, "Can't updated feature stage in stand alone branch with new sub-features" + response); 
		}
		
		private String createFeature(String featureStage, String parent) throws JSONException, IOException{
			feature.put("name", RandomStringUtils.randomAlphabetic(5));
			feature.put("stage", featureStage);
			return featureApi.addFeature(seasonID, feature.toString(), parent, adminToken);
		}
		
		private String createFeatureInBranch(String featureStage, String parent, String branchId) throws JSONException, IOException{
			feature.put("name", RandomStringUtils.randomAlphabetic(5));
			feature.put("stage", featureStage);
			return featureApi.addFeatureToBranch(seasonID, branchId, feature.toString(), parent, adminToken);
		}
		
		private String createConfigurationInBranch(String featureStage, String parent, String branchId) throws JSONException, IOException{
			feature.put("name", RandomStringUtils.randomAlphabetic(5));
			feature.put("stage", featureStage);
			return featureApi.addFeatureToBranch(seasonID, branchId, feature.toString(), parent, adminToken);
		}
		
		public void createExperiment() throws IOException, JSONException{
			JSONObject experiment = new JSONObject(FileUtils.fileToString(config + "experiments/experiment2.txt", "UTF-8", false));
			experiment.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));			
			String experimentID =  experimentsRestApi.createExperiment(productID, experiment.toString(), adminToken);
			
			String variant = FileUtils.fileToString(config + "experiments/variant2.txt", "UTF-8", false);
			String variantID = experimentsRestApi.createVariant(experimentID, variant, adminToken);
			Assert.assertFalse(variantID.contains("error"), "Failed to create a variant: " + variantID);
		}
}
