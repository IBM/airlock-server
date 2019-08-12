package com.ibm.airlock.admin;

import java.util.ArrayList;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptableObject;
// use Global to enable JS print
//import org.mozilla.javascript.tools.shell.Global;

import com.ibm.airlock.engine.SafeContextFactory;
//import com.ibm.airlock.engine.ScriptExecutionTimeoutException;

import com.google.gson.Gson;

public class SchemaValidator
{
    // thread-safe
    private static final Gson gson = new Gson();

//    Scriptable sharedScope;
//    String mainSchemaId;

    //---------------------------------------------------------------------------
    public static class ValidationError
    {
    	// the members are filled in by Gson.fromJson().
    	private String keyword;
    	private String dataPath;
    	private String schemaPath;
    	private Object params;
    	private String message;

//    	public ValidationError() {}

    	public String getKeyword() { return keyword; }
    	public String getDataPath() { return dataPath; }
    	public String getSchemaPath() { return schemaPath; }
    	public Object getParams() { return params; }
    	public String getMessage() { return message; }
    	
    	public String toString() {
    		return "keyword:" + keyword + ", message: " + message + ", dataPath:" + dataPath + ", schemaPath:" + schemaPath; 
    	}
    }
    public static class Result
    {
    	// the members are filled in by Gson.fromJson().
    	private boolean isSuccessful;
    	private ValidationError[] errors;

    	public boolean isSuccessful() { return isSuccessful; }
    	public ValidationError[] getErrors() { return errors; }

    	public String printErrors()
    	{
    		if (errors == null)
    			return "";

    		StringBuilder b = new StringBuilder();
    		for (int i = 0; i < errors.length; ++i)
    		{
    			if (i > 0)
    				b.append("\n");
    			b.append(errors[i].toString());
    		}
    		return b.toString();
    	}
    }
    
	//------------------------------------------------------------
	// stateless document validation
    static public void validation(String validator, String ajv, String schema, String doc, Boolean relaxed) throws ValidationException
    {
 		Context cx = Context.enter();

  		try {
  			ScriptableObject scope = cx.initStandardObjects();
  			
  			cx.evaluateString(scope, validator, "<validator>", 1, null);
  			cx.evaluateString(scope, ajv, "<ajv>", 1, null);
  	
  			// initialized during call to simpleValidate instead
  			// Function initValidatorFunction = (Function)scope.get("initSimpleValidator", scope);
 			// Object initValidatorArgs[] = { };
 			// initValidatorFunction.call(cx, scope, scope, initValidatorArgs);

		    Function validateFunction = (Function)scope.get("simpleValidate", scope);
			String resJson;
			Object validateArgs[] = { doc, schema, relaxed };
			resJson = (String)validateFunction.call(cx, scope, scope, validateArgs);
			Result validationResult =  gson.fromJson(resJson, Result.class);

			if (!validationResult.isSuccessful())
			{
				throw new ValidationException("Validation error: " + validationResult.printErrors());
			}
  		}
  		catch (ValidationException e)
  		{
  			throw e;
  		}
  		catch (Exception e)
  		{
  			throw new ValidationException(e.getMessage());
  		}
  		finally {
  			Context.exit();
  		}
    }
    //------------------------------------------------------------
	// stateless schema validation. verifyLeaves checks for stage/minAppVersion and should be null when validating output schemas
	static public void validateSchema(String validator, String verifyLeaves, String ajv, String schema) throws ValidationException
	{
		new SafeContextFactory().makeContext();
		Context rhino = Context.enter();

		try {
			ScriptableObject scope = rhino.initStandardObjects();
			// use Global() instead of initStandardObjects() to enable JS print()
			//ScriptableObject scope = new Global(rhino);

			rhino.evaluateString(scope, validator, "<validator>", 1, null);
			if (verifyLeaves != null)
				rhino.evaluateString(scope, verifyLeaves, "<verifyLeaves>", 1, null);

			rhino.evaluateString(scope, ajv, "<ajv>", 1, null);

			Boolean forInput = (verifyLeaves != null);

			// schemas are validated against meta-schema on AJV initialization
			Function initValidatorFunction = (Function)scope.get("initSchemaValidator", scope);
			Object argList[]= {  schema,  forInput };
			initValidatorFunction.call(rhino, scope, scope, argList);

			if (forInput)
			{
				Function leavesFunction = (Function)scope.get("getLeafErrors", scope);
				Object leavesArgs[]= { schema };
				NativeArray arr  = (NativeArray) leavesFunction.call(rhino, scope, scope, leavesArgs);
				if (arr.getLength() > 0)
					throw new ValidationException("Missing stage/version in leaves: " + printArray(arr));
			}
		}
  		catch (ValidationException e)
  		{
  			throw e;
  		}
		catch (Exception e)
		{
			throw new ValidationException(e.getMessage());
		}
		finally {
			Context.exit();
		}    	
	}
	static String printArray(NativeArray arr)
	{
		ArrayList<String> out = new ArrayList<String>();
		 for (int i = 0; i < arr.getLength(); ++i) {
			 Object obj = arr.get(i);
			 out.add(obj == null ? "" : obj.toString());
		 }
		return out.toString();
	}

	   //------------------------------------------------------------
		// more stringent validation of input schema.
		// after initial validation, generate a fake JSON from it and run it through the schema.
		// this catches errors such as a 'required' field which does not appear in 'properties'
		static public void extendedValidation(String validator, String verifyLeaves, String ajv, 
				String generator, String jsf, String prune, String stage, String minAppVer,
				String schema) throws ValidationException, GenerationException
		{
			validateSchema(validator, verifyLeaves, ajv, schema);
			String generated = JsonGenerator.generation(generator, jsf, prune, schema, stage, minAppVer);
			System.out.println(generated);
			boolean relaxed = false;
			validation(validator, ajv, schema, generated, relaxed);
		}

    //---------------------------------------------------------------------------
    // validate a set of connected schemas. The schemas are supplied in the constructor

	/* this one throws error: org.mozilla.javascript.UniqueTag incompatible with org.mozilla.javascript.Function
	   use the stateless 'validation()' method until we figure it out

	public SchemaValidator(String mainSchemaId, String[] schemas, String validator, String ajv)
	{
		this.mainSchemaId = mainSchemaId;

		//String validator = Utilities.getResource("scripts", "validator.js");
		//String ajv = Utilities.getResource("scripts", "ajv.min.js");

        new SafeContextFactory().makeContext();
        Context rhino = Context.enter();

        try {
        	sharedScope = rhino.initStandardObjects();
        	rhino.evaluateString(sharedScope, validator, "<cmd>", 1, null);
        	rhino.evaluateString(sharedScope, ajv, "<cmd>", 1, null);
     
    		Function initValidatorFunction = (Function)sharedScope.get("initValidator", sharedScope);
    		Object initValidatorArgs[]= {  schemas };
    		// schemas are validated against metaschema on Ajv initialization
    		initValidatorFunction.call(rhino, sharedScope, sharedScope, initValidatorArgs);
        }
        catch (Throwable e){
        	throw new RuntimeException("Javascript shared scope initialization error: " + e.getMessage());
        }
        finally {
        	Context.exit();
        }
	}
	public void validate(String document) throws ValidationException
	{
        new SafeContextFactory().makeContext();
        Context rhino = Context.enter();

        try {
        	Scriptable scope = rhino.newObject(sharedScope);
        	scope.setPrototype(sharedScope);
        	scope.setParentScope(null);

		    Function validateFunction = (Function)scope.get("validate", scope);
			String resJson;
			Object validateArgs[] = { document, mainSchemaId };
			resJson = (String)validateFunction.call(rhino, scope, scope, validateArgs);
			Result validationResult = gson.fromJson(resJson, Result.class);

			if (!validationResult.isSuccessful()) {
				//throw new ValidationException("Validation error: " + validationResult.getMessage());
				throw new ValidationException("Validation error: " + validationResult.getErrors());
			}
        }
        catch (ScriptExecutionTimeoutException e){
        	throw new ValidationException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	throw new ValidationException("Javascript error: " + e.getMessage());
        }
        finally {
            Context.exit();
        }
    }
    */

    //---------------------------------------------------------------------------
	// validate a single schema on the fly. The schema is supplied in the validate method

	/* this one throws error: org.mozilla.javascript.UniqueTag incompatible with org.mozilla.javascript.Function
	   use the stateless 'validation()' method until we figure it out

	public SchemaValidator(String validator, String ajv)
	{
        new SafeContextFactory().makeContext();
        Context rhino = Context.enter();

        try {
			sharedScope = rhino.initStandardObjects();
			rhino.evaluateString(sharedScope, validator, "<validator>", 1, null);
			rhino.evaluateString(sharedScope, ajv, "<ajv>", 1, null);

			Function initFunction = (Function)sharedScope.get("initSimpleValidator", sharedScope);
			Object initArgs[]= { };
			initFunction.call(rhino, sharedScope, sharedScope, initArgs);
        }
        catch (Throwable e){
        	throw new RuntimeException("Javascript shared scope initialization error: " + e.getMessage());
        }
        finally {
        	Context.exit();
        }
	}
	public void simpleValidate(String document, String schema) throws ValidationException
	{
		// create and enter safe execution context
        new SafeContextFactory().makeContext();
        Context rhino = Context.enter();

        try {
        	Scriptable scope = rhino.newObject(sharedScope);
        	scope.setPrototype(sharedScope);
        	scope.setParentScope(null);

		    Function validateFunction = (Function)scope.get("simpleValidate", scope);
			String resJson;
			Object validateArgs[] = { document, schema };
			resJson = (String)validateFunction.call(rhino, scope, scope, validateArgs);
			Result validationResult = gson.fromJson(resJson, Result.class);

			if (!validationResult.isSuccessful()) {
				//throw new ValidationException("Validation error: " + validationResult.getMessage());
				throw new ValidationException("Validation error: " + validationResult.getErrors());
			}
        }
        catch (ScriptExecutionTimeoutException e){
        	throw new ValidationException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	throw new ValidationException("Javascript error: " + e.getMessage());
        }
        finally {
            Context.exit();
        }
    }
    */

	//---------------------------------------
	public static void main(String[] args)
	{
		try {

			String ajv = Utilities.readString("C:/client/scripts/ajv.min.js");
			String validator = Utilities.readString("C:/client/scripts/validator.js");
			String verifyLeaves = Utilities.readString("C:/client/scripts/validateLeaves.js");

			String schema = Utilities.readString("C:/client/josemina/AirlockInputShema_profile.json");
			String doc = Utilities.readString("C:/client/v/iritDoc.txt");

			String jsf = Utilities.readString("C:/client/scripts/json-schema-faker.min.js");
			String generator = Utilities.readString("C:/client/scripts/generator.js");
			String prune = Utilities.readString("C:/client/scripts/prune.js");
			String stage = "PRODUCTION";
			String minAppVer = "8.6";

			SchemaValidator.extendedValidation(validator, verifyLeaves, ajv, generator, jsf, prune, stage, minAppVer, schema);

			//SchemaValidator.validateSchema(validator, verifyLeaves, ajv, schema);

			//boolean relaxed = false;
			//SchemaValidator.validation(validator, ajv, schema, doc, relaxed);

			//SchemaValidator sv = new SchemaValidator(validator, ajv);
			//sv.simpleValidate(doc, schema);
			//String[] schemas = { schema };
			//SchemaValidator sv = new SchemaValidator("myID", schemas, validator, ajv);
			//sv.validate(doc);

			System.out.println("ok");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
