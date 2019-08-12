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
import tests.restapi.ProductsRestApi;

public class DeleteComponents {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	private String variantID;
	private String branchID;
	private String branchID2;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private ExperimentsRestApi exp ;
	private BranchesRestApi br ;
	private ProductsRestApi p;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		
		m_analyticsUrl = analyticsUrl;
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
		
		p = new ProductsRestApi();
		p.setURL(m_url);

		br = new BranchesRestApi();
		br.setURL(m_url);
 
		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}
	
	/*
	 * - delete product deletes experiment/variant/branch
- delete variant - check that branch is not deleted
- delete experiment deletes variant
	 */

	@Test (description ="Add components") 
	public void addComponents () throws IOException, JSONException {
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Experiment was not created: " + branchID);

		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
	

	}


	@Test (dependsOnMethods="addComponents", description ="Delete variant") 
	public void deleteVariant() throws Exception{

		int responseCode = exp.deleteVariant(variantID, sessionToken);
		Assert.assertTrue(responseCode==200, "Variant was not deleted");
		
		String response = exp.getVariant(variantID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted variant");
		
		//delete variant does not delete the branch		
		response = br.getBranch(branchID2, sessionToken);
		Assert.assertFalse(response.contains("not found"), "Branch was deleted");
	}
	
	
	@Test (dependsOnMethods="deleteVariant", description ="Delete experiment also deletes variant") 
	public void deleteExperiment() throws Exception{
		//add 2 variants to experiment
		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
		
		String variantID2 = addVariant("variant2");
		Assert.assertFalse(variantID2.contains("error"), "Variant was not created: " + variantID2);

		//delete experiment
		int responseCode = exp.deleteExperiment(experimentID, sessionToken);
		Assert.assertTrue(responseCode==200, "Experiment was not deleted");
		
		String response = exp.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted branch");
		
		//all variant should be deleted
		response = exp.getVariant(variantID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted variant1");
		response = exp.getVariant(variantID2, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted variant2");
		
		//branch is not deleted
		response = br.getBranch(branchID2, sessionToken); 
		Assert.assertFalse(response.contains("not found"), "Got deleted branch");

	}
	
	
	@Test (dependsOnMethods="deleteExperiment", description ="Delete product also deletes all components") 
	public void deleteProduct() throws Exception{
		//add experiment and variant
		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		variantID = addVariant("variant1");
		Assert.assertFalse(variantID.contains("error"), "Variant was not created: " + variantID);
		
		
		int responseCode = p.deleteProduct(productID, sessionToken);
		Assert.assertTrue(responseCode==200, "Product was not deleted");
		
		String response = br.getBranch(branchID, sessionToken); 
		Assert.assertTrue(response.contains("not found"), "Got deleted branch");

		response = exp.getVariant(variantID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted variant");
		
		response = exp.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(response.contains("not found"), "Got deleted experiment");

		
	}
	
	private String addExperiment(String experimentName) throws IOException, JSONException{

		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private String addVariant(String variantName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
