package tests.utils.src;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.restapi.scenarios.capabilities.TestAllApi;

public class RemoveUnusedBranches {
	
	public static final String DELETION_CUTOFF_DATE = "2020-10-23T00:00:00+0000";
	public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	public static void main(String[] args) {
		if(args.length!=4) {
			System.out.println("Usage: removeUnusedBranches server-url productID apiKey apiKeyPasswork");
			System.exit(-1);
		}
		
		
		String serverUrl = args[0];
		String productID = args[1];
		String key = args[2];
		String keyPassword = args[3];
		
		String adminUrl = serverUrl + "/api/admin";
		String operationsUrl = serverUrl + "/api/ops";
		String translationsUrl = serverUrl + "/api/translations";
		String analyticsUrl = serverUrl + "/api/analytics";
		
		String[] branchessToKeepArray = new String[]{};
		Set<String> branchessToKeepSet = new HashSet<String>(); 
		for (int j = 0; j<branchessToKeepArray.length; j++) {
			branchessToKeepSet.add(branchessToKeepArray[j]);
		}
		
		try {
			Date deletionCutOffDate = formatter.parse(DELETION_CUTOFF_DATE);
			
			TestAllApi allApis = new TestAllApi(adminUrl, operationsUrl,translationsUrl,analyticsUrl, "");
			String sessionToken = allApis.startSessionFromKey(key, keyPassword);
			if (sessionToken.contains("error")) {
				System.err.println("Failed to generate session token from key: " + sessionToken);
				return;
			}
			
			JSONArray seasonsArray = allApis.getSeasonsPerProduct(productID, sessionToken);
			for (int i=0; i<seasonsArray.length(); i++) {
				String seasonID = seasonsArray.getJSONObject(i).getString("uniqueId");
				System.out.println("seasonID = " + seasonID);
				String response = allApis.getBranchesUsage(seasonID, sessionToken);
				JSONObject branchesArrayObj = new JSONObject(response);
				JSONArray branchesArray = branchesArrayObj.getJSONArray("branches");
				System.out.println("number of branches = " + branchesArray.length());
				for (int j=0; j<branchesArray.length(); j++) {
					String branchID = branchesArray.getJSONObject(j).getString("uniqueId");
					String branchName = branchesArray.getJSONObject(j).getString("name");
					String branchCreationDateStr = branchesArray.getJSONObject(j).getString("creationDate");
					String branchModificationDateStr = branchesArray.getJSONObject(j).getString("branchModificationDate");
					Date branchModificationDate = formatter.parse(branchModificationDateStr);
					Date branchCreationDate = formatter.parse(branchCreationDateStr);
					boolean isPartOfExperiment = branchesArray.getJSONObject(j).getBoolean("isPartOfExperiment");
					if (!branchessToKeepSet.contains(branchName) && branchModificationDate.before(deletionCutOffDate) && !isPartOfExperiment) {
						System.out.println("	" + j + ": " + branchName);
						//TODO: delete branch
						int delBranchCode = allApis.deleteBranch(branchID, sessionToken);
						Assert.assertTrue (delBranchCode==200, "branch " + branchName + " was  not deleted");	
					}
					else {
						System.out.println("	######### not deleting" + j + ": " + branchName + ", isPartOfExperiment = " + isPartOfExperiment + ", branchCreationDate: " + branchCreationDateStr + ", branchModificationDateStr: " + branchModificationDateStr);
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		System.out.print("END!!!!");
	}
	
}
