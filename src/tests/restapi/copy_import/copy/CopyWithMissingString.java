package tests.restapi.copy_import.copy;

import java.io.IOException;




import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StringsRestApi;

public class CopyWithMissingString {
	protected String seasonID;
	protected String seasonID2;
	protected String productID;
	protected String productID2;
	protected String featureID1;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	protected SeasonsRestApi s;
	protected StringsRestApi t;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String m_translationsUrl;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		m_translationsUrl = translationsUrl;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		t = new StringsRestApi();
		t.setURL(m_translationsUrl);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
		this.runOnMaster = runOnMaster;
	}
	
	@Test(description = "Add string")
	public void addString() throws Exception{
		
		String str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		t.addString(seasonID, str, sessionToken);
		
	}
	
	@Test(dependsOnMethods = "addString", description = "dd feature with configuration using string")
	public void addFeature() throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature, "ROOT", sessionToken );
		String configRule = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		JSONObject crJson = new JSONObject(configRule);
		String configuration =  "{ \"text\" :  translate(\"app.hello\", \"testing string\")	}" ;		
		crJson.put("configuration", configuration);
		String response = f.addFeatureToBranch(seasonID, srcBranchID, crJson.toString(), featureID1, sessionToken );
		Assert.assertFalse(response.contains("error"), "Test should pass, but instead failed: " + response );
	}
	
	@Test (dependsOnMethods = "addFeature", description="Create new product and season without utility")
	public void createNewProduct() throws IOException, JSONException{

		String product = FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false);
		product = JSONUtils.generateUniqueString(product, 5, "codeIdentifier");
		product = JSONUtils.generateUniqueString(product, 8, "name");
		productID2 = p.addProduct(product, sessionToken);
		baseUtils.printProductToFile(productID2);
		
		//add season to second product
		JSONObject sJson = new JSONObject();
		sJson.put("minVersion", "2.0");		
		seasonID2 = s.addSeason(productID2, sJson.toString(), sessionToken);
		Assert.assertFalse(seasonID2.contains("error"), "The season was not created in the new product: " + seasonID2);
		

		try {
			if (runOnMaster) {
				destBranchID = BranchesRestApi.MASTER;
			} else {
				baseUtils.setSeasonId(seasonID2);
				destBranchID = baseUtils.addBranch("b1");
			}
		}catch (Exception e){
			destBranchID = null;
		}

	}
	
	@Test (dependsOnMethods="createNewProduct", description="Copy feature to the new season with missing string")
	public void simulateCopyFeatureToRoot() throws IOException{
		
		String rootId = f.getBranchRootId(seasonID2, destBranchID, sessionToken);
		String response = f.copyItemBetweenBranches(featureID1, rootId, "VALIDATE", null, null, sessionToken, srcBranchID, destBranchID);
		Assert.assertTrue(response.contains("\"addedStrings\":[\"app.hello\"]"), "Missing assests were not recognized ");
	}
	
	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
		baseUtils.reset(productID2, sessionToken);
	}

}