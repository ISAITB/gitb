package net.validex.gitb.model;

import java.util.List;

/**
 * Created by serbay.
 */
public class Report {
	private String id;
	private String filename;
	private String documentType;
	private List<Selector> selectors;
	private List<Validation> validations;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public List<Selector> getSelectors() {
		return selectors;
	}

	public void setSelectors(List<Selector> selectors) {
		this.selectors = selectors;
	}

	public List<Validation> getValidations() {
		return validations;
	}

	public void setValidations(List<Validation> validations) {
		this.validations = validations;
	}
}
