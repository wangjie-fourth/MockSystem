package org.github.fourth.mocksystem.agent.entity;

public class MethodModifyInfo {
    private String methodName;

    private String runBeforeCode;
    private String mockResponseDataStr;

    private String runAfterCode;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getRunBeforeCode() {
        return runBeforeCode;
    }

    public void setRunBeforeCode(String runBeforeCode) {
        this.runBeforeCode = runBeforeCode;
    }

    public String getMockResponseDataStr() {
        return mockResponseDataStr;
    }

    public void setMockResponseDataStr(String mockResponseDataStr) {
        this.mockResponseDataStr = mockResponseDataStr;
    }

    public String getRunAfterCode() {
        return runAfterCode;
    }

    public void setRunAfterCode(String runAfterCode) {
        this.runAfterCode = runAfterCode;
    }
}
