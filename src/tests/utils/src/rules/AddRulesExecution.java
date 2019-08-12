package tests.utils.src.rules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.RestClientUtils;

public class AddRulesExecution {
	protected Properties properties;
	private static String product = "";
	private static String seasonMinVer = "";
	private static String rulesFile = "";
	private static String url = "";
	private static String sessionToken = "";
	private static String featureTemplate = "";
	private static JSONObject json = new JSONObject();
	
	
	public AddRulesExecution(Properties props) {
		properties = props;
	}
	
	public void doExecute(){
		validateProperties();
		doCreate();
	}
	
	private void validateProperties() {
		if (properties.getProperty("product") != null)
			product = properties.getProperty("product");
		
		if (properties.getProperty("seasonMinVer") != null)
			seasonMinVer = properties.getProperty("seasonMinVer");
		
		if (properties.getProperty("url") != null)
			url = properties.getProperty("url");
		
		if (properties.getProperty("sessionToken") != null)
			sessionToken = properties.getProperty("sessionToken");
		
		if (properties.getProperty("rulesFile") != null)
			rulesFile = properties.getProperty("rulesFile");
		
		if (properties.getProperty("featureTemplate") != null)
			featureTemplate = properties.getProperty("featureTemplate");
		
	}
	
	
	private void doCreate(){
		if (url.equals("")){
			System.out.println("URL of the new environment was not provided.");
			return;
		}
		
		if (featureTemplate.equals("")){
			System.out.println("Feature template file was not provided.");
			return;
		}else{
			try {
				String feature = FileUtils.fileToString(featureTemplate, "UTF-8", false);
				json = new JSONObject(feature);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if (rulesFile.equals("")){
			System.out.println("Rules file was not provided.");
			return;
		} 
		
		if (seasonMinVer.equals("")){
			System.out.println("Season's minVersion was not provided.");
			return;
		}
		
		
		try {
			//existing product +  new season
			UUID.fromString(product);
			String seasonID = createSeason(product, seasonMinVer);
			readRules(seasonID, rulesFile);
		}catch (Exception e){
			if (product.equals("")) {
				System.out.println("Provide either a product id to use an existing product or a new product name.");
			} else	{
				//new product + new season
				try {
					String productID = createProduct(product);
					String seasonID = createSeason(productID, seasonMinVer);
					readRules(seasonID, rulesFile);
				} catch (JSONException e1) {
					System.out.println("Can't process json: " + e1.getLocalizedMessage());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}	
		}

	}
	
	
	private static void createFeature(String seasonID, String rule, String featureName) throws JSONException, IOException{
		JSONObject ruleObj = new JSONObject();
		rule = rule.replaceAll("\"", "'");
		ruleObj.put("ruleString", rule);
		json.put("rule", ruleObj);
		json.put("name", featureName);
		RestClientUtils.sendPost(url+"/products/seasons/" + seasonID + "/features?parent=ROOT", json.toString());
  }
	
	
	private static void readRules(String seasonID, String filename) {
	    try {
	    	int count = 1;
	    	
	        @SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(filename));
	        //StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = br.readLine()) != null) {
	        	if (line != "") {
	        		String featureName = "feature" + count;
	        		createFeature(seasonID, line, featureName);
	        		count++;
	        	}	
	         }
	    } catch(Exception e) {
	    	System.out.println("Season file not found: "  + rulesFile);
	    }

	    System.out.println("DONE!");
	}
	

	
	
	private String createProduct(String productName) throws JSONException, IOException{
		JSONObject product = new JSONObject();
		product.put("name", productName);
		product.put("codeIdentifier", productName);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products", product.toString(), sessionToken);
		String result = res.message;
		String productID = parseResultId(result);
		return productID;
	}

	
	private String createSeason(String productID, String minVersion) throws JSONException, IOException{
		JSONObject season = new JSONObject();
		season.put("minVersion", minVersion);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(url+"/products/" + productID + "/seasons/", season.toString(), sessionToken);
		String result = res.message;
		String seasonID = parseResultId(result);
		return seasonID;
	}
	
	private String parseResultId(String result){
		String uniqueID = "";
		JSONObject response;
		try {
			response = new JSONObject(result);
			uniqueID = (String)response.get("uniqueId");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			uniqueID = result;
			System.out.println("An exception was thrown when trying  to add a component. Message: "+ uniqueID) ;
		}

		return uniqueID;

	}
	
	
}
