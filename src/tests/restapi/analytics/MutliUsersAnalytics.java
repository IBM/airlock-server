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


public class MutliUsersAnalytics {
	protected String seasonID;
	protected String branchID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	
	protected int iteration = 0;

		
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
	
	@Test(description="Add components")
	public void addComponents() throws IOException, JSONException, InterruptedException{
		
		//add feature
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, branchID, feature, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		//add featureID1 to analytics featureOnOff
		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);		
		String input = an.addFeatureOnOff(anResponse, featureID1);
		String response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics");
		
		//create feature2
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, branchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");

		//add configuration to feature2
		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject jsonConfig = new JSONObject(config);
		JSONObject newConfiguration = new JSONObject();
		newConfiguration.put("color", "red");
		newConfiguration.put("size", "small");
		jsonConfig.put("configuration", newConfiguration);
		configID1 = f.addFeatureToBranch(seasonID, branchID, jsonConfig.toString(), featureID2, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		//add feature2 and attribute to analytics
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		JSONArray attributes = new JSONArray();
		JSONObject attr1 = new JSONObject();
		attr1.put("name", "color");
		attr1.put("type", "REGULAR");
		attributes.add(attr1);
		JSONObject attr2 = new JSONObject();
		attr2.put("name", "size");
		attr2.put("type", "REGULAR");
		attributes.add(attr2);
		input = an.addFeaturesAttributesToAnalytics(response, featureID2, attributes);
		response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
		Assert.assertFalse(response.contains("error"), "Feature was not added to analytics" + response);

	}	
	
	//simultaneous Delete feature/analytics/update config
	@Test(threadPoolSize = 3, invocationCount = 3, timeOut = 10000, dependsOnMethods="addComponents", description = "Simultaneously delete one feature and update another")
	public void actions() throws InterruptedException, IOException, JSONException{
		//Thread.sleep(500);
		iteration ++;
 
		if (iteration == 1) {
        	//add feature2 to featureOnOff
        	String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);		
    		String input = an.addFeatureOnOff(anResponse, featureID2);
    		String response = an.updateGlobalDataCollection(seasonID, branchID, input, sessionToken);
    		//System.out.println("iteration add feature2 to featureOnOff ="+iteration + "   response: " + response);
        }	 
        else if (iteration == 2) {
        	//update configuration reported to analytics
    		String configuration = f.getFeatureFromBranch(configID1, branchID, sessionToken);
    		JSONObject jsonConfig = new JSONObject(configuration);
    		JSONObject newConfiguration = new JSONObject();
    		newConfiguration.put("color", "white");
    		newConfiguration.put("newsize", "medium");
    		jsonConfig.put("configuration", newConfiguration);
    		jsonConfig.put("rolloutPercentage", 50);
    		String response = f.updateFeatureInBranch(seasonID, branchID, configID1, jsonConfig.toString(), sessionToken);
    		//System.out.println("iteration update configuration="+iteration + "   response: " + response);
    		String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
    		//System.out.println("iteration update configuration="+iteration + " analytics  response: " + anResponse);

        } else if (iteration == 3){
        	//delete feature1 reported to analytics
        	int response = f.deleteFeatureFromBranch(featureID1, branchID, sessionToken);
        	//System.out.println("iteration delete feature1=" + iteration + "   response: " + response);
        	String anResponse = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
    		//System.out.println("iteration delete feature1="+iteration + " analytics  response: " + anResponse);

        }
	}
	   
	   
	@AfterTest()
	   public void validate(){
		baseUtils.reset(productID, sessionToken);
	   }

}
