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


public class AddInvalidAttributeToAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
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
	
	/*Test flow:
	 * add non-existing feature id
	 * add non-existing attribute to analytics
	 * add feature id without attributes (empty attributes)
	 * add non-unique featureId
	 * add the same attribute several times
	 * 
	 */
	
	@Test (description="Add a non-existing feature")
	public void addNonExistingFeature() throws IOException, JSONException{
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertFalse(response.contains("error"), "Incorrect globalDataCollection response");
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, "1306a855-40d4-40c5-85a7-f85259caaaaa", attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added non-existing feature to analytics" + response);
		
	}
	
	@Test (dependsOnMethods="addNonExistingFeature", description="Add feature and add non-existing attribute to analytics")
	public void addNonExistingAttribute() throws IOException, JSONException, InterruptedException{
		
		//add feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		//add feature and non-existing attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added non-existing feature to analytics" + response);

	}
	
	@Test (dependsOnMethods="addNonExistingAttribute", description="Add feature to analytics attribute with empty attribute value")
	public void addEmptyAttribute() throws IOException, JSONException, InterruptedException{
		
		//add feature and empty attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, new JSONArray());
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Added empty feature to analytics" + response);
	}

	@Test (dependsOnMethods="addEmptyAttribute", description="Add the same feature twice")
	public void addNonUniqueFeatureId() throws IOException, JSONException, InterruptedException{
		
		//add configuration
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		String configID = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season" + configID);
	
		//add feature and attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);
		
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken); 

		attributes.add(attr1);
		input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "The same feature was added twice to analytics:");
		
	}
	
	@Test (dependsOnMethods="addNonUniqueFeatureId", description="Add the same attribute twice")
	public void addNonUniqueAttribute() throws IOException, JSONException, InterruptedException{
		
	
		//add feature non unique attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "The same attribute was added twice to analytics:");
		

	}
	
	@Test (dependsOnMethods="addNonUniqueAttribute", description="Add attribute without type")
	public void addAttributeNoType() throws IOException, JSONException, InterruptedException{
		
	
		//add feature non unique attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "Attribute without type field added twice to analytics:");
		

	}
	
	@Test (dependsOnMethods="addAttributeNoType", description="Add attribute without name")
	public void addAttributeNoName() throws IOException, JSONException, InterruptedException{
		
	
		//add feature non unique attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "The attribute without name added to analytics:");
		

	}
	
	@Test (dependsOnMethods="addAttributeNoName", description="Add attribute with invalid structure")
	public void addAttributeInvalidStucture() throws IOException, JSONException, InterruptedException{		
	
		//add feature non unique attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		attributes.add("color");
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "Attribute with invalid structure added  to analytics:" + response);
		

	}
	
	@Test (dependsOnMethods="addAttributeInvalidStucture", description="Add attribute with invalid type")
	public void addAttributeInvalidType() throws IOException, JSONException, InterruptedException{
		
	
		//add feature non unique attribute to analytics
		String response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "TYPE");
		attributes.add(attr1);
		String input = an.addFeaturesAttributesToAnalytics(response, featureID1, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		//response = an.getGlobalDataCollection(seasonID, branchID, false, sessionToken); 
		Assert.assertTrue(response.contains("error"), "Attribute with invalid type field added to analytics");
		

	}
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
