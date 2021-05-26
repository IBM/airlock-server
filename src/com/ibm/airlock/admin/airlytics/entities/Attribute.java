package com.ibm.airlock.admin.airlytics.entities;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.DATA_TYPE;
import com.ibm.airlock.Constants.RoleType;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.airlytics.entities.AttributeType.AttributesPermission;
import com.ibm.airlock.admin.authentication.UserInfo;
import com.ibm.airlock.admin.db.DbHandler;

public class Attribute extends BaseAirlyticsItem{
	private UUID entityId; 
	private DATA_TYPE dataType;
	private UUID attributeTypeId;
	private Boolean returnedByDSR; //Returned by DSR portability request
	private String dbColumn;
	private String athenaColumn;
	private Boolean nullable;
	private Object defaultValue;
	private Boolean withDefaultValue;
	private Boolean deprecated = false;
	private Boolean isArray = false;
	
	public UUID getEntityId() {
		return entityId;
	}
	public void setEntityId(UUID entityId) {
		this.entityId = entityId;
	}
	public DATA_TYPE getDataType() {
		return dataType;
	}
	public void setDataType(DATA_TYPE dataType) {
		this.dataType = dataType;
	}
	public UUID getAttributeTypeId() {
		return attributeTypeId;
	}
	public void setAttributeTypeId(UUID attributeTypeId) {
		this.attributeTypeId = attributeTypeId;
	}
	public boolean isReturnedByDSR() {
		return returnedByDSR;
	}
	public void setReturnedByDSR(boolean returnedByDSR) {
		this.returnedByDSR = returnedByDSR;
	}
	public String getDbColumn() {
		return dbColumn;
	}
	public void setDbColumnName(String dbColumn) {
		this.dbColumn = dbColumn;
	}
	public boolean isNullable() {
		return nullable;
	}
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	public Object getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	public Boolean getWithDefaultValue() {
		return withDefaultValue;
	}
	public void setWithDefaultValue(Boolean withDefaultValue) {
		this.withDefaultValue = withDefaultValue;
	}
	public Boolean getDeprecated() {
		return deprecated;
	}
	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}
	public Boolean getIsArray() {
		return isArray;
	}
	public void setIsArray(Boolean isArray) {
		this.isArray = isArray;
	}
	public String getAthenaColumn() {
		return athenaColumn;
	}
	public void setAthenaColumn(String athenaColumn) {
		this.athenaColumn = athenaColumn;
	}
	public JSONObject toJson() throws JSONException {
		JSONObject res = super.toJson();
		res.put(Constants.JSON_FIELD_ENTITY_ID, entityId==null?null:entityId.toString());
		res.put(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID, attributeTypeId==null?null:attributeTypeId.toString());
		res.put(Constants.JSON_FIELD_DATA_TYPE, dataType.toString());
		res.put(Constants.JSON_FIELD_RETURNED_BY_DSR, returnedByDSR);
		res.put(Constants.JSON_FIELD_NULLBLE, nullable);
		res.put(Constants.JSON_FIELD_DB_COLUMN, dbColumn);
		res.put(Constants.JSON_FIELD_ATHENA_COLUMN, athenaColumn);
		res.put(Constants.JSON_FIELD_WITH_DEFAULT_VALUE, withDefaultValue);
		if (withDefaultValue) {
			res.put(Constants.JSON_FIELD_DEFAULT_VALUE, defaultValue);
		}
		res.put(Constants.JSON_FIELD_DEPRECATED, deprecated);
		res.put(Constants.JSON_FIELD_IS_ARRAY, isArray);
		return res;
	}
	
	public void fromJSON (JSONObject input) throws JSONException {
		super.fromJSON(input);
		if (input.containsKey(Constants.JSON_FIELD_ENTITY_ID) && input.get(Constants.JSON_FIELD_ENTITY_ID) != null) {
			String eIdStr = input.getString(Constants.JSON_FIELD_ENTITY_ID);			
			entityId = UUID.fromString(eIdStr);		
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID) && input.get(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID) != null) {
			String atIdStr = input.getString(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID);			
			attributeTypeId = UUID.fromString(atIdStr);		
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DATA_TYPE) && input.get(Constants.JSON_FIELD_DATA_TYPE)!=null)
			dataType = Utilities.valueOf(DATA_TYPE.class, input.getString(Constants.JSON_FIELD_DATA_TYPE));
		
		if (input.containsKey(Constants.JSON_FIELD_RETURNED_BY_DSR) && input.get(Constants.JSON_FIELD_RETURNED_BY_DSR)!=null)
			returnedByDSR = input.getBoolean(Constants.JSON_FIELD_RETURNED_BY_DSR);
		
		if (input.containsKey(Constants.JSON_FIELD_NULLBLE) && input.get(Constants.JSON_FIELD_NULLBLE)!=null)
			nullable = input.getBoolean(Constants.JSON_FIELD_NULLBLE);
		
		if (input.containsKey(Constants.JSON_FIELD_DB_COLUMN) && input.get(Constants.JSON_FIELD_DB_COLUMN)!=null)
			dbColumn = input.getString(Constants.JSON_FIELD_DB_COLUMN);
		
		if (input.containsKey(Constants.JSON_FIELD_ATHENA_COLUMN) && input.get(Constants.JSON_FIELD_ATHENA_COLUMN)!=null)
			athenaColumn = input.getString(Constants.JSON_FIELD_ATHENA_COLUMN);
		
		if (input.containsKey(Constants.JSON_FIELD_DEFAULT_VALUE) && input.get(Constants.JSON_FIELD_DEFAULT_VALUE)!=null)
			defaultValue = input.get(Constants.JSON_FIELD_DEFAULT_VALUE);
		
		if (input.containsKey(Constants.JSON_FIELD_WITH_DEFAULT_VALUE) && input.get(Constants.JSON_FIELD_WITH_DEFAULT_VALUE)!=null)
			withDefaultValue = input.getBoolean(Constants.JSON_FIELD_WITH_DEFAULT_VALUE);
		
		if (input.containsKey(Constants.JSON_FIELD_DEPRECATED) && input.get(Constants.JSON_FIELD_DEPRECATED)!=null)
			deprecated = input.getBoolean(Constants.JSON_FIELD_DEPRECATED);
		
		if (input.containsKey(Constants.JSON_FIELD_IS_ARRAY) && input.get(Constants.JSON_FIELD_IS_ARRAY)!=null)
			isArray = input.getBoolean(Constants.JSON_FIELD_IS_ARRAY);
		
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateJSON(JSONObject input, ServletContext context, Boolean ignoreExistence, UserInfo userInfo) {
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
			for (Attribute a:entity.getAttributes()) {
				if (uniqueId != null && uniqueId.toString().equals(a.getUniqueId().toString())) {
					continue; //skip the current attribute in update				
				}
				
				if (newName.equals(a.getName())) {
					return new ValidationResults(Strings.notUniqueAttributeNameWithinEntity, Status.BAD_REQUEST);
				}
			}
			
			//validate attribute type id + in the same entity as the attribute //entityId
			if (!input.containsKey(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID) || input.getString(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID) == null || input.getString(Constants.JSON_FIELD_ENTITY_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID), Status.BAD_REQUEST);
			}
			
			//attribute type
			String atId = input.getString(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID);
			String err = Utilities.validateLegalUUID(atId);
			if (err!=null) 	
				return new ValidationResults(Strings.illegalAttributeTypeUUID + err, Status.BAD_REQUEST);
			
			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
			AttributeType attributeType = attributeTypesDB.get(atId);
			if (attributeType == null) {
				return new ValidationResults(Strings.nonExistingAttributeType, Status.BAD_REQUEST);
			}
			if (!attributeType.getEntityId().toString().equals(eId)) {
				return new ValidationResults(Strings.attributeTypeInOtherEntity, Status.BAD_REQUEST);
			}
			
			//dataType
			if (!input.containsKey(Constants.JSON_FIELD_DATA_TYPE) || input.getString(Constants.JSON_FIELD_DATA_TYPE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DATA_TYPE), Status.BAD_REQUEST);
			}

			String dataTypeStr = input.getString(Constants.JSON_FIELD_DATA_TYPE);
			DATA_TYPE dataTypeObj = Utilities.valueOf(DATA_TYPE.class, dataTypeStr);
			if (dataTypeObj == null) {
				return new ValidationResults(String.format(Strings.IllegalDataType, dataTypeStr), Status.BAD_REQUEST);
			}
			
			//returnedByDSR
			if (!input.containsKey(Constants.JSON_FIELD_RETURNED_BY_DSR) || input.get(Constants.JSON_FIELD_RETURNED_BY_DSR) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_RETURNED_BY_DSR), Status.BAD_REQUEST);					
			}
			Boolean newRetByDSR = input.getBoolean(Constants.JSON_FIELD_RETURNED_BY_DSR); //validate that is boolean value	
			
			//withDefaultValue
			if (!input.containsKey(Constants.JSON_FIELD_WITH_DEFAULT_VALUE) || input.get(Constants.JSON_FIELD_WITH_DEFAULT_VALUE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_WITH_DEFAULT_VALUE), Status.BAD_REQUEST);					
			}
			Boolean wdv = input.getBoolean(Constants.JSON_FIELD_WITH_DEFAULT_VALUE); //validate that is boolean value	
			
			//cannot set default value for timestamp type
			if (wdv && dataTypeObj.equals(DATA_TYPE.TIMESTAMP)) {
				return new ValidationResults(Strings.cannotSetDefaultValueForTimestamp, Status.BAD_REQUEST);
			}
			
			//cannot set default value for json type
			if (wdv && dataTypeObj.equals(DATA_TYPE.JSON)) {
				return new ValidationResults(Strings.cannotSetDefaultValueForJson, Status.BAD_REQUEST);
			}
			
			//deprecated 
			if (!input.containsKey(Constants.JSON_FIELD_DEPRECATED) || input.get(Constants.JSON_FIELD_DEPRECATED) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DEPRECATED), Status.BAD_REQUEST);					
			}
			boolean dep = input.getBoolean(Constants.JSON_FIELD_DEPRECATED); //validate that is boolean value	
			
			//isArray - optional
			Boolean isArr = null;
			if (input.containsKey(Constants.JSON_FIELD_IS_ARRAY)) {
				isArr = input.getBoolean(Constants.JSON_FIELD_IS_ARRAY); //validate that is boolean value					
			}
			 
			//nullable
			if (!input.containsKey(Constants.JSON_FIELD_NULLBLE) || input.get(Constants.JSON_FIELD_NULLBLE) == null) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_NULLBLE), Status.BAD_REQUEST);					
			}
			Boolean newNullable = input.getBoolean(Constants.JSON_FIELD_NULLBLE); //validate that is boolean value	
			
			//dbColumn
			if (!input.containsKey(Constants.JSON_FIELD_DB_COLUMN) || input.get(Constants.JSON_FIELD_DB_COLUMN) == null || input.getString(Constants.JSON_FIELD_DB_COLUMN).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_COLUMN), Status.BAD_REQUEST);					
			}
			String newDbCol = input.getString(Constants.JSON_FIELD_DB_COLUMN);
			
			//athenaColumn
			if (!input.containsKey(Constants.JSON_FIELD_ATHENA_COLUMN) || input.get(Constants.JSON_FIELD_ATHENA_COLUMN) == null || input.getString(Constants.JSON_FIELD_ATHENA_COLUMN).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATHENA_COLUMN), Status.BAD_REQUEST);					
			}
			String newAthenaCol = input.getString(Constants.JSON_FIELD_ATHENA_COLUMN);
			
			//defaultValue
			if (wdv && !input.containsKey(Constants.JSON_FIELD_DEFAULT_VALUE)) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DEFAULT_VALUE), Status.BAD_REQUEST);					
			}
			
			if (!wdv && input.containsKey(Constants.JSON_FIELD_DEFAULT_VALUE)) {
				return new ValidationResults(String.format(Strings.cannotSpecifyDefVal, Constants.JSON_FIELD_DEFAULT_VALUE), Status.BAD_REQUEST);					
			}
			
			//validate that default value is of the specified data type
			Object newDefValue = null;
			if (input.containsKey(Constants.JSON_FIELD_DEFAULT_VALUE) && input.get(Constants.JSON_FIELD_DEFAULT_VALUE) != null) {
				newDefValue = input.get(Constants.JSON_FIELD_DEFAULT_VALUE);
				ValidationResults defValTypeRes = checkValueType(newDefValue, dataTypeObj);
				if (defValTypeRes!=null)
					return defValTypeRes;
			}
			
			ValidationResults validateAccess = validateAttributeAccessPerAttributType(userInfo, attributeType, AttributesPermission.READ_WRITE);
			if (validateAccess!=null) {
				return validateAccess;
			}
			
			Action action = Action.ADD;
			if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing object otherwise create a new one
				action = Action.UPDATE;
			}
			if (action == Action.UPDATE) {
				//entity id cannot changed
				if (!entityId.toString().equals(eId)) {
					return new ValidationResults(Strings.cannotUpdateEntityIdAtt, Status.BAD_REQUEST);
				}
				
				//dataType cannot be changed
				if(!dataType.equals(dataTypeObj)) {
					return new ValidationResults(Strings.cannotUpdateDataType, Status.BAD_REQUEST);
				}
				
				//nullable cannot be changed
				if(!nullable.equals(newNullable)) {
					return new ValidationResults(Strings.cannotUpdateNullable, Status.BAD_REQUEST);
				}
				
				//isArray cannot be changed
				if (isArr!=null && isArr!=isArray) {
					return new ValidationResults(Strings.cannotUpdateIsArray, Status.BAD_REQUEST);
				}
				
				//attribute type cannot be changed
				if(!atId.equals(attributeTypeId.toString())) {
					return new ValidationResults(Strings.cannotUpdateAttributeType, Status.BAD_REQUEST);
				}
				
				//cannot update deprecated attribute
				if (dep) {
					if (!newName.equals(name) || !newRetByDSR.equals(returnedByDSR) || !wdv.equals(withDefaultValue) ||
							!newDbCol.equals(dbColumn) || !EntitiesUtilities.compareObjects(newDefValue, defaultValue) ||
							!newAthenaCol.equals(athenaColumn)) {
						return new ValidationResults(Strings.CannotUpdateDeprecatedAtt, Status.BAD_REQUEST);
					}		
				}
				
				//turn attribute to non-deprecated/deprecated - validate permission
				if (dep != deprecated) {
					validateAccess = validateAttributeAccessPerAttributType(userInfo, attributeType, AttributesPermission.READ_WRITE_DEPRECATE);
					if (validateAccess!=null) {
						return validateAccess;
					}
				}
				
			} else { //ADD
				if (dep) {
					return new ValidationResults(Strings.cannotCreateDeprecatedAttribute, Status.BAD_REQUEST);
				}
				
				//cannot set default value for array type
				if (isArr!=null && isArr) {
					if (wdv) {
						return new ValidationResults(Strings.cannotSetDefaultValueForArrayType, Status.BAD_REQUEST);
					}
				}
			}
			
			//in add or when the dbCol was changed
			if (!newDbCol.equals(dbColumn)) {
				if (!ignoreExistence && isColumnInDBTable(context, eId, getDbSchema(context, eId), attributeType.getDbTable(), newDbCol)) {
					return new ValidationResults(String.format(Strings.columnAllreadyInDBTable, newDbCol, attributeType.getDbTable(), getDbSchema(context, eId)), Status.BAD_REQUEST);
				}
				
				String dbDevSchema = getDbDevSchema(context, eId);
				if (dbDevSchema!=null) {
					if (!ignoreExistence && isColumnInDBTable(context, eId, dbDevSchema, attributeType.getDbTable(), newDbCol)) {
						return new ValidationResults(String.format(Strings.columnAllreadyInDBTable, newDbCol, attributeType.getDbTable(), dbDevSchema), Status.BAD_REQUEST);
					}
				}
				
				String dbArchiveSchema = getDbArchiveSchema(context, eId);
				if (dbArchiveSchema!=null) {
					if (!ignoreExistence && isColumnInDBTable(context, eId, dbArchiveSchema, attributeType.getDbTable(), newDbCol)) {
						return new ValidationResults(String.format(Strings.columnAllreadyInDBTable, newDbCol, attributeType.getDbTable(), dbArchiveSchema), Status.BAD_REQUEST);
					}
				}
				
				if (!ignoreExistence && isColumnInDBTable(context, eId, getDbSchema(context, eId), attributeType.getDbTable(), newDbCol)) {
					return new ValidationResults(String.format(Strings.columnAllreadyInDBTable, newDbCol, attributeType.getDbTable(), getDbSchema(context, eId)), Status.BAD_REQUEST);
				}
				
				//If the column existed in the table and was deleted and now we are trying to create it with different type/nullable
				String errString = columnWasDeletedFromTable(context, eId, attributeType.getDbTable(), newDbCol, dataTypeObj, newNullable);
				if (errString!=null) {
					return new ValidationResults(errString, Status.BAD_REQUEST);
				}
			}
			
			//in add or when the athenaCol was changed
			if (!newAthenaCol.equals(athenaColumn)) {
				if (!ignoreExistence && isColumnInAthenaTable(context, entity.getAthenaDatabase(), attributeType.getAthenaTable(), newAthenaCol)) {
					return new ValidationResults(String.format(Strings.columnAllreadyInAthenaTable, newAthenaCol, attributeType.getAthenaTable(), entity.getAthenaDatabase()), Status.BAD_REQUEST);
				}
				
				if (entity.getAthenaDevDatabase()!=null) {
					if (!ignoreExistence && isColumnInAthenaTable(context, entity.getAthenaDevDatabase(), attributeType.getAthenaTable(), newAthenaCol)) {
						return new ValidationResults(String.format(Strings.columnAllreadyInAthenaTable, newAthenaCol, attributeType.getAthenaTable(), entity.getAthenaDevDatabase()), Status.BAD_REQUEST);
					}
				}
				
				//If the column existed in the table and was deleted and now we are trying to create it with different type/nullable
				String errString = columnWasDeletedFromTable(context, eId, attributeType.getDbTable(), newDbCol, dataTypeObj, newNullable);
				if (errString!=null) {
					return new ValidationResults(errString, Status.BAD_REQUEST);
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
	
	private String columnWasDeletedFromTable(ServletContext context, String entityId, String dbTable, String dbColumn, DATA_TYPE dataType, boolean nullable) {
		String dbSchema = getDbSchema(context, entityId);
		
		@SuppressWarnings("unchecked")
		Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
		
		Set<String> productIds = productsDB.keySet();
		for (String productId : productIds) {
			Product prod = productsDB.get(productId);
			for (AirlyticsEntities.DeletedAttributeData deletedAttributeData : prod.getEntities().getDeletedAttributesData()) {
				if (dbSchema.equals(deletedAttributeData.getDbSchema()) && dbTable.equals(deletedAttributeData.getDbTable()) &&
						dbColumn.equals(deletedAttributeData.getDbColumn()) ) {
					if (!dataType.equals(deletedAttributeData.getDataType())) {
						return String.format(Strings.columnWasDeletedFromTableWithOtherDataType, dbColumn, dbTable, dbSchema, deletedAttributeData.getEntity(), prod.getName(), deletedAttributeData.getDataType().toString(), dataType.toString());
					}
					
					if (nullable != deletedAttributeData.isNullable()) {
						return String.format(Strings.columnWasDeletedFromTableWithOtherNullable, dbColumn, dbTable, dbSchema, deletedAttributeData.getEntity(), prod.getName(), deletedAttributeData.isNullable(), nullable);
					}
				}
			}
		}
		
		return null;
	}
	
	private ValidationResults checkValueType(Object value, DATA_TYPE dataType) {
		switch (dataType) {
			case STRING:
				if (!(value instanceof String)) {
					return new ValidationResults(String.format(Strings.wrongDefaultValueType, value.toString(), "String"), Status.BAD_REQUEST);
				}
				break;
			case INTEGER:
				if (!(value instanceof Integer)) {
					return new ValidationResults(String.format(Strings.wrongDefaultValueType, value.toString(), "Integer"), Status.BAD_REQUEST);
				}
				break;
			case LONG:
				if (!(value instanceof Long) && !(value instanceof Integer)) {
					return new ValidationResults(String.format(Strings.wrongDefaultValueType, value.toString(), "Long"), Status.BAD_REQUEST);
				}
				break;
			case BOOLEAN:
				if (!(value instanceof Boolean)) {
					return new ValidationResults(String.format(Strings.wrongDefaultValueType, value.toString(), "Boolean"), Status.BAD_REQUEST);
				}
				break;
			case DOUBLE:
				if (!(value instanceof Double)) {
					return new ValidationResults(String.format(Strings.wrongDefaultValueType, value.toString(), "Double"), Status.BAD_REQUEST);
				}
				break;
			
		}
		return null;
	}
	
	public String updateAttribute(JSONObject input, ServletContext context, boolean ignoreExistence) throws JSONException, ClassNotFoundException, SQLException, InterruptedException {
		//dataType, nullable, isArray cannot be changed
		StringBuilder updateDetails = new StringBuilder(super.update(input));
        boolean wasChanged = updateDetails.length()>0;
       
        if (input.containsKey(Constants.JSON_FIELD_DB_COLUMN) && input.get(Constants.JSON_FIELD_DB_COLUMN)!=null) {
            String updateDbColumn = (input.getString(Constants.JSON_FIELD_DB_COLUMN));
            if (!updateDbColumn.equals(dbColumn)) {
            		//update column name in db
            		updateColumnNameInDB(context, updateDbColumn, ignoreExistence);
            		updateDetails.append("'dbColumn' changed from " + dbColumn + " to " + updateDbColumn + ";	"); 
            		dbColumn = updateDbColumn;
                wasChanged = true;
            }
        }
        
        if (input.containsKey(Constants.JSON_FIELD_ATHENA_COLUMN) && input.get(Constants.JSON_FIELD_ATHENA_COLUMN)!=null) {
            String updateAthenaColumn = (input.getString(Constants.JSON_FIELD_ATHENA_COLUMN));
            if (!updateAthenaColumn.equals(athenaColumn)) {
            		//update column name in db
            		updateColumnNameInAthena(context, updateAthenaColumn, ignoreExistence);
            		updateDetails.append("'athenaColumn' changed from " + athenaColumn + " to " + updateAthenaColumn + ";	"); 
            		athenaColumn = updateAthenaColumn;
                wasChanged = true;
            }
        }
        
        if (input.containsKey(Constants.JSON_FIELD_WITH_DEFAULT_VALUE) && input.get(Constants.JSON_FIELD_WITH_DEFAULT_VALUE)!=null) {
            Boolean updateWithDefaultValue = input.getBoolean(Constants.JSON_FIELD_WITH_DEFAULT_VALUE);
            if (!updateWithDefaultValue.equals(withDefaultValue)) {
	            if (withDefaultValue && !updateWithDefaultValue) { //update from with to without default value
	            		//drop default value in DB
	            		dropDefaultValueInDB(context, updateWithDefaultValue);
	            	}
	            else {
	            		//update from without to with default value
	            		updateDefaultValueInDB(context, defaultValue);
	            }
            		updateDetails.append("'withDefaultValue' changed from " + withDefaultValue + " to " + updateWithDefaultValue + ";	");
            		withDefaultValue = updateWithDefaultValue;
                wasChanged = true;
            }
        }
        
        if (withDefaultValue && input.containsKey(Constants.JSON_FIELD_DEFAULT_VALUE) && input.get(Constants.JSON_FIELD_DEFAULT_VALUE)!=null) {
            Object updateDefaultValue = input.get(Constants.JSON_FIELD_DEFAULT_VALUE);
            if (!updateDefaultValue.equals(defaultValue)) {
            		//update default value in DB
            		updateDefaultValueInDB(context, updateDefaultValue);
            		updateDetails.append("'defaultValue' changed from " + defaultValue + " to " + updateDefaultValue + ";	");
            		defaultValue = updateDefaultValue;
                wasChanged = true;
            }
        }
        
        if (input.containsKey(Constants.JSON_FIELD_RETURNED_BY_DSR) && input.get(Constants.JSON_FIELD_RETURNED_BY_DSR)!=null) {
            boolean updateReturnedByDSR = input.getBoolean(Constants.JSON_FIELD_RETURNED_BY_DSR);
            if (updateReturnedByDSR != returnedByDSR) {
            		updateDetails.append("'returnedByDSR' changed from " + returnedByDSR + " to " + updateReturnedByDSR + ";	");
            		returnedByDSR = updateReturnedByDSR;
                wasChanged = true;
            }
        }
        
        if (input.containsKey(Constants.JSON_FIELD_DEPRECATED) && input.get(Constants.JSON_FIELD_DEPRECATED)!=null) {
            boolean updateDeprecated = input.getBoolean(Constants.JSON_FIELD_DEPRECATED);
            if (updateDeprecated != deprecated) {
            		updateDetails.append("'deprecated' changed from " + deprecated + " to " + updateDeprecated + ";	");
            		deprecated = updateDeprecated;
                wasChanged = true;
            }
        }
        
        //TODO: can attributId be changed? (require column deletion in prev table and recreate in new table)
        if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID) && input.get(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID)!=null) {
            String attTypeIdUpdateValue = input.getString(Constants.JSON_FIELD_ATTRIBUTE_TYPE_ID);
            if (!attTypeIdUpdateValue.equals(attributeTypeId.toString())) {
            		updateDetails.append("'attributeTypeId' changed from " + attributeTypeId + " to " + attTypeIdUpdateValue + ";	");
            		attributeTypeId = UUID.fromString(attTypeIdUpdateValue);
                wasChanged = true;
            }
        }
        
        if (wasChanged) {
            lastModified = new Date();
        }

        return updateDetails.toString();
    }
	
	private void updateDefaultValueInDB(ServletContext context, Object updateDefaultValue) throws ClassNotFoundException, SQLException {
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		dbHandler.updateColumnDefaultValue(getDbSchema(context, entityId.toString()), attType.getDbTable(), dbColumn, dataType, updateDefaultValue);
		
		String devSchema = getDbDevSchema(context, entityId.toString());
		if (devSchema!=null) {
			dbHandler.updateColumnDefaultValue(devSchema, attType.getDbTable(), dbColumn, dataType, updateDefaultValue);
		}
		
		String archiveSchema = getDbArchiveSchema(context, entityId.toString());
		if (archiveSchema!=null) {
			dbHandler.updateColumnDefaultValue(archiveSchema, attType.getDbTable(), dbColumn, dataType, updateDefaultValue);
		}	
	}
	
	private void dropDefaultValueInDB(ServletContext context, Object updateDefaultValue) throws ClassNotFoundException, SQLException {
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		dbHandler.dropColumnDefaultValue(getDbSchema(context, entityId.toString()), attType.getDbTable(), dbColumn);
		
		String devSchema = getDbDevSchema(context, entityId.toString());
		if (devSchema!=null) {
			dbHandler.dropColumnDefaultValue(devSchema, attType.getDbTable(), dbColumn);
		}
		
		String archiveSchema = getDbArchiveSchema(context, entityId.toString());
		if (archiveSchema!=null) {
			dbHandler.dropColumnDefaultValue(archiveSchema, attType.getDbTable(), dbColumn);
		}	
	}
	
	private void updateColumnNameInDB(ServletContext context, String updateDbColumn, boolean ignoreExistence) throws ClassNotFoundException, SQLException {
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		if (ignoreExistence && isColumnInDBTable(context, entityId.toString(), getDbSchema(context, entityId.toString()), attType.getDbTable(), dbColumn)) {
			return;
		}
		
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		dbHandler.updateColumnName(getDbSchema(context, entityId.toString()), attType.getDbTable(), dbColumn, updateDbColumn); 
		
		String devSchema = getDbDevSchema(context, entityId.toString());
		if (devSchema!=null) {
			dbHandler.updateColumnName(devSchema, attType.getDbTable(), dbColumn, updateDbColumn); 
		}
		
		String archiveSchema = getDbArchiveSchema(context, entityId.toString());
		if (archiveSchema!=null) {
			dbHandler.updateColumnName(archiveSchema, attType.getDbTable(), dbColumn, updateDbColumn); 
		}	
	}
	
	private void updateColumnNameInAthena(ServletContext context, String updateAthenaColumn, boolean ignoreExistence) throws InterruptedException {
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
		
		if (!ignoreExistence || !isColumnInAthenaTable(context, entity.getAthenaDatabase(), attType.getAthenaTable(), athenaColumn)) {
			athenaHandler.updateColumnName(entity.getAthenaDatabase(), attType.getAthenaTable(), athenaColumn, updateAthenaColumn); 
		}
		
		if (entity.getAthenaDevDatabase()!=null) {
			if (!ignoreExistence || !isColumnInAthenaTable(context, entity.getAthenaDevDatabase(), attType.getAthenaTable(), athenaColumn)) {
				athenaHandler.updateColumnName(entity.getAthenaDevDatabase(), attType.getAthenaTable(), athenaColumn, updateAthenaColumn); 
			}
		}
	}
	
	public void addToDatabase(ServletContext context, Boolean ignoreExistence) throws ClassNotFoundException, SQLException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		String schema = getDbSchema(context, entityId.toString());
		if (!ignoreExistence || !isColumnInDBTable(context, entityId.toString(), schema, attType.getDbTable(), dbColumn)) {
			dbHandler.addColumnToTable(schema, attType.getDbTable(), dbColumn, dataType, nullable, withDefaultValue, defaultValue, isArray);
		}
		
		
		String devSchema = getDbDevSchema(context, entityId.toString());
		if (devSchema!=null) {
			if (!ignoreExistence || !isColumnInDBTable(context, entityId.toString(), devSchema, attType.getDbTable(), dbColumn)) {
				dbHandler.addColumnToTable(devSchema, attType.getDbTable(), dbColumn, dataType, nullable, withDefaultValue, defaultValue, isArray);
			}
		}
		
		String archiveSchema = getDbArchiveSchema(context, entityId.toString());
		if (archiveSchema!=null) {
			if (!ignoreExistence || !isColumnInDBTable(context, entityId.toString(), archiveSchema, attType.getDbTable(), dbColumn)) {
				dbHandler.addColumnToTable(archiveSchema, attType.getDbTable(), dbColumn, dataType, nullable, withDefaultValue, defaultValue, isArray);
			}
		}
	}
	
	public void addToAthena(ServletContext context, Boolean ignoreExistence) throws ClassNotFoundException, SQLException, InterruptedException {
		AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		
		if (!ignoreExistence || !isColumnInAthenaTable(context, entity.getAthenaDatabase(), attType.getAthenaTable(), athenaColumn)) {
			athenaHandler.addColumn(entity.getAthenaDatabase(), attType.getAthenaTable(), athenaColumn, dataType, isArray);
		}
		
		if (entity.getAthenaDevDatabase()!=null) {
			if (!ignoreExistence || !isColumnInAthenaTable(context, entity.getAthenaDevDatabase(), attType.getAthenaTable(), athenaColumn)) {
				athenaHandler.addColumn(entity.getAthenaDevDatabase(), attType.getAthenaTable(), athenaColumn, dataType, isArray);
			}
		}
	}
	
	private String getDbSchema(ServletContext context, String entityId) {
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		return entity.getDbSchema();
	}
	
	private String getDbDevSchema(ServletContext context, String entityId) {
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		return entity.getDbDevSchema();
	}
	
	private String getDbArchiveSchema(ServletContext context, String entityId) {
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		return entity.getDbArchiveSchema();
	}
	
	private boolean isColumnInDBTable(ServletContext context, String entityId, String schema, String table, String column) throws ClassNotFoundException, SQLException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		
		List<String> existingColumns = dbHandler.getTableCulomns(schema, table);
		for (String existingCol:existingColumns) {
			if (existingCol.equals(column)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isColumnInAthenaTable(ServletContext context, String athenaDB, String athenaTable, String athenaColumn) throws InterruptedException {
		AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
		
		List<String> existingColumns = athenaHandler.getAthenaTableColumns(athenaTable, athenaDB);
		for (String existingCol:existingColumns) {
			if (existingCol.equals(athenaColumn)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void removeFromDb(ServletContext context) throws ClassNotFoundException, SQLException {
		DbHandler dbHandler = (DbHandler)context.getAttribute(Constants.DB_HANDLER_PARAM_NAME);
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		dbHandler.removeColumnFromTable(getDbSchema(context, entityId.toString()), attType.getDbTable(), dbColumn);
		
		String devSchema = getDbDevSchema(context, entityId.toString());
		if (devSchema!=null) {
			dbHandler.removeColumnFromTable(devSchema, attType.getDbTable(), dbColumn);
		}
		
		String archiveSchema = getDbArchiveSchema(context, entityId.toString());
		if (archiveSchema!=null) {
			dbHandler.removeColumnFromTable(archiveSchema, attType.getDbTable(), dbColumn);
		}	
	}	
	
	public void removeFromAthena(ServletContext context) throws InterruptedException {
		AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attType = attributTypesDB.get(attributeTypeId.toString());
		
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		Entity entity = entitiesDB.get(entityId.toString());
		
		athenaHandler.deleteColumn(entity.getAthenaDatabase(), attType.getAthenaTable(), athenaColumn);
		
		if (entity.getAthenaDevDatabase()!=null) {
			athenaHandler.deleteColumn(entity.getAthenaDevDatabase(), attType.getAthenaTable(), athenaColumn);
		}
	}
	
	public ValidationResults validateAttributeAccess(UserInfo userInfo, AttributesPermission attributPermission, ServletContext context) {
		if (userInfo == null) { //no authentication
			return null;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		AttributeType attributeType = attributeTypesDB.get(attributeTypeId.toString());
		if (attributeType == null) {
			return new ValidationResults(Strings.nonExistingAttributeType, Status.BAD_REQUEST);
		}
		
		return validateAttributeAccessPerAttributType(userInfo, attributeType, attributPermission);
	}
	
	private ValidationResults validateAttributeAccessPerAttributType(UserInfo userInfo, AttributeType attributeType, AttributesPermission attributPermission) {
		if (userInfo == null) { //no authentication
			return null;
		}
		
		RoleType requiredRole = attributeType.getRequiredRoleForPerrmision(attributPermission);
		if (!userInfo.getRoles().contains(requiredRole)) {
			return new ValidationResults(String.format(Strings.NotSufficientAttributePermission, requiredRole, attributPermission.toString()), Status.UNAUTHORIZED);
		}
		return null;
	}
}
