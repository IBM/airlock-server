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

public class VariantCreateMissingFields {
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
	private String variant;
	
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
		
		variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);

	}
	
	@Test 
	public void emptyName() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("name");
		addVariant(json.toString(), "name");
		
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("creator");
		addVariant(json.toString(), "creator");
		
	}
	
	@Test 
	public void emptyStage() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("stage");
		addVariant(json.toString(), "stage");
		
	}
	
	@Test 
	public void emptyEnabled() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("enabled");
		addVariant(json.toString(), "enabled");		
	}
	
	
	@Test 
	public void emptyRolloutPercentage() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("rolloutPercentage");
		addVariant(json.toString(), "rolloutPercentage");		
	}
	
	@Test 
	public void emptyRule() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("rule");
		addVariant(json.toString(), "rule");		
	}

	@Test 
	public void emptyBranchName() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.remove("branchName");
		addVariant(json.toString(), "branchName");		
	}
	
	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		JSONObject json = new JSONObject(variant);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("description");
		String response = exp.createVariant(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't create a variant  without a description field"  );
	
	}
	
	//optional field
	@Test
	public void emptyInternalUserGroups() throws Exception{
		JSONObject json = new JSONObject(variant);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("internalUserGroups");
		String response = exp.createVariant(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't create a variant  without internalUserGroups field"  );

	}
	
	//optional field
	@Test 
	public void emptyDisplayName () throws Exception{
		JSONObject json = new JSONObject(variant);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.remove("displayName");
		String response = exp.createVariant(experimentID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "can't create a variant  without a displayName field"  );
	
	}
	
	private void addVariant(String input, String missingField) throws Exception{
		String response = exp.createVariant(experimentID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "variant  added with missing field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
