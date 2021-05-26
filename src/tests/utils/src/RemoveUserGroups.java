package tests.utils.src;

import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;

import tests.restapi.scenarios.capabilities.TestAllApi;

public class RemoveUserGroups {

	public static void main(String[] args) {
		if(args.length!=4) {
			System.out.println("Usage: genrateRuntimeFiles server-url productID apiKey apiKeyPasswork");
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
		
		String[] usersToKeepArray = new String[]{};
		Set<String> usersToKeepSet = new HashSet<String>(); 
		for (int j = 0; j<usersToKeepArray.length; j++) {
			usersToKeepSet.add(usersToKeepArray[j]);
		}
		
		try {
			TestAllApi allApis = new TestAllApi(adminUrl, operationsUrl,translationsUrl,analyticsUrl, "");
			//String sessionToken = "";
			String sessionToken = allApis.startSessionFromKey(key, keyPassword);
			if (sessionToken.contains("error")) {
				System.err.println("Failed to generate session token from key: " + sessionToken);
				return;
			}
			long getTokenTime = System.currentTimeMillis();
			
			String response = allApis.getUserGroupsUsage(productID, sessionToken);
			//System.out.println(response);
			/*if (response.contains("error")) {
				System.err.println("Couldn't get usergroups usage: " + response);
				return;
			}*/
			
			JSONObject usageObj = new JSONObject(response);
			JSONArray userGroupsArr = usageObj.getJSONArray("internalUserGroups");
			System.out.println("number of user groups = " + userGroupsArr.length());
			for (int i=0; i<userGroupsArr.length(); i++) {
				JSONObject userGruop = userGroupsArr.getJSONObject(i);				
				String userGroupName = userGruop.getString("internalUserGroup");
				System.out.println("_____________________________________________________________________________________________");
				System.out.println("**** userGroupName = " + userGroupName);
				
				if(!usersToKeepSet.contains(userGroupName)) { //if not in the users to keep list
					
					//skip users groups such as IOSFLAG-4XXX
					if (userGroupName.startsWith("IOSFLAG") || userGroupName.startsWith("ANDFLAG")) {
						/*String[] parts = userGroupName.split("-");
						if (parts.length>1) {
							try {
						        int d = Integer.parseInt(parts[1]);
						        if (d>4000) {
						        		System.out.println("skiping userGroup " + userGroupName);
						        		continue; 
						        }
						    } catch (NumberFormatException nfe) {
						        //do nothing
						    }
						}*/
						System.out.println("skiping userGroup " + userGroupName);
						continue;
					}
					
					System.out.println("deleting userGroup: " + userGroupName);
					//System.out.println(userGruop.toString(true));
					
					//remove userGroup from experiments
					JSONArray experiments = userGruop.getJSONArray("experiments");
					for (int k=0; k<experiments.length(); k++) {
						JSONObject experiment = experiments.getJSONObject(k);
						//System.out.println("	  experiment = " + experiment.toString(true));
						
						String experimentID =  experiment.getString("uniqueId");
						System.out.println("	  experiment = " + experiment.getString("name") + ", " + experimentID);
						
						response = allApis.getExperiment(experimentID, sessionToken);
						if (response.contains("\"error\":")) {
							System.err.println("Cannot get experiment : " + response);
							return;
						}
						
						JSONObject expObj = new JSONObject(response);
						updateInternalUserGroup(expObj, userGroupName);
						response = allApis.updateExperiment(experimentID, expObj.toString(), sessionToken);
						if (response.contains("\"error\":")) {
							System.err.println("Cannot update experiment : " + response);
							return;
						}
						
						JSONArray variants = experiment.getJSONArray("variants");
						for (int l=0; l<variants.length(); l++) {
							JSONObject variant = variants.getJSONObject(l);
							//System.out.println("	  	  variant = " + variant.toString(true));
							String variantID =  variant.getString("uniqueId");
							System.out.println("	  	  variant = " + variant.getString("name") + ", " + variantID);
							response = allApis.getVariant(variantID, sessionToken);
							if (response.contains("\"error\":")) {
								System.err.println("Cannot get variant : " + response);
								return;
							}
							
							JSONObject variantObj = new JSONObject(response);
							updateInternalUserGroup(variantObj, userGroupName);
							response = allApis.updateVariant(variantID, variantObj.toString(), sessionToken);
							if (response.contains("\"error\":")) {
								System.err.println("Cannot update variant : " + response);
								return;
							}
						}
					}
					
					JSONArray seasons = userGruop.getJSONArray("seasons");
					for (int s=0; s<seasons.length(); s++) {
						if ((System.currentTimeMillis()) - getTokenTime > (30* 60 * 1000)) {
							sessionToken = allApis.startSessionFromKey(key, keyPassword);
							if (sessionToken.contains("error")) {
								System.err.println("Failed to generate session token from key: " + sessionToken);
								return;
							}
							getTokenTime = System.currentTimeMillis();
						}
						JSONObject season = seasons.getJSONObject(s);
						String seasonID =  season.getString("seasonId");
						System.out.println("	  seasonID = " + seasonID);
						System.out.println("	  	  Branch = MASTER");
						JSONArray masterFeatures = season.getJSONArray("features");
						for (int k=0; k<masterFeatures.length(); k++) {
							JSONObject feature = masterFeatures.getJSONObject(k);
							String featureID =  feature.getString("uniqueId");
							System.out.println("	  	  	  feature = " + feature.getString("namespace") + "." + feature.getString("name") + ", " + featureID);
							response = allApis.getFeatureFromBranch(featureID, "MASTER", sessionToken);
							if (response.contains("\"error\":")) {
								System.err.println("Cannot get feature : " + response);
								return;
							}
							
							JSONObject featureObj = new JSONObject(response);
							updateInternalUserGroup(featureObj, userGroupName);
							response = allApis.updateFeatureInBranch(seasonID, "MASTER", featureID, featureObj.toString(), sessionToken);
							if (response.contains("\"error\":")) {
								System.err.println("Cannot update feature : " + response);
								return;
							}
						}
						
						JSONArray branches = season.getJSONArray("branches");
						System.out.println("number of branches = " + branches.length());
						
						for (int b=0; b<branches.length(); b++) {
							if ((System.currentTimeMillis()) - getTokenTime > (30* 60 * 1000)) {
								sessionToken = allApis.startSessionFromKey(key, keyPassword);
								if (sessionToken.contains("error")) {
									System.err.println("Failed to generate session token from key: " + sessionToken);
									return;
								}
								getTokenTime = System.currentTimeMillis();
							}
							
							JSONObject branch = branches.getJSONObject(b);
							String branchID =  branch.getString("uniqueId");
							System.out.println("	  	  Branch = " + branch.getString("name")+ ", "+ branchID);
							JSONArray branchFeatures = branch.getJSONArray("features");
							for (int k=0; k<branchFeatures.length(); k++) {
								JSONObject feature = branchFeatures.getJSONObject(k);
								String featureID =  feature.getString("uniqueId");
								System.out.println("	  	  	  feature = " + feature.getString("namespace") + "." + feature.getString("name") + ", " + featureID);
								response = allApis.getFeatureFromBranch(featureID, branchID, sessionToken);
								if (response.contains("\"error\":")) {
									System.err.println("Cannot get feature : " + response);
									return;
								}
								
								JSONObject featureObj = new JSONObject(response);
								updateInternalUserGroup(featureObj, userGroupName);
								response = allApis.updateFeatureInBranch(seasonID, branchID, featureID, featureObj.toString(), sessionToken);
								if (response.contains("\"error\":")) {
									System.err.println("Cannot update feature : " + response);
									return;
								}
							}
							
						}
					}
					
					//delete user group
					response = allApis.getUserGroups(productID, sessionToken);
					if (response.contains("\"error\":")) {
						System.err.println("Cannot get user groups list : " + response);
						return;
					}
					JSONObject userGroupsObj = new JSONObject(response);
					updateInternalUserGroup(userGroupsObj, userGroupName);
					response = allApis.setUserGroups(productID, userGroupsObj.toString(), sessionToken);
					if (response.contains("\"error\":")) {
						System.err.println("Cannot update user groups list : " + response);
						return;
					}
					
					System.out.println("&&&&&&  Deleted userGroup: " + userGroupName);
					/*sessionToken = allApis.startSessionFromKey(key, keyPassword);
					if (sessionToken.contains("error")) {
						System.err.println("Failed to generate session token from key: " + sessionToken);
						return;
					}*/
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		System.out.print("END!!!!");
	}

	private static void updateInternalUserGroup(JSONObject obj, String userGroupName ) throws JSONException {
		JSONArray userGroups = obj.getJSONArray("internalUserGroups");
		JSONArray newUserGroups = new JSONArray();
		for (int f=0; f<userGroups.length(); f++) {
			if (!userGroups.getString(f).equals(userGroupName)) {
				newUserGroups.add(userGroups.getString(f));
			}
		}
		obj.put("internalUserGroups", newUserGroups);
	}
	
}
