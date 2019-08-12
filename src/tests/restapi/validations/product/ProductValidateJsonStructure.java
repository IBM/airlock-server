package tests.restapi.validations.product;

import java.io.IOException;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;

public class ProductValidateJsonStructure {

	protected String featureID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		
		filePath = configPath;
		m_url = url;
		p = new ProductsRestApi();
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

	}
	
	@Test
	public void ValidateMissingBracket() throws IOException{
		String feature = FileUtils.fileToString(filePath + "ProductValidateJsonStructure_MissingBrackets.txt", "UTF-8", false);
		addProduct(feature);
	}
	
	@Test
	public void ValidateMissingQuotes() throws IOException{
		String feature = FileUtils.fileToString(filePath + "ProductValidateJsonStructure_MissingComma.txt", "UTF-8", false);
		addProduct(feature);
	}

	
	private void addProduct(String productJson){

		try {
			String response = p.addProduct(productJson, sessionToken);
			Assert.assertTrue(response.contains("error"), "Illegal json product was created " + response );
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
