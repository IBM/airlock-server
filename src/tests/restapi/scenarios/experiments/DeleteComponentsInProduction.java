package tests.restapi.scenarios.experiments;

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
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class DeleteComponentsInProduction {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private String m_analyticsUrl;

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		m_analyticsUrl = analyticsUrl;
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		
		p = new ProductsRestApi();
		p.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	/*
- can't delete experiment/variant in production
- can't delete product when experiment/variant in production
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		//add experiment in production
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), "PRODUCTION", false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		//add variant in development
		variantID = addVariant("variant1", "DEVELOPMENT");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);

	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Delete product with experiment in production") 
	public void deleteExperimentInProduction() throws Exception{
		//can't delete experiment in production
		int responseCode = exp.deleteExperiment(experimentID, sessionToken);
		Assert.assertFalse(responseCode==200, "Deleted experiment in production");
		
		//can't delete product with experiment in production
		responseCode = p.deleteProduct(productID, sessionToken) ;
		Assert.assertFalse(responseCode==200, "Deleted product with experiment in production");
		
	}

	@Test (dependsOnMethods="deleteExperimentInProduction", description ="Update variant to production, update experiment to development. Delete variant")
	public void deleteVariantInProduction() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("stage", "PRODUCTION");
		String response = exp.updateVariant(variantID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant stage was not updated");
		
			//can't delete variant in production
		int responseCode = exp.deleteVariant(variantID, sessionToken);
		Assert.assertFalse(responseCode==200, "Deleted variant in production");
		
		//can't delete product with variant in production
		responseCode = p.deleteProduct(productID, sessionToken) ;
		Assert.assertFalse(responseCode==200, "Deleted production with variant in production stage");
		
	}
	
	
	
	private String addExperiment(String experimentName, String stage, boolean enabled) throws IOException, JSONException{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", experimentName);
		expJson.put("stage", stage);
		expJson.put("enabled", enabled);
		return exp.createExperiment(productID, expJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), br.MASTER, sessionToken);

	}
	
	private String addVariant(String variantName, String stage) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("stage", stage);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
