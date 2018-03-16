package managers;

import com.gitb.tr.TestStepReportType;

public class TitledTestStepReportType {

    private String title;
    private TestStepReportType wrapped;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TestStepReportType getWrapped() {
        return wrapped;
    }

    public void setWrapped(TestStepReportType wrapped) {
        this.wrapped = wrapped;
    }
}
