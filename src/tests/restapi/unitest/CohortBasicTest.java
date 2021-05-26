package tests.restapi.unitest;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.CohortsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

import java.io.IOException;

public class CohortBasicTest {
	
	protected String productID;
	protected String cohortID;
	protected String filePath;
	protected String json;
	protected CohortsRestApi c;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath + "cohort1.txt";
		c = new CohortsRestApi();
		c.setUrl(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

	}
	
	@Test 
	public void testGetAllCohorts(){
		c.getAllCohorts(productID, sessionToken);
	}
	
	@Test
	public void testAddCohort(){
		try {
			String cohortJson = FileUtils.fileToString(filePath, "UTF-8", false);
			cohortID = c.createCohort(productID, cohortJson, sessionToken);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 			
	}
	
	@Test (dependsOnMethods="testAddCohort")
	public void testGetCohort(){
		json = c.getCohort(cohortID, sessionToken);
 			
	}
	
	@Test (dependsOnMethods="testGetCohort")
	public void testGetCohortsByProduct(){
		c.getAllCohorts(productID, sessionToken);
 			
	}
	
	@Test (dependsOnMethods="testGetCohortsByProduct")
	public void testUpdateCohort() throws JSONException, IOException{

			JSONObject cohortJson = new JSONObject(json);
			cohortJson.put("name", "New name");
			c.updateCohort(cohortID, cohortJson.toString(), sessionToken);
	 			
	}
	
	@Test (dependsOnMethods="testUpdateCohort")
	public void testDeleteCohort(){
		c.deleteCohort(cohortID, sessionToken);
 			
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
		
}
