package com.ibm.airlock.admin.operations;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.airlock.admin.Branch;
import com.ibm.airlock.admin.Product;
import com.ibm.airlock.admin.Season;

public class AirlockChange {
	private Collection<AirlockChangeContent> files = new ArrayList<AirlockChangeContent>();
	private Product product = null;
	private Season season = null;
	private Branch branch = null;
	
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	} 
	public Collection<AirlockChangeContent> getFiles() {
		return files;
	}
	public void setFiles(Collection<AirlockChangeContent> files) {
		this.files = files;
	}
	public Season getSeason() {
		return season;
	}
	public void setSeason(Season season) {
		this.season = season;
	}
	public Branch getBranch() {
		return branch;
	}
	public void setBranch(Branch branch) {
		this.branch = branch;
	}

}
