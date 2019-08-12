package tests.restapi.scenarios.prod_under_dev;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureInDevelopmentStageUpdateInBranch {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String branchID;
	private BranchesRestApi br;
	private ExperimentsRestApi exp;
	

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		if (sToken != null)
			sessionToken = sToken;
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		br = new BranchesRestApi();
		br.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
		 Assert.assertFalse(branchID.contains("error"), "Can't create branch: " + branchID );
		 
		 
			String experimentID = baseUtils.addExperiment(analyticsUrl, true, false);	//in production
			Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

			String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
			JSONObject variantJson = new JSONObject(variant);
			variantJson.put("branchName", "branch1");
			variantJson.put("stage", "PRODUCTION");
			String variantID = exp.createVariant(experimentID, variantJson.toString(), sessionToken);
			Assert.assertFalse(variantID.contains("error"), "Experiment was not created: " + variantID);

	}


	@Test (description = "Update parent feature to production")
	public void testUpdateToProduction() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			String parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			
			//Create a child feature
			String child = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			child = JSONUtils.generateUniqueString(child, 8, "name");
			String childID = f.addFeature(seasonID, child, parentID, sessionToken);
			
			//checkout features to branch
			br.checkoutFeature(branchID, parentID, sessionToken);
			br.checkoutFeature(branchID, childID, sessionToken);
			
			//Update parent to production in branch
			parent = f.getFeatureFromBranch(parentID, branchID, sessionToken);
			
			JSONObject json = new JSONObject(parent);
			json.put("stage", "PRODUCTION");
			String response = f.updateFeatureInBranch(seasonID, branchID, parentID, json.toString(), sessionToken);
			 Assert.assertFalse(response.contains("error"), "Can't update feature stage: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	
	@Test (description = "Update parent feature to production")
	public void testUpdateToProductionMTX() throws JSONException{
		try {			

			String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
			JSONObject json = new JSONObject(feature);
			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));			
			String parentID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			
			String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			String mtxID = f.addFeature(seasonID, featureMix, parentID, sessionToken);
			Assert.assertFalse(mtxID.contains("error"), "Feature was not added to the season: " + mtxID);


			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID1 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID1.contains("error"), "Feature was not created under development feature: " + childID1);

			json.put("stage", "DEVELOPMENT");
			json.put("name", RandomStringUtils.randomAlphabetic(5));
			String childID2 = f.addFeature(seasonID, json.toString(), mtxID, sessionToken);
			Assert.assertFalse(childID2.contains("error"), "Feature was not created under development feature: " + childID2);
			
			//checkout features to branch
			br.checkoutFeature(branchID, childID1, sessionToken);
			
			//Update parent to production in branch
			String parent = f.getFeatureFromBranch(parentID, branchID, sessionToken);
			
			JSONObject jsonParent = new JSONObject(parent);
			jsonParent.put("stage", "PRODUCTION");
			String response = f.updateFeatureInBranch(seasonID, branchID, parentID, jsonParent.toString(), sessionToken);
			 Assert.assertFalse(response.contains("error"), "Can't update feature stage: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}




	//@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
