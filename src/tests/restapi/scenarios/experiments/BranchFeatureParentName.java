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
import tests.restapi.FeaturesRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;


public class BranchFeatureParentName {
	private String productID;
	private String seasonID;
	private String branchID;
	private String featureID1;
	private String featureID2;
	private String featureID3;
	private String featureID4;
	private String featureMtxID;
	private String filePath;
	private String m_url;
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
	 * Add in branch:
	 * ROOT->F1
	 *     ->f2
	 *     ->F_MTX
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException, InterruptedException {
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);	
	}


	@Test(dependsOnMethods="addComponents", description="add development feature to branch")
	public void addFeaturesToBranch() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");

		String featureMTX = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		featureMtxID = f.addFeatureToBranch(seasonID, branchID, featureMTX, "ROOT", sessionToken);
		Assert.assertFalse(featureMtxID.contains("error"), "Feature mtx was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==3, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(1).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(2).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}


	@Test(dependsOnMethods="addFeaturesToBranch", description="move features to mtx")
	public void moveFeaturesToMTXInBranch() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature2 = new JSONObject(f.getFeatureFromBranch(featureID2, branchID, sessionToken));
		JSONObject featureMTX = new JSONObject (f.getFeatureFromBranch(featureMtxID, branchID, sessionToken));

		JSONArray mtxFeatures = new JSONArray();
		mtxFeatures.add(feature1);
		mtxFeatures.add(feature2);

		featureMTX.put("features", mtxFeatures);

		String response = f.updateFeatureInBranch(seasonID, branchID, featureMtxID, featureMTX.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update mtx in branch:" + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		JSONArray mtxSubFeatures = branchFeatures.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(mtxSubFeatures.size()==2, "Incorrect number of mtx sub features in branch development runtime file");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");	
	}

	//ROOT->F1->F2, F3
	@Test(dependsOnMethods="moveFeaturesToMTXInBranch", description="add development feature to master")
	public void addFeaturesToMaster() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		int code = f.deleteFeatureFromBranch(featureMtxID, branchID, sessionToken);
		Assert.assertTrue(code == 200, "cannot delete mtx bfrom branch");

		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, "MASTER", feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season: " + featureID1);

		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, "MASTER", feature2, featureID1, sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season: " + featureID2);

		String feature3 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		featureID3 = f.addFeatureToBranch(seasonID, "MASTER", feature3, featureID1, sessionToken);
		Assert.assertFalse(featureID3.contains("error"), "Feature was not added to the season: " + featureID3);

		String featureMTX = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		featureMtxID = f.addFeatureToBranch(seasonID, "MASTER", featureMTX, "ROOT", sessionToken);
		Assert.assertFalse(featureMtxID.contains("error"), "Feature mtx was not added to the season");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==200, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==0, "Incorrect number of features in branch development runtime file");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	@Test(dependsOnMethods="addFeaturesToMaster", description="chceckout feature2")
	public void checkoutFeature2() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String  response = br.checkoutFeature(branchID, featureID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout features: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("name").equals("Feature1"), "Incorrect name");

		JSONArray feature1SubFeatures = branchFeatures.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(feature1SubFeatures.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("Feature2"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	@Test(dependsOnMethods="checkoutFeature2", description="chceckout feature3")
	public void checkoutFeature3() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String  response = br.checkoutFeature(branchID, featureID3, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout features: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("name").equals("Feature1"), "Incorrect name");

		JSONArray feature1SubFeatures = branchFeatures.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(feature1SubFeatures.size()==2, "Incorrect number of features in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("Feature2"), "Incorrect name");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(1).getString("name").equals("Feature3"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");
	}

	//ROOT->F1->F2, F3 c_o
	//add:
	//     ->F4 new
	//     ->MTX c_o
	@Test(dependsOnMethods="checkoutFeature3", description="add development feature to master")
	public void addNewFeaturesToMaster() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String feature4 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		featureID4 = f.addFeatureToBranch(seasonID, branchID, feature4, "ROOT", sessionToken);
		Assert.assertFalse(featureID4.contains("error"), "Feature was not added to the season: " + featureID4);

		String  response = br.checkoutFeature(branchID, featureMtxID, sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot checkout features mtx: " + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==3, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("name").equals("Feature1"), "Incorrect name");
		Assert.assertTrue(branchFeatures.getJSONObject(2).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(2).getString("branchStatus").equals("CHECKED_OUT"), "Incorrect branchStatus");
		Assert.assertTrue(branchFeatures.getJSONObject(1).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(branchFeatures.getJSONObject(1).getString("name").equals("FCR1"), "Incorrect name");
		Assert.assertTrue(branchFeatures.getJSONObject(1).getString("branchStatus").equals("NEW"), "Incorrect branchStatus");
		
		JSONArray feature1SubFeatures = branchFeatures.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(feature1SubFeatures.size()==2, "Incorrect number of features in branch development runtime file");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(0).getString("name").equals("Feature2"), "Incorrect name");
		Assert.assertFalse(feature1SubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertTrue(feature1SubFeatures.getJSONObject(1).getString("name").equals("Feature3"), "Incorrect name");

		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="addNewFeaturesToMaster", description="move features to mtx")
	public void moveFeaturesToMTXInBranch2() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		JSONObject feature1 = new JSONObject(f.getFeatureFromBranch(featureID1, branchID, sessionToken));
		JSONObject feature4 = new JSONObject(f.getFeatureFromBranch(featureID4, branchID, sessionToken));
		JSONObject featureMTX = new JSONObject (f.getFeatureFromBranch(featureMtxID, branchID, sessionToken));

		JSONArray mtxFeatures = new JSONArray();
		mtxFeatures.add(feature1);
		mtxFeatures.add(feature4);

		featureMTX.put("features", mtxFeatures);

		String response = f.updateFeatureInBranch(seasonID, branchID, featureMtxID, featureMTX.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "cannot update mtx in branch:" + response);

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		JSONArray branchFeatures = getBranchFeatures(branchesRuntimeDev.message);
		Assert.assertTrue(branchFeatures.size()==1, "Incorrect number of features in branch development runtime file");
		Assert.assertTrue(branchFeatures.getJSONObject(0).getString("branchFeatureParentName").equals("ROOT"), "Incorrect branchFeatureParentName");
		JSONArray mtxSubFeatures = branchFeatures.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(mtxSubFeatures.size()==2, "Incorrect number of mtx sub features in branch development runtime file");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(0).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");
		Assert.assertFalse(mtxSubFeatures.getJSONObject(1).containsKey("branchFeatureParentName"), "Incorrect branchFeatureParentName");	
	}

	/*
	@Test(dependsOnMethods="addProdFeature", description="Move production feature to development")
	public void moveProdFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String feature2 = f.getFeatureFromBranch(featureID2, branchID, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "DEVELOPMENT");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID2, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production feature was not moved to development");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==2, "Incorrect number of features in branch development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeProd.message).size()==0, "Incorrect number of features in runtime production branch file");

	}

	@Test(dependsOnMethods="moveProdFeature", description="Delete development feature from branch")
	public void deleteDevFeature() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		int code = f.deleteFeatureFromBranch(featureID2, branchID, sessionToken);
		Assert.assertTrue(code == 200, "Development feature was not deleted from branch");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of features in branch development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==304, "Branch runtime production file was changed");

	}

	@Test(dependsOnMethods="deleteDevFeature", description="Move development feature to production")
	public void moveDevFeatureToProd() throws InterruptedException, IOException, JSONException{
		String dateFormat = f.setDateFormat();

		String feature2 = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject json = new JSONObject(feature2);
		json.put("stage", "PRODUCTION");
		String response = f.updateFeatureInBranch(seasonID, branchID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Production feature was not moved to development");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);		
		Assert.assertTrue(responseDev.code==304, "Runtime development file was changed");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code==304, "Runtime production file was changed");

		RuntimeRestApi.DateModificationResults branchesRuntimeDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeDev.code==200, "Branch runtime development file was not changed");
		Assert.assertTrue(getBranchFeatures(branchesRuntimeDev.message).size()==1, "Incorrect number of features in branch development runtime file");
		RuntimeRestApi.DateModificationResults branchesRuntimeProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(branchesRuntimeProd.code==200, "Branch runtime production file was changed");

		Assert.assertTrue(getFeatureStatus(branchesRuntimeProd.message, featureID1).equals("NEW"), "Incorrect feature status in runtime production branch file");

	}
	 */
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	private JSONArray getBranchFeatures(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		return json.getJSONArray("features");
	}

	private String getFeatureStatus(String result, String id) throws JSONException{
		JSONArray features = new JSONObject(result).getJSONArray("features");
		String status="";
		for(int i =0; i<features.size(); i++){
			if (features.getJSONObject(i).getString("uniqueId").equals(id)){
				status = features.getJSONObject(i).getString("branchStatus");
			}
		}

		return status;
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
