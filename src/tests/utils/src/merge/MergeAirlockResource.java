package tests.utils.src.merge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import org.apache.wink.json4j.JSONArray;

public class MergeAirlockResource {

	public static String MERGE_AIRLOCK_USERS = "MergeAirlockUsers";
	public static String MERGE_USER_GROUPS = "MergeUserGroups";
	
	public static void main(String[] args) {
		if (args.length!=4) {
			System.err.println("Usage: action [MergeUserGroups/MergeAirlockUsers] srcFilePath mergeFilePath outputFilePath");
			System.exit(-1);
		}
		
		String action = args[0];
		if (!action.equalsIgnoreCase(MERGE_AIRLOCK_USERS) && !action.equalsIgnoreCase(MERGE_USER_GROUPS)) {
			System.err.println("Illegal action " + action + ", should be MergeUserGroups or MergeAirlockUsers");
			System.exit(-1);
		}
		
		//the file to add missing userGroups to
		String srcFilePath = args[1];
		
		//the file to take the added userGroups from
		String mergeFilePath = args[2];
		
		//the results:  mergeFile merged into srcFile
		String outputFilePath = args[3];
		
		File srcFile  = new File (srcFilePath);
		if (!srcFile.exists()) {
			System.err.println("srcFile: " + srcFilePath + " is missing");
			System.exit(-1);
		}

		if (srcFile.isDirectory()) {
			System.err.println("srcFile: " + srcFilePath + " is directory.");
			System.exit(-1);
		}
		
		File mergeFile  = new File (mergeFilePath);
		if (!mergeFile.exists()) {
			System.err.println("mergeFile: " + mergeFilePath + " is missing.");
			System.exit(-1);
		}
		
		if (mergeFile.isDirectory()) {
			System.err.println("mergeFile: " + mergeFilePath + " is directory.");
			System.exit(-1);
		}
		
		String mergedData = null;
		try {
			if (action.equalsIgnoreCase(MERGE_USER_GROUPS)) {
				mergedData = mergeUserGroups(srcFile, mergeFile);
			}
			else if (action.equalsIgnoreCase(MERGE_AIRLOCK_USERS)) {
				mergedData = mergeAirlockUsers(srcFile, mergeFile);
			}
		} catch (Exception e) {
			System.err.println("Error computing merged data: " + e.getMessage());
			System.exit(-1);
		} 
		
		File outputFile = new File(outputFilePath);
		try {
			FileUtils.write(outputFile, mergedData, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.err.println("Error writing output file: " + e.getMessage());
			System.exit(-1);
		}
	}

	private static String mergeAirlockUsers(File srcFile, File mergeFile) throws IOException, JSONException {
		String srcFileContent = FileUtils.readFileToString(srcFile, Charset.forName("UTF-8"));
		String mergedFileContent = FileUtils.readFileToString(mergeFile, Charset.forName("UTF-8"));
		
		JSONObject srcAirlockUsers = new JSONObject(srcFileContent);
		JSONArray srcRolesArr = srcAirlockUsers.getJSONArray("roles");
		
		JSONObject mergeAirlockUsers = new JSONObject(mergedFileContent);
		JSONArray mergeRolesArr = mergeAirlockUsers.getJSONArray("roles");
		
		if (srcRolesArr.size()!=mergeRolesArr.size()) {
			throw new JSONException("source and destination dont have the same roles types");
		}
		
		//counting on the fact that the roles order is the same in both files. If not - throw an exception
		for (int i=0; i<srcRolesArr.size(); i++) {
			JSONObject srcRoleUsersObj = (JSONObject)srcRolesArr.get(i);
			JSONObject mergeRoleUsersObj = (JSONObject)mergeRolesArr.get(i);
			
			if (!srcRoleUsersObj.getString("role").equals(mergeRoleUsersObj.getString("role"))) {
				throw new JSONException("source and destination dont have the same roles types in the same order");
			}
			
			String role = srcRoleUsersObj.getString("role");
			
			JSONArray srcRoleUsersArr = srcRoleUsersObj.getJSONArray("users");
			Set<String> srcRoleUsersSet = jsonArrToStringsSet(srcRoleUsersArr);
			
			JSONArray mergeRoleUsersArr = mergeRoleUsersObj.getJSONArray("users");
			for (int j=0; j<mergeRoleUsersArr.size(); j++) {
				if (!srcRoleUsersSet.contains(mergeRoleUsersArr.getString(j))) {
					srcRoleUsersArr.add(mergeRoleUsersArr.getString(j));
					System.out.println("user '" + mergeRoleUsersArr.getString(j) + "' added to role " + role );
				}
			}
		}

		return srcAirlockUsers.write(true);

	}

	private static String mergeUserGroups(File srcFile, File mergeFile) throws IOException, JSONException {
		String srcFileContent = FileUtils.readFileToString(srcFile, Charset.forName("UTF-8"));
		String mergedFileContent = FileUtils.readFileToString(mergeFile, Charset.forName("UTF-8"));
		
		JSONObject srcUserGroups = new JSONObject(srcFileContent);
		JSONArray srcUserGroupsArr = srcUserGroups.getJSONArray("internalUserGroups");
		Set<String> srcUserGroupsSet = jsonArrToStringsSet(srcUserGroupsArr);
		
		JSONObject mergeUserGroups = new JSONObject(mergedFileContent);
		JSONArray mergeUserGroupsArr = mergeUserGroups.getJSONArray("internalUserGroups");
		
		for (int i=0; i<mergeUserGroupsArr.size(); i++) {
			if (!srcUserGroupsSet.contains(mergeUserGroupsArr.getString(i))) {
				srcUserGroupsArr.add(mergeUserGroupsArr.getString(i));
				System.out.println("user group '" + mergeUserGroupsArr.getString(i) + "' added" );
			}
		}

		return srcUserGroups.write(true);
	}
	
	public static Set<String> jsonArrToStringsSet(JSONArray jsonArr) throws JSONException {
		if (jsonArr == null)
			return null;

		Set<String> res = new HashSet<String>();
		for (int i=0; i<jsonArr.size(); i++) {
			res.add(jsonArr.getString(i));
		}

		return res;
	}

}
