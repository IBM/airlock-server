package tests.restapi.copy_import.import_features;


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
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ImportWithMinVersion {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	private BranchesRestApi br ;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
		this.runOnMaster = runOnMaster;

	}
	
	
	@Test(description = "Add feature with minAppVersion=1")
	public void addFeatureToBranch() throws IOException, JSONException{

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("minAppVersion", "1.0");
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not created: " + featureID1 );
		
	
	}
	
	@Test (dependsOnMethods = "addFeatureToBranch", description="Create new season with minVersion=0.85")
	public void createNewProduct() throws Exception{

		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "0.85");		
		seasonID2 = s.addSeason(productID, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);
		
		if (runOnMaster) {
			destBranchID = BranchesRestApi.MASTER;
		}
		else {
			String allBranches = br.getAllBranches(seasonID2,sessionToken);
			JSONObject jsonBranch = new JSONObject(allBranches);
			destBranchID = jsonBranch.getJSONArray("branches").getJSONObject(1).getString("uniqueId");
		}
	}
	
	@Test (dependsOnMethods="createNewProduct", description="Import feature to the same season with high version in closed season")
	public void importWithHighGivenVersion() throws IOException{
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		//target season range- 0.8-0.85, given version=5.0
		String rootId = f.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", "5.0", "suffix1", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalGivenMinAppVersion"), "Feature was copied with illegal Given MinAppVersion ");
	}
	
	
	@Test(dependsOnMethods="importWithHighGivenVersion", description = "Import feature  with high minVersion")
	public void importWithHighFeatureVersion() throws IOException, JSONException{
		
		//target season range- 0.8-0.85, feature version=5.0 
		String feature = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		JSONObject fJson = new JSONObject(feature);
		fJson.put("minAppVersion", "5.0");
		featureID2 = f.addFeatureToBranch(seasonID2, destBranchID, fJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not created: " + featureID2 );
		
		String featureToImport = f.getFeatureFromBranch(featureID2, destBranchID, sessionToken);
		String rootId = f.getBranchRootId(seasonID, srcBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", null, "suffix2", true, sessionToken, srcBranchID);
		Assert.assertTrue(response.contains("illegalMinAppVersion"), "Feature was copied with illegal MinAppVersion ");

	}
	
	@Test(dependsOnMethods="importWithHighFeatureVersion", description = "import feature withgiven minAppVersion=0.1 to the new season. ")
	public void importWithLowGivenVersion() throws IOException, JSONException{

		//target season range- 0.85-, given version=0.05 
		String featureToImport = f.getFeatureFromBranch(featureID1, srcBranchID, sessionToken);
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.importFeatureToBranch(featureToImport, rootId, "ACT", "0.05", "suffix3", true, sessionToken, destBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied with legal MinAppVersion ");

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}