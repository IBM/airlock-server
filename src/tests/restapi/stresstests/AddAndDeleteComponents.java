package tests.restapi.stresstests;

import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;

public class AddAndDeleteComponents {
	
	protected String seasonID;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	protected String feature;
	protected AirlockUtils baseUtils;
	protected String season;
	private String sessionToken = "";
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		s = new SeasonsRestApi();
		s.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		season = FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false);
	}
	
	/*
	 * add 30 products
	 * for each product add 30 seasons
	 * for each season add 100 features
	 */
	@Test
	public void addComponents() throws IOException, JSONException{
		   DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		   Date date = new Date();
		   System.out.println("Test started: " + dateFormat.format(date));
		for (int i=1; i<30; i++){
			//System.out.println("Product #" + i);
			productID = baseUtils.createProduct();
			for(int j=1; j<30; j++){
				//System.out.println("      Season #" + j); 
				JSONObject seasonJson = new JSONObject(season);
				seasonJson.put("minVersion", j);
				seasonID = s.addSeason(productID, seasonJson.toString(), sessionToken);
					for(int k=1; k<100; k++){
						System.out.println("Product #" + i + "      Season #" + j + "      Feature #" + k);
						baseUtils.createFeature(seasonID);									
					}		
			}
		}
		date = new Date();
		System.out.println("Test end: " + dateFormat.format(date));
	}
	
	@Test (dependsOnMethods = "addComponents")
	public void getProducts(){
		p.getAllProducts(sessionToken);
	}
	
	
	@Test (dependsOnMethods = "getProducts")
	public void deleteProducts() throws JSONException{
		String response = p.getAllProducts(sessionToken);
		JSONObject products = new JSONObject(response);
		JSONArray allProducts = products.getJSONArray("products");
		for(int i=0; i<allProducts.size(); i++){
			productID = allProducts.getJSONObject(i).getString("uniqueId");
			p.deleteProduct(productID, sessionToken);
		}
	}
}
