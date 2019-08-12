package tests.restapi.validations.utilities;

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.UtilitiesRestApi;

public class UtilityCreateValidateEmptyFields {
	protected String seasonID;
	protected String utilityID;
	protected String filePath;
	protected UtilitiesRestApi u;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	protected String utility;
	private String sessionToken = "";
	private AirlockUtils baseUtils;

	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		u = new UtilitiesRestApi();
		u.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);		
		//utility = FileUtils.fileToString(filePath + "utilities" + File.separator + "utility1.txt", "UTF-8", false); 

	}
	
	/*
	private String utility = null; //required in create and update
	private Stage stage = null; //required in create and update
	private UUID uniqueId = null;//required update. forbidden in create
	private Date lastModified = null; // required in update. forbidden in create
	private String minAppVersion = null; //required in create and in update
	private UUID seasonId = null; //required in create and update
	*/


	@Test 
	public void emptyUtility() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.put("utility", "");
		addUtility(props);
	}
	
	@Test 
	public void emptyStage() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.put("stage", "");
		addUtility(props);
	}

	//minAppVersion is removed from utilities
	/*
	@Test 
	public void emptyMinAppVersion() throws Exception{
		Properties props = getPropValues(filePath + "utilities" + File.separator + "utility1.txt");
		props.put("minAppVersion", "");
		addUtility(props);
	}*/


	private void addUtility(Properties utilProps) throws IOException, JSONException{
		String response = u.addUtility(seasonID, utilProps, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	
	public Properties getPropValues(String configPath) throws IOException {
		Properties props = new Properties();
		try {
			
			String propFileName = configPath;
			InputStream inputStream = new FileInputStream(propFileName);
			props.load(inputStream);
			
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} 
		return props;
	}

	

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
