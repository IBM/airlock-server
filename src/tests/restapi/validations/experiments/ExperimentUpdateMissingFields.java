package tests.restapi.validations.experiments;


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

public class ExperimentUpdateMissingFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private String experimentID;
	
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

		experimentID = baseUtils.addExperiment(analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "experiment was not added " + experimentID );

	}
	
	/*
	 protected Stage stage = null; //c+u
	protected UUID uniqueId = null; //nc + u
	protected String name = null; //c+u
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	protected Boolean enabled = null; //required in create and update
	LinkedList<Variant> variants = new LinkedList<Variant>(); //not in create must in update
	private UUID productId = null; //required in create and update
	private String minVersion = null; //required in create and update
	private String maxVersion = null; //optional
	private String hypothesis = null; //optional
	private String measurments = null; //optional
	 */
	
	@Test 
	public void emptyName() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("name");
		updateExperiment(json.toString(), "name");
		
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("creator");
		updateExperiment(json.toString(), "creator");		
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("uniqueId");
		updateExperimentWithOptionalField(json.toString(), "uniqueId");
		
	}
	
	@Test 
	public void emptyLastModified () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("lastModified");
		updateExperiment(json.toString(), "lastModified");			
	}
	
	@Test 
	public void emptyCreationDate  () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("creationDate");
		updateExperiment(json.toString(), "creationDate");			
	}
	
	@Test 
	public void emptyProductId() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("productId");
		updateExperiment(json.toString(), "productId");
		
	}
	
	@Test 
	public void emptyStage() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("stage");
		updateExperiment(json.toString(), "stage");
		
	}
	
	@Test 
	public void emptyEnabled() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("enabled");
		updateExperiment(json.toString(), "enabled");		
	}
	
	@Test 
	public void emptyMinVersion() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("minVersion");
		updateExperiment(json.toString(), "minVersion");		
	}
	
	
	@Test 
	public void emptyRolloutPercentage() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("rolloutPercentage");
		updateExperiment(json.toString(), "rolloutPercentage");		
	}
	
	@Test 
	public void emptyRule() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("rule");
		updateExperiment(json.toString(), "rule");		
	}
	

	@Test 
	public void emptyVariants () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("variants");
		updateExperiment(json.toString(), "variants");			
	}
	
	//optional field
	@Test 
	public void emptyMaxVersion() throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("maxVersion");
		updateExperimentWithOptionalField(json.toString(), "maxVersion");		
	}
	
	//optional field
	@Test 
	public void emptyHypothesis () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("hypothesis");
		updateExperimentWithOptionalField(json.toString(), "hypothesis");		
	}
	
	//optional field
	@Test 
	public void emptyDisplayName () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("displayName");
		updateExperimentWithOptionalField(json.toString(), "displayName");		
	}
	
	//optional field
	@Test 
	public void emptyMeasurements () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("measurements");
		updateExperimentWithOptionalField(json.toString(), "measurements");		
	}

	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.remove("description");
		updateExperimentWithOptionalField(json.toString(), "description");		
	}	
	private void updateExperiment(String input, String missingField) throws Exception{
		String response = exp.updateExperiment(experimentID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "experiment was updated with missing field " + missingField );
	}
	
	private void updateExperimentWithOptionalField(String input, String missingField) throws Exception{
		String response = exp.updateExperiment(experimentID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment was not updated without optional field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
