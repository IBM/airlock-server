package com.ibm.airlock.admin.airlytics.entities;

import java.sql.SQLException;
import java.util.ArrayList;
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
import com.ibm.airlock.Constants.Action;
import com.ibm.airlock.Constants.OutputJSONMode;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.ValidationResults;
import com.ibm.airlock.admin.airlytics.athena.AthenaHandler;
import com.ibm.airlock.admin.airlytics.entities.AttributeType.AttributesPermission;
import com.ibm.airlock.admin.authentication.UserInfo;

public class Entity extends BaseAirlyticsItem {

	private UUID productId = null;
	private String dbSchema = null;
	private String dbDevSchema = null; //mandatory but can be null - means no dev tables
	private String dbArchiveSchema = null; ////mandatory but can be null - means no archive tables
	private LinkedList<Attribute> attributes = new LinkedList<Attribute>();
	private LinkedList<AttributeType> attributeTypes = new LinkedList<AttributeType>();
	private String athenaDatabase;
	private String athenaDevDatabase = null; //mandatory but can be null - means no dev tables
	
	public UUID getProductId() {
		return productId;
	}
	public void setProductId(UUID productId) {
		this.productId = productId;
	}
	public LinkedList<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(LinkedList<Attribute> attributes) {
		this.attributes = attributes;
	}
	public LinkedList<AttributeType> getAttributeTypes() {
		return attributeTypes;
	}
	public void setAttributeTypes(LinkedList<AttributeType> attributeTypes) {
		this.attributeTypes = attributeTypes;
	}	
	public String getDbSchema() {
		return dbSchema;
	}
	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}
	public String getAthenaDatabase() {
		return athenaDatabase;
	}
	public void setAthenaDatabase(String athenaDatabase) {
		this.athenaDatabase = athenaDatabase;
	}
	public String getDbDevSchema() {
		return dbDevSchema;
	}
	public void setDbDevSchema(String dbDevSchema) {
		this.dbDevSchema = dbDevSchema;
	}
	public String getDbArchiveSchema() {
		return dbArchiveSchema;
	}
	public void setDbArchiveSchema(String dbArchiveSchema) {
		this.dbArchiveSchema = dbArchiveSchema;
	}
	public String getAthenaDevDatabase() {
		return athenaDevDatabase;
	}
	public void setAthenaDevDatabase(String athenaDevDatabase) {
		this.athenaDevDatabase = athenaDevDatabase;
	}
	public Entity(UUID productId) {
        this.productId=productId;
    }
	
	public JSONObject toJson(UserInfo userInfo, ServletContext context,OutputJSONMode outputMode) throws JSONException {
		JSONObject res = super.toJson();
		res.put(Constants.JSON_FIELD_PRODUCT_ID, productId==null?null:productId.toString()); 
		
		res.put(Constants.JSON_FIELD_ATTRIBUTES, attributesToJsonArray(userInfo, context, outputMode));
		res.put(Constants.JSON_FIELD_ATTRIBUTE_TYPES, attributeTypesToJsonArray());
		res.put(Constants.JSON_FIELD_DB_SCHEMA, dbSchema);
		res.put(Constants.JSON_FIELD_DB_DEV_SCHEMA, dbDevSchema);
		res.put(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA, dbArchiveSchema);
		res.put(Constants.JSON_FIELD_ATHENA_DB, athenaDatabase);
		res.put(Constants.JSON_FIELD_ATHENA_DEV_DB, athenaDevDatabase);
		
		return res;
	}
	
	private JSONArray attributesToJsonArray(UserInfo userInfo, ServletContext context, OutputJSONMode outputMode) throws JSONException  {
		JSONArray attributesArr = new JSONArray();
		for (Attribute attribute:attributes) {
			
			ValidationResults validateAccess = null;
			if (outputMode.equals(OutputJSONMode.DISPLAY)) { //only in display mode - filter out unauthorizes attributes 
				validateAccess = attribute.validateAttributeAccess(userInfo, AttributesPermission.READ_ONLY, context);
			}	
			if (validateAccess == null) {
				attributesArr.add(attribute.toJson());
			}
		}
		return attributesArr;
	}
	
	private JSONArray attributeTypesToJsonArray() throws JSONException  {
		JSONArray attributeTypesArr = new JSONArray();
		for (AttributeType attributeType:attributeTypes) {
			attributeTypesArr.add(attributeType.toJson());
		}
		return attributeTypesArr;
	}
	
	public JSONObject attributesToJson(UserInfo userInfo, ServletContext context, OutputJSONMode outputMode) throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_ENTITY_ID, uniqueId==null?null:uniqueId.toString()); 
		res.put(Constants.JSON_FIELD_ATTRIBUTES, attributesToJsonArray(userInfo, context, outputMode));
		return res;
	}
	
	public JSONObject attributeTypesToJson() throws JSONException {
		JSONObject res = new JSONObject();
		res.put(Constants.JSON_FIELD_ENTITY_ID, uniqueId==null?null:uniqueId.toString()); 
		res.put(Constants.JSON_FIELD_ATTRIBUTE_TYPES, attributeTypesToJsonArray());
		return res;
	}
	
	public void fromJSON (JSONObject input, Map<String, Attribute> attributesDB, Map<String, AttributeType> attributeTypesDB) throws JSONException {
		super.fromJSON(input);
		if (input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) && input.get(Constants.JSON_FIELD_PRODUCT_ID) != null) {
			String pStr = input.getString(Constants.JSON_FIELD_PRODUCT_ID);			
			productId = UUID.fromString(pStr);		
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DB_SCHEMA) && input.get(Constants.JSON_FIELD_DB_SCHEMA) != null) {
			dbSchema = input.getString(Constants.JSON_FIELD_DB_SCHEMA);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DB_DEV_SCHEMA)  && input.get(Constants.JSON_FIELD_DB_DEV_SCHEMA) != null) {
			dbDevSchema = input.getString(Constants.JSON_FIELD_DB_DEV_SCHEMA);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA)  && input.get(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA) != null) {
			dbArchiveSchema = input.getString(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ATHENA_DB) && input.get(Constants.JSON_FIELD_ATHENA_DB) != null) {
			athenaDatabase = input.getString(Constants.JSON_FIELD_ATHENA_DB);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ATHENA_DEV_DB)  && input.get(Constants.JSON_FIELD_ATHENA_DEV_DB) != null) {
			athenaDevDatabase = input.getString(Constants.JSON_FIELD_ATHENA_DEV_DB);
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTES) && input.get(Constants.JSON_FIELD_ATTRIBUTES) != null) {
			JSONArray attributesArray = input.getJSONArray(Constants.JSON_FIELD_ATTRIBUTES);
			for (int i=0; i<attributesArray.length(); i++) {
				JSONObject attObj = attributesArray.getJSONObject(i);
				Attribute attribute = new Attribute();
				attribute.fromJSON(attObj);
				attributes.add(attribute);
				if (attributesDB!=null) {
					attributesDB.put(attribute.getUniqueId().toString(), attribute);
				}
			}
		}
		
		if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTE_TYPES) && input.get(Constants.JSON_FIELD_ATTRIBUTE_TYPES) != null) {
			JSONArray attributeTypesArray = input.getJSONArray(Constants.JSON_FIELD_ATTRIBUTE_TYPES);
			for (int i=0; i<attributeTypesArray.length(); i++) {
				JSONObject attTypeObj = attributeTypesArray.getJSONObject(i);
				AttributeType attributeType = new AttributeType();
				attributeType.fromJSON(attTypeObj);
				attributeTypes.add(attributeType);
				if (attributeTypesDB!=null) {
					attributeTypesDB.put(attributeType.getUniqueId().toString(), attributeType);
				}
			}
		}
	}
	
	//return null if valid, ValidationResults otherwise
	public ValidationResults validateJSON(JSONObject input, ServletContext context) {
		ValidationResults res = super.validateJSON(input);
		if (res !=null) {
			return res;
		}
		
		try {
			//productId
			if (!input.containsKey(Constants.JSON_FIELD_PRODUCT_ID) || input.getString(Constants.JSON_FIELD_PRODUCT_ID) == null || input.getString(Constants.JSON_FIELD_PRODUCT_ID).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_PRODUCT_ID), Status.BAD_REQUEST);
			}
			String pId = input.getString(Constants.JSON_FIELD_PRODUCT_ID); 
		
			@SuppressWarnings("unchecked")
			Map<String, Product> productsDB = (Map<String, Product>)context.getAttribute(Constants.PRODUCTS_DB_PARAM_NAME);
			Product prod = productsDB.get(pId);
			if (prod == null) {
				return new ValidationResults(Strings.productNotFound, Status.BAD_REQUEST);
			}
			
			//validate name uniqueness within product
			String newName = input.getString(Constants.JSON_FIELD_NAME); //exists - after base class validation 
			for (Entity e: prod.getEntities().getEntities()) {
				if (uniqueId != null && uniqueId.toString().equals(e.getUniqueId().toString())) {
					continue; //skip the current entity in update				
				}
				
				if (newName.equals(e.getName()) && !pId.equals(e.getUniqueId().toString())) {
					return new ValidationResults(Strings.notUniqueEntityName, Status.BAD_REQUEST);
				}
			}
			
			//attributes should not appear in entity add/update
			if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTES) && input.get(Constants.JSON_FIELD_ATTRIBUTES)!=null) {
				return new ValidationResults(Strings.attributesSpeciedForEntity, Status.BAD_REQUEST);
			}
			
			//attributeTypes should not appear in entity add/update
			if (input.containsKey(Constants.JSON_FIELD_ATTRIBUTE_TYPES) && input.get(Constants.JSON_FIELD_ATTRIBUTE_TYPES)!=null) {
				return new ValidationResults(Strings.attributeTypesSpeciedForEntity, Status.BAD_REQUEST);
			}
			
			//deletedAttributeColumnNames should not appear in entity add/update
			if (input.containsKey(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA) && input.get(Constants.JSON_FIELD_DELETED_ATTRIBUTES_DATA)!=null) {
				return new ValidationResults(Strings.deletedAttributesDataSpeciedForEntity, Status.BAD_REQUEST);
			}
			
			//dbSchema
			if (!input.containsKey(Constants.JSON_FIELD_DB_SCHEMA) || input.get(Constants.JSON_FIELD_DB_SCHEMA) == null || input.getString(Constants.JSON_FIELD_DB_SCHEMA).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_SCHEMA), Status.BAD_REQUEST);
			}
			String newDbSchema = input.getString(Constants.JSON_FIELD_DB_SCHEMA);
			
			//dbDevSchema - null is legal value but empty string does not
			if (!input.containsKey(Constants.JSON_FIELD_DB_DEV_SCHEMA) || (input.get(Constants.JSON_FIELD_DB_DEV_SCHEMA) != null && input.getString(Constants.JSON_FIELD_DB_DEV_SCHEMA).isEmpty() )) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_DEV_SCHEMA), Status.BAD_REQUEST);
			}			
			String newDbDevSchema = null;
			if (input.get(Constants.JSON_FIELD_DB_DEV_SCHEMA) != null) { 
				newDbDevSchema = input.getString(Constants.JSON_FIELD_DB_DEV_SCHEMA);
			}
			
			//dbArchiveSchema - null is legal value but empty string does not
			if (!input.containsKey(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA) || (input.get(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA) != null && input.getString(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA).isEmpty() )) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA), Status.BAD_REQUEST);
			}
			String newDbArchiveSchema = null;
			if (input.get(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA)!=null) {
				newDbArchiveSchema = input.getString(Constants.JSON_FIELD_DB_ARCHIVE_SCHEMA);
			}
			
			//athenaDatabase
			if (!input.containsKey(Constants.JSON_FIELD_ATHENA_DB) || input.get(Constants.JSON_FIELD_ATHENA_DB) == null || input.getString(Constants.JSON_FIELD_ATHENA_DB).isEmpty()) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATHENA_DB), Status.BAD_REQUEST);
			}
			String newAthenaDB = input.getString(Constants.JSON_FIELD_ATHENA_DB);
			
			//athenaDevDatabase - null is legal value but empty string does not
			if (!input.containsKey(Constants.JSON_FIELD_ATHENA_DEV_DB) || (input.get(Constants.JSON_FIELD_ATHENA_DEV_DB) != null && input.getString(Constants.JSON_FIELD_ATHENA_DEV_DB).isEmpty() )) {
				return new ValidationResults(String.format(Strings.fieldIsMissing, Constants.JSON_FIELD_ATHENA_DEV_DB), Status.BAD_REQUEST);
			}
			String newAthenaDevDB = null;
			if (input.get(Constants.JSON_FIELD_ATHENA_DEV_DB)!=null) {
				newAthenaDevDB = input.getString(Constants.JSON_FIELD_ATHENA_DEV_DB);
			}
			
			Action action = Action.ADD;
			if (input.containsKey(Constants.JSON_FIELD_UNIQUE_ID) && input.get(Constants.JSON_FIELD_UNIQUE_ID)!=null) {
				//if JSON contains uniqueId - update an existing object otherwise create a new one
				action = Action.UPDATE;
			}
			if (action == Action.UPDATE) {
				//product id cannot changed
				if (!productId.toString().equals(pId)) {
					return new ValidationResults(Strings.cannotUpdateProductId, Status.BAD_REQUEST);
				}
				
				//dbSchema cannot be changed
				if (!dbSchema.equals(newDbSchema)) {
					return new ValidationResults(Strings.cannotUpdateDbSchema, Status.BAD_REQUEST);
				}
				
				//dbDevSchema cannot be changed
				if (!areStringsEqual(dbDevSchema, newDbDevSchema)) {
					return new ValidationResults(Strings.cannotUpdateDbDevSchema, Status.BAD_REQUEST);
				}
				
				//dbArchiveSchema cannot be changed
				if (!areStringsEqual(dbArchiveSchema, newDbArchiveSchema)) {
					return new ValidationResults(Strings.cannotUpdateDbArchiveSchema, Status.BAD_REQUEST);
				}
				
				//athenaDatabase cannot be changed
				if (!athenaDatabase.equals(newAthenaDB)) {
					return new ValidationResults(Strings.cannotUpdateAthenaDatabaseAtt, Status.BAD_REQUEST);
				}
				
				//athenaDevDatabase cannot be changed
				if (!areStringsEqual(athenaDevDatabase, newAthenaDevDB)) {
					return new ValidationResults(Strings.cannotUpdateAthenaDevDatabaseAtt, Status.BAD_REQUEST);
				}
			}
			else { //ADD
				//validate schemas existence in db
				ValidationResults schemaExistenceRes = EntitiesUtilities.validateSchemaExistance(context, newDbSchema);
				if (schemaExistenceRes!=null) {
					return schemaExistenceRes;
				}
				
				if (newDbDevSchema!=null) {
					schemaExistenceRes = EntitiesUtilities.validateSchemaExistance(context, newDbDevSchema);
					if (schemaExistenceRes!=null) {
						return schemaExistenceRes;
					}
				}
				
				if (newDbArchiveSchema!=null) {
					schemaExistenceRes = EntitiesUtilities.validateSchemaExistance(context, newDbArchiveSchema);
					if (schemaExistenceRes!=null) {
						return schemaExistenceRes;
					}
				}
				
				//validate Athena databases existence in Athena
				AthenaHandler athenaHandler = (AthenaHandler)context.getAttribute(Constants.ATHENA_HANDLER_PARAM_NAME);
				ArrayList<String> athenaDatabases = athenaHandler.getAthenaDatabases();
				if (!Utilities.isStringInList(athenaDatabases, newAthenaDB)) {
					return new ValidationResults(String.format(Strings.noAthenaDatabase, newAthenaDB), Status.BAD_REQUEST);
				}
				
				if (newAthenaDevDB!=null) {
					if (!Utilities.isStringInList(athenaDatabases, newAthenaDevDB)) {
						return new ValidationResults(String.format(Strings.noAthenaDatabase, newAthenaDevDB), Status.BAD_REQUEST);
					}
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
	
	public void addAttribute(Attribute newAtttribute) {
		attributes.add(newAtttribute);
	}
	
	public void addAttributeType(AttributeType newAtttributeType) {
		attributeTypes.add(newAtttributeType);
	}
	
	//deletes the attributes, attribute types and entity from db on context + Remove entity from product
	//returns null upon success. Error string upon failure
	public String deleteEntity(ServletContext context, Product product) throws ClassNotFoundException, SQLException, InterruptedException {
		
		if (attributes.size() > 0) {
			return Strings.cannotDeleteEntityWithAttributes;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, AttributeType> attributeTypesDB = (Map<String, AttributeType>)context.getAttribute(Constants.ATTRIBUTE_TYPES_DB_PARAM_NAME);				
		
		@SuppressWarnings("unchecked")
		Map<String, Entity> entitiesDB = (Map<String, Entity>)context.getAttribute(Constants.ENTITIES_DB_PARAM_NAME);				
		
		/*@SuppressWarnings("unchecked")
		Map<String, Attribute> attributesDB = (Map<String, Attribute>)context.getAttribute(Constants.ATTRIBUTES_DB_PARAM_NAME);				
		
		for (Attribute attribute : attributes) {
			addDeletedAttToDeletedAttDataList(attribute, product);
			attribute.removeFromDb(context);
			attribute.removeFromAthena(context);
			attributesDB.remove(attribute.getUniqueId().toString());
		}
*/		
		for (AttributeType attributeType : attributeTypes) {
			attributeTypesDB.remove(attributeType.getUniqueId().toString());
		}
		
		entitiesDB.remove(uniqueId.toString());
		
		product.removeEntity(uniqueId);
		
		return null;
	}
	
	private boolean areStringsEqual(String str1, String str2) {
		if (str1 == null && str2 == null) {
			return true;
		}
		
		if (str1 == null || str2 == null) {
			return false;
		}
		
		return str1.equals(str2);
	}
	
	//return error string upon failure, null upon success
	public String removeAttribute(Attribute attribute, Product product) {
		for (int i=0; i<attributes.size(); i++) {
			if (attributes.get(i).getUniqueId().toString().equals(attribute.getUniqueId().toString())) {
				String err = addDeletedAttToDeletedAttDataList(attributes.get(i), product);
				if (err!=null) {
					return err;
				}
				attributes.remove(i);
				
				return null;
			}
		}
		return String.format(Strings.failDeleteAttribute, attribute.getName() );
	}
	
	private String addDeletedAttToDeletedAttDataList(Attribute attribute, Product product) {
		AttributeType attributeType = null;
		for (AttributeType at:attributeTypes) {
			if (at.getUniqueId().equals(attribute.getAttributeTypeId())) {
				attributeType = at;
				break;
			}
		}
		if (attributeType == null) {
			return String.format(Strings.AttributeTypeDoesNotExist);
		}
		product.getEntities().addRemovedAttribute(dbSchema, attributeType.getDbTable(), attribute.getDbColumn(), attribute.getDataType(), attribute.isNullable(), name, attribute.getName());
		
		return null;
	}
	
	//return error string upon failure, null upon success
	public String removeAttributeType(UUID attributeTypeId) {
		for (int i=0; i<attributes.size(); i++) {
			if (attributes.get(i).getAttributeTypeId().equals(attributeTypeId)) {
				return String.format(Strings.attributeTypeInUse, attributeTypeId.toString(), attributes.get(i).getName());
			}
		}
		
		for (int i=0; i<attributeTypes.size(); i++) {
			if (attributeTypes.get(i).getUniqueId().toString().equals(attributeTypeId.toString())) {
				attributeTypes.remove(i);
				return null;
			}
		}
		return String.format(Strings.failDeleteAttributeType, attributeTypeId.toString() );
	}
	
	public String updateEntity(JSONObject input) throws JSONException {
		return super.update(input);
    }
}
