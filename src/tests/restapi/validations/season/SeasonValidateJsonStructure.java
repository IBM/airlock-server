package tests.restapi.validations.season;

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

public class SeasonValidateJsonStructure {
	protected String seasonID;
	protected String productID;
	protected String filePath;
	protected String m_url;
	protected SeasonsRestApi s;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		m_url = url;
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		
		s = new SeasonsRestApi();
		s.setURL(m_url);
 		
	}
	
	@Test
	public void ValidateMissingBracket() throws IOException{
		String feature = FileUtils.fileToString(filePath + "ProductValidateJsonStructure_MissingBrackets.txt", "UTF-8", false);
		String response = s.addSeasonNoEncManipulations(productID, feature, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal input JSON"), "Test should fail, but instead passed: " + response );
	}
	
	@Test
	public void ValidateMissingQuotes() throws IOException{
		String feature = FileUtils.fileToString(filePath + "ProductValidateJsonStructure_MissingComma.txt", "UTF-8", false);
		String response = s.addSeasonNoEncManipulations(productID, feature, sessionToken);
		Assert.assertTrue(response.contains("error") && response.contains("Illegal input JSON"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}


}
