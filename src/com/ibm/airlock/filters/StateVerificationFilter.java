package com.ibm.airlock.filters;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.Utilities;


@WebFilter("/StateVerificationFilter")
public class StateVerificationFilter implements Filter {

	public static final Logger logger = Logger.getLogger(StateVerificationFilter.class.getName());

	
	private ServletContext context;	

	public void init(FilterConfig fConfig) throws ServletException {
		context = fConfig.getServletContext();
		logger.info("StateVerificationFilter initialized");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		Constants.ServiceState state = (Constants.ServiceState)this.context.getAttribute(Constants.SERVICE_STATE_PARAM_NAME);
		switch (state) {
		case RUNNING:
			// pass the request along the filter chain
			chain.doFilter(request, response);
			break;
		case S3_IO_ERROR:
			//return error status
			String err = "An S3 I/O error occured. Check the server logs for details. Restart the Airlock service and try again.";
			HttpServletResponse resp=(HttpServletResponse) response;
			resp.sendError(500, Utilities.errorMsgToErrorJSON(err)); //Status.INTERNAL_SERVER_ERROR			
			break;
		case S3_DATA_CONSISTENCY_ERROR:
			//return error status
			err = "The Airlock data on S3 is in an unexpected format. For example, a JSON format is illegal. Check the server logs for details, fix the data error and restart the Airlock service.";
			resp=(HttpServletResponse) response;
			resp.sendError(500,  Utilities.errorMsgToErrorJSON(err)); //Status.INTERNAL_SERVER_ERROR			
			break;
		case INITIALIZING:
			//return initializing state
			String msg = "The Airlock service is initializing - try again later."; 
			resp=(HttpServletResponse) response;
			resp.sendError(503,  Utilities.errorMsgToErrorJSON(msg)); //Status.SERVICE_UNAVAILABLE - the server is temporarily unavailable
			/*
			    503 Service Unavailable
				The server is currently unable to handle the request due to a temporary overloading or maintenance of the server. The implication is that this is a temporary condition which will be alleviated after some delay. If known, 
				the length of the delay MAY be indicated in a Retry-After header. If no Retry-After is given, the client SHOULD handle the response as it would for a 500 response.
			 */
			
			break;
		}         
	}

	public void destroy() {
		//no resource to close
	}

}

