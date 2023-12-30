package pers.chaos.jsondartserializable.windows;

public class UserAdvanceConfiguration {
    // 是否开启试试JSON字段值作为生成文件的属性默认值
    private boolean enableRealtimeJsonDefaultValueAnalysis;
    // 是否开启所有对象生成在一个dart文件中
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
