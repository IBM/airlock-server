package tests.restapi.scenarios.experiments;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
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

public class UpdateIndexExperiment {
	protected String productID;
	protected String seasonID;
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
		String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
		JSONObject expJson = new JSONObject(experiment);
		expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
		expJson.put("enabled", true);
		expJson.put("indexExperiment", true);
		String response = expApi.createExperiment(productID, expJson.toString(), sessionToken);
		Assert.assertTrue (response.contains("error"), "Created experiment without range with indexExperiment=true");
		
	}
	
	  @Test (description="Delete range")
	    public void deleteRange() throws Exception {
			//add experiment
		  String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
			JSONObject expJson = new JSONObject(experiment);
			expJson.put("name", "experiment."+RandomStringUtils.randomAlphabetic(5));
			expJson.put("enabled", false);
			expJson.put("indexExperiment", false);
			String experimentID = expApi.createExperiment(productID, expJson.toString(), sessionToken);			
			Assert.assertFalse (experimentID.contains("error"), "can't create experiment");

			//add range
		  	experiment = expApi.getExperiment(experimentID, sessionToken);
		  	expJson = new JSONObject(experiment);
			JSONObject range = new JSONObject();
			range.put("start", dateToMilliseconds("01/01/2020"));
			range.put("end", dateToMilliseconds("02/01/2020"));
			
			JSONArray ranges = new JSONArray();
			ranges.add(range);
			expJson.put("ranges", ranges);			
			String response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);		
			Assert.assertFalse (response.contains("error"), "can't update experiment with range");
			
		  	experiment = expApi.getExperiment(experimentID, sessionToken);
		  	expJson = new JSONObject(experiment);
			expJson.put("ranges", ranges);	
			expJson.put("indexExperiment", true);
			response = expApi.updateExperiment(experimentID, expJson.toString(), sessionToken);		
			Assert.assertFalse (response.contains("error"), "can't update experiment with indexExperiment");



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
