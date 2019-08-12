package tests.restapi.validations.experiments;


import org.apache.commons.lang3.RandomStringUtils;

import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentCreateMissingFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private String experiment;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
	
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);

	}
	
	@Test 
	public void missingName() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("name");
		json.put("enabled", false);
		addExperiment(json.toString(), "name");
		
	}
	
	@Test 
	public void missingCreator() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("creator");
		json.put("enabled", false);
		addExperiment(json.toString(), "creator");
		
	}
	
	@Test 
	public void missingStage() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("stage");
		json.put("enabled", false);
		addExperiment(json.toString(), "stage");
		
	}
	
	@Test 
	public void missingEnabled() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("enabled");
		//json.put("enabled", false);
		addExperiment(json.toString(), "enabled");		
	}
	
	@Test 
	public void missingMinVersion() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("minVersion");
		json.put("enabled", false);
		addExperiment(json.toString(), "minVersion");		
	}
	

	@Test 
	public void missingRolloutPercentage() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("rolloutPercentage");
		json.put("enabled", false);
		addExperiment(json.toString(), "rolloutPercentage");		
	}
	
	@Test 
	public void missingRule() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.remove("rule");
		json.put("enabled", false);
		addExperiment(json.toString(), "rule");		
	}
	
	//optional field
	@Test 
	public void emptyMaxVersion() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("maxVersion");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "maxVersion");
	}
	
	//optional field
	@Test 
	public void emptyHypothesis () throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("hypothesis");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "hypothesis");	
	}
	
	//optional field
	@Test 
	public void emptyMeasurements () throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("measurements");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "measurements");			
	}
	
	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("measurements");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "description");		
	}
	
	//optional field
	@Test 
	public void emptyDisplayName () throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("displayName");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "displayName");		
	}
	
	//optional field
	@Test
	public void emptyInternalUserGroups() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("internalUserGroups");
		json.put("enabled", false);
		addExperimentWithOptionalField(json.toString(), "internalUserGroups");
	}
	
	private void addExperiment(String input, String missingField) throws Exception{
		String response = exp.createExperiment(productID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment  added with missing field " + missingField );
	}
	
	private void addExperimentWithOptionalField(String input, String missingField) throws Exception{
		String response = exp.createExperiment(productID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment was not added without optional field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
