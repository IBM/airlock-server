package tests.restapi.scenarios.experiments;

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
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.UtilitiesRestApi;


public class GetInputSampleInExperiment {
	protected String seasonID1;
	private String seasonID2;
	private String seasonID3;
	protected String featureID;
	protected String productID;
	protected String filePath;
	private String experimentID1;
	private String experimentID2;
	private String experimentID3;
	protected FeaturesRestApi f;
	protected UtilitiesRestApi u;
	private ExperimentsRestApi exp ;
	private SeasonsRestApi s;
	private String sessionToken = "";	
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;	
	private InputSchemaRestApi schema;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;

		u = new UtilitiesRestApi();
		u.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl); 
		br = new BranchesRestApi();
		br.setURL(url);
        schema = new InputSchemaRestApi();
        schema.setURL(url);

		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
	}
	
	/*
	 * season1: 1.0-2.0
	 * season2: 2.0-3.0
	 * season3: 3.0-
	 * exp1: 0.5-2.5 (season1, season2)
	 * exp2: 1.5-4.5 (season1, season2, season3)
	 * exp3: 3.5-5.0 (season3)
	 */
	
	@Test(description = "Add seasons, utilities and experiments")
	public void addSeasons() throws Exception{
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		
		//season1
		JSONObject season = new JSONObject();
		season.put("minVersion", "1.0");
		seasonID1 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "0.5");
		expJson.put("maxVersion", "2.5");
		expJson.put("enabled", false);
		experimentID1 = exp.createExperiment(productID, expJson.toString(), sessionToken);

		//season2
		season.put("minVersion", "2.0");
		seasonID2 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "1.5");
		expJson.put("maxVersion", "4.5");
		expJson.put("enabled", false);
		experimentID2 = exp.createExperiment(productID, expJson.toString(), sessionToken);
		
		//season3
		season.put("minVersion", "3.0");
		seasonID3 = s.addSeason(productID, season.toString(), sessionToken);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("minVersion", "3.5");
		expJson.put("maxVersion", "5.0");
		expJson.put("enabled", false);
		experimentID3 = exp.createExperiment(productID, expJson.toString(), sessionToken);

		String sch = schema.getInputSchema(seasonID1, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "experiments/schema1.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID1, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema1 was not added to season1" + response);
        
		sch = schema.getInputSchema(seasonID2, sessionToken);
        jsonSchema = new JSONObject(sch);
        schemaBody = FileUtils.fileToString(filePath + "experiments/schema2.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        response = schema.updateInputSchema(seasonID2, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema2 was not added to season2" + response);

		sch = schema.getInputSchema(seasonID3, sessionToken);
        jsonSchema = new JSONObject(sch);
        schemaBody = FileUtils.fileToString(filePath + "experiments/schema3.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        response = schema.updateInputSchema(seasonID3, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema2 was not added to season3" + response);

	}
	
	@Test(dependsOnMethods = "addSeasons",  description = "Get input sample")
	public void getAvailableBranches() throws Exception{
		JSONObject res = new JSONObject(exp.getInputSample(experimentID1, "DEVELOPMENT", "2.5", sessionToken, "MAXIMAL", 0.7));		
		//System.out.println(res);	
		//{"context":{"profile":{},"testData":{"precipitationForecast":[{"severity":40000000}],"dsx":{"effectiveDateTime":"4188-03-18T04:26:40.000Z","eventName":"Lorem ipsum laborum dolor","teaserTitle":"Lorem ipsum laborum dolor"}}}}
		//eventName field is version 2.5 and season has maxVersion 2.0
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONArray("precipitationForecast").size()==1);
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONObject("dsx").size()==3);
		
		//{"context":{"profile":{},"testData":{"precipitationForecast":[{"severity":40000000}],"dsx":{"effectiveDateTime":"4188-03-18T04:26:40.000Z","eventName":"Lorem ipsum laborum dolor","teaserTitle":"Lorem ipsum laborum dolor"}}}}
		res = new JSONObject(exp.getInputSample(experimentID2, "DEVELOPMENT", "2.5", sessionToken, "MAXIMAL", 0.7));		
		//System.out.println(res);
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONArray("precipitationForecast").size()==1);
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONObject("dsx").size()==3);
		
		
		res = new JSONObject(exp.getInputSample(experimentID3, "DEVELOPMENT", "5.0", sessionToken, "MAXIMAL", 0.7));		
		//System.out.println(res);
		//{"context":{"profile":{},"testData":{"precipitationForecast":[{"eventStart":40000000,"severity":40000000,"characteristic":40000000,"eventEnd":40000000}],"dsx":{"effectiveDateTime":"4188-03-18T04:26:40.000Z","eventName":"Lorem ipsum laborum dolor","teaserTitle":"Lorem ipsum laborum dolor","mode":"Lorem ipsum laborum dolor"}}}}
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONArray("precipitationForecast").getJSONObject(0).size()==4);
		Assert.assertTrue(res.getJSONObject("context").getJSONObject("testData").getJSONObject("dsx").size()==4);

	}
	
		
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
