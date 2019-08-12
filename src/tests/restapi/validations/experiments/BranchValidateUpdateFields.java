package tests.restapi.validations.experiments;

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

public class BranchValidateUpdateFields {
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private String branchID;

	
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
		
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		branchID = br.createBranch(seasonID, branch, "MASTER", sessionToken);
	}

	@Test 
	public void updateName() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "new branch name");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "branch  was not updated " + response );
		
		branch = br.getBranch(branchID, sessionToken);
		JSONObject updatedJson = new JSONObject(branch);
		Assert.assertTrue(updatedJson.getString("name").equals("new branch name"), "Name was not udated");
		
	}
	
	@Test 
	public void updateDescripion() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("description", "new branch description");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "branch  was not updated " + response );
		
		branch = br.getBranch(branchID, sessionToken);
		JSONObject updatedJson = new JSONObject(branch);
		Assert.assertTrue(updatedJson.getString("description").equals("new branch description"), "description was not udated");
				
	}
	
	
	@Test 
	public void updateLastModified() throws Exception{
			long timestamp = System.currentTimeMillis();
			String branch = br.getBranch(branchID, sessionToken);
			JSONObject json = new JSONObject(branch);
			json.put("lastModified", timestamp - 10000000);
			String response = br.updateBranch(branchID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@Test 
	public void updateCreationDate() throws Exception{
			long timestamp = System.currentTimeMillis();
			String branch = br.getBranch(branchID, sessionToken);
			JSONObject json = new JSONObject(branch);
			json.put("creationDate", timestamp);
			String response = br.updateBranch(branchID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@Test 
	public void updateUniqueID() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("uniqueId", "780cd507-1b86-56c3-88b8-1f44910c0f94");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		
		branch = br.getBranch(branchID, sessionToken);
		JSONObject updatedJson = new JSONObject(branch);

		Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@Test 
	public void updateSeasonID() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		
		String seasonID2 = s.addSeason(productID, "{\"minVersion\":\"5.0\"}", sessionToken);
		json.put("seasonId", seasonID2);
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		branch = br.getBranch(branchID, sessionToken);
		JSONObject updatedJson = new JSONObject(branch);

		Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@Test 
	public void updateCreator() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("creator", "creatorname");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@Test 
	public void updatebBanchFeatures() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("features", "");
		String response = br.updateBranch(branchID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "branch  was updated " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
