package tests.restapi.scenarios.feature;

import java.io.IOException;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;

public class AddFeatureToNonExistingSeason {
	protected String seasonID;
	protected String featureID;
	protected String filePath;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		seasonID = "a11a1a11-1a11-4520-9770-b2916b8278d0";
		filePath = configPath + "feature2.txt";
		f = new FeaturesRestApi();
		f.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		
	}
	
	@Test (description = "Add feature to a non existing season")
	public void addFeatureToNonExistingSeason() throws IOException{
		
		String feature = FileUtils.fileToString(filePath, "UTF-8", false);
		String response = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		 Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}

}
