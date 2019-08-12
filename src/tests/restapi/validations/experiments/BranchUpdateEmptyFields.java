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

public class BranchUpdateEmptyFields {
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
	
	/*
	 *      private String name = null; //c+u
	private UUID uniqueId = null; //nc + u
	private LinkedList<BaseAirlockItem> branchFeatures = new  LinkedList<BaseAirlockItem>(); //nc 
	private String description = null; //opt in c+u (if missing or null in update don't change)
	private UUID seasonId = null; //c+u
	private Date lastModified = null; // required in update. forbidden in create
	protected Date creationDate = null; //nc + u (not changed)
	protected String creator = null;	//c+u (creator not changed)
	 */

	@Test 
	public void emptyName() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("name", "");
		updateBranch(json.toString(), "name");
		
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("uniqueId", "");
		updateBranch(json.toString(), "uniqueId");
		
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("creator", "");
		updateBranch(json.toString(), "creator");
		
	}
	
	@Test 
	public void emptySeason() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("seasonId", "");
		updateBranch(json.toString(), "seasonId");
		
	}
	
	public void emptyBranchFeatures() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("features", "");
		updateBranch(json.toString(), "branchFeatures");
		
	}
	
	//optional field
	@Test 
	public void emptyDescription () throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("description", "");
		updateBranchWithOptionalField(json.toString(), "description");		
	}
	
	@Test 
	public void emptyLastModified() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("lastModified", "");
		updateBranch(json.toString(), "lastModified");
		
	}
	
	@Test 
	public void emptyCreationDate() throws Exception{
		String branch = br.getBranch(branchID, sessionToken);
		JSONObject json = new JSONObject(branch);
		json.put("creationDate", "");
		updateBranch(json.toString(), "creationDate");
		
	}

	
	private void updateBranch(String input, String missingField) throws Exception{
		String response = br.updateBranch(branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "branch  added with missing field " + missingField );
	}
	
	private void updateBranchWithOptionalField(String input, String missingField) throws Exception{
		String response = br.updateBranch(branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "branch was not added without optional field " + missingField );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
