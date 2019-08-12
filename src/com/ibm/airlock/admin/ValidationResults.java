package com.ibm.airlock.admin;

import javax.ws.rs.core.Response.Status;

public class ValidationResults {
	public String error;
	public Status status;
		
	public ValidationResults (String error, Status status) {
		this.error = error;
		this.status = status;
	}	
}
