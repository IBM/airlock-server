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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;

public class ExperimentInProductionNoUpdate {
	protected String seasonID;
	protected FeaturesRestApi f;
	protected String feature;
	protected String filePath;
	protected String productID;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	private String experimentID;
	private String variantID;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_analyticsUrl = analyticsUrl;
		f = new FeaturesRestApi();
		f.setURL(url);


		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
 
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}

	@Test (description ="Add feature in production stage")
	public void addExperimentInProd() throws IOException, JSONException{

		experimentID =  baseUtils.addExperiment(m_analyticsUrl, true, false);

		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", "branch1");
		String branchID = br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantJson.put("stage", "PRODUCTION");
		variantID = exp.createVariant(experimentID, variantJson.toString(), sessionToken);


	}
	
	@Test (dependsOnMethods="addExperimentInProd", description = "If experiment in productions stage, its name can't be updated")
	public void updateExperimentName() throws Exception{		
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("name", "experiment in production");
		String response = exp.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Experiment in production can't be updated");

	}
	
	
	@Test (dependsOnMethods="updateExperimentName", description = "If varaint in productions stage, its name can't be updated")
	public void updateVariantName() throws Exception{		
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("name", "variant in production");
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "variant in production can't be updated");

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
