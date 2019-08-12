package tests.utils.src.sdk_utils;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wink.json4j.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.SeasonsRestApi;


public class CreateProducts {
	
	protected String productID;
	protected String filePath;
	protected ProductsRestApi p;
	protected AirlockUtils baseUtils;
	private String sessionToken = "";
	private FeaturesRestApi f;
	private SeasonsRestApi s;
	private ExperimentsRestApi exp;
	private BranchesRestApi br;
	private String sdkFile = "sdk_products.txt";

	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		productID = "";
		filePath = configPath;
		p = new ProductsRestApi();
 		p.setURL(url);
		s = new SeasonsRestApi();
 		s.setURL(url);
 		exp = new ExperimentsRestApi();
 		exp.setURL(analyticsUrl); 
 		br = new BranchesRestApi();
 		br.setURL(url);

		baseUtils = new AirlockUtils(url, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);	
		sessionToken = baseUtils.sessionToken;

		f = new FeaturesRestApi();
		f.setURL(url);
		
		// file that contains all products created by tests. In SuiteCleanup products are deleted according to this list
		File file = new File(sdkFile); 
		if (!file.exists()) {
			file.createNewFile();
		}
	}
	
	
	@Test ()
	public void addProduct() throws Exception{
		HashMap<String, List<String>> mapOfList = new HashMap<String, List<String>>();
		/*mapOfList.put("ExperimentInProdWithDevDev", Arrays.asList("DEVELOPMENT", "DEVELOPMENT"));
		mapOfList.put("ExperimentInProdWithDevProd", Arrays.asList("DEVELOPMENT", "PRODUCTION"));
		mapOfList.put("ExperimentInProdWithProdDev", Arrays.asList("PRODUCTION", "DEVELOPMENT"));
		mapOfList.put("ExperimentInProdWithProdProd", Arrays.asList("PRODUCTION", "PRODUCTION"));
		*/
		//mapOfList.put("ExperimentDisabledInProdWithVariantDisabled", Arrays.asList("PRODUCTION", "DEVELOPMENT", "PRODUCTION"));
		mapOfList.put("ExperimentInDevWithDevDev", Arrays.asList("DEVELOPMENT", "DEVELOPMENT"));
		JSONObject product = new JSONObject(FileUtils.fileToString(filePath + "product1.txt", "UTF-8", false));
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		JSONObject season = new JSONObject(FileUtils.fileToString(filePath + "season1.txt", "UTF-8", false));	
		Set set = mapOfList.entrySet();
		  Iterator iterator = set.iterator();
		      while(iterator.hasNext()) {
		         Map.Entry mentry = (Map.Entry)iterator.next();

		         List<Object> stages = (List<Object>) mentry.getValue();
		         	
		        String productName  = (String) mentry.getKey();
			
		         product.put("codeIdentifier", productName);
		         product.put("name", productName);
		        // product.put("description", "2 experiments: 1st disabled, 2nd enabled. 3 variants in 2nd experiment: 1st disabled, 2nd enabled dev, 3d enabled prod ");
		         productID = p.addProduct(product.toString(), sessionToken);			
		         
		 		
				
		         season.put("minVersion", "8.0");
		         String seasonID = s.addSeason(productID, season.toString(), sessionToken);
		         
		         feature.put("name", "F1");
		         feature.put("stage", "PRODUCTION");
		        // JSONObject rule = new JSONObject();
		        // rule.put("ruleString", "true");
		         //feature.put("rule", rule);
		         String configuration = "\"text\":\"master\"";
		         feature.put("defaultConfiguration", configuration);		        		 
		         String featureID1 = f.addFeature(seasonID, feature.toString(), "ROOT", sessionToken);
		         
		         feature.put("name", "F2");
		         feature.put("stage", "DEVELOPMENT");
		        // rule.put("ruleString", "false");
		       //  feature.put("rule", rule);
		         feature.put("configuration", configuration);
		         String featureID2 = f.addFeature(seasonID, feature.toString(), "ROOT", sessionToken);
		         
		 		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
				JSONObject branchJson = new JSONObject(branch);
				branchJson.put("name", "branch1");
				String branchID1 =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
				
				branchJson.put("name", "branch2");
				String branchID2 =  br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);
				
				String experiment = FileUtils.fileToString(filePath + "experiments/experiment1.txt", "UTF-8", false);
				JSONObject expJson = new JSONObject(experiment);
				expJson.put("name", "experiment1");
				expJson.put("stage", "PRODUCTION");
				expJson.put("minVersion", "8.0");
				expJson.put("maxVersion", "9.0");
				//expJson.put("enabled", false);
		       /* JSONObject exprule = new JSONObject();
		        exprule.put("ruleString", "context.userLocation.country == 'US'");	
		        expJson.put("rule", exprule);
		        */
				String experimentID1 =  exp.createExperiment(productID, expJson.toString(), sessionToken);
			/*	
				expJson.put("name", "experiment2");
				expJson.put("stage", "PRODUCTION");
				expJson.put("minVersion", "8.0");
				expJson.put("maxVersion", "9.0");
				String experimentID2 =  exp.createExperiment(productID, expJson.toString(), sessionToken);

			*/
				String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
				JSONObject variantJson = new JSONObject(variant);
				variantJson.put("name", "variant1");
				variantJson.put("stage", stages.get(0));
				variantJson.put("branchName", "branch1");
				exp.createVariant(experimentID1, variantJson.toString(), sessionToken);
				

				variantJson.put("name", "variant2");
				variantJson.put("stage", stages.get(1));
				variantJson.put("branchName", "branch2");
				exp.createVariant(experimentID1, variantJson.toString(), sessionToken);
/*
				
				variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
				variantJson = new JSONObject(variant);
				variantJson.put("name", "variant1");
				variantJson.put("stage", stages.get(0));
				variantJson.put("branchName", "branch1");
				variantJson.put("enabled", false);
				exp.createVariant(experimentID2, variantJson.toString(), sessionToken);

				variantJson = new JSONObject(variant);
				variantJson.put("name", "variant2");
				variantJson.put("stage", stages.get(1));
				variantJson.put("branchName", "branch2");
				exp.createVariant(experimentID2, variantJson.toString(), sessionToken);
				
				variantJson = new JSONObject(variant);
				variantJson.put("name", "variant3");
				variantJson.put("stage", stages.get(2));
				variantJson.put("branchName", "branch2");
				exp.createVariant(experimentID2, variantJson.toString(), sessionToken);
			*/	
				br.checkoutFeature(branchID1, featureID1, sessionToken);
				br.checkoutFeature(branchID2, featureID2, sessionToken);
				
				printProductToFile(productName, productID, seasonID);

		    }

		
 			
	}
	
	public void printProductToFile(String productName, String productId, String seasonId){
		try {
				FileWriter fw = new FileWriter(sdkFile, true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw);
			
			    out.println("product: " + productName + "\t productId: " + productId + "\t seasonId: " + seasonId);
			    out.close();

			} catch (IOException e) {
			    System.out.println("cannot write productID to file: " + e.getLocalizedMessage());
			}
	}


}
