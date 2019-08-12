package tests.restapi.validations.experiments;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class ExperimentCheckNameCharacters {
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
	protected List<String> illegalCharacters;
	
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
		
		illegalCharacters= new ArrayList<String>(Arrays.asList("(", ")", "[", "]", "{", "}", "|", "/", "\\", "\"", ">", "<", 
				",", "!", "?", "@", "#", "$", "%", "^", "&", "*", "~", ";", "'", "-","_"));

	}
	
	@Test 
	public void legalVariantName() throws Exception{
		JSONObject json = new JSONObject(experiment);
		json.put("name", "name 123");
		json.put("enabled", false);
		String response = exp.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment was not added with legal name \"name 123\"" );

		json.put("name", "name.123a");
		response = exp.createExperiment(productID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "experiment was not added with legal name \"name.123a\" ");

		
	}
	
	@Test (description = "Validate illegal special characters in variant name")
	public void illegalCharactersInName() throws JSONException, IOException{
		JSONObject json = new JSONObject(experiment);
		for (String character : illegalCharacters) {
			json.put("name", "name" + character);
			json.put("enabled", false);			
			String response = exp.createExperiment(productID, json.toString(), sessionToken);
			Assert.assertTrue(response.contains("error"), "Test failed: " + response );
		}		
	}
		
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
