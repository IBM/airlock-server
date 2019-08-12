
function pruneOneSchema(schemaStr, stage, minAppVer)
{
	var schema = JSON.parse(schemaStr);
	pruneSchema(schema, stage, minAppVer);
	return JSON.stringify(schema);
}

function pruneSchema(schema, stage, minAppVer) {   	
	if ( (stage === null) || (minAppVer === null) || !(schema && typeof schema === 'object')){
        return false; 
    }
    
        // leaf
    if (schema.stage && schema.minAppVersion) {
         return (!matchingStage(schema.stage, stage) || !matchingVersion(schema.minAppVersion, minAppVer));
    }
    
    // handle case of an object with properties
    var container = schema.properties || schema.anyOf || schema.oneOf || schema.allOf ||
                    (schema.items && schema.items.properties);
    
    if (!container) return false;
    
    // handle case of an array
   if (container.constructor == Array) {
        var i = container.length
        while (i--) {
            if (pruneSchema(container[i], stage, minAppVer)) {
                container.splice(i, 1);
            }
        }
        return (container.length == 0);
    }   
   
     // object which is not an array
     var numOfProperties = 0;
     var numOfPropertiesRemoved = 0;
     for (var key in container) {
         if (container.hasOwnProperty(key) && ++numOfProperties && pruneSchema(container[key], stage, minAppVer)) {
             // delete the key & and its required existance if any
             numOfPropertiesRemoved ++ 
             delete container[key];
             removeKeyFromArray(schema.required, key);             
         }
     }
    // if there are no more properties, remove the object
    return (numOfProperties  && numOfProperties == numOfPropertiesRemoved);     
    


}

function matchingStage(schemaStage, stage) {
    return (stage == 'DEVELOPMENT' || stage == schemaStage )
}

function matchingVersion(schemaVersion, minAppVersion) {
	if(schemaVersion === minAppVersion) {
		return true;
	}
	
	if((schemaVersion === null) || (minAppVersion === null)) {
		return false;
	}
	
    var ver1Arr = schemaVersion.split(".");
    var ver2Arr = minAppVersion.split(".");
    addPaddingToShorterArray(ver1Arr, ver2Arr);

    for (var i = 0; i < ver1Arr.length; i++) {
      if(isNaN(ver1Arr[i]) || isNaN(ver2Arr[i])) {
          // compare as strings
          if(ver1Arr[i].localeCompare(ver2Arr[i]) === (-1)) {
             // ver1Arr[i] is sorted before ver2Arr[i]
             return true;    
          }
          if(ver1Arr[i].localeCompare(ver2Arr[i]) === 1) {
             // ver1Arr[i] is sorted after ver2Arr[i]
             return false;    
          }          
      } else {
          // compare as numbers
          var ver1SegmentNum = parseInt(ver1Arr[i]);            
          var ver2SegmentNum = parseInt(ver2Arr[i]);
          if(ver1SegmentNum > ver2SegmentNum) return false; 
          if(ver1SegmentNum < ver2SegmentNum) return true;
      }
    }
    return true; // versions are equal
}            
            
            
function addPaddingToShorterArray(arr1, arr2) {
    if(arr1.length === arr2.length) return;
	var shortArr = (arr1.length > arr2.length) ? arr2 : arr1;
    var longArr = (arr1.length > arr2.length) ? arr1 : arr2;
    var prevShortArrLength = shortArr.length;
    shortArr.length += (longArr.length - shortArr.length);
    for(var i = prevShortArrLength; i < shortArr.length; i++) {
    	shortArr[i] = "0";
    }
}

function removeKeyFromArray(array, key) {
    if (array && array.constructor == Array){
        var index = array.indexOf(key);
        if (index != -1) {
        	array.splice(index, 1);
        }
    }
}

function pruneNulls(schema) {   
    if (!(schema && typeof schema === 'object')){
        return; 
    }
    schema.type && removeKeyFromArray(schema.type, "null");
    
    for (var key in schema) {
     if (schema.hasOwnProperty(key)) {
         pruneNulls(schema[key])         
     }
    }
}

function forceRequired(schema) {  
    if (!(schema && typeof schema === 'object')){
        return; 
    }
    
    if (schema.properties) {
        var keys = [];
        for (var key in schema.properties) {
            schema.properties.hasOwnProperty(key) &&  keys.push(key);
        }
        if (keys.length != 0) {
            schema.required = keys;
        } 
    }
    
    for (var key in schema) {
        schema.hasOwnProperty(key) && forceRequired(schema[key]);               
    }
}

function removeOptional(schema) {  
    if (!(schema && typeof schema === 'object')){
        return; 
    }
    
    if (schema.properties) {
        for (var key in schema.properties) {
            if (schema.properties.hasOwnProperty(key) && (!schema.required || schema.required.indexOf(key) == -1)) {
                 delete schema.properties[key];
            }
        }
    }
    
    for (var key in schema) {
        schema.hasOwnProperty(key) && removeOptional(schema[key]);               
    }
}

function maximalArrays(schema) {  
    if (!(schema && typeof schema === 'object')){
        return; 
    }

    if (isArrayObject(schema)) {
  		var nItems = schema.maxItems;
		if (nItems === undefined || nItems === null)
			nItems = schema.minItems;
		if (nItems === undefined || nItems === null || nItems < 1)
			nItems = 1;

		schema.maxItems = schema.minItems = nItems;
    }

    for (var key in schema) {
        schema.hasOwnProperty(key) && maximalArrays(schema[key]);               
    }
}

function minimalArrays(schema) {  
    if (!(schema && typeof schema === 'object')){
        return; 
    }

    if (isArrayObject(schema)) {
    	var minItems = schema.minItems;
    	if (minItems !== undefined && minItems !== null)
    		schema.maxItems = minItems;
    }

    for (var key in schema) {
        schema.hasOwnProperty(key) && minimalArrays(schema[key]);               
    }
}

function isArrayObject(obj)
{
    if (obj.type && obj.hasOwnProperty('type')) {
    	for (var i in obj.type) {
    		if (obj.type[i] === 'array')
    		  return true;
    	}
    }
    return false;
}