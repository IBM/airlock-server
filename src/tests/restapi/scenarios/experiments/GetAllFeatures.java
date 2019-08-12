package tests.restapi.scenarios.experiments;

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
import tests.restapi.SeasonsRestApi;

public class GetAllFeatures {
	protected String productID;
	protected String seasonID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String configID2;
	private String configID3;
	private String mixConfigID;
	private String mixID1;
	private String mixID2;
	private JSONObject fJson;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private FeaturesRestApi f;
	private JSONArray legalIds = new JSONArray();
	private JSONArray illegalIds = new JSONArray();

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		fJson = new JSONObject(feature);

	}
	

	
	@Test (description ="F1 + (MIXCR->C1+C2) -> MIX -> (F2 + MIX -> (F3 + F4) ),  checkout F2 ") 
	public void addSeason () throws Exception {

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeature(seasonID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		legalIds.add(featureID1);
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeature(seasonID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");
		illegalIds.add(mixConfigID);

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", "CR2");
		configID2 = f.addFeature(seasonID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");
		illegalIds.add(configID2);
				
	
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeature(seasonID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		illegalIds.add(mixID1);
				
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID2 = f.addFeature(seasonID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		legalIds.add(featureID2);
		
		featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID2 = f.addFeature(seasonID, featureMix, mixID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);
		illegalIds.add(mixID2);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID3 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
		legalIds.add(featureID3);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID4 = f.addFeature(seasonID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);
		legalIds.add(featureID4);

	}
	
	@Test (dependsOnMethods="addSeason") 
	public void addBranches () throws Exception {
		add("branch1");
		add("branch2");


	}
	
	@Test (dependsOnMethods="addBranches", description="Get all features")
	public void getAllFeatures() throws JSONException, Exception{				

		String response = f.featuresInSeason(seasonID, sessionToken);
		JSONArray list = new JSONObject(response).getJSONArray("features");
		JSONArray ids = new JSONArray();
		for(int i=0; i<list.size(); i++){
			JSONObject feature = list.getJSONObject(i);
			ids.add(feature.getString("uniqueId"));
		}

		Assert.assertEqualsNoOrder(legalIds.toArray(), ids.toArray(), "A list of features contains unexpected items or is missing an item");
	}	


	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private void add(String branchName) throws JSONException, IOException{
		String branchID = addBranch(branchName);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID1 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);
		legalIds.add(featureID1);
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, branchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");
		illegalIds.add(mixConfigID);
		
		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonCR = new JSONObject(configuration1);
		jsonCR.put("name", RandomStringUtils.randomAlphabetic(5));
		configID2 = f.addFeatureToBranch(seasonID, branchID, jsonCR.toString(), mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");
		illegalIds.add(configID2);
						
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID1 = f.addFeatureToBranch(seasonID, branchID, featureMix, featureID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "Feature was not added to the season: " + mixID1);
		illegalIds.add(mixID1);
				
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID2 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);
		legalIds.add(featureID2);
		
		featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		mixID2 = f.addFeatureToBranch(seasonID, branchID, featureMix, mixID1, sessionToken);
		Assert.assertFalse(mixID2.contains("error"), "Feature was not added to the season: " + mixID2);
		illegalIds.add(mixID2);

		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID3 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);
		legalIds.add(featureID3);
		
		fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		featureID4 = f.addFeatureToBranch(seasonID, branchID, fJson.toString(), mixID2, sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);
		legalIds.add(featureID4);
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
