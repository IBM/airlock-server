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

public class AddInputFieldsToAnalytics {
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
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, String branchType) throws Exception{
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
	
	@Test (description="Add a field  to inputFieldsToAnalytics without input schema in the season")
	public void addFieldToAnalyticsWithoutSchema() throws IOException, JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.closestLightning");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "There is no input schema, but a context field was added to analytics" + response);
		
	}
	
	@Test (dependsOnMethods="addFieldToAnalyticsWithoutSchema", description="Add input schema to the season")
	public void addSchema() throws Exception{
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);
	}
	
	@Test (dependsOnMethods="addSchema", description="Add a required field  to inputFieldsToAnalytics")
	public void addRequiredFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.closestLightning.cardinalDirection");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.weatherSummary.closestLightning.cardinalDirection"), "The field \"context.weatherSummary.closestLightning.cardinalDirection\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="addRequiredFieldToAnalytics", description="Add an optional field  to inputFieldsToAnalytics")
	public void addOptionalFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.userPreferences.unitsOfMeasure");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.userPreferences.unitsOfMeasure"), "The field \"context.userPreferences.unitsOfMeasure\" was not found in the runtime development file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
	}
	
	@Test (dependsOnMethods="addOptionalFieldToAnalytics", description="Add a production field  to inputFieldsToAnalytics")
	public void addProdFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.viewedLocation.country");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);

		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(ifRuntimeContainsField(responseDev.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not found in the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(ifRuntimeContainsField(responseProd.message, "context.viewedLocation.country"), "The field \"context.viewedLocation.country\" was not found in the runtime production file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="addProdFieldToAnalytics", description="Add the same field twice to inputFieldsToAnalytics")
	public void addNonUniqueFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.closestLightning.cardinalDirection");
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was not updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	
	@Test (dependsOnMethods="addNonUniqueFieldToAnalytics", description="Add several fields to inputFieldsToAnalytics")
	public void addSeveralFieldsToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, "context.weatherSummary.nearestSnowAccumulation.dayPart"); //production field
		input = an.addInputFieldsToAnalytics(input, "context.weatherSummary.contentMode.effectiveDateTime");	//production field
		input = an.addInputFieldsToAnalytics(input, "context.device.localeCountryCode");	//development field
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(numberOfFields(responseProd.message) == 3, "Incorrect number of input fields to analytics in the runtime production file");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		if(m_branchType.equals("Master")|| m_branchType.equals("ProdExp")) {
			Assert.assertTrue(prodChanged.code == 200, "productionChanged.txt file was changed");
		}
		else{
			Assert.assertTrue(prodChanged.code != 200, "productionChanged.txt file should not have changed");
		}
	}
	
	@Test (dependsOnMethods="addSeveralFieldsToAnalytics", description="Add empty field to inputFieldsToAnalytics")
	public void addEmptyFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		String input = an.addInputFieldsToAnalytics(response, ""); 
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Analytics was not updated" + response);
		
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was updated");		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	
	@Test (dependsOnMethods="addEmptyFieldToAnalytics", description="Remove all fields from inputFieldsToAnalytics")
	public void removeFieldsFromAnalytics() throws IOException, JSONException, InterruptedException{
		String dateFormat = an.setDateFormat();
		
		String response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		
		
		JSONObject json = new JSONObject(response);
		JSONObject analyticsDataCollection = json.getJSONObject("analyticsDataCollection");
		analyticsDataCollection.remove("inputFieldsForAnalytics");
		analyticsDataCollection.put("inputFieldsForAnalytics", new JSONArray());
		response = an.updateGlobalDataCollection(seasonID, branchID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Analytics was not updated" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "VERBOSE", sessionToken);
		//Assert.assertTrue(numberOfFields(response)==0, "Analytics was not updated and fields were not removed.");
		//check if files were changed
		an.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");		
		Assert.assertTrue(numberOfFields(responseDev.message) == 0, "Input fields to analytics were not removed from the runtime development file");
		
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionBranchFileDateModification(m_url, productID, seasonID, branchID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(numberOfFields(responseProd.message) == 0, "Input fields to analytics were not removed from the runtime production file");
		
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
	
	
	private int numberOfFields(String input){
		
		try{
			JSONObject json = new JSONObject(input);
			//JSONObject analytics = json.getJSONObject("analyticsDataCollection");
			if (json.containsKey("inputFieldsForAnalytics")){
				JSONArray inputFields = json.getJSONArray("inputFieldsForAnalytics");
				return inputFields.size();
			} else {
				return -1;
			}
		} catch (Exception e){
				return -1;
		}
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
