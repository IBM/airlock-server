package tests.utils.src;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import tests.restapi.BranchesRestApi;
import tests.restapi.scenarios.capabilities.TestAllApi;

public class genrateRuntimeFiles {

	public static void main(String[] args) {
		if(args.length!=4) {
			System.out.println("Usage: genrateRuntimeFiles server-url productID seasonID configPath");
			System.exit(-1);
		}
		//NOTE - this work against non encrypted server only
		//TODO: skipAuth, add notification capability, run, remove notification capability
		String serverUrl = args[0];
		String productID = args[1];
		String seasonID = args[2];
		String configPath = args[3];
		
		//serverUrl = http://localhost:9090/airlock
		String adminUrl = serverUrl + "/api/admin";
		String operationsUrl = serverUrl + "/api/ops";
		String translationsUrl = serverUrl + "/api/translations";
		String analyticsUrl = serverUrl + "/api/analytics";
		try {
			TestAllApi allApis = new  TestAllApi(adminUrl, operationsUrl,translationsUrl,analyticsUrl, configPath);
			
			String sessionToken = "";
			
			//add notification capability to product
			JSONArray capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
			capabilities.remove("NOTIFICATIONS"); 
			capabilities.add("NOTIFICATIONS"); 
			String response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
			if (response.contains("error")) {
				System.err.println("Can't set capabilities " + response);
				System.exit(-1);
			}
			
			//add branch and delete it
			String branchID = allApis.createBranch(seasonID, sessionToken);
			if (branchID.contains("error")) {
				System.err.println("Can't craete branch " + response);
				System.exit(-1);
			}
			int responseCode = allApis.deleteBranch(branchID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete branch " + responseCode);
				System.exit(-1);
			}
				
		
			//add production feature, revert to development and delete it
			String featureID = allApis.createStagedFeature(seasonID, BranchesRestApi.MASTER, sessionToken, true);
			if (featureID.contains("error")) {
				System.err.println("Can't craete production feature " + response);
				System.exit(-1);
			}
			
			featureID = allApis.updateFeatureStageInBranch(seasonID, BranchesRestApi.MASTER, featureID, sessionToken, "DEVELOPMENT");
			if (featureID.contains("error")) {
				System.err.println("Can't revert feature to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteFeature(featureID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete feature " + responseCode);
				System.exit(-1);
			}
			
			//add production stream, revert to development and delete it
			String streamID = allApis.createStagedStream(seasonID, sessionToken, true);
			if (streamID.contains("error")) {
				System.err.println("Can't craete production stream " + response);
				System.exit(-1);
			}
			
			streamID = allApis.updateStreamStage(streamID, sessionToken, "DEVELOPMENT");
			if (streamID.contains("error")) {
				System.err.println("Can't revert stream to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteStream(streamID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete stream " + responseCode);
				System.exit(-1);
			}
			
			//add production notification, revert to development and delete it
			String notificationID = allApis.createStagedNotification(seasonID, sessionToken, true);
			if (notificationID.contains("error")) {
				System.err.println("Can't craete production notification " + response);
				System.exit(-1);
			}
			
			notificationID = allApis.updateNotificationStage(notificationID, sessionToken, "DEVELOPMENT");
			if (notificationID.contains("error")) {
				System.err.println("Can't revert notification to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteNotification(notificationID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete notification " + responseCode);
				System.exit(-1);
			}
			
			//add production utility, revert to development and delete it
			String utilityID = allApis.addStagedUtility(seasonID, sessionToken, true);
			if (utilityID.contains("error")) {
				System.err.println("Can't craete production utility " + response);
				System.exit(-1);
			}
			
			utilityID = allApis.updateUtilityStage(utilityID, sessionToken, "DEVELOPMENT");
			if (utilityID.contains("error")) {
				System.err.println("Can't revert utility to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteUtility(utilityID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete utility " + responseCode);
				System.exit(-1);
			}
			
			//add production streams utility, revert to development and delete it
			String streamUtilityID = allApis.addStagedStreamUtility(seasonID, sessionToken, true);
			if (streamUtilityID.contains("error")) {
				System.err.println("Can't craete production streams utility " + response);
				System.exit(-1);
			}
			
			streamUtilityID = allApis.updateUtilityStage(streamUtilityID, sessionToken, "DEVELOPMENT");
			if (streamUtilityID.contains("error")) {
				System.err.println("Can't revert streams utility to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteUtility(streamUtilityID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete streams utility " + responseCode);
				System.exit(-1);
			}
			
			//add production string, revert to development and delete it
			String stringID = allApis.addStagedString(seasonID, "/strings/string1.txt", sessionToken, true);
			if (stringID.contains("error")) {
				System.err.println("Can't craete production string " + response);
				System.exit(-1);
			}
			
			stringID = allApis.updateStringStage(stringID, sessionToken, "DEVELOPMENT");
			if (utilityID.contains("error")) {
				System.err.println("Can't revert string to development" + response);
				System.exit(-1);
			}
			
			responseCode = allApis.deleteString(stringID, sessionToken);
			if (responseCode != 200){
				System.err.println("Can't delete string " + responseCode);
				System.exit(-1);
			}
			
			//update user groups and revert
			response = allApis.getUserGroups(productID, sessionToken);
			JSONObject json = new JSONObject(response);
			JSONArray groups = json.getJSONArray("internalUserGroups");
			groups.add("XXX");
			json.put("internalUserGroups", groups);
			response = allApis.setUserGroups(productID, json.toString(), sessionToken);
			if (utilityID.contains("error")) {
				System.err.println("Can't add user group" + response);
				System.exit(-1);
			}
			
			response = allApis.getUserGroups(productID, sessionToken);
			json = new JSONObject(response);
			groups = json.getJSONArray("internalUserGroups");
			groups.remove("XXX");
			json.put("internalUserGroups", groups);
			response = allApis.setUserGroups(productID, json.toString(), sessionToken);
			if (utilityID.contains("error")) {
				System.err.println("Can't add remove group" + response);
				System.exit(-1);
			}
			
			
			//remove notification capability to product
			capabilities = allApis.getCapabilitiesInProduct(productID, sessionToken);
			capabilities.remove("NOTIFICATIONS"); 
			response = allApis.setCapabilitiesInProduct(productID, capabilities, sessionToken);
			if (response.contains("error")) {
				System.err.println("Can't remove capabilities " + response);
				System.exit(-1);
			}
			
			
			System.out.println("********** Runtiem fiel generation Completed Successfuly ************");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

}
