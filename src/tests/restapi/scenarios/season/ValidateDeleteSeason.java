package tests.restapi.scenarios.season;

import java.io.IOException;







import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class ValidateDeleteSeason {
	
	protected String productID;
	protected String seasonID;
	protected String filePath;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
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
	
	
	@Test (description = "Validate delete season")
	public void testDeleteSeason() throws IOException{
			String seasonJson = FileUtils.fileToString(filePath, "UTF-8", false);
			seasonID = s.addSeason(productID, seasonJson, sessionToken);
			int responseCode = s.deleteSeason(seasonID, sessionToken);
			responseCode = s.deleteSeason(seasonID, sessionToken);
			Assert.assertEquals(responseCode, 404, "Should fail when deleting a season that was already deleted");
 			
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
