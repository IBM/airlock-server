package tests.restapi.analytics_in_experiments;

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
import tests.restapi.*;

public class GetQuotaInExperiment {
	protected String seasonID1;
	protected String seasonID2;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private SeasonsRestApi s;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		

	}
	
	@Test ( description="get default experiment quota")
	public void getDefaultQuota() throws IOException, JSONException, InterruptedException{
		experimentID = addExperiment("0.5", "3.0");
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		JSONObject jsonResp = new JSONObject(an.getExperimentQuota(experimentID, sessionToken));
		Assert.assertTrue(jsonResp.getInt("analyticsQuota")==100, "Incorrect default quota");
	}
	
	
	@Test ( dependsOnMethods="getDefaultQuota", description="Add seasons")
	public void addSeasons() throws Exception{
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);
		String branchID1 = addBranch("branch1", seasonID1);
		Assert.assertFalse(branchID1.contains("error"), "Branch1 was not created: " + branchID1);

		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);
		
		
		String variantID1 = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
		String variantID2 = addVariant("variant2", "branch1");
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);

	}

	


	@Test (dependsOnMethods="addSeasons", description="get experiment quota")
	public void updateQuota() throws IOException, JSONException, InterruptedException{
		String response = an.updateQuota(seasonID1, 3, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not set in season1" + response);
		
		response = an.updateQuota(seasonID2, 10, sessionToken);
		Assert.assertFalse(response.contains("error"), "Quota was not set in season2" + response);
		
		JSONObject jsonResp = new JSONObject(an.getExperimentQuota(experimentID, sessionToken));
		Assert.assertTrue(jsonResp.getInt("analyticsQuota")==10, "Incorrect quota");
	}
	

	
	private String addExperiment(String minVersion, String maxVersion) throws IOException, JSONException{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("stage", "PRODUCTION");
		expJson.put("minVersion", minVersion);
		expJson.put("maxVersion", maxVersion);
		expJson.put("enabled", "false");
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		return exp.createExperiment(productID, expJson.toString(), sessionToken);
	}
	
	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		variantJson.put("stage", "PRODUCTION");
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);
	}
	
	private String addBranch(String branchName, String seasonId) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonId, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
