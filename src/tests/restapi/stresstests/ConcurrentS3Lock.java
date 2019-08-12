package tests.restapi.stresstests;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.com.ibm.qautils.JSONUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InputSchemaRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;
import tests.restapi.StreamsRestApi;
import tests.restapi.StringsRestApi;
import tests.restapi.TranslationsRestApi;
import tests.restapi.UtilitiesRestApi;


public class ConcurrentS3Lock {
		   
	protected FeaturesRestApi featureApi;
	protected ProductsRestApi p;
	protected String feature;
	private String config;
	protected List<Integer> actualResult = new ArrayList<Integer>();
	protected int iteration=0;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
    protected InputSchemaRestApi schemaApi;
    protected UtilitiesRestApi utilitiesApi;
    protected TranslationsRestApi translationApi;
    protected AnalyticsRestApi analyticsApi;
    protected StringsRestApi stringApi;
    protected StreamsRestApi streamApi;
    protected SeasonsRestApi seasonApi;
    private List<String[]> products = new ArrayList<String[]>();
 		
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		config = configPath;
		p = new ProductsRestApi();
		p.setURL(url);
		seasonApi = new SeasonsRestApi();
		seasonApi.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		
		featureApi = new FeaturesRestApi();
		featureApi.setURL(url);
		feature = FileUtils.fileToString(configPath + "feature1.txt", "UTF-8", false);

		
        schemaApi = new InputSchemaRestApi();
        utilitiesApi = new UtilitiesRestApi();
        stringApi = new StringsRestApi();
        translationApi = new TranslationsRestApi();
        analyticsApi = new AnalyticsRestApi();
        streamApi = new StreamsRestApi();
        schemaApi.setURL(url);
        utilitiesApi.setURL(url);
        stringApi.setURL(translationsUrl);
        translationApi.setURL(translationsUrl);
        analyticsApi.setURL(analyticsUrl);
        streamApi.setURL(url);

	}

	   
//	   @Test(threadPoolSize = 50, invocationCount = 50, timeOut = 10000, description = "Simultaneously create products/seasons/features")
	    public void addFeature() throws IOException, InterruptedException, JSONException {
		    
			long startTime = System.currentTimeMillis();
			System.out.println("Start product #" + iteration);
	        iteration++;
			
			String productID = baseUtils.createProduct();
			baseUtils.printProductToFile(productID);
			
			String seasonID = baseUtils.createSeason(productID);
			
	        feature = JSONUtils.generateUniqueString(feature, 5, "name");
	        String featureID =  featureApi.addFeature(seasonID, feature, "ROOT", sessionToken);
	        
	        if (iteration%2 == 0) {
	 		   String featureString = "";
			   featureString = featureApi.getFeature(featureID, sessionToken);
			   JSONObject json = new JSONObject(featureString);
			   json.put("description", "new description");
			   featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);	
			   printTime(productID, startTime);
				 
			 } else  {
				 featureApi.deleteFeature(featureID, sessionToken);
					printTime(productID, startTime);
			} 
	        	        

	    }
	    
	    
	  @Test(threadPoolSize = 50, invocationCount = 50,  description="Create product & season") 
	    
	     public void addProduct() throws IOException, JSONException {

			String productID = baseUtils.createProduct();
			baseUtils.printProductToFile(productID);			
			String seasonID = baseUtils.createSeason(productID);

			products.add(new String[]{productID, seasonID});
	     }
	  
	  @Test(dependsOnMethods="addProduct", threadPoolSize = 50, invocationCount = 50, description="Actions") 
	  public void actions() throws IOException{
		/*	String productID = baseUtils.createProduct();
			baseUtils.printProductToFile(productID);			
			String seasonID = baseUtils.createSeason(productID);
			
		//	System.out.println("product: " + productID + " season: " + seasonID);
		*/
		  String productID = products.get(iteration)[0];
		  String seasonID = products.get(iteration)[1];
		  iteration++;
		  long startTime = System.currentTimeMillis();	
		  
		  //System.out.println("product: " + productID + " season: " + seasonID);
		  addInputSchema(seasonID);
		  addUtility(seasonID);
		  addString(seasonID);
		  String featureID = addFeature(seasonID);
		  addConfiguration(featureID, seasonID);
		  addAnalytics(featureID, seasonID);
		 // String seasonID2 = addSeason2(productID);
		 // System.out.println("product: " + productID + " season2: " + seasonID2);
		  String featureID2 = addFeature(seasonID);
		 //copyFeatures(featureID, seasonID2);
		  updateSchemaProd(seasonID);
		  moveFeatureToProd(seasonID, featureID);
		  String streamID = createStream(seasonID);
		  updateStream(streamID);
		  deleteStream(streamID);
		  deleteFeature(featureID2);
		  
		  printTime(productID, startTime);
	  }
	  


	   //  @Test(dependsOnMethods = "actions", description = "Delete products")
	     public void deleteProducts() throws Exception {
	    	 for (int i=0; i < products.size(); i++){
	    		 String productID = products.get(i)[0];
	    		 baseUtils.reset(productID, sessionToken);
	    	 }	 
	        
	     }

	     
	     private void addInputSchema(String seasonID)  {


			try {
				 String file = FileUtils.fileToString(config + "inputSchema.txt", "UTF-8", false);
		         String schema = schemaApi.getInputSchema(seasonID, sessionToken);
		         JSONObject is = new JSONObject(file);
		         JSONObject jsonSchema = new JSONObject(schema);
		         jsonSchema.put("inputSchema", is);
		         schemaApi.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


	     }

	     private void addUtility(String seasonID)  {
	    	 try {
		         String utility = FileUtils.fileToString(config + "/utilities/utility1.txt", "UTF-8", false);
		         Properties utilProps1 = new Properties();
		         utilProps1.load(new StringReader(utility));
		         utilitiesApi.addUtility(seasonID, utilProps1, sessionToken);

	    	 }   catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}

	     }

	   
	     private void addString(String seasonID) {
	    	 try{
		         String str = FileUtils.fileToString(config + "/strings/string2.txt", "UTF-8", false);
		         String stringID = stringApi.addString(seasonID, str, sessionToken);
		         translationApi.markForTranslation(seasonID, new String[]{stringID}, sessionToken);
		         translationApi.reviewForTranslation(seasonID, new String[]{stringID}, sessionToken);
		         translationApi.sendToTranslation(seasonID, new String[]{stringID}, sessionToken);
		         String frTranslation = FileUtils.fileToString(config + "strings/translationFR.txt", "UTF-8", false);
		         translationApi.addTranslation(seasonID, "fr", frTranslation, sessionToken);
	    	 } catch (Exception e) {
	    		 e.printStackTrace();
	    	 }

	     }

	     private String addFeature(String seasonId){
	    	 String featureId="";
	    	 try {
		         String feature = FileUtils.fileToString(config + "feature1.txt", "UTF-8", false);
	
		         JSONObject fJson = new JSONObject(feature);
		         fJson.put("name", RandomStringUtils.randomAlphabetic(5));
		         fJson.put("minAppVersion", "1.1.1");
		         JSONObject rule = new JSONObject();
		         rule.put("ruleString", "context.viewedLocation.country == isTrue()");
		         fJson.put("rule", rule);
		         featureId = featureApi.addFeature(seasonId, fJson.toString(), "ROOT", sessionToken);
	    	 } catch (Exception e) {
	    		 e.printStackTrace();
	    	 }
	         return featureId;
	     }


	     private void addConfiguration(String parentID, String seasonID)  {
	    	 try {
		         String configuration = FileUtils.fileToString(config + "configuration_rule1.txt", "UTF-8", false);
		         JSONObject jsonConfig = new JSONObject(configuration);
		         JSONObject newConfiguration = new JSONObject();
		         newConfiguration.put("color", "red");
		         newConfiguration.put("minAppVersion", "0.1");
		         jsonConfig.put("configuration", newConfiguration);
		         featureApi.addFeature(seasonID, jsonConfig.toString(), parentID, sessionToken);
	    	 }   catch (Exception e) {
	    		 e.printStackTrace();
	    	 }
	     }

	     
	     private void addAnalytics(String featureID, String seasonID) {
	    	 try {
		         analyticsApi.addFeatureToAnalytics(featureID, BranchesRestApi.MASTER, sessionToken);
	
		         JSONArray inputFields = new JSONArray();
		         //field in DEVELOPMENT stage
		         inputFields.put("context.device.locale");
		         analyticsApi.updateInputFieldToAnalytics(seasonID, BranchesRestApi.MASTER, inputFields.toString(), sessionToken);
	    	 } catch (Exception e) {
	    		 e.printStackTrace();
	    	 }    
	     }

	     private String  addSeason2(String productID) {
	    	 String seasonID2 = "";
	    	 try {
	    		 String season = FileUtils.fileToString(config + "season2.txt", "UTF-8", false);
	    		 seasonID2 = seasonApi.addSeason(productID, season, sessionToken);
	    	 }catch (Exception e) {
	    		 e.printStackTrace();
	    	 }  
	    	 return seasonID2;
	     }

	     private void copyFeatures(String sourceFeatureId, String seasonID2) {
	    	 try {
	    		 String rootId = featureApi.getRootId(seasonID2, sessionToken);

	    		 featureApi.copyFeature(sourceFeatureId, rootId, "ACT", null, "suffix3", sessionToken);
	    	 }catch (Exception e) {
	    		 e.printStackTrace();
	    	 }  
	    	 
	     }

	     private void updateSchemaProd(String seasonID) {
	    	 try {
		         String inputSchema = schemaApi.getInputSchema(seasonID, sessionToken);
		         JSONObject jsonSchema = new JSONObject(inputSchema);
		         JSONObject viewedLocation = jsonSchema.getJSONObject("inputSchema").getJSONObject("properties")
		                 .getJSONObject("context").getJSONObject("properties").getJSONObject("viewedLocation");
		         viewedLocation.put("stage", "PRODUCTION");
		         schemaApi.updateInputSchema(seasonID, jsonSchema.toString(), sessionToken);
	    	 } catch (Exception e) {
	    		 e.printStackTrace();
	    	 }  
	     }

	     private void moveFeatureToProd(String seasonID, String featureID)  {
	    	 try {
		         String feature = featureApi.getFeature(featureID, sessionToken);
		         JSONObject json = new JSONObject(feature);
		         json.put("stage", "PRODUCTION");
		         featureApi.updateFeature(seasonID, featureID, json.toString(), sessionToken);
	    	 } 
		       catch (Exception e) {
		    		e.printStackTrace();
		    }  
	     }


	    	 private void deleteFeature(String featureID) {
	    	 try {
	    		 featureApi.deleteFeature(featureID, sessionToken);
	    	 } catch (Exception e){
	    		 e.printStackTrace();
	    	 }
	     }
	     
	    private String createStream(String seasonID){
	    	String streamID = "";
			try {
		    	String stream = FileUtils.fileToString(config + "streams/stream1.txt", "UTF-8", false);
				JSONObject json = new JSONObject(stream);
				json.put("minAppVersion", "0.5");
				streamID = streamApi.createStream(seasonID, json.toString(), sessionToken);
			} catch (Exception e){
				e.printStackTrace();
			}
			return streamID;

		}
	     

	     private void updateStream(String streamID){
	 		try {
	 			String response = streamApi.getStream(streamID, sessionToken);
				JSONObject json = new JSONObject(response);
				json.put("description", "new stream description");
				response = streamApi.updateStream(streamID, json.toString(), sessionToken);
			} catch (Exception e){
				e.printStackTrace();
			}


		}
	     

	 	private void deleteStream(String streamID){

			
	 		try {
	 			streamApi.deleteStream(streamID, sessionToken);	
			} catch (Exception e){
				e.printStackTrace();
			}


		}

	   private void printTime(String productID, long startTime){
	        long estimatedTime = System.currentTimeMillis() - startTime;
	        System.out.println("Elapsed time for product  " + productID + " is " + estimatedTime +  " milliseconds");

	   }


}
