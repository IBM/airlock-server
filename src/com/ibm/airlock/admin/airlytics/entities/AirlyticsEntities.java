package com.ibm.airlock.admin.airlytics.entities;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Strings;
import com.ibm.airlock.Constants.DATA_TYPE;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.authentication.UserInfo;

public class AirlyticsEntities {
	class DeletedAttributeData {
		private String dbSchema;
		private String dbTable;
		private String dbColumn;
		private DATA_TYPE dataType;
		private boolean nullable;
		private String entity;
		private String attribute;
		
		public String getDbSchema() {
			return dbSchema;
		}

		public void setDbSchema(String dbSchema) {
			this.dbSchema = dbSchema;
		}

		public String getDbTable() {
			return dbTable;
		}

		public void setDbTable(String dbTable) {
			this.dbTable = dbTable;
		}

		public String getDbColumn() {
			return dbColumn;
		}

		public void setDbColumn(String dbColumn) {
			this.dbColumn = dbColumn;
		}

		public DATA_TYPE getDataType() {
			return dataType;
		}

		public void setDataType(DATA_TYPE dataType) {
			this.dataType = dataType;
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		public String getAttribute() {
			return attribute;
		}

		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}

		public String getEntity() {
			return entity;
		}

		public void setEntity(String entity) {
			this.entity = entity;
		}

		public DeletedAttributeData() {
			
		}
		
		public DeletedAttributeData(String dbSchema, String dbTable, String dbColumn, DATA_TYPE dataType, boolean nullable, String entity, String attribute) {
			this.dbSchema = dbSchema;
			this.dbTable = dbTable;
			this.dbColumn = dbColumn;
			this.dataType = dataType;
			this.nullable = nullable;
			this.entity = entity;
			this.attribute = attribute;
		}
		
		public JSONObject toJson() throws JSONException {
			JSONObject res = new JSONObject();
			res.put(Constants.JSON_FIELD_DB_SCHEMA, dbSchema); 
			res.put(Constants.JSON_FIELD_DB_TABLE, dbTable); 
			res.put(Constants.JSON_FIELD_DB_COLUMN, dbColumn); 
			res.put(Constants.JSON_FIELD_DATA_TYPE, dataType.toString());
			res.put(Constants.JSON_FIELD_NULLBLE, nullable);
			res.put(Constants.JSON_FIELD_ENTITY, entity);
			res.put(Constants.JSON_FIELD_ATTRIBUTE, attribute);
			return res;
		}
		
		public void fromJSON (JSONObject input) throws JSONException {
			if (input.containsKey(Constants.JSON_FIELD_DB_SCHEMA) && input.get(Constants.JSON_FIELD_DB_SCHEMA) != null) {
				dbSchema = input.getString(Constants.JSON_FIELD_DB_SCHEMA);
			}
			
			if (input.containsKey(Constants.JSON_FIELD_DB_TABLE) && input.get(Constants.JSON_FIELD_DB_TABLE) != null) {
				dbTable = input.getString(Constants.JSON_FIELD_DB_TABLE);
			}
			
			if (input.containsKey(Constants.JSON_FIELD_DB_COLUMN) && input.get(Constants.JSON_FIELD_DB_COLUMN) != null) {
				dbColumn = input.getString(Constants.JSON_FIELD_DB_COLUMN);
			}
			
			if (input.containsKey(Constants.JSON_FIELD_DATA_TYPE) && input.get(Constants.JSON_FIELD_DATA_TYPE) != null) {
				dataType = Utilities.valueOf(DATA_TYPE.class, input.getString(Constants.JSON_FIELD_DATA_TYPE));
			}
			
			if (input.containsKey(Constants.JSON_FIELD_NULLBLE) && input.get(Constants.JSON_FIELD_NULLBLE) != null) {
				nullable = input.getBoolean(Constants.JSON_FIELD_NULLBLE);
			}
			
			if (input.containsKey(Constants.JSON_FIELD_ENTITY) && input.get(Constants.JSON_FIELD_ENTITY) != null) {
				entity = input.getString(Constants.JSON_FIELD_ENTITY);
			}
			
			if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTE) && input.get(Constants.JSON_FIELD_ATTRIBUTE) != null) {
				attribute = input.getString(Constants.JSON_FIELD_ATTRIBUTE);
			}
		}	
	}
	
	private UUID productId;
	
	private LinkedList<Entity> entities = new LinkedList<Entity>();
	private Date lastModified = new Date();
	private LinkedList<DeletedAttributeData> deletedAttributesData = new LinkedList<DeletedAttributeData>();
	

	public UUID getProductId() {
		return productId;
	}

	public void setProductId(UUID productId) {
		this.productId = productId;
	}

	public LinkedList<Entity> getEntities() {
		return entities;
	}

	public void setEntities(LinkedList<Entity> entities) {
		this.entities = entities;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public AirlyticsEntities(UUID productId) {
		this.productId = productId;
	}

	public void addEntity(Entity entity) {
		entities.add(entity);
	}

	public LinkedList<DeletedAttributeData> getDeletedAttributesData() {
		return deletedAttributesData;
	}
	public void setDeletedAttributesData(LinkedList<DeletedAttributeData> deletedAttributesData) {
		this.deletedAttributesData = deletedAttributesData;
	}
	
	public String removeEntity(UUID entityId) {
		for (int i=0; i<entities.size(); i++) {
			if (entities.get(i).getUniqueId().toString().equals(entityId.toString())) {
				entities.remove(i);
				return null;
			}
		}
		return String.format(Strings.failDeleteEntity, entityId.toString() );
	}

	public JSONObject toJSON(boolean includeEntities, UserInfo userInfo, ServletContext context, OutputJSONMode outputMode) throws JSONException{
		JSONObject res = new JSONObject();

		if (includeEntities) {
			JSONArray entitiesArr = new JSONArray();
			for (Entity e : entities) {
				JSONObject eObj = e.toJson(userInfo, context, outputMode);
				entitiesArr.add(eObj);
			}
			res.put(Constants.JSON_FIELD_ENTITIES, entitiesArr);
		}
		res.put(Constants.JSON_FIELD_PRODUCT_ID, productId.toString());
		res.put(Constants.JSON_FIELD_LAST_MODIFIED, lastModified.getTime());
		
		JSONArray deletedAttributesArr = new JSONArray();
		for (DeletedAttributeData da:deletedAttributesData) {
			JSONObject daObj = da.toJson();
			deletedAttributesArr.add(daObj);
		}
		res.put(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA, deletedAttributesArr);
		
		return res; 
	}

	public void fromJSON(JSONObject input, ServletContext context) throws JSONException {
		if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
			String sStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);
			productId = UUID.fromString(sStr);
		}

		if (input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) && input.get(Constants.JSON_FIELD_LAST_MODIFIED)!=null) {
			long timeInMS = (Long)input.get(Constants.JSON_FIELD_LAST_MODIFIED);
			lastModified = new Date(timeInMS);
		}  else {
			lastModified = new Date();
		}

		if (input.containsKey(Constants.JSON_FIELD_ENTITIES) && input.get(Constants.JSON_FIELD_ENTITIES) != null) {
			entities.clear();

			@SuppressWarnings("unchecked")
			Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);

			@SuppressWarnings("unchecked")
			Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);

			JSONArray entitiesArr = input.getJSONArray(Constants.JSON_FIELD_ENTITIES); //after validation - i know it is json array
			for (int i=0; i<entitiesArr.size(); i++) {
				JSONObject cohortJsonObj = entitiesArr.getJSONObject(i); //after validation - i know it is json object
				Entity entity = new Entity(productId);
				entity.fromJSON(cohortJsonObj, attributesDB, attributeTypesDB);
				entities.add(entity);
				entitiesDB.put(entity.getUniqueId().toString(), entity); //this function is only called when server initialized - hence added to entities db
			}
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA) && input.get(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA) != null) {
			JSONArray deletedAttributeArr = input.getJSONArray(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA);
			deletedAttributesData.clear();
			for (int i=0; i<deletedAttributeArr.length(); i++) {
				JSONObject daObj = deletedAttributeArr.getJSONObject(i);
				DeletedAttributeData da = new DeletedAttributeData();
				da.fromJSON(daObj);
				deletedAttributesData.add(da);
			}
		}
	}
	
	//return null if valid, ValidationResults otherwise
	//In this case only update is possible. And the only thing to update is the dbSchema
	public ValidationResults validateJSON(JSONObject input, ServletContext context) {
		try {
			//productId
			if (!input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || input.getString(Constants.JSON_FIELD_PRODUCT_ID) == null || input.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}
			String pId = input.getString(Constants.JSON_FIELD_PRODUCT_ID); 
		
			//product id cannot changed
			if (!productId.toString().equals(pId)) {
				return new ValidationResults(Strings.cannotUpdateProductId, Status.BAD_REQUEST);
			}
			
			//cannot specify entities list when updating. 
			if (input.containsKey(Constants.JSON_FIELD_ENTITIES)) {
				return new ValidationResults(String.format(Strings.entitiesSpeciedDuringUpdate, Constants.JSON_FIELD_DB_SCHEMA), Status.BAD_REQUEST);
			}
			
			//modification date must appear
			if (!input.containsKey(Constants.JSON_FIELD_LAST_MODIFIED) || input.get(Constants.JSON_FIELD_LAST_MODIFIED)==null) {
				return new ValidationResults("The lastModified field is missing. This field must be specified during feature update.", Status.BAD_REQUEST);
			}
			
			//verify that given modification date is not older that current modification date
			long givenModoficationDate = input.getLong(Constants.JSON_FIELD_LAST_MODIFIED);  //verify that legal long
			Date givenDate = new Date(givenModoficationDate);
			if (givenDate.before(lastModified)) {
				return new ValidationResults("Item was changed by another user. Refresh your browser and try again.", Status.CONFLICT);			
			}	
			
			//db schema
			if (!input.containsKey(Constants.JSON_FIELD_DB_SCHEMA)) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_SCHEMA), Status.BAD_REQUEST);
			}
		} catch (JSONException jsne) {
			return new ValidationResults(jsne.getMessage(), Status.BAD_REQUEST);
		}
		
		return null;
	}
	/*
	public String update(JSONObject input) throws JSONException {
		boolean wasChanged = false;
        StringBuilder updateDetails = new StringBuilder();
        
        if (input.containsKey(Constants.JSON_FIELD_DB_SCHEMA) && input.get(Constants.JSON_FIELD_DB_SCHEMA)!=null) {
            String updatedbSchema = input.getString(Constants.JSON_FIELD_DB_SCHEMA);
            if (!updatedbSchema.equals(dbSchema)) {
                updateDetails.append("'dbSchema' changed from " + dbSchema + " to " + updatedbSchema + ";	");
                dbSchema = updatedbSchema;
                wasChanged = true;
            }
        }

        if (wasChanged) {
            lastModified = new Date();
        }

        return updateDetails.toString();
	}*/

	public void addRemovedAttribute(String dbSchema, String dbTable, String dbColumn, DATA_TYPE dataType,boolean nullable, String entity, String attribute) {
		DeletedAttributeData deletedAttribute = new DeletedAttributeData(dbSchema, dbTable, dbColumn, dataType, nullable, entity, attribute);
		deletedAttributesData.add(deletedAttribute);
	}
}
