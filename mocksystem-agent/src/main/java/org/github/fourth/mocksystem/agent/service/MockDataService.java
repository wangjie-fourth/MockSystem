package org.github.fourth.mocksystem.agent.service;

import org.github.fourth.mocksystem.agent.entity.ClassModifyInfo;
import org.github.fourth.mocksystem.agent.entity.MethodModifyInfo;
import org.github.fourth.mocksystem.testapplication.testcase.AddBeforeAfterCodeCase;
import org.github.fourth.mocksystem.testapplication.testcase.MockMethodResponse;

import java.util.ArrayList;
import java.util.List;

public class MockDataService {
    public static List<ClassModifyInfo> needReTransformClassNameList = new ArrayList<>();

    static {
        ClassModifyInfo classModifyInfo = new ClassModifyInfo();
        classModifyInfo.setFullClassName(AddBeforeAfterCodeCase.class.getCanonicalName());
        classModifyInfo.setMethodNameList(new ArrayList<>());
        MethodModifyInfo methodModifyInfo = new MethodModifyInfo();
        methodModifyInfo.setMethodName(AddBeforeAfterCodeCase.class.getMethods()[0].getName());
        methodModifyInfo.setRunBeforeCode(AddBeforeAfterCodeCase.BEFORE_CODE);
        methodModifyInfo.setRunAfterCode(AddBeforeAfterCodeCase.AFTER_CODE);
        classModifyInfo.getMethodNameList().add(methodModifyInfo);
        needReTransformClassNameList.add(classModifyInfo);
    }

    static {
        ClassModifyInfo classModifyInfo = new ClassModifyInfo();
        classModifyInfo.setFullClassName(MockMethodResponse.class.getCanonicalName());
        classModifyInfo.setMethodNameList(new ArrayList<>());
        MethodModifyInfo methodModifyInfo = new MethodModifyInfo();
        methodModifyInfo.setMethodName(MockMethodResponse.class.getMethods()[0].getName());
        methodModifyInfo.setRunBeforeCode(null);
        methodModifyInfo.setRunAfterCode(null);
        methodModifyInfo.setMockResponseDataStr(MockMethodResponse.MOCK_RESPONSE_DATA_STR);
        classModifyInfo.getMethodNameList().add(methodModifyInfo);
        needReTransformClassNameList.add(classModifyInfo);
    }


}
