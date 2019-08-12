package tests.restapi.airlock_analytics_server;


import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentAnalyticsApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ExperimentVariants {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private Config utils;
	private ExperimentsRestApi expApi ;
	private ExperimentAnalyticsApi expAnalyticsApi ;
	private String m_analyticsUrl;
	private ProductsRestApi p;
	private String experimentName;
	private BranchesRestApi br ;
	private String variantID1;
	private String variantID2;
	private String variantID3;
	
	@BeforeClass
	@Parameters({"url", "analyticsServerUrl", "configPath", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String airlockServerUrl, String analyticsServerUrl, String configPath, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = airlockServerUrl;
		utils = new Config(m_url, configPath, userName, userPassword, appName, productsToDeleteFile);
		m_analyticsUrl = utils.getAnalyticsUrl();
		filePath = configPath ;
		s = new SeasonsRestApi();
		s.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_analyticsUrl); 
		expAnalyticsApi = new ExperimentAnalyticsApi();
		expAnalyticsApi.setURL(analyticsServerUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		
		sessionToken = utils.sessionToken;
 		productID = utils.createProduct();
		seasonID = utils.createSeason(productID);
		
		experimentName = utils.setExperimentName();
	}


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

		JSONObject json = utils.getExperimentFromFile();
		json.put("name", experimentName);
		json.put("enabled", false);
		experimentID = expApi.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		
		//no ranges so not published to AA
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		Assert.assertTrue(aasResponse.contains("error"), "Experiment was not found in aas: " + aasResponse);
		
	}
	
	
	@Test (dependsOnMethods="addExperiment", description ="Update experiment controlGroupVariant") 
	public void addVariants () throws Exception {

		//add variant 
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		String branchID = br.createBranch(seasonID, branch, BranchesRestApi.MASTER, sessionToken);
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson1 = new JSONObject(variant);
		
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "variant1 desc");
		variantJson1.put("displayName", "variant1 DN");
		variantID1 = expApi.createVariant(experimentID, variantJson1.toString(), sessionToken);
		Assert.assertFalse(variantID1.contains("error"), "Variant1 was not created: " + variantID1);
		
		//enable experiment
		String experiment = expApi.getExperiment(experimentID, sessionToken);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("enabled", true);
		experimentID = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		experiment = expApi.getExperiment(experimentID, sessionToken);
		expJson = new JSONObject(experiment);
		
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
				
		JSONObject variantJson2 = new JSONObject(variant);
		variantJson2.put("name", "variant2");
		variantJson2.put("description", "variant2 desc");
		variantJson2.put("displayName", "variant2 DN");
		variantID2 = expApi.createVariant(experimentID, variantJson2.toString(), sessionToken);
		Assert.assertFalse(variantID2.contains("error"), "Variant2 was not created: " + variantID2);
		
		
		JSONObject variantJson3 = new JSONObject(variant);
		variantJson3.put("name", "variant3");
		variantJson3.put("description", JSONObject.NULL);
		variantJson3.put("displayName", JSONObject.NULL);
		variantID3 = expApi.createVariant(experimentID, variantJson3.toString(), sessionToken);
		Assert.assertFalse(variantID3.contains("error"), "Variant3 was not created: " + variantID3);

		
		//check that variants array was updated in aas
		aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		//"variantsDescriptions": ["variant desc", "variant desc"],
		//"variantsDisplayNames": [null, null],
		aasJson = new JSONObject(aasResponse);
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(0).equals("variant1 desc"), "variant1 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(1).equals("variant2 desc"), "variant2 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(0).equals("variant1 DN"), "variant1 displayName was not found in a list of variantsDisplayNames");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(1).equals("variant2 DN"), "variant2 displayName was not found in a list of variantsDisplayNames");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(2).equals(""), "variant3 desc was not found in a list of variantsDisplayNames");
		//Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").get(2).equals(null), "variant3 displayName was not found in a list of variantsDisplayNames");
		Assert.assertTrue(JSONObject.NULL.equals(aasJson.getJSONArray("variantsDisplayNames").get(2)), "Error");

	}
	

	
	@Test (dependsOnMethods="addVariants", description ="Update") 
	public void updateVariant () throws Exception {
		
		String variant1 = expApi.getVariant(variantID1, sessionToken);
		JSONObject variantJson1 = new JSONObject(variant1);		
		//variantJson1.put("name", "variant1a");
		variantJson1.put("name", "variant1");
		variantJson1.put("description", "");
		variantJson1.put("displayName", "");
		String response = expApi.updateVariant(variantID1, variantJson1.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update variant1: " + response);
		
		String variant2 = expApi.getVariant(variantID2, sessionToken);
		JSONObject variantJson2 = new JSONObject(variant2);		
		variantJson2.put("description", "variant2a desc");
		variantJson2.put("displayName", "variant2a DN");
		
		response = expApi.updateVariant(variantID2, variantJson2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update variant2: " + response);
		
		String variant3 = expApi.getVariant(variantID3, sessionToken);
		JSONObject variantJson3 = new JSONObject(variant3);		
		variantJson3.put("description", "variant3a desc");
		variantJson3.put("displayName", "variant3a DN");
		
		response = expApi.updateVariant(variantID3, variantJson3.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update variant3: " + response);

		
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		Assert.assertTrue(aasJson.getJSONArray("variants").getString(0).equals("variant1"), "variant1 name was not found in a list of variants");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(0).equals(""), "variant1 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(1).equals("variant2a desc"), "variant2 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(2).equals("variant3a desc"), "variant3 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(0).equals(""), "variant1 displayName was not found in a list of variantsDisplayNames");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(1).equals("variant2a DN"), "variant2 displayName was not found in a list of variantsDisplayNames");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(2).equals("variant3a DN"), "variant3 displayName was not found in a list of variantsDisplayNames");
			
	}
	
	@Test (dependsOnMethods="updateVariant", description ="delete variant") 
	public void deleteVariant () throws Exception {
		
		int codeResp = expApi.deleteVariant(variantID1, sessionToken);
		Assert.assertEquals(codeResp,  200, "Variant1 was not deleted: " + codeResp);
					
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		Assert.assertFalse(aasJson.getJSONArray("variants").contains("variant1a"), "deleted variant1 name was  found in a list of variants");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").size() == 2, "Incorrect variantsDescriptions size");
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").getString(0).equals("variant2a desc"), "variant2 desc was not found in a list of variantsDescpriptions");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").size() == 2, "Incorrect variantsDisplayNames size");		
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").getString(0).equals("variant2a DN"), "variant2 displayName was not found in a list of variantsDisplayNames");
					
	}
	
	@Test (dependsOnMethods="deleteVariant", description ="Update displayName and description to null") 
	public void updateVariantFieldsToEmpty () throws Exception {
		
		
		String variant2 = expApi.getVariant(variantID2, sessionToken);
		JSONObject variantJson2 = new JSONObject(variant2);		
		variantJson2.put("description", "");
		variantJson2.put("displayName", "");
		
		String response = expApi.updateVariant(variantID2, variantJson2.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update variant2: " + response);
		
					
		//get updated experiment in aas
		String aasResponse = expAnalyticsApi.getExperiment(experimentID, sessionToken);
		JSONObject aasJson = new JSONObject(aasResponse);
		
		Assert.assertTrue(aasJson.getJSONArray("variantsDescriptions").size() == 2, "Incorrect variantsDescriptions size");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").size() == 2, "Incorrect variantsDisplayNames size");
		Assert.assertTrue(aasJson.getJSONArray("variantsDisplayNames").get(0).equals(""), "Incorrect variantsDisplayNames");
	}
	
	
	@AfterTest
	private void reset(){
		utils.reset(productID, sessionToken);
	}
}
