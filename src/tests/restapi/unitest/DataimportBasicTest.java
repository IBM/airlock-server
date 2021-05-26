package tests.restapi.unitest;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.DataimportRestApi;
import tests.restapi.ProductsRestApi;

import java.io.IOException;

public class DataimportBasicTest {
	
	protected String productID;
	protected String jobID;
	protected String filePath;
	protected String json;
	protected DataimportRestApi c;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath + "cohort1.txt";
		c = new DataimportRestApi();
		c.setUrl(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);

	}
	
	@Test 
	public void testGetAllJobs(){
		c.getAllJobs(productID, sessionToken);
	}
	
	@Test
	public void testAddJob(){
		try {
			String jobJSON = FileUtils.fileToString(filePath, "UTF-8", false);
			jobID = c.createJob(productID, jobJSON, sessionToken);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 			
	}
	
	@Test (dependsOnMethods="testAddJob")
	public void testGetJob(){
		json = c.getJob(jobID, sessionToken);
 			
	}
	
	@Test (dependsOnMethods="testGetJob")
	public void testGetJobsByProduct(){
		c.getAllJobs(productID, sessionToken);
 			
	}

	@Test (dependsOnMethods="testGetJobsByProduct")
	public void testDeleteJob(){
		c.deleteJob(jobID, sessionToken);
 			
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
		
}
