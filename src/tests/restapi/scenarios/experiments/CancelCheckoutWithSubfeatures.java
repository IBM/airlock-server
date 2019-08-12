package tests.restapi.scenarios.experiments;

import java.io.IOException;






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


public class CancelCheckoutWithSubfeatures {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String mixID1;
	private String mixConfigID;
	private String configID1;
	private String configID2;
	private String configID3;
	private String configID4;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/*
	F1 -> MIX	->F2 -> MIXCR ->CR1, CR2
				->F3 -> CR3 -> CR4
				
	- checkout f2 - mix and f1 are also checked out
	- cancel checkout mix with subfeatures
	
	- checkout f2 - mix and f1 are also checked out
	- cancel checkout f1
	
	- checkout f2 and checkout root
	- cancel checkout root with subfeatures

 */
		
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchID =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID3 = f.addFeature(seasonID, feature3, mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeature(seasonID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		configID1 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		configID3 = f.addFeature(seasonID, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		configID4 = f.addFeature(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
	}
	
	@Test (dependsOnMethods = "addComponents", description="Cancel checkout of MIX")
	public void uncheckMix() throws IOException, JSONException{
		//checkout F2, it also checks out F1 & mix
		String response = br.checkoutFeature(branchID, featureID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature type was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect feature branchStatus ");
		
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, mixID1, sessionToken);
		Assert.assertTrue(res.equals("{}"), "MIX was not unchecked: " + response);
		
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(feature2.getString("branchStatus").equals("NONE"), "Feature2 status is not NONE");
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(feature3.getString("branchStatus").equals("NONE"), "Feature3 status is not NONE");
		JSONObject configmix = new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		JSONObject config1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		JSONObject config2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		JSONObject config3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		JSONObject config4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	
	}
	
	@Test (dependsOnMethods = "uncheckMix", description="Cancel checkout of parent feature F1")
	public void uncheckParentFeature() throws IOException, JSONException{
		//checkout F2, it also checks out F1 & mix
		String response = br.checkoutFeature(branchID, featureID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature1 was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect feature branchStatus ");
		
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, featureID1, sessionToken);
		Assert.assertTrue(res.equals("{}"), "Feature1 was not unchecked: " + response);
		
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "MTX status is not NONE");
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(feature2.getString("branchStatus").equals("NONE"), "Feature2 status is not NONE");
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(feature3.getString("branchStatus").equals("NONE"), "Feature3 status is not NONE");
		JSONObject configmix = new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		JSONObject config1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		JSONObject config2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		JSONObject config3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		JSONObject config4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	
	}
	
	@Test (dependsOnMethods = "uncheckParentFeature", description="Cancel checkout of root")
	public void uncheckRoot() throws IOException, JSONException{
		//checkout F2, it also checks out F1 & mix
		String response = br.checkoutFeature(branchID, featureID2, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature1 was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect feature branchStatus ");
		
		//checkout root
		String rootID = f.getRootId(seasonID, sessionToken);
		response = br.checkoutFeature(branchID, rootID, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Root was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect root branchStatus ");

		
		String res = br.cancelCheckoutFeatureWithSubFeatures(branchID, rootID, sessionToken);
		Assert.assertTrue(res.equals("{}"), "Root was not unchecked: " + response);
		
		JSONObject f1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		Assert.assertTrue(f1.getString("branchStatus").equals("NONE"), "Feature1 status is not NONE");
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getString("branchStatus").equals("NONE"), "MTX status is not NONE");
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		Assert.assertTrue(feature2.getString("branchStatus").equals("NONE"), "Feature2 status is not NONE");
		JSONObject feature3 = new JSONObject(f.getFeatureFromBranch(featureID3, branchID, sessionToken));
		Assert.assertTrue(feature3.getString("branchStatus").equals("NONE"), "Feature3 status is not NONE");
		JSONObject configmix = new JSONObject(f.getFeatureFromBranch(mixConfigID, branchID, sessionToken));
		Assert.assertTrue(configmix.getString("branchStatus").equals("NONE"), "Configuration mtx status is not NONE");
		JSONObject config1 = new JSONObject(f.getFeatureFromBranch(configID1, branchID, sessionToken));
		Assert.assertTrue(config1.getString("branchStatus").equals("NONE"), "Configuration1  status is not NONE");
		JSONObject config2 = new JSONObject(f.getFeatureFromBranch(configID2, branchID, sessionToken));
		Assert.assertTrue(config2.getString("branchStatus").equals("NONE"), "Configuration2  status is not NONE");
		JSONObject config3 = new JSONObject(f.getFeatureFromBranch(configID3, branchID, sessionToken));
		Assert.assertTrue(config3.getString("branchStatus").equals("NONE"), "Configuration3  status is not NONE");
		JSONObject config4 = new JSONObject(f.getFeatureFromBranch(configID4, branchID, sessionToken));
		Assert.assertTrue(config4.getString("branchStatus").equals("NONE"), "Configuration4  status is not NONE");
	
	}
	
	private String getCheckoutStatus(String id) throws JSONException{
		String feature = f.getFeatureFromBranch(id, branchID, sessionToken);
		JSONObject json = new JSONObject(feature);
		return json.getString("branchStatus");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
