package tests.restapi.testsmartling;

import java.util.Date;
import java.util.TreeMap;

public class TranslationState
{
	String source, sourceHash;
	TreeMap<String, Translation> translations = new TreeMap<String, Translation>(); // grouped by locale

	static public class Translation
	{
		String localeId;
		Date modifiedDate;
		String translation;
		String plural; // needed?
		String issueId;
	}
}
