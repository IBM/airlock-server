package tests.restapi.analytics;

import java.io.IOException;


import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class DeleteSchemaFieldsReportedToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
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
	
	@Test (description="Add input schema and 3 fields in dev and prod stage")
	public void addComponents() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String schemaResponse = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(schemaResponse.contains("error"), "Schema was not added to the season" + schemaResponse);
        
        String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
        //development field
        String input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.closestLightning.lat");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//production field
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		input = an.addInputFieldsToAnalytics(response, "context.device.connectionType");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		
		//development field
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
        input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.lifeStyleIndices.drivingDifficultyIndex");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
	}
	
	@Test (dependsOnMethods="addComponents", description="Simulate deletion of the first schema field using VALIDATE mode")
	public void simulateDeletionOneField() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_no_closestLightening.txt", "UTF-8", false);
		String response = schema.validateSchema(seasonID, schemaBody, sessionToken);
		JSONObject jsonResponse = new JSONObject(response);
		Assert.assertTrue(jsonResponse.getJSONArray("deletedAnalyticsInputFields").get(0).equals("context.weatherSummary.closestLightning.lat"), "Incorrect field in schema deletion validation for deletedAnalyticsInputFields");
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	}	
	
	@Test (dependsOnMethods="simulateDeletionOneField", description="Simulate deletion of 2 schema field using VALIDATE mode")
	public void simulateDeletionTwoFields() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_no_2fields.txt", "UTF-8", false);
		String response = schema.validateSchema(seasonID, schemaBody, sessionToken);
		JSONObject jsonResponse = new JSONObject(response);
		
		//an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		
		Assert.assertTrue(jsonResponse.getJSONArray("deletedAnalyticsInputFields").size() == 2, "Incorrect number of fields in schema deletion validation for deletedAnalyticsInputFields");
		
		String deletedString1 = jsonResponse.getJSONArray("deletedAnalyticsInputFields").getString(0);
		String deletedString2 = jsonResponse.getJSONArray("deletedAnalyticsInputFields").getString(1);
		Assert.assertTrue(deletedString1.equals("context.weatherSummary.closestLightning.lat") || deletedString2.equals("context.weatherSummary.closestLightning.lat"), "Incorrect field in schema deletion validation for deletedAnalyticsInputFields");
		Assert.assertTrue(deletedString1.equals("context.device.connectionType") || deletedString2.equals("context.device.connectionType"), "Incorrect field in schema deletion validation for deletedAnalyticsInputFields");

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");		
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	
	}
	
	@Test (dependsOnMethods="simulateDeletionTwoFields", description="Delete a production field from schema reported to analytics")
	public void deleteProductionField() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_no_productionField_device_connectionType.txt", "UTF-8", false);
        String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
		//String response = schema.updateInputSchema(seasonID, schemaBody, sessionToken);
		Assert.assertFalse(response.contains("error"), "Input schema was not updated");
		
		String responseAnalytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(ifRuntimeContainsField(responseAnalytics,"context.device.connectionType" ), "The field was removed from schema but not removed from analytics");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		Assert.assertFalse(ifRuntimeContainsField(responseDev.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not removed the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not update");
		Assert.assertFalse(ifRuntimeContainsField(responseProd.message, "context.device.connectionType"), "The field \"context.device.connectionType\" was not removed the runtime production file");
		
		//TODO: Amitai: this file shouldnt be touched, right?
		//RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		//Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");
	
	}
	
	@Test (dependsOnMethods="deleteProductionField", description="Delete a development field from schema reported to analytics")
	public void deleteDevelopmentField() throws Exception{
		String dateFormat = an.setDateFormat();
		
		String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_no_2fields.txt", "UTF-8", false);
        String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
		//String response = schema.updateInputSchema(seasonID, schemaBody, sessionToken);
		Assert.assertFalse(response.contains("error"), "Input schema was not updated");
		
		String responseAnalytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(ifRuntimeContainsField(responseAnalytics,"context.weatherSummary.closestLightning.lat" ), "The field was removed from schema but not removed from analytics");
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		Assert.assertFalse(ifRuntimeContainsField(responseDev.message, "context.weatherSummary.closestLightning.lat"), "The field \"context.weatherSummary.closestLightning.lat\" was not removed the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was update");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
	
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

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
