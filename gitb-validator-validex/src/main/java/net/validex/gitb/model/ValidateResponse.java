package net.validex.gitb.model;

/**
 * Created by serbay.
 */
public class ValidateResponse {
	private String id;
	private boolean successful;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}
}
