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

public class CheckoutFeatureTypes {
	protected String productID;
	protected String seasonID;
	private String branchID;
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
				
1. Only feature can be checked out (not mtx, config ...)
2. verify that when checking out a feature - all of its configurations are checked out
3. Verify that all features till the root are checked out 
4. Verify that if there is mtx on the path to root, all the other children of the mtx are checked out as well and in the correct order
5. Check the checkout of a feature that has mtx witin mtx on the path to root 
a feature can only be checked out once (cannot checkout a checked out feature) 

 */
		
	@Test (description="Add components")
	public void addComponents() throws IOException, JSONException{
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchID =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		String featureID1 = f.addFeature(seasonID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		String featureID2 = f.addFeature(seasonID, feature2, mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		String featureID3 = f.addFeature(seasonID, feature3, mixID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season");

		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID = f.addFeature(seasonID, configurationMix, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR1");
		String configID1 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Feature was not added to the season");
				
		jsonCR.put("name", "CR2");
		String configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR3");
		String configID3 = f.addFeature(seasonID, jsonCR.toString(),featureID3, sessionToken);
		Assert.assertFalse(configID3.contains("error"), "Feature was not added to the season");

		jsonCR.put("name", "CR4");
		String configID4 = f.addFeature(seasonID, jsonCR.toString(),configID3, sessionToken);
		Assert.assertFalse(configID4.contains("error"), "Feature was not added to the season");
		
		//regular feature
		String response = br.checkoutFeature(branchID, featureID1, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature type was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect feature branchStatus ");
		String res = br.cancelCheckoutFeature(branchID, featureID1, sessionToken);
		Assert.assertTrue(res.equals("{}"), "Feature type was not unchecked: " + response);
				
		//mix feature
		response = br.checkoutFeature(branchID, mixID1, sessionToken); 
		Assert.assertFalse(response.contains("error"), "MIX feature type was not checked out: " + response);
		//check that its children were not checked out
		JSONObject mix = new JSONObject(f.getFeatureFromBranch(mixID1, branchID, sessionToken));
		Assert.assertTrue(mix.getJSONArray("features").getJSONObject(0).getString("branchStatus").equals("NONE"), "Check out of MIX also checked out its child");
		Assert.assertTrue(mix.getJSONArray("features").getJSONObject(1).getString("branchStatus").equals("NONE"), "Check out of MIX also checked out its child");		
		res = br.cancelCheckoutFeature(branchID, mixID1, sessionToken); 
		Assert.assertFalse(res.contains("not checked"), "MIX feature type was not unchecked out: " + response);
		
		//sub-feature with configurations
		response = br.checkoutFeature(branchID, featureID3, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Feature type was not checked out: " + response);
		Assert.assertTrue(getCheckoutStatus(response).equals("CHECKED_OUT"), "Incorrect feature branchStatus ");
		res = br.cancelCheckoutFeature(branchID, featureID3, sessionToken);
		Assert.assertTrue(res.equals("{}"), "Feature was not unchecked out: " + response);
				
		//mix of configuraton rules
		response = br.checkoutFeature(branchID, mixConfigID, sessionToken);
		Assert.assertTrue(response.contains("error"), "MIX of configurations type was checked out: " + response);
		res = br.cancelCheckoutFeature(branchID, mixConfigID, sessionToken);
		Assert.assertFalse(res.equals("{}"), "MIX of configurations was unchecked out: " + response);
		
		//configuraton rule
		response = br.checkoutFeature(branchID, configID1, sessionToken); 
		Assert.assertTrue(response.contains("error"), "Configuration rule type was checked out: " + response);
		res = br.cancelCheckoutFeature(branchID, configID1, sessionToken);
		Assert.assertFalse(res.equals("{}"), "Configuration rule type was unchecked out: " + response);
		
		//root
		String rootId = f.getRootId(seasonID, sessionToken);
		response = br.checkoutFeature(branchID, rootId, sessionToken); 
		Assert.assertFalse(response.contains("error"), "Root type was not checked out: " + response);
		res = br.cancelCheckoutFeature(branchID, rootId, sessionToken); 
		Assert.assertTrue(res.equals("{}"), "Root was not unchecked out: " + response);

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
