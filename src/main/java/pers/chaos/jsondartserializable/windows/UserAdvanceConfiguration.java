package pers.chaos.jsondartserializable.windows;

public class UserAdvanceConfiguration {
    private boolean enableRealtimeJsonDefaultValueAnalysis;
    private boolean enableAllClassGeneratedIntoSingleFile;

    public boolean isEnableRealtimeJsonDefaultValueAnalysis() {
        return enableRealtimeJsonDefaultValueAnalysis;
    }

    public void setEnableRealtimeJsonDefaultValueAnalysis(boolean enableRealtimeJsonDefaultValueAnalysis) {
        this.enableRealtimeJsonDefaultValueAnalysis = enableRealtimeJsonDefaultValueAnalysis;
    }

    public boolean isEnableAllClassGeneratedIntoSingleFile() {
        return enableAllClassGeneratedIntoSingleFile;
    }

    public void setEnableAllClassGeneratedIntoSingleFile(boolean enableAllClassGeneratedIntoSingleFile) {
        this.enableAllClassGeneratedIntoSingleFile = enableAllClassGeneratedIntoSingleFile;
    }
}
