package org.github.fourth.mocksystem.agent.entity;


import java.util.List;

public class ClassModifyInfo {
    private String fullClassName;
    private List<MethodModifyInfo> methodNameList;

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    public List<MethodModifyInfo> getMethodNameList() {
        return methodNameList;
    }

    public void setMethodNameList(List<MethodModifyInfo> methodNameList) {
        this.methodNameList = methodNameList;
    }
}
