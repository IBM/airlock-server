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

import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

public class ExecOrderReporter implements IReporter {
	
	private PrintWriter mOut;

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
			String outputDirectory) {
		
		        for (ISuite suite : suites) {
		        new File(outputDirectory+File.separator+suite.getName()).mkdirs();
		            print("Suite>" + suite.getName());
		            Map<String, ISuiteResult> suiteResults = suite.getResults();
		            for (String testName : suiteResults.keySet()) {
		            	 try {
		 		            mOut = new PrintWriter(new BufferedWriter(new FileWriter(new File( outputDirectory+File.separator+suite.getName(), testName+".html"))));
		 		        }catch (IOException e) {
		 		            System.out.println("Error in creating writer: " + e);
		 		        }
		            	 writeHtmlHead(testName);
		            	 mOut.println("<body>");
		                ISuiteResult suiteResult = suiteResults.get(testName);
		                ITestContext testContext = suiteResult.getTestContext();
		                 writeSummaryTable(testName, testContext);
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
		               ArrayList<ITestResult> allTests = new ArrayList<ITestResult>(testsFailed);
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
		               //Now print tests in exec order
		               writeResultsTable(allTests);
		               closeHtml();
				        mOut.flush();
				        mOut.close();
		            }
		        }
		        
	}
	
	private void print(String text) {
		        System.out.println(text);
		       if ((text!=null)&&(mOut!=null))  mOut.println(text + "");
		    }
	
	
			private void closeHtml(){
				mOut.println("</body></html>");
				
			}
			
		    private void writeHtmlHead(String title) {
		        mOut.println("<html><head><title>TestNG: "+title+"</title>"
		        			+"<link href=\"../testng.css\" rel=\"stylesheet\" type=\"text/css\" />"
		        			+"<link href=\"../my-testng.css\" rel=\"stylesheet\" type=\"text/css\" />"
		        			+ "<style type=\"text/css\">"
		        			+ ".log { display: none;} "
		        			+ ".stack-trace { display: none;} "
		        			+ "</style>"
		        			+ "<script type=\"text/javascript\">"
		        			+ "<!-- "
		        			+ "function flip(e) {"
		        			+ "  current = e.style.display;"
		        			+ "if (current == 'block') {"
		        			+ "e.style.display = 'none';"
		        			+ "  return 0;"
		        			+ "}"
		        			+ "else {"
		        			+ "e.style.display = 'block';"
		        			+ "return 1;"
		        			+ "  }"
		        			+ "}"
		        			+ "function toggleBox(szDivId, elem, msg1, msg2)"
		        			+ "{"
		        			+ "var res = -1;  if (document.getElementById) {"
		        			+ "res = flip(document.getElementById(szDivId));"
		        			+ "}"
		        			+ "else if (document.all) { // this is the way old msie versions work"
		        			+ "res = flip(document.all[szDivId]);"
		        			+ "}"
		        			+ "if(elem) {"
		        			+ "if(res == 0) elem.innerHTML = msg1; else elem.innerHTML = msg2;"
		        			+ "}"
		        			+ "}"
		        			+ "function toggleAllBoxes() {"
		        			+ "if (document.getElementsByTagName) {"
		        			+ "d = document.getElementsByTagName('div');"
		        			+ "for (i = 0; i < d.length; i++) {"
		        			+ "if (d[i].className == 'log') {"
		        			+ "flip(d[i]);"
		        			+ "}"
		        			+ "}"
		        			+ "}"
		        			+ "} // -->"
		        			+ "</script>"
		        			+ "</head>"
		        			+ "");
		    }
		    
		    private void writeSummaryTable(String testName, ITestContext test){
		    	
		    	long totalTimeMilli = test.getEndDate().getTime() - test.getStartDate().getTime() ;
		    	int totalTimeSec = (int)(totalTimeMilli/1000);
		    	mOut.println("<h2 align='center'>"+testName+"</h2><table border='1' align=\"center\">"
		    			+ "<tr><td>Run against url: </td><td>"+test.getSuite().getParameter("url")+"</td></tr>"
		    			+ "<tr>"
		    			+ "<td>Tests passed/Failed/Skipped:</td><td>"+test.getPassedTests().size()+"/"+test.getFailedTests().size()+"/"+test.getSkippedTests().size()+"</td>"
		    			+"</tr><tr>"
		    			+ "<td>Started on:</td><td>"+test.getStartDate()+"</td></tr>"
		    			+ "<tr><td>Total time:</td><td>"+totalTimeSec+" seconds ("+totalTimeMilli+" ms)</td></tr><tr>"
		    					+ "<td>Included groups:</td><td>"+strArrToString(test.getIncludedGroups())+"</td></tr><tr>"
		    							+ "<td>Excluded groups:</td><td>"+strArrToString(test.getExcludedGroups())+"</td></tr>"
		    									+ "</table><p/>");
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
