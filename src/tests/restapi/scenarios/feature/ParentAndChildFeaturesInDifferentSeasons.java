package tests.restapi.scenarios.feature;

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
import tests.restapi.SeasonsRestApi;


public class ParentAndChildFeaturesInDifferentSeasons {
	protected String seasonID;
	protected String seasonID2;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
 	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		String season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false); 
		seasonID = s.addSeason(productID, season, sessionToken);
		String season2 = FileUtils.fileToString(filePath + "season2.txt", "UTF-8", false); 
		seasonID2 = s.addSeason(productID, season2, sessionToken);

	}



	/**
	 * Parent and child features are not in the same season both in create and update feature - not allowed actions
	 * 
	 */
	@Test (description = "Parent and child features are not in the same season")
	public void testAddParent() throws JSONException{
		try {			
			String parent = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			String parentID = f.addFeature(seasonID, parent, "ROOT", sessionToken);
			
			//create a child feature  child
			String child = FileUtils.fileToString(filePath + "parent-feature.txt", "UTF-8", false);
			String response = f.addFeature(seasonID2, child, parentID, sessionToken );
			Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
