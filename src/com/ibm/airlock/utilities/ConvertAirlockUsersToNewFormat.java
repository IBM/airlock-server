package com.ibm.airlock.utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Utilities;

public class ConvertAirlockUsersToNewFormat {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("usage: ConvertAirlockUsersToNewFormat oldUsersFilePath newUsersFilePath productId (or none for global roleSets)");
			return;
		}
		
		String oldUsersFilePath = args[0];
		String newUsersFilePath = args[1];
		String productId = args[2];
		
		BufferedWriter writer = null;
		HashMap<String, LinkedList<String>> usersToRolesMap = new HashMap<String, LinkedList<String>>();
		try {
			JSONObject oldUsersJSON = fileToJson(oldUsersFilePath);
			JSONObject newUsersJSON = new JSONObject();
			
			long lastModified = oldUsersJSON.getLong("lastModified");
			JSONArray roles = oldUsersJSON.getJSONArray("roles");
			for (int i=0; i<roles.size(); i++) {
				JSONObject role = roles.getJSONObject(i);
				String roleName = role.getString("role");
				JSONArray roleUsers = role.getJSONArray("users");
				for (int j=0; j<roleUsers.size(); j++) {
					String user = roleUsers.getString(j);
					if (usersToRolesMap.containsKey(user)) {
						usersToRolesMap.get(user).add(roleName);
					}
					else {
						LinkedList<String> newUserRoles = new LinkedList<String>();
						newUserRoles.add(roleName);
						usersToRolesMap.put(user, newUserRoles);
					}
				}
			}
			
			Set<String> userIdentifiers = usersToRolesMap.keySet();
			JSONArray newUsersArray = new JSONArray();
			for (String userIdentifier:userIdentifiers) {
				JSONObject newUser = new JSONObject();
				newUser.put("identifier", userIdentifier);
				newUser.put("roles", usersToRolesMap.get(userIdentifier));
				newUser.put("lastModified", lastModified);
				newUser.put("creationDate", lastModified);
				newUser.put("creator", "auto converet");
				newUser.put("isGroupRepresentation", userIdentifier.startsWith("*")?true:false);
				newUser.put("uniqueId", UUID.randomUUID().toString());
				newUser.put("productId", productId.equalsIgnoreCase("none")?null:productId);
				newUser = setRolesListByHigherPermission(newUser);
				
				newUsersArray.add(newUser);
			}
			newUsersJSON.put("users", newUsersArray);
			
			writer = new BufferedWriter( new FileWriter( newUsersFilePath));
		    writer.write( newUsersJSON.write(true));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (writer!=null)
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
	}

	//An Administrator is also a ProductLead, Editor and Viewer
	//A ProductLead is also Editor and Viewer
	//An Editor is also a viewer
	//A TranslationSpecialist is also a viewer
	//An AnalyticsViewer is also a viewer
	//An AnalyticsEditor is also a viewer
	private static JSONObject setRolesListByHigherPermission(JSONObject input) throws JSONException {
		//this function is called after validate so we know that the roles list is valid (no duplications, existing roles)
		LinkedHashSet<String> newRolesSet = new LinkedHashSet<String>();
		JSONArray existingRolesArray = input.getJSONArray(Constants.JSON_FIELD_ROLES);
		for (int i=0; i<existingRolesArray.size(); i++) {
			RoleType existingRole = Utilities.strToRoleType(existingRolesArray.getString(i));
			switch (existingRole) {
				case Administrator: 
					newRolesSet.add(RoleType.Administrator.toString());
					newRolesSet.add(RoleType.ProductLead.toString());
					newRolesSet.add(RoleType.Editor.toString());
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case ProductLead:
					newRolesSet.add(RoleType.ProductLead.toString());						
					newRolesSet.add(RoleType.Editor.toString());
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case Editor:
					newRolesSet.add(RoleType.Editor.toString());
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case TranslationSpecialist:
					newRolesSet.add(RoleType.TranslationSpecialist.toString());						
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case AnalyticsViewer:
					newRolesSet.add(RoleType.AnalyticsViewer.toString());
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case AnalyticsEditor:
					newRolesSet.add(RoleType.AnalyticsEditor.toString());
					newRolesSet.add(RoleType.Viewer.toString());
					break;
				case Viewer:
					newRolesSet.add(RoleType.Viewer.toString());
					break;
			}
		}
		input.put(Constants.JSON_FIELD_ROLES, newRolesSet);
		return input;
	}
		
	private static JSONObject fileToJson (String filePath) throws IOException, JSONException {
		byte[] encoded = Files.readAllBytes(Paths.get(filePath));
		return new JSONObject(new String(encoded, StandardCharsets.UTF_8));
	}
}
