package tests.restapi.scenarios.mutual_exclusion_configuration;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;


public class AddConfigurationToRegularMutuallyExclusive {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
 		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, parent, "ROOT", sessionToken);

	}
	
	/**
	 * Create mutually exclusive group
	 */
	@Test (description = "Create configuration rule under a regular mutually exclusive group - not allowed")
	public void createMutuallyExclusiveWithFeature() throws JSONException{
		try {
			
			String child1 = FileUtils.fileToString(filePath+"configuration_rule1.txt", "UTF-8", false);
			childID1 = f.addFeature(seasonID, child1, featureID, sessionToken);
			Assert.assertTrue(childID1.contains("error"), "Test should fail, but instead passed: " + childID1 );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
