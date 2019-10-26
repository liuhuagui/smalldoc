package com.github.liuhuagui.smalldoc.core.storer;

import com.github.liuhuagui.smalldoc.util.Assert;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;

import java.util.HashMap;
import java.util.Map;

/**
 * 方法参数存储器
 *
 * @author KaiKang 799600902@qq.com
 */
public class MethodParamsStorer {
    private Map<String, Parameter> paramsMap = new HashMap<>();
    private MethodDoc methodDoc;

    public MethodParamsStorer(MethodDoc methodDoc) {
        this.methodDoc = methodDoc;
        initParamsMap(methodDoc);
    }

    private void initParamsMap(MethodDoc methodDoc) {
        for (Parameter parameterDoc : methodDoc.parameters()) {
            paramsMap.put(parameterDoc.name(), parameterDoc);
        }
    }

    /**
     * @param paramName
     * @return
     */
    public Parameter getParam(String paramName) {
        Parameter parameter = paramsMap.get(paramName);
        Assert.notNull(parameter, "Method: %s, This param %s doesn't exist.", methodDoc.qualifiedName() + methodDoc.flatSignature(), paramName);
        return parameter;
    }

}
