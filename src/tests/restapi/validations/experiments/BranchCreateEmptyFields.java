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
import tests.restapi.BranchesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class BranchCreateEmptyFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private String branch;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url); 
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
	}

	@Test 
	public void emptyName() throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("name", "");
		addBranch(json.toString(), "name");
		
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("uniqueId", "");
		addBranch(json.toString(), "uniqueId");
		
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("creator", "");
		addBranch(json.toString(), "creator");
		
	}
	
	@Test 
	public void emptySeason() throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("seasonId", "");
		addBranch(json.toString(), "season");
		
	}
	
	public void emptyBranchFeatures() throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("features", "");
		addBranch(json.toString(), "season");
		
	}
	
	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		JSONObject json = new JSONObject(branch);
		json.put("name", RandomStringUtils.randomAlphabetic(5));
		json.put("description", "");
		addBranchWithOptionalField(json.toString(), "description");		
	}
	
	private void addBranch(String input, String missingField) throws Exception{
		String response = br.createBranch(seasonID, input, "MASTER", sessionToken);
		Assert.assertTrue(response.contains("error"), "branch  added with missing field " + missingField );
	}
	
	private void addBranchWithOptionalField(String input, String missingField) throws Exception{
		String response = br.createBranch(seasonID, input, "MASTER", sessionToken);
		Assert.assertFalse(response.contains("error"), "branch was not added without optional field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
