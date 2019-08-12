package tests.com.ibm.qautils;

import java.io.IOException;
import java.util.ArrayList;

public class TestngUtils {
	
	
	public static void createSharedParametersFile(String filePath,ArrayList<String[]> parameters) throws IOException{
		String sharedParametersFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" ;
		for (int i=0;i<parameters.size();i++){
			String[] current = parameters.get(i);
			sharedParametersFile+="<parameter name=\""+current[0]+"\"" ;
			sharedParametersFile+=" value=\""+current[1]+"\"";
			sharedParametersFile+="></parameter>\n" ;
		}
		FileUtils.stringToFile(sharedParametersFile, filePath);
	}
	
	public static void appendToSharedParametersFile(String filePath,ArrayList<String[]> parameters) throws IOException{
		String appendMe = "" ;
		for (int i=0;i<parameters.size();i++){
			String[] current = parameters.get(i);
			appendMe+="<parameter name=\""+current[0]+"\"" ;
			appendMe+=" value=\""+current[1]+"\"";
			appendMe+="></parameter>\n" ;
		}
		FileUtils.appendStringToEndOfFile(appendMe, filePath);
	}
	
	public static void createTestngSuiteOfSuiteFiles(String suiteName,String sharedParametersFile, String[] suites, String suiteFilePath) throws IOException{
		
		String suiteOfSuitesFile = "<!DOCTYPE suite SYSTEM \"http://beust.com/testng/testng-1.0.dtd\" " ;
		
		if (sharedParametersFile==null) suiteOfSuitesFile += ">\n" ;
		else suiteOfSuitesFile += "[<!ENTITY parent SYSTEM \""+sharedParametersFile+"\">]>\n" ;
		suiteOfSuitesFile+="<suite name=\""+suiteName+"\" verbose=\"3\">\n";
		if (sharedParametersFile!=null) suiteOfSuitesFile+="&parent;\n" ;
		suiteOfSuitesFile+="<suite-files>\n";
		for(int i=0;i<suites.length;i++){
			suiteOfSuitesFile+="<suite-file path=\""+suites[i]+"\"/>\n" ;
		}
		
		suiteOfSuitesFile+="</suite-files>\n";
		suiteOfSuitesFile+="</suite>";
		
		FileUtils.stringToFile(suiteOfSuitesFile, suiteFilePath);
	}

}
