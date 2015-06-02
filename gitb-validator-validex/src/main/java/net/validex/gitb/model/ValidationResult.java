package net.validex.gitb.model;

import java.util.List;

/**
 * Created by serbay.
 */
public class ValidationResult {
	private String type;
	private boolean success;
	private List<ValidationError> errors;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public List<ValidationError> getErrors() {
		return errors;
	}

	public void setErrors(List<ValidationError> errors) {
		this.errors = errors;
	}
}
