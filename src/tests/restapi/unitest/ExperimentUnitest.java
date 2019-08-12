package tests.restapi.unitest;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentUnitest {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi ex ;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		ex = new ExperimentsRestApi();
		ex.setURL(analyticsUrl); 

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	@Test (description="Add experiment")
	public void addExperiment() throws IOException, JSONException{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, true, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created " + experimentID);
	}
	
	@Test (dependsOnMethods="addExperiment", description="Get all experiments")
	public void getAllExperiments() throws Exception{
		
		String response = ex.getAllExperiments(productID, sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not created " + response);
	}
	
	
	@Test (dependsOnMethods="getAllExperiments", description="Update experiment")
	public void updateExperiment() throws Exception{
		String experiment = ex.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("description", "new experiment description");
		json.put("stage", "DEVELOPMENT");
		String response = ex.updateExperiment(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated " + response);
	}
	
	@Test (dependsOnMethods="updateExperiment", description="Delete experiment")
	public void deleteExperiment() throws Exception{
		int respCode = ex.deleteExperiment(experimentID, sessionToken);
		Assert.assertTrue(respCode==200, "Experiment was not deleted ");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
