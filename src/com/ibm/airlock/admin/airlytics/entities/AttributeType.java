package com.ibm.airlock.admin.airlytics.entities;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.db.DbHandler;

//TODO: handle default roles
public class AttributeType extends BaseAirlyticsItem{
	public static enum PI_STATE {
		PI,
		NOT_PI
	}
	
	public static enum AttributesPermission {
		READ_ONLY, //by default AnalyticsViewer
		READ_WRITE, //by default AnalyticsEditor
		READ_WRITE_DEPRECATE, //by default AnalyticsPowerUser
		READ_WRITE_DELETE ////by default Administrator
	}
	
	private String dbTable;
	private PI_STATE piState;
	private UUID entityId; 
	private String athenaTable;
	
	private Map<AttributesPermission, RoleType> permissionsMap = generateDefaultAttributesPermissionsMap();
	
	public UUID getEntityId() {
		return entityId;
	}
	
	private static Map<AttributesPermission, RoleType> generateDefaultAttributesPermissionsMap() {
		Map<AttributesPermission, RoleType> res = new LinkedHashMap<>();
		res.put(AttributesPermission.READ_ONLY, RoleType.AnalyticsViewer);
		res.put(AttributesPermission.READ_WRITE, RoleType.AnalyticsEditor);
		res.put(AttributesPermission.READ_WRITE_DEPRECATE, RoleType.AnalyticsPowerUser);
		res.put(AttributesPermission.READ_WRITE_DELETE, RoleType.Administrator);
		return res;
	}
	
	public void setEntityId(UUID entityId) {
		this.entityId = entityId;
	}
	public String getDbTable() {
		return dbTable;
	}
	public void setDbTable(String dbTable) {
		this.dbTable = dbTable;
	}
	public PI_STATE getPiState() {
		return piState;
	}
	public void setPiState(PI_STATE piState) {
		this.piState = piState;
	}
	
	public String getAthenaTable() {
		return athenaTable;
	}
	public void setAthenaTable(String athenaTable) {
		this.athenaTable = athenaTable;
	}
	
	public Map<AttributesPermission, RoleType> getPermissionsMap() {
		return permissionsMap;
	}

	public void setPermissionsMap(Map<AttributesPermission, RoleType> permissionsMap) {
		this.permissionsMap = permissionsMap;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject res = super.toJson();
		res.put(Constants.JSON_FIELD_ENTITY_ID, entityId==null?null:entityId.toString()); 
		res.put(Constants.JSON_FIELD_PI_STATE, piState.toString());
		res.put(Constants.JSON_FIELD_DB_TABLE, dbTable);
		res.put(Constants.JSON_FIELD_ATHENA_TABLE, athenaTable);
		res.put(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION, permissionsMapToJsonObject());
		return res;
	}
	
	private JSONObject permissionsMapToJsonObject() throws JSONException {
		JSONObject attributesPermissionObj = new JSONObject();
		for (Map.Entry<AttributesPermission, RoleType> entry : permissionsMap.entrySet()) {
			attributesPermissionObj.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return attributesPermissionObj;
	}

	public void fromJSON (JSONObject input) throws JSONException {
		super.fromJSON(input);
		if (input.containsKey(Constants.JSON_FIELD_ENTITY_ID) && input.get(Constants.JSON_FIELD_ENTITY_ID) != null) {
			String pStr = input.getString(Constants.JSON_FIELD_ENTITY_ID);			
			entityId = UUID.fromString(pStr);		
		}
		
		piState = Utilities.valueOf(PI_STATE.class, input.getString(Constants.JSON_FIELD_PI_STATE));
		
		dbTable = input.getString(Constants.JSON_FIELD_DB_TABLE);
		athenaTable = input.getString(Constants.JSON_FIELD_ATHENA_TABLE);
		
		if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION)) {
			JSONObject attributesPermissionObj = input.getJSONObject(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION);
			JsonObjectToPermissionMap(attributesPermissionObj);
		}
	}
	
	private void JsonObjectToPermissionMap(JSONObject attributesPermissionObj) throws JSONException {
		permissionsMap.clear();
		permissionsMap.put(AttributesPermission.READ_ONLY, Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_ONLY.toString())));
		permissionsMap.put(AttributesPermission.READ_WRITE, Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE.toString())));
		permissionsMap.put(AttributesPermission.READ_WRITE_DEPRECATE, Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DEPRECATE.toString())));
		permissionsMap.put(AttributesPermission.READ_WRITE_DELETE, Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DELETE.toString())));
	}
	
	private boolean isAttributesPermissionEqual(JSONObject attributesPermissionObj, Map<AttributesPermission, RoleType> attributesPermissionMap) throws JSONException {
		if (!attributesPermissionMap.get(AttributesPermission.READ_ONLY).equals(Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_ONLY.toString())))) {
			return false;
		}
		
		if (!attributesPermissionMap.get(AttributesPermission.READ_WRITE).equals(Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE.toString())))) {
			return false;
		}
		
		if (!attributesPermissionMap.get(AttributesPermission.READ_WRITE_DEPRECATE).equals(Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DEPRECATE.toString())))) {
			return false;
		}
		
		if (!attributesPermissionMap.get(AttributesPermission.READ_WRITE_DELETE).equals(Utilities.strToRoleType(attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DELETE.toString())))) {
			return false;
		}
		
		return true;
	}

	//return null if valid, ValidationResults otherwise
	public ValidationResults validateJSON(JSONObject input, ServletContext context) {
		ValidationResults res = super.validateJSON(input);
		if (res !=null) {
			return res;
		}
		
		try {
			//entityId
			if (!input.containsKey(Constants.JSON_FIELD_ENTITY_ID) || input.getString(Constants.JSON_FIELD_ENTITY_ID) == null || input.getString(Constants.JSON_FIELD_ENTITY_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ENTITY_ID), Status.BAD_REQUEST);
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
			String newName = input.getString(Constants.JSON_FIELD_NAME); //exists - after base class validation
			String eId = input.getString(Constants.JSON_FIELD_ENTITY_ID); 
			Entity entity = entitiesDB.get(eId);
			if (entity == null) {
				return new ValidationResults(Strings.entityNotFound, Status.BAD_REQUEST);
			}
			
			//validate name uniqueness within entity
			for (AttributeType at:entity.getAttributeTypes()) {
				if (uniqueId != null && uniqueId.toString().equals(at.getUniqueId().toString())) {
					continue; //skip the current attribute in update				
				}
				
				if (newName.equals(at.getName())) {
					return new ValidationResults(Strings.notUniqueAttributeTypeNameWithinEntity, Status.BAD_REQUEST);
				}
			}
			
			//piState
			if (!input.containsKey(Constants.JSON_FIELD_PI_STATE) || input.get(Constants.JSON_FIELD_PI_STATE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PI_STATE), Status.BAD_REQUEST);
			}

			String piStateStr = input.getString(Constants.JSON_FIELD_PI_STATE);
			PI_STATE piStateObj = Utilities.valueOf(PI_STATE.class, piStateStr);
			if (piStateObj == null) {
				return new ValidationResults(String.format(Strings.IllegalPiState, piStateStr), Status.BAD_REQUEST);
			}
			
			//permissionsMap
			if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION)) { //can create/update att type with out permissions list - take defaults in create, ignore in update
				if (input.get(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION) == null) {
					return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATTRIBUTES_PERMISSION), Status.BAD_REQUEST);
				}
				JSONObject attributesPermissionObj = input.getJSONObject(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION); //validate that is json
				res = validateAttributesPermissions(attributesPermissionObj);
				if (res != null) {
					return res;
				}
			}
			
			//dbTable
			if (!input.containsKey(Constants.JSON_FIELD_DB_TABLE) || input.get(Constants.JSON_FIELD_DB_TABLE) == null || input.getString(Constants.JSON_FIELD_DB_TABLE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_TABLE), Status.BAD_REQUEST);
			}
			
			String newDbTable = input.getString(Constants.JSON_FIELD_DB_TABLE);
			
			//athenaTable
			if (!input.containsKey(Constants.JSON_FIELD_ATHENA_TABLE) || input.get(Constants.JSON_FIELD_ATHENA_TABLE) == null || input.getString(Constants.JSON_FIELD_ATHENA_TABLE).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATHENA_TABLE), Status.BAD_REQUEST);
			}
			
			String newAthenaTable = input.getString(Constants.JSON_FIELD_ATHENA_TABLE);
			
			
			Action action = Action.ADD;
			if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing object otherwise create a new one
				action = Action.UPDATE;
			}
			if (action == Action.ADD) {
				//validate table existence in db
				DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
				ArrayList<String> dbTables = dbHandler.getTableNames(entity.getDbSchema());
				if (!Utilities.isStringInList(dbTables, newDbTable)) {
					return new ValidationResults(String.format(Strings.noDbTable, newDbTable, entity.getDbSchema()), Status.BAD_REQUEST);
				}
				
				//validate table existence in dev db
				if (entity.getDbDevSchema()!=null) {
					ArrayList<String> dbDevTables = dbHandler.getTableNames(entity.getDbDevSchema());
					if (!Utilities.isStringInList(dbDevTables, newDbTable)) {
						return new ValidationResults(String.format(Strings.noDbTable, newDbTable, entity.getDbDevSchema()), Status.BAD_REQUEST);
					}
				}
				
				//validate table existence in archive db
				if (entity.getDbArchiveSchema()!=null) {
					ArrayList<String> dbArchiveTables = dbHandler.getTableNames(entity.getDbArchiveSchema());
					if (!Utilities.isStringInList(dbArchiveTables, newDbTable)) {
						return new ValidationResults(String.format(Strings.noDbTable, newDbTable, entity.getDbArchiveSchema()), Status.BAD_REQUEST);
					}
				}
				
				//validate table existence in Athena under the entity's database
				AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
				ArrayList<String> athenaTables = athenaHandler.getAthenaTablesInDatabase(entity.getAthenaDatabase());
				if (!Utilities.isStringInList(athenaTables, newAthenaTable)) {
					return new ValidationResults(String.format(Strings.noAthenaTable, newAthenaTable, entity.getAthenaDatabase()), Status.BAD_REQUEST);
				}
				
				if (entity.getAthenaDevDatabase()!=null) {
					//validate table existence in Athena under the entity's dev database
					ArrayList<String> athenaDevTables = athenaHandler.getAthenaTablesInDatabase(entity.getAthenaDevDatabase());
					if (!Utilities.isStringInList(athenaDevTables, newAthenaTable)) {
						return new ValidationResults(String.format(Strings.noAthenaTable, newAthenaTable, entity.getAthenaDevDatabase()), Status.BAD_REQUEST);
					}
				}
			} 
			else { //update
				//entity id cannot changed
				if (!entityId.toString().equals(eId)) {
					return new ValidationResults(Strings.cannotUpdateEntityIdAttType, Status.BAD_REQUEST);
				}
				
				//dbTable cannot be changed
				if (!newDbTable.equals(dbTable)) {
					return new ValidationResults(Strings.cannotUpdateDbTableAtt, Status.BAD_REQUEST);
				}
				
				//athenaTable cannot be changed
				if (!athenaTable.equals(newAthenaTable)) {
					return new ValidationResults(Strings.cannotUpdateAthenaTableAtt, Status.BAD_REQUEST);
				}
				
			}

		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		} catch (ClassNotFoundException cnfe) {
			return new ValidationResults(cnfe.getMessage(), Status.INTERNAL_SERVER_ERROR);
		} catch (SQLException sqle) {
			return new ValidationResults(sqle.getMessage(), Status.INTERNAL_SERVER_ERROR);
		} catch (InterruptedException ie) {
			return new ValidationResults(ie.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
		
		return res;
	}

	private ValidationResults validateAttributesPermissions(JSONObject attributesPermissionObj) throws JSONException {
		//READ_ONLY
		if (!attributesPermissionObj.containsKey(AttributesPermission.READ_ONLY.toString())) {
			return new ValidationResults(String.format(Strings.missingPermission, AttributesPermission.READ_ONLY.toString()), Status.BAD_REQUEST);
		}
		
		String roleTypeStr = attributesPermissionObj.getString(AttributesPermission.READ_ONLY.toString());
		RoleType rt = Utilities.strToRoleType(roleTypeStr);
		if (rt == null) {
			return new ValidationResults(String.format(Strings.roleTypeDoesNotExist, roleTypeStr), Status.BAD_REQUEST);
		}
		
		//READ_WRITE
		if (!attributesPermissionObj.containsKey(AttributesPermission.READ_WRITE.toString())) {
			return new ValidationResults(String.format(Strings.missingPermission, AttributesPermission.READ_WRITE.toString()), Status.BAD_REQUEST);
		}
		
		roleTypeStr = attributesPermissionObj.getString(AttributesPermission.READ_WRITE.toString());
		rt = Utilities.strToRoleType(roleTypeStr);
		if (rt == null) {
			return new ValidationResults(String.format(Strings.roleTypeDoesNotExist, roleTypeStr), Status.BAD_REQUEST);
		}
		
		//READ_WRITE_DEPRECATE
		if (!attributesPermissionObj.containsKey(AttributesPermission.READ_WRITE_DEPRECATE.toString())) {
			return new ValidationResults(String.format(Strings.missingPermission, AttributesPermission.READ_WRITE_DEPRECATE.toString()), Status.BAD_REQUEST);
		}
		
		roleTypeStr = attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DEPRECATE.toString());
		rt = Utilities.strToRoleType(roleTypeStr);
		if (rt == null) {
			return new ValidationResults(String.format(Strings.roleTypeDoesNotExist, roleTypeStr), Status.BAD_REQUEST);
		}
		
		//READ_WRITE_DELETE
		if (!attributesPermissionObj.containsKey(AttributesPermission.READ_WRITE_DELETE.toString())) {
			return new ValidationResults(String.format(Strings.missingPermission, AttributesPermission.READ_WRITE_DELETE.toString()), Status.BAD_REQUEST);
		}
		
		roleTypeStr = attributesPermissionObj.getString(AttributesPermission.READ_WRITE_DELETE.toString());
		rt = Utilities.strToRoleType(roleTypeStr);
		if (rt == null) {
			return new ValidationResults(String.format(Strings.roleTypeDoesNotExist, roleTypeStr), Status.BAD_REQUEST);
		}
		
		return null;
	}

	public String updateAttributeType(JSONObject input) throws JSONException {
		StringBuilder updateDetails = new StringBuilder(super.update(input));
        boolean wasChanged = updateDetails.length()>0;
        
        if (input.containsKey(Constants.JSON_FIELD_PI_STATE) && input.get(Constants.JSON_FIELD_PI_STATE)!=null) {
            PI_STATE updatePiState = strToPiState(input.getString(Constants.JSON_FIELD_PI_STATE));
            if (!updatePiState.equals(piState)) {
            		updateDetails.append("'piState' changed from " + piState + " to " + updatePiState + ";	");
            		piState = updatePiState;
                wasChanged = true;
            }
        }
        
        if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION) && input.get(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION)!=null) {
            JSONObject updateAttributesPermission = (input.getJSONObject(Constants.JSON_FIELD_ATTRIBUTES_PERMISSION));
            if (!isAttributesPermissionEqual(updateAttributesPermission, permissionsMap)) {
            		updateDetails.append("'attributesPermission' changed from " + permissionsMapToJsonObject().toString() + " to " + updateAttributesPermission.toString() + ";	");
            		JsonObjectToPermissionMap(updateAttributesPermission);
                wasChanged = true;
            }
        }
        
        if (wasChanged) {
            lastModified = new Date();
        }

        return updateDetails.toString();
    }
	
	private static PI_STATE strToPiState(String piStateStr) {
		return Utilities.valueOf(PI_STATE.class, piStateStr);
	}

	public RoleType getRequiredRoleForPerrmision(AttributesPermission attributPermission) {
		return permissionsMap.get(attributPermission);
	}
}
