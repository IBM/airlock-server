package tests.restapi.in_app_purchases;

import java.io.IOException;



import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.SeasonsRestApi;

public class DeleteSeasonWithBranch {
	protected String productID;
	protected String seasonID;
	protected String seasonID2;
	private String experimentID;
	private String branchID;
	private String variantID;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private SeasonsRestApi s;
	protected InAppPurchasesRestApi purchasesApi;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(url);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	/*
	* CAN delete season if its branch is in use by variant if there is another season with the same branch
	* CAN delete a branch when entitlement in production if it is not used by variant
	*/

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1", seasonID);
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
	}
	
	@Test (dependsOnMethods="addComponents", description ="delete season if its branch is in use by variant if there is another season with the same branch") 
	public void addSeason () throws IOException, JSONException {
		//can't delete season if it's branch is in use by variant, if it's the only season using this branch
		int responseCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertTrue(responseCode!=200, "Season was not deleted");
		
		//add second season
		String season = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false);
		seasonID2 = s.addSeason(productID, season, sessionToken);
		
		//can delete season with branch in use if it's not the only season
		responseCode = s.deleteSeason(seasonID, sessionToken);
		Assert.assertTrue(responseCode==200, "Season was not deleted");
	}
	
	@Test (dependsOnMethods="addSeason", description ="delete a branch with entitlements in production if it is not used by variant") 
	public void deleteBranchWithProductionEntitlements () throws Exception {
		branchID = addBranch("branch3", seasonID2);
		Assert.assertFalse(branchID.contains("error"), "Branch was not created: " + branchID);

		//add entitlement in production 
		String entitlement = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(entitlement);
		json.put("stage", "PRODUCTION");
		String response = purchasesApi.addPurchaseItemToBranch(seasonID2, branchID, json.toString(), "ROOT", sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't add entitlement in production " + response);
		
		int responseCode = br.deleteBranch(branchID, sessionToken);
		Assert.assertTrue(responseCode==200, "Branch was not deleted");
	}
	
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}
	
	private String addBranch(String branchName, String season) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(season, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
	}
	
	private String addVariant(String variantName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);
	}
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
