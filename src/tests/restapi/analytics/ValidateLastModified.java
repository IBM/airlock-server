package tests.restapi.analytics;

import java.io.IOException;






import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.*;

public class ValidateLastModified {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String configID2;
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
	@Test (description="Add feature, configuration rule and schema to the season")
	public void addComponents() throws Exception{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "feature was not added to the season" + featureID1);
		
		//add first configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);
		
		String sch = schema.getInputSchema(seasonID, sessionToken);
        JSONObject jsonSchema = new JSONObject(sch);
        String schemaBody = FileUtils.fileToString(filePath + "inputSchemas/inputSchema_optionalField_unitsOfMeasure.txt", "UTF-8", false);
        jsonSchema.put("inputSchema", new JSONObject(schemaBody));
        String response = schema.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
        Assert.assertFalse(response.contains("error"), "Schema was not added to the season" + response);

	}
	@Test (dependsOnMethods="addComponents", description="Add feature to analytics using separate api, validate lastModified in analytics object")
	public void addFeatureToAnalytics() throws IOException, JSONException, InterruptedException{
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		String response = an.addFeatureToAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not added to featuresOnOff");
		
		String newResponse = an.updateGlobalDataCollection(seasonID, branchID, analytics, sessionToken);
		Assert.assertTrue(newResponse.contains("error"), "LastModified was not updated after adding feature to featuresOnOff");
				
	}
	
	@Test (dependsOnMethods="addFeatureToAnalytics", description="Add input Field to analytics using separate api, validate lastModified in analytics object")
	public void addInputFieldToAnalytics() throws IOException, JSONException, InterruptedException{
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray inputFields = new JSONArray();
		inputFields.put("context.device.locale");
		String response = an.updateInputFieldToAnalytics(seasonID, branchID, inputFields.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Input field was not added to analytics" + response);

		
		String newResponse = an.updateGlobalDataCollection(seasonID, branchID, analytics, sessionToken);
		Assert.assertTrue(newResponse.contains("error"), "LastModified was not updated after adding feature to featuresOnOff");
				
	}

	@Test (dependsOnMethods="addInputFieldToAnalytics", description="Add attributes to analytics using separate api, validate lastModified in analytics object")
	public void addAttributesToAnalytics() throws IOException, JSONException, InterruptedException{
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);

		String response = an.addAttributesToAnalytics(featureID1, branchID, attributes.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Attributes were not added to analytics" + response);

		
		String newResponse = an.updateGlobalDataCollection(seasonID, branchID, analytics, sessionToken);
		Assert.assertTrue(newResponse.contains("error"), "LastModified was not updated after adding feature to featuresOnOff");
				
	}
	
	@Test (dependsOnMethods="addAttributesToAnalytics", description="Delete feature from analytics using separate api, validate lastModified in analytics object")
	public void deleteFeatureFromAnalytics() throws IOException, JSONException, InterruptedException{
		String analytics = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);

		String response = an.deleteFeatureFromAnalytics(featureID1, branchID, sessionToken);
		Assert.assertFalse(response.contains("error"), "feature was not deleted from featuresOnOff");
		
		String newResponse = an.updateGlobalDataCollection(seasonID, branchID, analytics, sessionToken);
		Assert.assertTrue(newResponse.contains("error"), "LastModified was not updated after adding feature to featuresOnOff");
				
	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
