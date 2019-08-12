function getLeafErrors(schemaStr)
{
	var schema = JSON.parse(schemaStr);
	return validateLeaves(schema);
//	var errs = validateLeaves(schema);
//	return JSON.stringify(errs);
}

function validateLeaves(schema) {
    var errorArray = [];
    _validateLeaves(schema, "#", errorArray);
    
    return errorArray;
}

function _validateLeaves(schema, pathToError, errorsArray) {   
    // not an object or schema is null
    if (!(schema && typeof schema === 'object')){
        return true; 
    }
    
    // leaf
    if (schema["$ref"] || (schema.type && containsPrimitiveType(schema.type))) {
            return isLeafValid(schema);
    } 
    
    // this is not a leaf, make sure there is no stage & minAPPVersion elements
    if (schema.stage || schema.minAppVersion) return false;
    
     // handle case of an array/object with properties
    var container = schema.properties || schema.anyOf || schema.oneOf || schema.allOf ||
                    (schema.items && schema.items.properties);
    if (!container)  return true;
    
    pathToError +=  (schema.properties && "/properties") ||
                    (schema.anyOf && "/anyOf") || 
                    (schema.oneOf && "/oneOf") || 
                    (schema.allOf && "/allOf") || 
                    (schema.items && schema.items.properties && "/items/properties");   
    
    // object with elements, none is type
    for (var key in container) {
     var errorLocation = pathToError + "/" + key;    
     if (container.hasOwnProperty(key) && !_validateLeaves(container[key], errorLocation, errorsArray)) {
        errorsArray.push(errorLocation);      
     }
    }
    return true;
}

function containsPrimitiveType(schemaType) {
   if (!schemaType) return false;    
   
   if (typeof schemaType === 'object') {
       return (schemaType.constructor == Array && 
                   (schemaType.indexOf("number") != -1 ||
                    schemaType.indexOf("integer") != -1 ||
                    schemaType.indexOf("string") != -1  ||
                    schemaType.indexOf("boolean") != -1)   
               )
   } else {
       return (schemaType == "number" || schemaType == "integer" || 
               schemaType == "string" || schemaType == "boolean");
   }

}

function isLeafValid(leaf) {
    return (leaf.stage && (leaf.stage == "DEVELOPMENT" || leaf.stage == "PRODUCTION") && 
            leaf.minAppVersion);
}