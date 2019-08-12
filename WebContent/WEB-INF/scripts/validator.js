/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2006, 2016
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

// some ajv paths use 'console' for warnings.
// Rhino does not use console, but if initialized with Global()
// it can use 'print'; redirecting console messages to print 
 var console = {
	"warn" : function(str) { print(str) },
	"error" : function(str) { print(str) },
	"log" : function(str) { print(str) }
 };
 
var window = {};

function initValidator(schemaStrs) {
    var schemaArr = [];
	for (i = 0; i < schemaStrs.length; i++) {
		schemaArr.push(JSON.parse(schemaStrs[i]));
	}
    
	window.__my_ajv__ = window.Ajv({ 
     allErrors: true,
     schemas: schemaArr
  });	
}


function validate(docStr, mainSchemaId) {
	var res = {"isSuccessful": true};
	if(window.__my_ajv__ === undefined) {
		res.isSuccessful = false;
		res.message =  "Validator is not initialized";
	} else {
		var validate = window.__my_ajv__.getSchema(mainSchemaId);
	    var valid = validate(JSON.parse(docStr));
	    if (!valid) {
	      res.isSuccessful = false;
	      res.message =  window.__my_ajv__.errorsText(validate.errors);	
	    }
	}	
    return JSON.stringify(res);
}

function pruneRequired(obj) { 
    for (var key in obj) {
        if (obj.hasOwnProperty(key)) {
             if (key == "required" ) {
                delete obj[key];
              }  else if (typeof obj[key] === 'object'){
                  pruneRequired(obj[key]);
              }

        }
    }

    return obj;    
}

function initSimpleValidator() {
	 
	window.__my_ajv__ = window.Ajv({ 
     allErrors: true
  });	
}

function simpleValidate(docStr, schemaStr, relaxed)
{
	var res = { "isSuccessful" : true };

	if (window.__my_ajv__ === undefined)
	{
		window.__my_ajv__ = window.Ajv({allErrors : true});
	}

	var schema = JSON.parse(schemaStr);
	if (relaxed) {
		pruneRequired(schema);
	}
	var validate = window.__my_ajv__.compile(schema);
    var valid = validate(JSON.parse(docStr));
    if (!valid) {
      res.isSuccessful = false;
     // res.message =  window.__my_ajv__.errorsText(validate.errors);
      res.errors =  validate.errors;	
    }

    return JSON.stringify(res);
}

function initSchemaValidator(schemaStr, forInput)
{
	window.__my_ajv__ = window.Ajv({  allErrors: true });	

	var schema = JSON.parse(schemaStr);
	if (forInput)
	{
		var isObj = (schema !== null) && (typeof schema === "object") && !Array.isArray(schema);
		var hasProperties = schema.properties !== undefined;
		if (!isObj || !hasProperties)
			throw "schema does not have properties";
	}
	
	// throw an error if the schema is not valid
	var validate = window.__my_ajv__.compile(schema);
}