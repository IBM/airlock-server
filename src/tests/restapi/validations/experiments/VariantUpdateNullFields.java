package tests.restapi.validations.experiments;

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

public class VariantUpdateNullFields {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private String variantID;
	
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
		baseUtils.createBranch(seasonID);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		variantID = exp.createVariant(experimentID, variant, sessionToken);
		Assert.assertFalse(variantID.contains("error"), "can't create a variant" +  variantID);


	}
	
	/*
	protected String name = null; //c+u
	protected Stage stage = null; //c+u
	protected Boolean enabled = null; //required in create and update
	private UUID experimentId = null; //c+u
	private String branchName = null; //c+u
	protected UUID uniqueId = null; //nc + u
	protected String description = null; //opt in c+u (if missing or null in update don't change)
	 */
	@Test 
	public void emptyName() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("name", JSONObject.NULL);
		updateVariant(json.toString(), "name");
		
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("creator", JSONObject.NULL);
		updateVariant(json.toString(), "creator");
		
	}
	
	@Test 
	public void emptyStage() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("stage", JSONObject.NULL);
		updateVariant(json.toString(), "stage");
		
	}
	
	@Test 
	public void emptyEnabled() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("enabled", JSONObject.NULL);
		updateVariant(json.toString(), "enabled");		
	}
	
	@Test 
	public void emptyCreationDate () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("creationDate", JSONObject.NULL);
		updateVariant(json.toString(), "creationDate");			
	}
	
	@Test 
	public void emptyLastModified () throws Exception{
		String experiment = exp.getExperiment(experimentID, sessionToken);
		JSONObject json = new JSONObject(experiment);
		json.put("lastModified", JSONObject.NULL);
		updateVariant(json.toString(), "lastModified");			
	}
	
	
	@Test 
	public void emptyRolloutPercentage() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("rolloutPercentage", JSONObject.NULL);
		updateVariant(json.toString(), "rolloutPercentage");		
	}
	
	@Test 
	public void emptyRule() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("rule", JSONObject.NULL);
		updateVariant(json.toString(), "rule");		
	}

	@Test 
	public void emptyBranchName() throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("branchName", JSONObject.NULL);
		updateVariant(json.toString(), "branchName");		
	}
	
	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("description", JSONObject.NULL);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't update a variant  without an optional description field"  );
	
	}
	
	//optional field
	@Test 
	public void emptyDisplayName () throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("displayName", JSONObject.NULL);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't create a variant  without a displayName field"  );
	
	}

	@Test 
	public void emptyUniqueId () throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("uniqueId", JSONObject.NULL);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't update a variant  without an optional uniqueId field"  );

	}
	
	@Test 
	public void emptyExperimentId () throws Exception{
		String variant = exp.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("experimentId", JSONObject.NULL);
		String response = exp.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "can't update a variant  without experimentId field"  );

	}
	
	private void updateVariant(String input, String missingField) throws Exception{
		String response = exp.updateVariant(variantID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "variant  updated with missing field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
