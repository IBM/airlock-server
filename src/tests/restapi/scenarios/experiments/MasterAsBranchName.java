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
import tests.restapi.SeasonsRestApi;

public class MasterAsBranchName {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	protected String filePath;
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
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		

	}
	
	/*
		- Can create variant with branch name "MASTER"
	 */

	@Test (description ="Create variant with MASTER as branch name when season doesn't exist") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5), false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		variantID = addVariant("variant1", "MASTER");
		Assert.assertTrue(variantID.contains("error"), "Variant was created with branch name \"MASTER\" when season doesn't exist");


	}
	
	
	@Test (dependsOnMethods="addComponents", description ="Create season and create variant with MASTER as branch name") 
	public void addLegalBranchName() throws Exception{
		seasonID = baseUtils.createSeason(productID);
		
		variantID = addVariant("variant1", "master");
		Assert.assertTrue(variantID.contains("error"), "Variant was created with incorrect branch name \"master\"");
		
		variantID = addVariant("variant1", "MASTER");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created with branch name \"MASTER\" " + variantID);

	}
	
	
	private String addExperiment(String experimentName, boolean enabled) throws IOException, JSONException{

		return baseUtils.addExperiment(m_analyticsUrl, false, enabled);

	}

	
	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
