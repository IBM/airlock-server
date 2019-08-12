package tests.restapi.scenarios.experiments;


import java.io.IOException;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSON;
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

public class ExperimentRanges {
	protected String productID;
	protected String seasonID;
	private String experimentID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected String m_url;
	private String sessionToken = "";
	private ExperimentsRestApi expApi ;
	private String m_analyticsUrl;
	private ProductsRestApi p;
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath ;
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		expApi = new ExperimentsRestApi();
		expApi.setURL(m_analyticsUrl); 
		baseUtils = new AirlockUtils(m_url, m_analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);

	}


	@Test (description ="Add experiment") 
	public void addExperiment () throws Exception {

		experimentID = addExperiment("experiment."+RandomStringUtils.randomAlphabetic(5));
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", "variant1");
		variantJson.put("branchName", "MASTER");
		String response =  expApi.createVariant(experimentID, variantJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Add variant failed");
		
		//enable experiment
		String airlockExperiment = expApi.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		


		
	}
	
	
	  @Test (dependsOnMethods="addExperiment", description="Null start range")
	    public void nullStartRange() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", JSONObject.NULL);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted null start date");
		
	    }
	  
	  @Test (dependsOnMethods="nullStartRange", description="Same start and end date")
	    public void sameStartAndEnd() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", dateToMilliseconds("01/01/2020"));
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("01/01/2020"));
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted the same start and end date");
		
	    }
	  
	  @Test (dependsOnMethods="sameStartAndEnd", description="Start earlier than end date")
	    public void earlyStart() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", dateToMilliseconds("10/01/2020"));
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("01/01/2020"));
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted the same start and end date");
		
	    }
	  
	  @Test (dependsOnMethods="earlyStart", description="Update correct range")
	    public void correctRange() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	long start = dateToMilliseconds("01/01/2020");
		  	long end = dateToMilliseconds("10/01/2020");
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", start);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", end);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertFalse(response.contains("error"), "Can't update range");
		  	
		  	experiment = expApi.getExperiment(experimentID, sessionToken);
		  	expJson = new JSONObject(experiment);
		  	Assert.assertEquals(expJson.getJSONArray("ranges").getJSONObject(0).getLong("start"), start, "Can't update start range");
		  	Assert.assertEquals(expJson.getJSONArray("ranges").getJSONObject(0).getLong("end"), end, "Can't update end range");
		
	    }
	  
	  @Test (dependsOnMethods="correctRange", description="Same range")
	    public void sameRange() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	JSONObject range = new JSONObject();
		  	range.put("start", expJson.getJSONArray("ranges").getJSONObject(0).getLong("start"));
			range.put("end", expJson.getJSONArray("ranges").getJSONObject(0).getLong("end"));
			expJson.getJSONArray("ranges").add(range);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Added the same range twice");
		
	    }

	  @Test (dependsOnMethods="sameRange", description="Missing start date")
	    public void missingStart() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).remove("start");
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("01/01/2020"));
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted missing start date");
		
	    }
	  
	  @Test (dependsOnMethods="missingStart", description="Missing end date (allowed)")
	    public void missingEnd() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).remove("end");
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", dateToMilliseconds("01/01/2020"));
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertFalse(response.contains("error"), "Didn't accept missing end date");
		
	    }
	
	  @Test (dependsOnMethods="missingStart", description="Same start date in 2 ranges")
	    public void sameStart() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("start", dateToMilliseconds("01/01/2020"));
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("01/01/2020"));
		  	JSONObject range = new JSONObject();
		  	range.put("start", dateToMilliseconds("01/01/2020"));
			range.put("end", dateToMilliseconds("15/01/2020"));
			expJson.getJSONArray("ranges").add(range);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted the same start date in 2 ranges");
		
	    }
	  
	  @Test (dependsOnMethods="sameStart", description="Overlapping ranges")
	    public void overlappingRanges() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("02/01/2020"));
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("10/01/2020"));
		  	JSONObject range = new JSONObject();
		  	range.put("start", dateToMilliseconds("01/01/2020"));
			range.put("end", dateToMilliseconds("01/03/2020"));
			expJson.getJSONArray("ranges").add(range);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted overlapping ranges");
		
	    }
	  
	  @Test (dependsOnMethods="overlappingRanges", description="First range end date can't be null")
	    public void nullInFirstRanges() throws Exception {
		  	String experiment = expApi.getExperiment(experimentID, sessionToken);
		  	JSONObject expJson = new JSONObject(experiment);
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", dateToMilliseconds("01/01/2020"));
		  	expJson.getJSONArray("ranges").getJSONObject(0).put("end", JSON.NULL);
		  	JSONObject range = new JSONObject();
		  	range.put("start", dateToMilliseconds("01/01/2020"));
			range.put("end", dateToMilliseconds("01/03/2020"));
			expJson.getJSONArray("ranges").add(range);
		  	String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);
		  	Assert.assertTrue(response.contains("error"), "Accepted overlapping ranges");
		
	    }
	  
	private String addExperiment(String experimentName) throws IOException, JSONException{
		return baseUtils.addExperiment(experimentName, m_analyticsUrl, false, false);

	}

    public static long dateToMilliseconds(String inputDate){
        //date format: "01/01/2020";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date date = null;
        try {
            date = sdf.parse(inputDate);
        } catch (ParseException e) {
            System.out.println("Can't convert input date " + inputDate + " to milliseconds:" + e.getLocalizedMessage());
        }
        return date.getTime();
    }
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
