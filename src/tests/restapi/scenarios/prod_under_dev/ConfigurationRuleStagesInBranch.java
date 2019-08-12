package tests.restapi.scenarios.prod_under_dev;

import java.io.IOException;

















import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;

public class ConfigurationRuleStagesInBranch {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String m_url;
	private BranchesRestApi br ;
	private String branchID;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		br = new BranchesRestApi();
		br.setURL(m_url);
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

/*
 *
- 	checkout dev feature and add it a CR in prod
- 	Checkout CR in dev and add it CR in prod
- 	Checkout CR in prod (with subCR) in prod and move it to dev
 */

	@Test (description = "checkout dev feature and add it a CR in prod")
	public void test1() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, json.toString(), "ROOT", sessionToken);
			
			br.checkoutFeature(branchID, parentID, sessionToken);

			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID.contains("error"), "Feature was not created: " + configID);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			//Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");	
	}

	
	@Test (description = "Checkout CR in dev and add it CR in prod")
	public void test2() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, json.toString(), "ROOT", sessionToken);
			
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "DEVELOPMENT");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			
			br.checkoutFeature(branchID, parentID, sessionToken);

			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), configID1, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			//Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
	
	}
	
	@Test (description = "Checkout CR in prod (with subCR) in prod and move it to dev")
	public void test3() throws JSONException, IOException, InterruptedException{
			String dateFormat = f.setDateFormat();
			
			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeatureToBranch(seasonID, BranchesRestApi.MASTER, json.toString(), "ROOT", sessionToken);
			
			
			String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
			JSONObject jsonCR = new JSONObject(configuration);
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID1 = f.addFeature(seasonID, jsonCR.toString(), parentID, sessionToken);
			Assert.assertFalse(configID1.contains("error"), "Feature was not created: " + configID1);
			
			jsonCR.put("stage", "PRODUCTION");
			jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
			String configID2 = f.addFeature(seasonID, jsonCR.toString(), configID1, sessionToken);
			Assert.assertFalse(configID2.contains("error"), "Feature was not created: " + configID2);

			
			br.checkoutFeature(branchID, parentID, sessionToken);

			String cr1 = f.getFeatureFromBranch(configID1, branchID, sessionToken);
			JSONObject jsonCR1 = new JSONObject(cr1);
			jsonCR1.put("stage", "DEVELOPMENT");
			String response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonCR1.toString(), sessionToken);
			Assert.assertFalse(response.contains("error"), "Feature was not updated: " + response );

			
			f.setSleep();
			RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
			RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
			Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
			//Assert.assertTrue(validateProductionFeatures(responseProd.message, parentID)==0, "Incorrect number of production features");
	
	}
	
	private int validateProductionFeatures(String input, String featureID) throws JSONException{
		int totalFeatures = 0;
		Integer[] forCount = new Integer[1];
		forCount[0]=0;
		JSONObject runtime = new JSONObject(input);
		JSONArray features = runtime.getJSONArray("features");

		if (features.size()==0)
			return 0;
		else {
			for (int i=0; i< features.size(); i++){
				JSONObject singleFeature = features.getJSONObject(i);
				if (singleFeature.getString("uniqueId").equals(featureID)){
					if (singleFeature.getString("stage").equals("PRODUCTION")){
						forCount[0]++;
					}
					
					totalFeatures = countFeatures(singleFeature, forCount);
				}
			}
		}
		
		return totalFeatures;
	}
	
	private int countFeatures(JSONObject parentFeature, Integer[] forCount) throws JSONException{
		if (parentFeature.getJSONArray("features").size() != 0){
			if (parentFeature.getJSONArray("features").getJSONObject(0).getString("stage").equals("PRODUCTION")){
				forCount[0]++;
			}
			countFeatures(parentFeature.getJSONArray("features").getJSONObject(0), forCount);
		} 
		return forCount[0];
	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
