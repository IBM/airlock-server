package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class UpdateInputFieldsToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected String m_branchType;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	protected InputSchemaRestApi schema;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "branchType"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws IOException{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
        schema = new InputSchemaRestApi();
        schema.setURL(m_url);
        baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		m_branchType = branchType;
		try {
			if(branchType.equals("Master")) {
				branchID = BranchesRestApi.MASTER;
			}
			else if(branchType.equals("StandAlone")) {
				branchID = baseUtils.addBranchFromBranch("branch1",BranchesRestApi.MASTER,seasonID);
			}
			else if(branchType.equals("DevExp")) {
				branchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
			else if(branchType.equals("ProdExp")) {
				branchID = baseUtils.createBranchInProdExperiment(analyticsUrl).getString("brId");
			}
			else{
				branchID = null;
			}
		}catch (Exception e){
			branchID = null;
		}
	}
	
	
	
	@Test (description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	@Test (dependsOnMethods="addSchema", description="Add a field  to inputFieldsToAnalytics")
	public void addFieldToAnalytics1() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONArray inputFields = new JSONArray();
		//field in PRODUCTION stage
		inputFields.put("context.viewedLocation.country");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==1, "Incorrect number of input fields");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not found in the runtime production file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}

	@Test (dependsOnMethods="addFieldToAnalytics1", description="Add another  field  to inputFieldsToAnalytics")
	public void addFieldToAnalytics2() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		JSONArray inputFields = new JSONArray();
		//field in DEVELOPMENT stage
		inputFields.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==1, "Incorrect number of input fields");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.device.locale"), "The field \"context.device.locale\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertFalse(ifRuntimeContainsField(responseProd.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not removed from the runtime production file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="addFieldToAnalytics2", description="Add several  fields  to inputFieldsToAnalytics")
	public void addFieldToAnalytics3() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		JSONArray inputFields = new JSONArray();
		//fields in DEVELOPMENT stage
		inputFields.put("context.device.locale");
		inputFields.put("context.weatherSummary.closestLightning.distance");
		//fields in PRODUCTION stage
		inputFields.put("context.viewedLocation.country");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(inputFields(response).size()==3, "Incorrect number of input fields");
		
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not added to the runtime production file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	private boolean ifRuntimeContainsField(String input, String field){
		
		try{
			JSONObject json = new JSONObject(input);
			if (json.containsKey("inputFieldsForAnalytics")){
				JSONArray inputFields = json.getJSONArray("inputFieldsForAnalytics");
				for (Object s : inputFields) {
					if (s.equals(field)) 
						return true;
				}
				//Arrays.asList(inputFields).contains(field);
				return false;
			} else {

				return false;
			}
		} catch (Exception e){
				return false;
		}
	}
	
	private JSONArray inputFields(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getJSONArray("inputFieldsForAnalytics");
	}
	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
