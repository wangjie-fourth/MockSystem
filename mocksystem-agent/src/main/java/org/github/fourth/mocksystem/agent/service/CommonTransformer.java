package org.github.fourth.mocksystem.agent.service;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.apache.commons.lang3.StringUtils;
import org.github.fourth.mocksystem.agent.entity.ClassModifyInfo;
import org.github.fourth.mocksystem.agent.entity.MethodModifyInfo;
import org.github.fourth.mocksystem.agent.util.LOGGER;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static org.github.fourth.mocksystem.agent.service.MockDataService.NeedReTransformClassNameList;

public class CommonTransformer implements ClassFileTransformer {

    /** The class loader of the class we want to transform */
    private final ClassLoader targetClassLoader;

    public CommonTransformer(ClassLoader targetClassLoader) {
        this.targetClassLoader = targetClassLoader;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        // 判断这个class需要不需要转换
        ClassModifyInfo classModifyInfo = NeedReTransformClassNameList.stream().filter(x -> className.equals(x.getFullClassName().replaceAll("\\.", "/"))).findFirst().orElse(null);
        if (classModifyInfo == null) {
            return byteCode;
        }
        // 对应的classLoader一致
        if (!loader.equals(targetClassLoader)) {
            return byteCode;
        }

        // 依次转换方法
        for (MethodModifyInfo methodModifyInfo : classModifyInfo.getMethodNameList()) {
            try {
                LOGGER.info(String.format("[Agent] Transforming class :%s", classModifyInfo.getFullClassName()));
                // todo: 如何把这个插入变量也能变成配置化？？？
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(classModifyInfo.getFullClassName());
                CtMethod m = cc.getDeclaredMethod(methodModifyInfo.getMethodName());
                m.addLocalVariable("startTime", CtClass.longType);
                if (StringUtils.isNotEmpty(methodModifyInfo.getRunBeforeCode())) {
                    m.insertBefore("startTime = System.currentTimeMillis();");
                }

                m.addLocalVariable("endTime", CtClass.longType);
                m.addLocalVariable("opTime", CtClass.longType);
                if (StringUtils.isNotEmpty(methodModifyInfo.getRunAfterCode())) {
                    m.insertAfter(methodModifyInfo.getRunAfterCode());
                }
                byteCode = cc.toBytecode();
                cc.detach();
            } catch (Throwable e) {
                LOGGER.error("Exception" + e);
            }
        }
        return byteCode;
    }
}
