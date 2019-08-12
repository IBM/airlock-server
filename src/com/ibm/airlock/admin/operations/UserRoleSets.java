package com.ibm.airlock.admin.operations;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.authentication.UserRoles;

/*
 	map<prodId->roles> instead of roles
 	add field authMethod - can be updated?????
 	addUser (even if in some group - in file: empty permissions in memory - will have the group permissions.) () admin
 	associateUserRolesForProduct (user. roles[], prodId) -pl. the user should exist in the file - if in group seperate it to individual entry. 	
 	removeUserFromProduct (user. prodId) pl (will not get permission for this product even if belongs to group (implicitly)) 

 */
public class UserRoleSets {
	public class UserRoleSet {
		LinkedList<RoleType> userRoles = new LinkedList<RoleType>(); //c+u cannot be empty
		String userIdentifier; //c+u cannot be updated
		String creator; //c+u  cannot be updated
		UUID uniqueId; //required in update. forbidden in create
		private Date lastModified = null; //mandatory in update forbidden in create
		boolean isGroupRepresentation = false; //c+u  cannot be updated
		private Date creationDate = null; //nc + u (not changed)
		private UUID productId; //if not set - the user is a global user (as opposed to product user). cannot be changed during update

		public List<RoleType> getUserRoles() {
			return userRoles;
		}

		public void setUserRoles(LinkedList<RoleType> userRoles) {
			this.userRoles = userRoles;
		}

		public String getUserIdentifier() {
			return userIdentifier;
		}

		public void setUserIdentifier(String userIdentifier) {
			this.userIdentifier = userIdentifier;
		}

		public UUID getUniqueId() {
			return uniqueId;
		}

		public void setUniqueId(UUID uniqueId) {
			this.uniqueId = uniqueId;
		}

		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(Date lastModified) {
			this.lastModified = lastModified;
		}

		public boolean isGroupRepresentation() {
			return isGroupRepresentation;
		}

		public void setGroupRepresentation(boolean isGroupRepresentation) {
			this.isGroupRepresentation = isGroupRepresentation;
		}

		public String getCreator() {
			return creator;
		}

		public void setCreator(String creator) {
			this.creator = creator;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public UUID getProductId() {
			return productId;
		}

		public void setProductId(UUID productId) {
			this.productId = productId;
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject res = new JSONObject();

			res.put(Constants.JSON_FIELD_ROLES, Utilities.convertRolesListToStringList(userRoles));
			res.put(Constants.JSON_FIELD_IDENTIFIER, userIdentifier);
			res.put(Constants.JSON_FEATURE_FIELD_CREATOR, creator);
			res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified == null?null:lastModified.getTime());						
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate == null?null:creationDate.getTime());
			res.put(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION, isGroupRepresentation);
			res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString());

			return res;			
		}

		public void fromJSON (JSONObject input) throws JSONException {		
			if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
				String sStr = input.getString(Constants.JSON_FIELD_UNIQUE_ID);			
				uniqueId = UUID.fromString(sStr);		
			}

			if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
				long timeInMS = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
				lastModified = new Date(timeInMS);
			}  else {
				lastModified = new Date();
			}

			if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
				long timeInMS = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
				creationDate = new Date(timeInMS);			
			} else {
				creationDate = new Date();
			}

			if (input.containsKey(Constants.JSON_FIELD_IDENTIFIER) && input.get(Constants.JSON_FIELD_IDENTIFIER)!=null) 
				userIdentifier = input.getString(Constants.JSON_FIELD_IDENTIFIER);

			if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) && input.get(Constants.JSON_FEATURE_FIELD_CREATOR)!=null) 
				creator = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR);

			if (input.containsKey(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION) && input.get(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION)!=null) 
				isGroupRepresentation = input.getBoolean(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION);

			//can be null if global user
			if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID))  {
				if (input.get(Constants.JSON_FIELD_PRODUCT_ID)==null) {
					productId = null;
				}
				else {
					String pStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);			
					productId = UUID.fromString(pStr);
				}
			}

			userRoles.clear();
			if (input.containsKey(Constants.JSON_FIELD_ROLES) && input.get(Constants.JSON_FIELD_ROLES)!=null) {				
				JSONArray rolesArr = input.getJSONArray(Constants.JSON_FIELD_ROLES);
				for (int i=0; i<rolesArr.size(); i++) {
					String role = rolesArr.getString(i);
					userRoles.add(Utilities.strToRoleType(role));
				}
			}
		}

		//An Administrator is also a ProductLead, Editor and Viewer
		//A ProductLead is also Editor and Viewer
		//An Editor is also a viewer
		//A TranslationSpecialist is also a viewer
		//An AnalyticsViewer is also a viewer
		//An AnalyticsEditor is also a viewer
		public JSONObject setRolesListByHigherPermission(JSONObject input) throws JSONException {
			//this function is called after validate so we know that the roles list is valid (no duplications, existing roles)
			JSONArray existingRolesArray = input.getJSONArray(Constants.JSON_FIELD_ROLES);
			LinkedHashSet<String> newRolesSet = Utilities.setRolesListByHigherPermission(existingRolesArray);
			input.put(Constants.JSON_FIELD_ROLES, newRolesSet);
			return input;
		}
		
 		public ValidationResults validateAirlockUser(JSONObject input, ServletContext context) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);					

				Action action = Action.ADD;
				if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
					//if JSON contains uniqueId - update an existing feature otherwise create a new feature
					action = Action.UPDATE;
				}

				//validate product existence 
				if (!input.containsKey(Constants.JSON_FIELD_PRODUCT_ID)) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
				}

				Product prod = null;
				String prodId = null;
				if (input.get(Constants.JSON_FIELD_PRODUCT_ID)!=null) { //null is legal => global user
					prodId = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
					prod = productsDB.get(prodId);
					if (prod == null) {
						return new ValidationResults(Strings.productNotFound, Status.BAD_REQUEST);
					}
				}

				//userIdentifier
				if (!input.containsKey(Constants.JSON_FIELD_IDENTIFIER) || input.getString(Constants.JSON_FIELD_IDENTIFIER) == null || input.getString(Constants.JSON_FIELD_IDENTIFIER).isEmpty()) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_IDENTIFIER), Status.BAD_REQUEST);
				}	
				String userIdentifierStr = input.getString(Constants.JSON_FIELD_IDENTIFIER); 

				//creator
				if (!input.containsKey(Constants.JSON_FEATURE_FIELD_CREATOR) || input.getString(Constants.JSON_FEATURE_FIELD_CREATOR) == null || input.getString(Constants.JSON_FEATURE_FIELD_CREATOR).isEmpty()) {
					return new ValidationResults("The creator field is missing.", Status.BAD_REQUEST);
				}

				//isGroupRepresentation
				if (!input.containsKey(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION) || input.get(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION) == null) {
					return new ValidationResults("The isGroupRepresentation field is missing.", Status.BAD_REQUEST);
				}

				input.getBoolean(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION); //validate boolean value

				//userRoles
				if (!input.containsKey(Constants.JSON_FIELD_ROLES) || input.get(Constants.JSON_FIELD_ROLES) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ROLES), Status.BAD_REQUEST);
				}

				JSONArray rolesArr = input.getJSONArray(Constants.JSON_FIELD_ROLES);
				if (rolesArr.size() == 0) {
					return new ValidationResults("The roles field is empty.", Status.BAD_REQUEST);
				}

				for (int i=0; i<rolesArr.size(); i++) {
					String roleStr = rolesArr.getString(i);
					RoleType roleType = Utilities.strToRoleType(roleStr);
					if (roleType == null) { 
						return new ValidationResults("Illegal role '" + roleStr + "'." , Status.BAD_REQUEST);
					}
				}

				//verify that there are no duplications in the roles list
				for(int j = 0; j < rolesArr.length(); j++){
					for(int k = j+1; k < rolesArr.length(); k++){
						if (rolesArr.get(j).equals(rolesArr.get(k))){
							return new ValidationResults("The role value '" + rolesArr.get(k) + "' appears more than once in the roles list.", Status.BAD_REQUEST);
						}
					}
				}

				if (action == Action.ADD) {
					//modification date => should not appear in creation
					if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
						return new ValidationResults("The lastModified field cannot be specified during string creation.", Status.BAD_REQUEST);						
					}

					//creation date => should not appear in creation
					if (input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
						return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
					}

					//userIdentifier must be unique
					//if product user look in the product users else if product == null look in the global users
					if(prod!=null) {
						if (prod.getProductUsers().getUserByIdentifier(userIdentifierStr)!=null) {
							return new ValidationResults("An Airlock user with the specified identifier already exists in the product.", Status.BAD_REQUEST);
						}
					}
					else {
						UserRoleSets globalUsers = (UserRoleSets)context.getAttribute(Constants.AIRLOCK_GLOBAL_USERS_PARAM_NAME);
						if (globalUsers.getUserByIdentifier(userIdentifierStr)!=null) {
							return new ValidationResults("An Airlock user with the specified identifier already exists.", Status.BAD_REQUEST);
						}
					}
					
					//verify that user is global user (cannot ad user to product if he is not global user)
					if(prod!=null) {
						UserRoles userRoles = UserRoles.get(context);
						Set<RoleType> globalUserRoles = userRoles.getUserRoles(userIdentifierStr);
						if (globalUserRoles == null || globalUserRoles.size() == 0) {
							return new ValidationResults(String.format(Strings.notGlobalUser, userIdentifierStr), Status.BAD_REQUEST);
						}
					}
				}
				else { //update
					//creator must exist and not be changed
					String creatorStr = input.getString(Constants.JSON_FEATURE_FIELD_CREATOR);
					if (!creator.equals(creatorStr)) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FEATURE_FIELD_CREATOR), Status.BAD_REQUEST);
					}

					//modification date must appear
					if (!input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || input.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
						return new ValidationResults("The lastModified field is missing. This field must be specified during Airlock user update.", Status.BAD_REQUEST);
					}				

					//verify that given modification date is not older that current modification date
					long givenModoficationDate = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
					Date givenDate = new Date(givenModoficationDate);
					if (givenDate.before(lastModified)) {
						return new ValidationResults("The Airlock user was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
					}

					//creation date must appear
					if (!input.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || input.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
						return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
					}

					//verify that legal long
					long creationdateLong = input.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);

					//verify that was not changed
					if (!creationDate.equals(new Date(creationdateLong))) {
						return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
					}

					//userIdentifier must exist and not be changed
					if (!userIdentifier.equals(userIdentifierStr)) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_IDENTIFIER), Status.BAD_REQUEST);
					}

					//verify that productId was not changed
					if ((prodId == null && productId!=null) || (prodId != null && productId==null) || (prodId!=null && productId!=null && !prodId.equals(productId.toString()))) {
						return new ValidationResults(String.format(Strings.fieldCannotBeChangedDuringUpdate, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
					}					
				}



			} catch (JSONException jsne) {
				return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
			} catch (ClassCastException cce) {
				return new ValidationResults("Illegal AirlockUser JSON: " + cce.getMessage(), Status.BAD_REQUEST);
			}

			return null;
		}

		public String updateAirlockUser (JSONObject updatedUserJSON) throws JSONException {
			//creator, creationDate, userIdentifier, productId should not be updated

			boolean wasChanged = false;
			StringBuilder updateDetails = new StringBuilder();

			//roles
			JSONArray updatedRolesArr = updatedUserJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
			if (!Utilities.compareRolesIgnoreOrder(updatedRolesArr, userRoles)) {
				updateDetails.append(" 'roles' changed from " + Utilities.RoleTypeListToString(userRoles) + " to " + Arrays.toString(Utilities.jsonArrToStringArr(updatedRolesArr)) + "\n");
				
				userRoles.clear();
				for (int i=0; i<updatedRolesArr.size(); i++) {
					String role = updatedRolesArr.getString(i);
					userRoles.add(Utilities.strToRoleType(role));
				}
				wasChanged = true;
			}
			
			//isGroupRepresentation
			Boolean updatedIsGroupRepresentation = updatedUserJSON.getBoolean(Constants.JSON_FIELD_IS_GROUP_REPRESENTATION);
			if (updatedIsGroupRepresentation != isGroupRepresentation) {
				updateDetails.append(" 'isGroupRepresentation' changed from " + isGroupRepresentation + " to " + updatedIsGroupRepresentation + "\n");
				isGroupRepresentation = updatedIsGroupRepresentation;
				wasChanged = true;
			}

			if (wasChanged) {
				lastModified = new Date();
			}

			return updateDetails.toString();
		}		
		
		public UserRoleSet clone(UUID newProductId, UserInfo productCreatorUserInfo) {
			UserRoleSet res = new UserRoleSet();
			res.setUniqueId(UUID.randomUUID());
			res.setLastModified(lastModified);
			res.setUserIdentifier(userIdentifier);
			res.setGroupRepresentation(isGroupRepresentation);			
			res.setCreationDate(creationDate);
			res.setCreator(creator);
			res.setProductId(newProductId);
			res.setUserRoles(Utilities.cloneRoleTypesList(userRoles));
			
			if (productCreatorUserInfo!=null && productCreatorUserInfo.getId().equalsIgnoreCase(userIdentifier)) {
				//for the user created this product - be sure it is a product lead and administrator in the product
				res.addRoleIfMissing(RoleType.ProductLead);
				res.addRoleIfMissing(RoleType.Administrator);
			}
			
			return res;
		}

		private void addRoleIfMissing(RoleType roleToAdd) {
			for (RoleType rt:userRoles) {
				if (rt.equals(roleToAdd))
					return;
			}
			userRoles.add(roleToAdd);
		}
	}

	//Map between user identifier and the user - preserve order
	LinkedHashMap<String, UserRoleSet> airlockUsers = new LinkedHashMap<String, UserRoleSet>();

	public LinkedHashMap<String, UserRoleSet> getAirlockUsers() {
		return airlockUsers;
	}

	public UserRoleSet getUserByIdentifier(String userIdentifier) {
		return airlockUsers.get(userIdentifier.toLowerCase());
	}

	public void addUser(UserRoleSet user) {
		airlockUsers.put(user.getUserIdentifier().toLowerCase(), user);
	}

	public JSONObject toJSON() throws JSONException {
		JSONArray usersArr = new JSONArray();
		for (Map.Entry<String, UserRoleSet> entry : airlockUsers.entrySet()) {
			usersArr.add(entry.getValue().toJSON());
		}
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_USERS, usersArr);

		return res;
	}

	public void fromJSON(JSONObject input, Map<String, UserRoleSet> usersDB) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_USERS) && input.get(Constants.JSON_FIELD_USERS)!=null) {
			JSONArray usersArr = input.getJSONArray(Constants.JSON_FIELD_USERS);
			for (int i=0; i<usersArr.length(); i++ ) {
				JSONObject userJSON = usersArr.getJSONObject(i);
				UserRoleSet user = new UserRoleSet();
				user.fromJSON(userJSON);
				addUser(user);			
				if (usersDB!=null)
					usersDB.put(user.getUniqueId().toString(), user);
			}
		}
	}

	public void removeUser(UserRoleSet user) {
		airlockUsers.remove(user.getUserIdentifier().toLowerCase());
	}
	
	public UserRoleSets clone(Map<String, UserRoleSet> usersDb, UUID newProductId, UserInfo productCreatorUserInfo) {
		UserRoleSets res = new UserRoleSets();
		for (Map.Entry<String, UserRoleSet> entry : airlockUsers.entrySet()) {
			UserRoleSet user = entry.getValue();
			UserRoleSet newUser = user.clone(newProductId, productCreatorUserInfo);
			res.addUser(newUser);
			//add new users to db
			if (usersDb!=null) {
				usersDb.put(newUser.getUniqueId().toString(), newUser);
			}
		}
		return res;
	}
}
