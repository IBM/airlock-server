package tests.testng;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.IInvokedMethod;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

public class ExecOrderEmailReporter implements IReporter {
	
	private PrintWriter mOut;
	private PrintWriter emWriter ;

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
			String outputDirectory) {
		
				try {
					emWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDirectory, "exec-order-emailable-report.html"))));
				} catch (IOException e1) {
					System.out.println("Cannot create a writer for emailable-report.html file");
				}
				writeEmailHtmlHead();
				writeEmailHtmlBodyTopTable(suites);
				for (ISuite suite : suites)
					writeSuiteTestsTable(suite);
				emWriter.println("</body></html>");
				emWriter.flush();
				emWriter.close();
		        }
		        
	
	
			private int countFailures(List<ISuite> suites){
				int count = 0 ;
					for (int i=0;i<suites.size();i++){
						List<IInvokedMethod> methods = suites.get(i).getAllInvokedMethods() ;
						for (int k=0;k<methods.size();k++)
							if (methods.get(k).getTestResult().getStatus() == 2) count ++ ;
					}
				return count ;
				
			}
			//.suite-res { display: none;}
			private void writeEmailHtmlHead(){
				emWriter.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
								+"<html xmlns=\"http://www.w3.org/1999/xhtml\">"
								+ "<head>"
								+ "<title>TestNG Report</title>"
								+ "<script type=\"text/javascript\" src=\"jquery-1.7.1.min.js\"></script>"
								+"<script type=\"text/javascript\"> $(document).ready(function(){$(\".suite-res\").show(); $(\".suite-res\").click(function(){ $(this).nextUntil(\".suite-res\").slideToggle(1000);});});</script>"
								+ "<style type=\"text/css\">table {margin-bottom:10px;border-collapse:collapse;empty-cells:show}th,td {border:1px solid #009;padding:.25em .5em}th {vertical-align:bottom}td {vertical-align:top}table a {font-weight:bold}.stripe td {background-color: #E6EBF9}.num {text-align:right}.passedodd td {background-color: #3F3}.passedeven td {background-color: #0A0}.skippedodd td {background-color: #DDD}.skippedeven td {background-color: #CCC}.failedodd td,.attn {background-color: #F33}.failedeven td,.stripe .attn {background-color: #D00}.stacktrace {white-space:pre;font-family:monospace}.totop {font-size:85%;text-align:center;border-bottom:2px solid #000}.invocation-failed,  .test-failed  { background-color: #DD0000; }.invocation-passed,  .test-passed  { background-color: #00AA00; }.invocation-skipped, .test-skipped { background-color: #CCCC00; }</style>"
								+ "</head>");
			}
			
			private void writeEmailHtmlBodyTopTable(List<ISuite> suites){
				emWriter.println("<body><table>"
								+"<tr><th>Test</th><th># Passed</th><th># Skipped</th><th># Failed</th><th>Time (ms)</th></tr>");
				//write the suites summary 
				for (ISuite suite : suites) {
					int totalPassed = 0 ;
					int totalFailed = 0 ;
					int totalSkipped = 0 ;
					long totalTime = 0 ;
					
					emWriter.println("<tr><th colspan=\"5\">"+suite.getName()+"</th></tr>");
					emWriter.println("<tr><th>Run against url:</th><th colspan=\"4\">"+suite.getParameter("url")+"</th></tr>");
					Map<String, ISuiteResult> suiteResults = suite.getResults();
					Set<String> keys = suiteResults.keySet() ;
					String[] orderedKeys = keys.toArray(new String[keys.size()]) ;
		            	for(int i=0;i<orderedKeys.length;i++){
		            		ISuiteResult result = suiteResults.get(orderedKeys[i]);
		            		ITestContext test = result.getTestContext();
		            		long totalTimeMilli = test.getEndDate().getTime() - test.getStartDate().getTime() ;
		            		String testName = result.getTestContext().getName() ;
		    		    	emWriter.println("<tr><td class=\"num\"><a href=\"#"+testName+"\">"+testName+"</a></td><td class=\"num\">"+test.getPassedTests().size()+"</td><td class=\"num\">"+test.getSkippedTests().size()+"</td><td class=\"num\">"+test.getFailedTests().size()+"</td><td class=\"num\">"+totalTimeMilli+"</td></tr>");
		            		totalPassed += test.getPassedTests().size() ;
		            		totalFailed+= test.getFailedTests().size() ;
		            		totalSkipped+= test.getSkippedTests().size() ;
		            		totalTime+=totalTimeMilli ;
		            	}
		            	emWriter.println("<tr><th>Total</th><th class=\"invocation-passed\">"+totalPassed+"</th><th class=\"invocation-skipped\">"+totalSkipped+"</th><th class=\"invocation-failed\">"+totalFailed+"</th><th class=\"num\">"+totalTime+"</th></tr>");
		            }
				emWriter.println("</table>");
			}
			
			
			private void writeSuiteTestsTable(ISuite suite){
				emWriter.println("<table class=\"suite-res\"><thead><tr><th colspan=\"6\">"+suite.getName()+"</th></tr></thead>");
				 Map<String, ISuiteResult> suiteResults = suite.getResults();
		            for (String testName : suiteResults.keySet()) {
		            	emWriter.println("<tbody><tr><th id=\""+testName+"\" colspan=\"6\">"+testName+"</th></tr><tr><th>Class</th><th>Method</th><th>Exception</th><th>Start</th><th>Time (ms)</th><th>Status</th></tr></tbody>");
		            	ISuiteResult suiteResult = suiteResults.get(testName);
		                ITestContext testContext = suiteResult.getTestContext();
		                ArrayList<ITestResult> allTests = orderTestsByExecTime(testContext);
		                for (int i=0 ;i<allTests.size();i++){
		                	ITestResult testMethodResult = allTests.get(i);
		                	String className = testMethodResult.getTestClass().getRealClass().getCanonicalName() ;
				    		String methodName = testMethodResult.getMethod().getMethodName() ;
				    		String description = testMethodResult.getMethod().getDescription() ;
				    		
				    		String exception = "" ;
				    		if (testMethodResult.getThrowable()!=null ) exception = testMethodResult.getThrowable().getMessage() ;
				    		long startTime = testMethodResult.getStartMillis() ;
				    		long endTime = testMethodResult.getEndMillis() ;
				    		int totalTime = (int) (endTime - startTime) ;
				    		int testStatus = testMethodResult.getStatus() ;
				    		String cssClass = "" ;
				    		String status = "" ;
				    		switch (testStatus) {
					    		case 1 : 
					    			cssClass = "invocation-passed" ;
					    			status = "PASSED" ;
					    		break ;
					    		
					    		case 2 : 
					    			cssClass = "invocation-failed" ;
					    			status = "FAILED" ;
					    			exception = testMethodResult.getThrowable().getMessage() ;
					    		break ;
					    		
					    		case 3 : 
					    			cssClass = "invocation-skipped" ;
					    			status = "SKIPPED" ;
					    			exception = testMethodResult.getThrowable().getMessage() ;
					    		break ;
				    		}
		                	emWriter.println("<tbody><tr class=\""+cssClass+"\"><td>"+className+"</td><td><br><b>"+methodName+"<\b></br><br><b>Description: "+description+"</b></br></td>"
		                			+"<td>"+exception+"</td>"
		                					+ "<td>"+startTime+"</td>"
		                							+ "<td>"+totalTime+"</td><td>"+status+"</td></tr></tbody>");
		                }
		            }
		            emWriter.println("</table>");
				
			}
			
			private ArrayList<ITestResult> orderTestsByExecTime(ITestContext testContext){
				
				ArrayList<ITestResult> allTests = new ArrayList<ITestResult>();
				 //take all failed tests
	                IResultMap failedResult = testContext.getFailedTests();
	                Set<ITestResult> testsFailed = failedResult.getAllResults();
	                //take all passed tests
	                IResultMap passResult = testContext.getPassedTests();
	                Set<ITestResult> testsPassed = passResult.getAllResults();
	                //take all skipped tests
	                IResultMap skippedResult = testContext.getSkippedTests();
	                Set<ITestResult> testsSkipped = skippedResult.getAllResults();
	                //Put all tests together
	               allTests.addAll(testsFailed);
	               allTests.addAll(testsPassed);
	               allTests.addAll(testsSkipped);
	               // Sorting
	               Collections.sort(allTests, new Comparator<ITestResult>() {
	                       @Override
	                       public int compare(ITestResult test1, ITestResult test2)
	                       {
	                    	  int time =  (int) (test1.getEndMillis() - test2.getEndMillis() );
	                    	  return time;
	                       }
	                   });
				 
				 return allTests ;
	                 
			}
		    
		    private void writeResultsTable(ArrayList<ITestResult> allTests){
		    	mOut.println("<table width='100%' border='1'>"
		    			+ "<tr><td><b>Test method</b></td>"
		    			+ "<td width=\"30%\"><b>Exception</b></td>"
		    			+ "<td width=\"5%\"><b>Start Time (millis)</b></td>"
		    			+ "<td width=\"5%\"><b>End Time (millis)</b></td>"
		    			+ "<td width=\"5%\"><b>Total Time (millis)</b></td>"
		    			+ "<td><b>Status</b></td></tr>");
		    	for (int i=0;i<allTests.size();i++){
		    		ITestResult testMethodResult = allTests.get(i);
		    		String className = testMethodResult.getTestClass().getRealClass().getCanonicalName() ;
		    		String methodName = testMethodResult.getMethod().getMethodName() ;
		    		String description = testMethodResult.getMethod().getDescription() ;
		    		long startTime = testMethodResult.getStartMillis() ;
		    		long endTime = testMethodResult.getEndMillis() ;
		    		int totalTime = (int) (endTime - startTime) ;
		    		String cssClass = "" ;
		    		String status = "" ;
		    		String exception = "" ;
		    		int testStatus = testMethodResult.getStatus() ;
		    		switch (testStatus) {
			    		case 1 : 
			    			cssClass = "invocation-passed" ;
			    			status = "PASSED" ;
			    		break ;
			    		
			    		case 2 : 
			    			cssClass = "invocation-failed" ;
			    			status = "FAILED" ;
			    			exception = testMethodResult.getThrowable().getMessage() ;
			    		break ;
			    		
			    		case 3 : 
			    			cssClass = "invocation-skipped" ;
			    			status = "SKIPPED" ;
			    			exception = testMethodResult.getThrowable().getMessage() ;
			    		break ;
		    		}
		    		mOut.println("<tr class=\""+cssClass+"\">"
		    				+ "<td title='"+className+"."+methodName+"'><b>"+methodName+"</b><br><b>Test class: </b>"+className+"</br>"
		    						+ "<br><b>Description: </b>"+description+"</br></td>"
		    						+ "<td>"+exception+"</td>"
		      						+ "<td>"+startTime+"</td>"
		      						+ "<td>"+endTime+"</td>"
		      						+ "<td>"+totalTime+"</td>"
		      						+ "<td>"+status+"</td>");
		    	}
		    	
		    }
		    
		    
		    private String strArrToString(String[] arr){
		    	String res = "";
		    	for (int i=0;i<arr.length;i++)
		    		if (i!=(arr.length -1)) res+=arr[i]+"," ;
		    		else res+=arr[i] ;
		    	
		    	return res ;
		    }
		

}
