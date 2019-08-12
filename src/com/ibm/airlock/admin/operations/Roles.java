package com.ibm.airlock.admin.operations;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;

public class Roles {
	public class Role {
		String role;
		String[] actions;
		
		public void fromJSON (JSONObject input) throws JSONException {
			//after validate - i know that all fields appears, not empty and the right object
		
			role = (String)input.get(Constants.JSON_FIELD_ROLE);
			actions = Utilities.jsonArrToStringArr(input.getJSONArray(Constants.JSON_FIELD_ACTIONS));
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject res = new JSONObject();
			
			res.put(Constants.JSON_FIELD_ROLE, role);
			res.put(Constants.JSON_FIELD_ACTIONS, actions);
			
			return res;			
		}
		
		//return null if valid, ValidationResults otherwise
		public ValidationResults validateRoleJSON(JSONObject roleJSON) throws JSONException {
			//role
			if (!roleJSON.containsKey(Constants.JSON_FIELD_ROLE) || roleJSON.get(Constants.JSON_FIELD_ROLE) == null || roleJSON.getString(Constants.JSON_FIELD_ROLE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ROLE), Status.BAD_REQUEST);
			}
			
			//actions
			if (!roleJSON.containsKey(Constants.JSON_FIELD_ACTIONS) || roleJSON.get(Constants.JSON_FIELD_ACTIONS) == null) {				
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ACTIONS), Status.BAD_REQUEST);								
			}
			
			roleJSON.getJSONArray(Constants.JSON_FIELD_ACTIONS); //validate that array
		
			//all fields should appear
			return null;
		}

		public String getRole() { return role; }
		public String[] getActions() { return actions; }
	}

	private Date lastModified = null;
	private LinkedList<Role> roles = new LinkedList<Role>();
//	private Map<String, List<String>> impliedRoles = new HashMap<String, List<String>>(); 

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public LinkedList<Role> getRoles() {
		return roles;
	}
	public void setRoles(LinkedList<Role> roles) {
		this.roles = roles;
	}
/*	public Map<String, List<String>> getImpliedRoles() {
		return impliedRoles;
	}
	public void setImpliedRoles(Map<String, List<String>> impliedRoles) {
		this.impliedRoles = impliedRoles;
	}*/

	public JSONObject toJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		JSONArray rolesArr = new JSONArray();
		for (int i=0; i<roles.size(); i++) {
			rolesArr.add(roles.get(i).toJSON());			
		}

		res.put(Constants.JSON_FIELD_ROLES, rolesArr);
/*
		JSONObject implied = new JSONObject();
		for (Map.Entry<String, List<String>> ent : impliedRoles.entrySet())
		{
			implied.put(ent.getKey(), ent.getValue());
		}
		res.put(Constants.JSON_FIELD_IMPLIED_ROLES, implied);
*/
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {		
		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
				
		roles.clear();
		if (input.containsKey(Constants.JSON_FIELD_ROLES) && input.get(Constants.JSON_FIELD_ROLES)!=null) {
			JSONArray rolesJSONArr = input.getJSONArray(Constants.JSON_FIELD_ROLES);
			if (rolesJSONArr != null && rolesJSONArr.size()>0) {
				for (int i=0; i<rolesJSONArr.size(); i++) {
					Role r = new Role();
					r.fromJSON(rolesJSONArr.getJSONObject(i));
					roles.add(r);															
				}
			}
		}
/*
		impliedRoles.clear();
		JSONObject implied = input.optJSONObject(Constants.JSON_FIELD_IMPLIED_ROLES);
		if (implied != null)
		{
			@SuppressWarnings("unchecked")
			Set<String> keys = implied.keySet();
			for (String key : keys)
			{
				JSONArray values = implied.getJSONArray(key);
				ArrayList<String> array = new ArrayList<String>();
				for (int i = 0; i < values.length(); ++i)
					array.add(values.getString(i));

				impliedRoles.put(key, array);
			}
		}
*/
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateRolesJSON(JSONObject rolesJson) {		
		try {
			if (!rolesJson.containsKey(Constants.JSON_FIELD_ROLES) || rolesJson.get(Constants.JSON_FIELD_ROLES)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ROLES), Status.BAD_REQUEST);
			}
			
			JSONArray rolesArr = rolesJson.getJSONArray(Constants.JSON_FIELD_ROLES); //validate that is array value
			
			//modification date must appear
			if (!rolesJson.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || rolesJson.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_LAST_MODIFIED), Status.BAD_REQUEST);
			}				
			
			//verify that given modification date is not older that current modification date
			long givenModoficationDate = rolesJson.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("The roles JSON was changed by another user. Get the roles again and resubmit the request.", Status.CONFLICT);			
			}
		
			
			//verify that there are no duplications in the user groups
			HashMap<String, Integer> tmpRolesMap = new HashMap<String, Integer>(); 
			for(int j = 0; j < rolesArr.length(); j++){
			    JSONObject roleJSON = rolesArr.getJSONObject(j);
			    Role r = new Role();
			    ValidationResults res = r.validateRoleJSON(roleJSON);
			    if (res != null)
			    	return res;
			    String roleName = roleJSON.getString(Constants.JSON_FIELD_ROLE);
			    if (tmpRolesMap.containsKey(roleName)) {
			    	return new ValidationResults("Role '" + roleName + "' apears more than once in the roles JSON.", Status.BAD_REQUEST);
			    }
			    tmpRolesMap.put(roleJSON.getString(Constants.JSON_FIELD_ROLE), 1);
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		catch (ClassCastException cce) {
			return new ValidationResults("Illegal roles JSON: " + cce.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}

}
