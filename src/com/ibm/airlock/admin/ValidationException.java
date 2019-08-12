package com.ibm.airlock.admin;

@SuppressWarnings("serial")
public class ValidationException extends Exception
{
    public ValidationException(String errMsg) {
    	super(errMsg);
    }
    public static class Translation extends ValidationException
    {
		public Translation(String errMsg) {
			super(errMsg);
		}
    }

    // patch for missing translationId errors
    public static ValidationException factory(String prefix, String error)
    {
    	final String id = "translation id:";
    	int loc = error.indexOf(id);

    	if (loc >= 0)
    	{
    		String newError = "translate() function called with unknown parameters. String key(s) are either missing or in the wrong stage: " + error.substring(loc + id.length());
    		return new Translation(prefix + newError);
    	}
		else
			return new ValidationException(prefix + error);
    }
}
