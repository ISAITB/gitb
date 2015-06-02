package net.validex.gitb.model;

import java.util.List;

/**
 * Created by serbay.
 */
public class Validation {
	private String id;
	private String syntaxName;
	private String syntaxVersion;
	private String validationLevel;
	private boolean success;
    private boolean isMessage;
	private List<ValidationResult> results;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSyntaxName() {
		return syntaxName;
	}

	public void setSyntaxName(String syntaxName) {
		this.syntaxName = syntaxName;
	}

	public String getSyntaxVersion() {
		return syntaxVersion;
	}

	public void setSyntaxVersion(String syntaxVersion) {
		this.syntaxVersion = syntaxVersion;
	}

	public String getValidationLevel() {
		return validationLevel;
	}

	public void setValidationLevel(String validationLevel) {
		this.validationLevel = validationLevel;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public List<ValidationResult> getResults() {
		return results;
	}

	public void setResults(List<ValidationResult> results) {
		this.results = results;
	}
}
