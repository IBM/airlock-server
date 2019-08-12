package tests.restapi.multiusers;

import java.io.IOException;

import java.util.List;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class FeatureDeleteAndUpdate {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected String feature;
	protected List<Integer> actualResult;
	protected int iteration = 1;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		
		seasonID = baseUtils.createSeason(productID);
		
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);
		featureID = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		feature = f.getFeature(featureID, sessionToken);
	}
	
	//simultaneous delete and update
	@Test(threadPoolSize = 2, invocationCount = 2, timeOut = 10000, description = "Simultaneously delete one feature and update another")
	public void udpateAndDelete() throws InterruptedException, IOException{
   	 	iteration = iteration + 1;
        Thread.currentThread();	
        Thread.sleep(500);
        if (iteration % 2 == 0) {
        	 f.deleteFeature(featureID, sessionToken);        	
        }	 
        else  {
        	 f.updateFeature(seasonID, featureID, feature, sessionToken);
        }	
	}
	   
	   
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
