package tests.restapi.unitest;

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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class VariantUnitest {
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
		ex = new ExperimentsRestApi();
		ex.setURL(analyticsUrl); 
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		seasonID = baseUtils.createSeason(productID);
		baseUtils.createBranch(seasonID);
	}
	
	@Test (description="Add experiment and variant")
	public void addVariant() throws IOException, JSONException{
		JSONObject experiment = new JSONObject( FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false));
		experiment.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		experiment.put("enabled", false);
		experimentID = ex.createExperiment(productID, experiment.toString(), sessionToken);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created " + experimentID);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		variantID = ex.createVariant(experimentID, variant, sessionToken);
		Assert.assertFalse(variantID.contains("error"), "Variant was not created " + variantID);
	}
	
	@Test (dependsOnMethods="addVariant", description="Update variant")
	public void updateVariant() throws Exception{
		String variant = ex.getVariant(variantID, sessionToken);
		JSONObject json = new JSONObject(variant);
		json.put("description", "new variant description");
		String response = ex.updateVariant(variantID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Variant was not updated " + response);
	}
	
	@Test (dependsOnMethods="updateVariant", description="Delete variant")
	public void deleteVariant() throws Exception{
		int respCode = ex.deleteVariant(variantID, sessionToken);
		Assert.assertTrue(respCode==200, "Variant was not deleted ");
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
