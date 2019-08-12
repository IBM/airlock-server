package tests.restapi.stresstests;

import java.io.IOException;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class CreateDeepHierarchy {
	protected String seasonID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	protected String feature;
	protected AirlockUtils baseUtils;
	protected String m_url;	
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		s = new SeasonsRestApi();
		s.setURL(m_url);
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
	}
	
	@Test
	public void createHierarchyOfFeatures() throws IOException{
		feature = JSONUtils.generateUniqueString(feature, 8, "name");
		String featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		for (int i=0; i < 10; i++){
			feature = JSONUtils.generateUniqueString(feature, 8, "name");
			featureID  = f.addFeature(seasonID, feature, featureID, sessionToken);
		}
	}
	
	@Test (dependsOnMethods = "createHierarchyOfFeatures")
	public void getSeason() throws Exception{
		s.getSeason(productID, seasonID, sessionToken);
	}
	
	
	@Test (dependsOnMethods = "getSeason")
	public void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
