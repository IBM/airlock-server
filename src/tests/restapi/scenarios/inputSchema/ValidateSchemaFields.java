package tests.restapi.scenarios.inputSchema;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;

public class ValidateSchemaFields {
	   protected String seasonID;
	    protected String productID;
	    protected String feature;
	    protected String filePath;
	    protected ProductsRestApi p;
	    protected FeaturesRestApi f;
	    protected InputSchemaRestApi schema;
	    private String sessionToken = "";
	    protected AirlockUtils baseUtils;
	    protected String murl;
	    private String m_rules;
	    private int countFeature=0;

	    @BeforeClass
	 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
		public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
	 	      filePath = configPath;
	        m_rules = configPath + "inputSchemaFields.txt";
	        murl=url;
	        p = new ProductsRestApi();
	        p.setURL(url);
	        schema = new InputSchemaRestApi();
	        schema.setURL(url);
	        f = new FeaturesRestApi();
	        f.setURL(url);
			baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
			sessionToken = baseUtils.sessionToken;
	  
	        productID = baseUtils.createProduct();
	        baseUtils.printProductToFile(productID);
	        seasonID = baseUtils.createSeason(productID);
	        feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
	    }
	    
		@Test (description="Add input schema to the season")
		public void addSchema() throws Exception{
			String sch = schema.getInputSchema(seasonID, sessionToken);
	        JSONObject jsonSchema = new JSONObject(sch);
	        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/Android__AirlockInputShema_profile_turbo_NoTestData_WithSettings.json", "UTF-8", false);
	        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
	        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
		}
		
		@Test(dependsOnMethods="addSchema", dataProvider = "rulesReader", description = "Create features")
		public void positiveScenarios(String rule) throws JSONException, IOException{
			countFeature++;
			String featureName  = "featureName" + countFeature;
			JSONObject json = new JSONObject(feature);
			JSONObject ruleObj = new JSONObject();
			rule = rule.replaceAll("\"", "'");
			ruleObj.put("ruleString", rule);
			json.put("rule", ruleObj);
			json.put("name", featureName);
			json.put("minAppVersion", "11.0");
			String featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);
			Assert.assertFalse(featureID.contains("error"), "rule: " + rule + " error: " + featureID);
		}
		
		
		@DataProvider (name="rulesReader")
		private Iterator<Object[]> positiveScenarios() throws IOException{

			List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
			BufferedReader br = new BufferedReader(new FileReader(m_rules));
	        String line;
	        while ((line = br.readLine()) != null) {
	        	if (line != "" && !line.startsWith("#")) {
	        		dataToBeReturned.add(new Object[] {line });

	        	}	
	         }
	        br.close();
			return dataToBeReturned.iterator(); 

		}
		
	/*	
	    @AfterTest
	    private void reset(){
	    	baseUtils.reset(p, productID, sessionToken);
	    }
	*/
}
