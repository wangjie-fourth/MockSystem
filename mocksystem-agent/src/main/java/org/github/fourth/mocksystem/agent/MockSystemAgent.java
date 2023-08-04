package org.github.fourth.mocksystem.agent;

import org.github.fourth.mocksystem.agent.entity.ClassModifyInfo;
import org.github.fourth.mocksystem.agent.service.CommonTransformer;
import org.github.fourth.mocksystem.agent.service.MockDataService;
import org.github.fourth.mocksystem.agent.util.LOGGER;

import java.lang.instrument.Instrumentation;

/**
 * @author jwang55
 */
public class MockSystemAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] In premain method");
        transformClass(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] In agentmain method");
        transformClass(inst);
    }

    private static void transformClass(Instrumentation instrumentation) {
        Class<?> targetCls;
        ClassLoader targetClassLoader;
        for (ClassModifyInfo classModifyInfo : MockDataService.needReTransformClassNameList) {
            String className = classModifyInfo.getFullClassName();
            // see if we can get the class using forName
            try {
                targetCls = Class.forName(className);
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                continue;
            } catch (Exception ex) {
                LOGGER.error("Class [{}] not found with Class.forName");
            }
            // otherwise iterate all loaded classes and find what we want
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                if (clazz.getName().equals(className)) {
                    targetCls = clazz;
                    targetClassLoader = targetCls.getClassLoader();
                    transform(targetCls, targetClassLoader, instrumentation);
                    break;
                }
            }
            throw new RuntimeException("Failed to find class [" + className + "]");
        }
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
        CommonTransformer ct = new CommonTransformer(classLoader);
        instrumentation.addTransformer(ct, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }

}
