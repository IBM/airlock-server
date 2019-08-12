package tests.restapi.unitest;

import java.io.IOException;





import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.SeasonsRestApi;
import tests.restapi.ProductsRestApi;

public class SeasonBasicTest {
	
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		seasonID = "";
		filePath = configPath + "season1.txt";
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		s = new SeasonsRestApi();
		s.setURL(url);
 		
	}
	
	@Test
	public void testGetAllSeasons(){
		s.getAllSeasons(sessionToken);
	}
	
	@Test
	public void testAddSeason() throws IOException{
			String seasonJson = FileUtils.fileToString(filePath, "UTF-8", false);
			seasonID = s.addSeason(productID, seasonJson, sessionToken);
	}
	
	@Test (dependsOnMethods="testAddSeason")
	public void testDeleteSeason(){
		s.deleteSeason(seasonID, sessionToken);
 			
	}
	
	@AfterTest 
	public void reset(){
		baseUtils.reset(productID, sessionToken);
 			
	}

}
