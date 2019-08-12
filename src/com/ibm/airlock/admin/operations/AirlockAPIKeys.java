package com.ibm.airlock.admin.operations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.APIKeyOutputMode;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.authentication.UserRoles;
import com.ibm.airlock.admin.operations.AirlockAPIKey;

public class AirlockAPIKeys {
	// index by uniqueId
	Map<String, AirlockAPIKey> airlockKeysById = new LinkedHashMap<String, AirlockAPIKey>();

	// index by keyName
	Map<String, AirlockAPIKey> airlockKeysByKey = new LinkedHashMap<String, AirlockAPIKey>();

	//Map between AirlockAPIKey password to AirlockAPIKey
	//Map<String, AirlockAPIKey> airlockKeysByPassword = new HashMap<String, AirlockAPIKey>();
	
	//replace if exists
	public void addAPIKey(AirlockAPIKey apiKey) {
		airlockKeysById.put(apiKey.getUniqueId().toString(), apiKey);
		airlockKeysByKey.put(apiKey.getKey(), apiKey);		
	}
	
	public void removeAPIKey(AirlockAPIKey apiKey) {
		airlockKeysById.remove(apiKey.getUniqueId().toString());
		airlockKeysByKey.remove(apiKey.getKey());		
	}
	
	public JSONObject toJSON(APIKeyOutputMode mode) throws JSONException{
		JSONObject res = new JSONObject();
		JSONArray keysArr = new JSONArray();
		Set<String> keysIds = airlockKeysById.keySet();
		for (String keyId:keysIds) {
			keysArr.add(airlockKeysById.get(keyId).toJSON(mode));		
		}
		res.put (Constants.JSON_FIELD_AIRLOCK_API_KEYS, keysArr);
		
		return res;
	}
	
	public JSONObject toJSONForOwner(APIKeyOutputMode mode, String owner) throws JSONException{
		JSONObject res = new JSONObject();
		JSONArray keysArr = new JSONArray();
		Set<String> keysIds = airlockKeysById.keySet();
		for (String keyId:keysIds) {
			AirlockAPIKey apiKey = airlockKeysById.get(keyId);
			if (apiKey.getOwner().toLowerCase().equals(owner.toLowerCase())) {
				keysArr.add(apiKey.toJSON(mode));
			}
		}
		res.put (Constants.JSON_FIELD_AIRLOCK_API_KEYS, keysArr);
		
		return res;
	}
	
	public void fromJSON (JSONObject keysJSON, JSONObject keysPasswordsJSON) throws JSONException {
		JSONArray keysArr = keysJSON.getJSONArray(Constants.JSON_FIELD_AIRLOCK_API_KEYS);
		JSONArray keysPasswordsArr = keysPasswordsJSON.getJSONArray(Constants.JSON_FIELD_AIRLOCK_API_KEYS);
		
		//build map between id and password
		HashMap<String, String> passwordsMap = new HashMap<String, String>();
		for (int i=0; i<keysPasswordsArr.size(); i++) {
			JSONObject apiKeyPasswordJSON = keysPasswordsArr.getJSONObject(i);			
			passwordsMap.put(apiKeyPasswordJSON.getString(Constants.JSON_FIELD_UNIQUE_ID), apiKeyPasswordJSON.getString(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD));
		}
		
		//both arrays are of the same size		
		for (int i=0; i<keysArr.size(); i++) {
			AirlockAPIKey apiKey = new AirlockAPIKey();
			
			JSONObject apiKeyJSON = keysArr.getJSONObject(i);
			
			apiKey.fromJSON(apiKeyJSON, passwordsMap);
			
			addAPIKey(apiKey);			
		}
	}
	
	//return null if does not exist
	public AirlockAPIKey getAPIKeyByKeyname(String name) {
		return airlockKeysByKey.get(name); 
	}

	
	//return null if does not exist
	public AirlockAPIKey getAPIKeyById(String keyId) {
		return airlockKeysById.get(keyId); 
	}
	
	//return null if does not exist
	public AirlockAPIKey getAPIKeyByKey(String key) {
		Set<String> keyIds = airlockKeysById.keySet(); 
		for (String keyId:keyIds) {
			AirlockAPIKey apiKey = airlockKeysById.get(keyId);
			if (apiKey.getKey().equals(key))
				return apiKey;
		}
		return null; 
	}

	static public AirlockAPIKeys get(ServletContext context)
	{
		return (AirlockAPIKeys) context.getAttribute(Constants.API_KEYS_PARAM_NAME);
	}

	//when users list is updated the keys list may be updated (when user that owns key is deleted or when some of his permissions are removed) 
	public String updateApiKeysWithNewUsersRoles(ServletContext context, Product product, String userChanged) {
		StringBuilder sb = new StringBuilder();
		
		
		if (product == null) { //global role set was changed - update global roles in keys
			UserRoles userRoles= UserRoles.get(context);

			LinkedList<AirlockAPIKey> apiKeysToRemove = new LinkedList<AirlockAPIKey>();
			Set<String> keysIds = airlockKeysById.keySet();
			for (String keyId:keysIds) {
				AirlockAPIKey apiKey = airlockKeysById.get(keyId);
				String keyOwner = apiKey.getOwner();
				
				Set<RoleType> userNewRoles = userRoles.getUserRoles(keyOwner);
				if (userNewRoles == null || userNewRoles.size() == 0) {
					//the user's global role set was deleted
					sb.append("key '" + apiKey.getKey() + "' global roles were deleted because its owner " + apiKey.getOwner() + " global role set was removed \n");
					apiKey.getRoles().clear();
					
					if (apiKey.getRolesPerPropduct().size() == 0) {
						apiKeysToRemove.add(apiKey);
						sb.append("key '" + apiKey.getKey() + "' was deleted because its owner " + apiKey.getOwner() + " global role set was removed and the key has no product roles.\n");
					}					
				}
				else {						
					//update key's permissions if roles were removed from owner
					Iterator<RoleType> keyRolesIterator = apiKey.getRoles().iterator();
					while (keyRolesIterator.hasNext()){
						RoleType keyRole = keyRolesIterator.next();
						if (!userNewRoles.contains(keyRole)) {
							keyRolesIterator.remove();
							sb.append("Role '" + keyRole.toString() + "' was removed from key '" + apiKey.getKey() + "' because the role was removed from the key owner " + apiKey.getOwner() + "\n");
						}
					}	
					if (apiKey.getRoles().isEmpty()) {					
						sb.append("key '" + apiKey.getKey() + "' global roles were deleted because the global role of the key owner " + apiKey.getOwner() + " was removed.\n");
						if (apiKey.getRolesPerPropduct().size() == 0) {
							apiKeysToRemove.add(apiKey);
							sb.append("key '" + apiKey.getKey() + "' was deleted because its owner " + apiKey.getOwner() + " global role set was removed and the key has no product roles.\n");
						}
					}
				}
			}
			for (AirlockAPIKey key:apiKeysToRemove) {
				removeAPIKey(key);
			}
		}
		else { //product role set was changed - update the product roles in keys
			Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
			
			UserRoles userRoles= rolesPerProductMap.get(product.getUniqueId().toString());

			LinkedList<AirlockAPIKey> apiKeysToRemove = new LinkedList<AirlockAPIKey>();
			Set<String> keysIds = airlockKeysById.keySet();
			for (String keyId:keysIds) {
				AirlockAPIKey apiKey = airlockKeysById.get(keyId);
				String keyOwner = apiKey.getOwner();
				
				
				Set<RoleType> userNewRoles = userRoles.getUserRoles(keyOwner);
				if (userNewRoles == null || userNewRoles.size() == 0) {
					//the user's global role set was deleted
					sb.append("key '" + apiKey.getKey() + "' roles for product " + product.getUniqueId().toString() +" were deleted because its owner " + apiKey.getOwner() + " product role set was removed \n");
					apiKey.removeRolesForProduct(product);
					
					if (apiKey.getRolesPerPropduct().size() == 0 && apiKey.getRoles().isEmpty()) {
						apiKeysToRemove.add(apiKey);
						sb.append("key '" + apiKey.getKey() + "' was deleted because its owner " + apiKey.getOwner() + " product role set was removed and the key has no global roles or other product roles.\n");
					}					
				}
				else {						
					if (apiKey.getProductRoles(product)!=null) {
						//update key's permissions if roles were removed from owner
						Iterator<RoleType> keyRolesIterator = apiKey.getProductRoles(product).iterator();
						while (keyRolesIterator.hasNext()){
							RoleType keyRole = keyRolesIterator.next();
							if (!userNewRoles.contains(keyRole)) {
								keyRolesIterator.remove();
								sb.append("Role '" + keyRole.toString() + "' was removed from key '" + apiKey.getKey() + "' for product " + product.getUniqueId().toString() + " because the role was removed from the key owner " + apiKey.getOwner() + "\n");
							}
						}	
						if (apiKey.getProductRoles(product).isEmpty()) {	
							apiKey.removeRolesForProduct(product);
							sb.append("key '" + apiKey.getKey() + "' product roles for product " + product.getUniqueId().toString()  + " were deleted because the product role of the key owner " + apiKey.getOwner() + " was removed.\n");
							if (apiKey.getRolesPerPropduct().size() == 0 && apiKey.getRoles().isEmpty()) {
								apiKeysToRemove.add(apiKey);
								sb.append("key '" + apiKey.getKey() + "' was deleted because its owner " + apiKey.getOwner() + " product role set for product " + product.getUniqueId().toString() + " was removed and the key has no other product roles or global roles.\n");
							}
						}
					}
				}
			}
			
			for (AirlockAPIKey key:apiKeysToRemove) {
				removeAPIKey(key);
			}
		}
		return sb.toString();
	}

	//when users list is updated the keys list may be updated (when user that owns key is deleted or when some of his permissions are removed) 
	public String deleteApiKeysForDeletedUser(ServletContext context, String userIdentifier) {
		StringBuilder sb = new StringBuilder();
		
		LinkedList<AirlockAPIKey> apiKeysToRemove = new LinkedList<AirlockAPIKey>();
		Set<String> keysIds = airlockKeysById.keySet();
		for (String keyId:keysIds) {
			AirlockAPIKey apiKey = airlockKeysById.get(keyId);
			String keyOwner = apiKey.getOwner();
			if (keyOwner.equals(userIdentifier)) {
				apiKeysToRemove.add(apiKey);				
				sb.append("key '" + apiKey.getKey() + "' was deleted because its owner " + keyOwner + " was removed \n");				
			}
		}
		for (AirlockAPIKey key:apiKeysToRemove) {
			removeAPIKey(key);
		}
		return sb.toString();
	}

	//return true if product roles exist in one or more keys
	public boolean removeProductRolesFromKeys (Product product) {
		boolean found = false;
		Set<String> keysIds = airlockKeysById.keySet();
		for (String keyId:keysIds) {
			AirlockAPIKey apiKey = airlockKeysById.get(keyId);
			if (apiKey.getProductRoles(product) != null) {
				found = true;
				apiKey.removeRolesForProduct(product);
				if (apiKey.getRolesPerPropduct().isEmpty() && apiKey.getRoles().isEmpty()) {
					removeAPIKey(apiKey); //if the product's roles were the only roles in this key - delete the key
				}
			}
		}
		return found;
	}
}
