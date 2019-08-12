package tests.restapi.validations.feature;

import java.io.IOException;







import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureValidateJsonStructure {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		m_url = url;
		f = new FeaturesRestApi();
		f.setURL(url);
 		
	}
	
	@Test
	public void ValidateMissingBracket() throws IOException{
		String feature = FileUtils.fileToString(filePath + "FeatureValidateJsonStructure_MissingBrackets.txt", "UTF-8", false);
		String response = f.addFeature(seasonID,feature, "ROOT", sessionToken);
		
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@Test
	public void ValidateMissingQuotes() throws IOException{
		String feature = FileUtils.fileToString(filePath + "FeatureValidateJsonStructure_MissingComma.txt", "UTF-8", false);
		String response = f.addFeature(seasonID,feature, "ROOT", sessionToken);
		
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );

	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
