package net.validex.gitb.model;

/**
 * Created by serbay.
 */
public class ReportResponse {
	private Report report;
    private String link;

	public Report getReport() {
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
