package com.ibm.airlock.admin.operations;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.APIKeyOutputMode;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.authentication.UserRoles;

//TODO: validation during product roles change
public class AirlockAPIKey {
	private class ProductRolesPair {
		private UUID productId;
		private Set<RoleType> productRoles = new TreeSet<RoleType>();
		public UUID getProductId() {
			return productId;
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_PRODUCT_ID, productId.toString());
			res.put(Constants.JSON_FIELD_ROLES, Utilities.roleTypeslistToJsonArray(productRoles));
			
			return res;
		}
		
		public void fromJSON (JSONObject prodRolesJSON) throws JSONException {
			if (prodRolesJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && prodRolesJSON.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
				String sStr = prodRolesJSON.getString(Constants.JSON_FIELD_PRODUCT_ID);			
				productId = UUID.fromString(sStr);
			}
			
			if (prodRolesJSON.containsKey(Constants.JSON_FIELD_ROLES) && prodRolesJSON.get(Constants.JSON_FIELD_ROLES)!=null) {				
				JSONArray rolesArr = prodRolesJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
			
				productRoles.clear();
				
				for (int j=0; j<rolesArr.size(); j++) {
					String roleStr = rolesArr.getString(j);
					RoleType role = Utilities.strToRoleType(roleStr);
					if (role == null)
						throw new JSONException("invalid role " + roleStr);
					productRoles.add(role);
				}
			}	
		}	
		
		public ValidationResults validateProductRolesJSON (JSONObject prodRolesJSON, ServletContext context, UserInfo userInfo) throws JSONException {
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			
			//product id 
			if (!prodRolesJSON.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || prodRolesJSON.get(Constants.JSON_FIELD_PRODUCT_ID) == null || prodRolesJSON.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}		
			String prodIdStr = prodRolesJSON.getString(Constants.JSON_FIELD_PRODUCT_ID);
			
			String err = Utilities.validateLegalUUID(prodIdStr);
			if (err!=null) {
				return new ValidationResults(Strings.illegalProductUUID + err, Status.BAD_REQUEST);				
			}
			
			Product prod = productsDB.get(prodIdStr);
			if (prod == null) {
				return new ValidationResults(Strings.productNotFound, Status.NOT_FOUND);			
			}
			
			//roles
			if (!prodRolesJSON.containsKey(Constants.JSON_FIELD_ROLES) || prodRolesJSON.get(Constants.JSON_FIELD_ROLES) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ROLES), Status.BAD_REQUEST);
			}
			
			JSONArray rolesArr = prodRolesJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
			ValidationResults res = validateRolesList(rolesArr);
			if (res!=null)
				return res;
			
			@SuppressWarnings("unchecked")
			Map<String,UserRoles> rolesPerProductMap = (Map<String,UserRoles>) context.getAttribute(Constants.USER_ROLES_PER_PRODUCT_PARAM_NAME);
			UserRoles userProdRoles = rolesPerProductMap.get(prodIdStr);
			try {
				userProdRoles.getRoleSubset(userInfo.getId(), rolesStrArrayToRoleTypeSet(rolesArr) );
			} catch (Exception e) {
				return new ValidationResults(e.getMessage(), Status.BAD_REQUEST);
			}
						
			return null;
		}
	}
	
	private UUID uniqueId;
	private Set<RoleType> roles = new TreeSet<RoleType>();//c+u
	private String owner; //nc+u (not changed)
	private Date creationDate; 
	private String password; //nc+nu - this is actually the password hash.
	private String key; //c+u (not changed)
	private Date lastModified = null; // nc + u
	
	//map between product_id and object containing prod_id and set of roles
	private LinkedHashMap<String, ProductRolesPair> rolesPerPropduct = new LinkedHashMap<String, ProductRolesPair>(); //nc+nu
	
	public UUID getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}
	public Set<RoleType> getRoles() {
		return roles;
	}
	public void setRoles(Set<RoleType> roles) {
		this.roles = roles;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		//this.password = password;
		this.password = PasswordHash.generatePasswordHash(password);
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	
	//no need to validate since is not created with json but from generateAirlockKey call.
	
	public LinkedHashMap<String, ProductRolesPair> getRolesPerPropduct() {
		return rolesPerPropduct;
	}
	public void setRolesPerPropduct(LinkedHashMap<String, ProductRolesPair> rolesPerPropduct) {
		this.rolesPerPropduct = rolesPerPropduct;
	}
	public JSONObject toJSON(APIKeyOutputMode mode) throws JSONException {
		JSONObject res = new JSONObject();
		
		res.put(Constants.JSON_FIELD_UNIQUE_ID, uniqueId==null?null:uniqueId.toString());
		
		if (!mode.equals(APIKeyOutputMode.WITHOUT_PASSWORD)) {
			res.put(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD, password);
		}
		
		if (!mode.equals(APIKeyOutputMode.ONLY_PASSWORD)) {
			res.put(Constants.JSON_FIELD_ROLES, Utilities.roleTypeslistToJsonArray(roles));
			res.put(Constants.JSON_FEATURE_FIELD_OWNER, owner);
			res.put(Constants.JSON_FIELD_KEY, key);
			res.put(Constants.JSON_FEATURE_FIELD_CREATION_DATE, creationDate == null?null:creationDate.getTime());
			res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
			res.put(Constants.JSON_FIELD_PRODUCTS, rolesPerPropduct2JsonArray());
		}
		
		return res;			
	}
	
	private JSONArray rolesPerPropduct2JsonArray() throws JSONException {
		JSONArray riolesPerProdArr = new JSONArray();
		Set<String> productIds = rolesPerPropduct.keySet();
		for (String prodId:productIds) {
			riolesPerProdArr.add(rolesPerPropduct.get(prodId).toJSON());				
		}
		return riolesPerProdArr;
	}
	
	private void jsonArray2rolesPerPropduct(JSONArray rolesPerProdJsonArr, LinkedHashMap<String, ProductRolesPair> rolesPerPropduct) throws JSONException {
		if (rolesPerProdJsonArr == null)
			return;
		
		rolesPerPropduct.clear();
		for (int i=0; i<rolesPerProdJsonArr.size(); i++) {
			ProductRolesPair productRolesPair = new ProductRolesPair();
			JSONObject prodRolesJSON = rolesPerProdJsonArr.getJSONObject(i);
			productRolesPair.fromJSON(prodRolesJSON);
			rolesPerPropduct.put(productRolesPair.getProductId().toString(), productRolesPair);
		}
	}
	
	public String updateAPIKeyJSON (JSONObject apiKeyJSON) throws JSONException {
		//the only roles and products fields that can be updated
		
		boolean wasChanged = false;
		StringBuilder updateDetails = new StringBuilder();

		JSONArray updatedRoles = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
		Set<RoleType> newRoles = new TreeSet<RoleType>();
		for (int i = 0; i < updatedRoles.size(); ++i)
		{
			String roleStr = updatedRoles.getString(i);
			RoleType role = Utilities.strToRoleType(roleStr);
			if (role == null)
				throw new JSONException("invalid role type " + roleStr);
			newRoles.add(role);
		}

		if (!roles.equals(newRoles))
		{
			updateDetails.append(" 'roles' changed from " + roles + " to " + newRoles + "\n");
			roles = newRoles;
			wasChanged = true;
		}
		
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_PRODUCTS) && apiKeyJSON.get(Constants.JSON_FIELD_PRODUCTS)!=null) {
			JSONArray rolesPerProdArr = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);
			String updateProdRolesDetails = compareProductsRolesSetsIgnoreOrder(rolesPerPropduct, rolesPerProdArr);
			if (!updateProdRolesDetails.isEmpty()) {
				updateDetails.append(updateProdRolesDetails);
				wasChanged = true;
				jsonArray2rolesPerPropduct(rolesPerProdArr, rolesPerPropduct);	
			}
		}	
		
		if (wasChanged) {
			lastModified = new Date();
		}
		
		return updateDetails.toString();
	}

	public ValidationResults validateAPIKeyJSON(JSONObject apiKeyJSON, AirlockAPIKeys apiKeys, UserInfo userInfo,
			UserRoles userRoles, ServletContext context) {
		try {
			if (userInfo == null) {
				return new ValidationResults(Strings.systemIsNotAuthenticated, Status.BAD_REQUEST);
			}
			
			
			//key 
			if (!apiKeyJSON.containsKey(Constants.JSON_FIELD_KEY) || apiKeyJSON.get(Constants.JSON_FIELD_KEY) == null || apiKeyJSON.getString(Constants.JSON_FIELD_KEY).isEmpty()) {
				return new ValidationResults("The key field is missing.", Status.BAD_REQUEST);
			}		
			
			String keyName = apiKeyJSON.getString(Constants.JSON_FIELD_KEY);
			String validateNameErr = Utilities.validateName(keyName);
			if(validateNameErr!=null) {
				return new ValidationResults(validateNameErr, Status.BAD_REQUEST);
			}
			
			//password
			if (apiKeyJSON.containsKey(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD) && apiKeyJSON.get(Constants.JSON_FIELD_AIRLOCK_KEY_PASSWORD)!=null) {
				return new ValidationResults("The keyPassword field cannot be specified during API key creation or update.", Status.BAD_REQUEST);
			}			
			
			//products roles
			JSONArray rolesPerProductArr = null;
			Set<String> existingProdIds = new HashSet<String>();
			if (apiKeyJSON.containsKey(Constants.JSON_FIELD_PRODUCTS) && apiKeyJSON.get(Constants.JSON_FIELD_PRODUCTS)!=null ) { //not mandatory (can be also null or empty)
				
				rolesPerProductArr = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);
				for (int i=0; i<rolesPerProductArr.size(); i++) {
					JSONObject prodRolesJSON = rolesPerProductArr.getJSONObject(i);
					ProductRolesPair prodRolesPair = new ProductRolesPair();
					ValidationResults vr = prodRolesPair.validateProductRolesJSON(prodRolesJSON, context, userInfo);
					if (vr!=null) {
						return vr;
					}
					String prodId = prodRolesJSON.getString("productId");
					if (existingProdIds.contains(prodId)) {
						return new ValidationResults("product is '" + prodId + "' appears more than once.", Status.BAD_REQUEST);
					}
						
					existingProdIds.add(prodId);
				}
			}
			
			//roles
			if (!apiKeyJSON.containsKey(Constants.JSON_FIELD_ROLES) || apiKeyJSON.get(Constants.JSON_FIELD_ROLES) == null) {
				return new ValidationResults("The roles field is missing.", Status.BAD_REQUEST);
			}
			
			JSONArray rolesArr = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
			if (rolesArr.isEmpty()) {
				//global roles can be empty if there are products roles
				if (rolesPerProductArr == null || rolesPerProductArr.isEmpty()) {
					return new ValidationResults("The roles field is missing.", Status.BAD_REQUEST);
				}
			}
			else {
				ValidationResults res = validateRolesList(rolesArr);
				if (res!=null)
					return res;
				
				try {
					userRoles.getRoleSubset(userInfo.getId(), rolesStrArrayToRoleTypeSet(rolesArr) );
				} catch (Exception e) {
					return new ValidationResults(e.getMessage(), Status.BAD_REQUEST);
				}
			}
			Action action = Action.ADD;			
			if (apiKeyJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && apiKeyJSON.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing feature otherwise create a new feature
				action = Action.UPDATE;
			}						
			
			if (action == Action.ADD) {
				//modification date => should not appear in feature creation
				if (apiKeyJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && apiKeyJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
					return new ValidationResults("The lastModified field cannot be specified during API key creation.", Status.BAD_REQUEST);
				}	
				
				//creation date => should not appear in branch creation
				if (apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) {
					return new ValidationResults("The creationDate field cannot be specified during creation.", Status.BAD_REQUEST);
				}
								
				//owner
				if (apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_OWNER) != null) {
					return new ValidationResults("The owner field cannot be specified during API key creation.", Status.BAD_REQUEST);
				}
				
				//key is unique - verify that such key does not already exist
				String newKey = apiKeyJSON.getString(Constants.JSON_FIELD_KEY);
				if (apiKeys.getAPIKeyByKey(newKey)!=null) {
					return new ValidationResults("An API key with the specified 'key' already exists.", Status.BAD_REQUEST);
				}
				
			}
			else {
				//modification date must appear in update
				if (!apiKeyJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || apiKeyJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
					return new ValidationResults("The lastModified field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}				
	
				//verify that given modification date is not older that current modification date
				long givenModoficationDate = apiKeyJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
				Date givenDate = new Date(givenModoficationDate);
				if (givenDate.before(lastModified)) {
					return new ValidationResults("The API key was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
				}
				
				//creation date must appear
				if (!apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) || apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)==null) {
					return new ValidationResults("The creationDate field is missing. This field must be specified during update.", Status.BAD_REQUEST);
				}
				
				//verify that legal long
				long creationdateLong = apiKeyJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
				
				//verify that was not changed
				if (!creationDate.equals(new Date(creationdateLong))) {
					return new ValidationResults("creationDate cannot be changed during update", Status.BAD_REQUEST);
				}								
				
				//owner
				if (!apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) || apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_OWNER) == null) {
					return new ValidationResults("The owner field is missing.", Status.BAD_REQUEST);
				}
				
				//owner cannot be changed during update
				String updatedOwner = apiKeyJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
				if (!owner.equals(updatedOwner)) {
					return new ValidationResults("owner cannot be changed during update", Status.BAD_REQUEST);
				}
				
				//key cannot be changed during update
				String updatedKey = apiKeyJSON.getString(Constants.JSON_FIELD_KEY);
				if (!key.equals(updatedKey)) {
					return new ValidationResults("key cannot be changed during update", Status.BAD_REQUEST);
				}
					
			}
		}catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
			
		
		return null;
		
	}
			
	private Set<RoleType> rolesStrArrayToRoleTypeSet(JSONArray rolesArr) throws JSONException {
		Set<RoleType> roleTypesSet = new HashSet<>();
		for (int i=0; i<rolesArr.size(); i++) {
			String roleStr = rolesArr.getString(i);
			RoleType role = Utilities.strToRoleType(roleStr);			
			roleTypesSet.add(role);
		}

		return roleTypesSet;
	}
	
	public void fromJSON (JSONObject apiKeyJSON, HashMap<String, String> passwordsMap) throws JSONException {		
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && apiKeyJSON.get(Constants.JSON_FIELD_UNIQUE_ID) != null) {
			String sStr = apiKeyJSON.getString(Constants.JSON_FIELD_UNIQUE_ID);			
			uniqueId = UUID.fromString(sStr);		
		}
			
		if (apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_CREATION_DATE) && apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_CREATION_DATE)!=null) { 
			long timeInMS = apiKeyJSON.getLong(Constants.JSON_FEATURE_FIELD_CREATION_DATE);
			creationDate = new Date(timeInMS);			
		} 
		else {
			creationDate = new Date();
		}
		
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && apiKeyJSON.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) { 
			long timeInMS = apiKeyJSON.getLong(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}
		
		if (apiKeyJSON.containsKey(Constants.JSON_FEATURE_FIELD_OWNER) && apiKeyJSON.get(Constants.JSON_FEATURE_FIELD_OWNER)!=null) 
			owner = apiKeyJSON.getString(Constants.JSON_FEATURE_FIELD_OWNER);
		
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_KEY) && apiKeyJSON.get(Constants.JSON_FIELD_KEY)!=null) 
			key = apiKeyJSON.getString(Constants.JSON_FIELD_KEY);
		
		roles.clear();
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_ROLES) && apiKeyJSON.get(Constants.JSON_FIELD_ROLES)!=null) {				
			JSONArray rolesArr = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_ROLES);
			for (int i=0; i<rolesArr.size(); i++) {
				String roleStr = rolesArr.getString(i);
				RoleType role = Utilities.strToRoleType(roleStr);
				if (role == null)
					throw new JSONException("invalid role " + roleStr);
				roles.add(role);
			}
		}
		
		if (passwordsMap!= null && passwordsMap.containsKey(uniqueId.toString())) 
			password = passwordsMap.get(uniqueId.toString());
		
		if (apiKeyJSON.containsKey(Constants.JSON_FIELD_PRODUCTS) && apiKeyJSON.get(Constants.JSON_FIELD_PRODUCTS)!=null) {
			JSONArray rolesPerProdArr = apiKeyJSON.getJSONArray(Constants.JSON_FIELD_PRODUCTS);
			jsonArray2rolesPerPropduct(rolesPerProdArr, rolesPerPropduct);
		}		
	}
	
	private ValidationResults validateRolesList(JSONArray roles) {
		try {
			if (roles == null || roles.isEmpty()) {
				return new ValidationResults("No roles specified", Status.BAD_REQUEST);
			}
			
			TreeSet<String> rolesSet = new TreeSet<>();		
			for (int i=0; i<roles.size(); i++) {
				String role=roles.getString(i);
				if (rolesSet.contains(role)) {
					return new ValidationResults("role '" + role + "' appears more than once.", Status.BAD_REQUEST);
				}
				if (Utilities.strToRoleType(role) == null) {
					return new ValidationResults("Illegal role '" + role + "'", Status.BAD_REQUEST);
				}
				
				rolesSet.add(role);
			}
		} catch (JSONException je) {
			return new ValidationResults(je.getMessage(), Status.BAD_REQUEST);
		}
		
		
		return null;
	}		

	//return empty string if equals. String with the differences otherwise
	private String compareProductsRolesSetsIgnoreOrder(LinkedHashMap<String, ProductRolesPair> rolesPerPropductMap, JSONArray rolesPerProdArray) throws JSONException {
		if (rolesPerPropductMap == null && rolesPerProdArray == null)
			return "";
			
		StringBuilder updateDetails = new StringBuilder();

		Set<String> existingProductIds = rolesPerPropductMap.keySet();
		
		LinkedHashMap<String, ProductRolesPair> updatedRolesPerPropduct = new LinkedHashMap<String, ProductRolesPair>();
		jsonArray2rolesPerPropduct(rolesPerProdArray, updatedRolesPerPropduct);
		Set<String> updatedProductIds = updatedRolesPerPropduct.keySet();
		
		for (String existingProdId:existingProductIds) {
			if (updatedProductIds.contains(existingProdId)) {
				//product in both maps
				ProductRolesPair existingProdRoles = rolesPerPropductMap.get(existingProdId);
				ProductRolesPair updatedProdRoles = updatedRolesPerPropduct.get(existingProdId);
				
				if (!existingProdRoles.productRoles.equals(updatedProdRoles.productRoles)) {
					updateDetails.append(" 'roles' of product " + existingProdId + " changed from " + existingProdRoles.productRoles + " to " + updatedProdRoles.productRoles + "\n");
				}
			}
			else {
				//product in existing but not in updated - its roles were removed
				updateDetails.append(" 'roles' of product " + existingProdId + " were removed\n");
			}
		}
		
		for (String updatedProdId:updatedProductIds) {
			if (!existingProductIds.contains(updatedProdId)) {
				//product in updated but not in exiting - its roles were added
				ProductRolesPair updatedProdRoles = updatedRolesPerPropduct.get(updatedProdId);
				
				updateDetails.append(" 'roles' " + updatedProdRoles.productRoles + " added to product " + updatedProdId + "\n");				
			}
		}
		
		return updateDetails.toString(); 
	}
	
	//return null if no roles set to the given product
	public Set<RoleType> getProductRoles (Product product) {
		String prodId = product.getUniqueId().toString();
		if (rolesPerPropduct.get(prodId)!=null) {
			return rolesPerPropduct.get(prodId).productRoles;
		}
		return  null;
	}
	
	public void removeRolesForProduct(Product product) {
		String prodId = product.getUniqueId().toString();
		rolesPerPropduct.remove(prodId);
	}
	public void addProductAdminRole(Product product) {
		String prodId = product.getUniqueId().toString();
		ProductRolesPair productRoles = rolesPerPropduct.get(prodId);
		if (productRoles == null) {
			productRoles = new ProductRolesPair();
			productRoles.productId = product.getUniqueId();
			rolesPerPropduct.put(prodId, productRoles);
		}
		productRoles.productRoles.add(RoleType.Administrator);
		productRoles.productRoles.add(RoleType.ProductLead);
		productRoles.productRoles.add(RoleType.Editor);
		productRoles.productRoles.add(RoleType.Viewer);		
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
		
		if (input.containsKey(Constants.JSON_FIELD_PRODUCTS) && input.get(Constants.JSON_FIELD_PRODUCTS)!=null) {
			JSONArray productsArray = input.getJSONArray(Constants.JSON_FIELD_PRODUCTS);
			for (int i=0; i<productsArray.size(); i++) {
				JSONObject prodObj = productsArray.getJSONObject(i);
				JSONArray prodExistingRolesArray = prodObj.getJSONArray(Constants.JSON_FIELD_ROLES);
				LinkedHashSet<String> prodNewRolesSet = Utilities.setRolesListByHigherPermission(prodExistingRolesArray);
				prodObj.put(Constants.JSON_FIELD_ROLES, prodNewRolesSet);	
			}
		}
		
		return input;
	}

}