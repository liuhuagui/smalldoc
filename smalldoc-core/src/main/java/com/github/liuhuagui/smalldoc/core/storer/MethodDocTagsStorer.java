package com.github.liuhuagui.smalldoc.core.storer;

import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;

import java.util.HashMap;
import java.util.Map;

/**
 * MethodDoc标签存储器
 *
 * @author KaiKang 799600902@qq.com
 */
public class MethodDocTagsStorer {
    private Map<String,String> paramTagsMaps = new HashMap<String, String>();

    public MethodDocTagsStorer(MethodDoc doc) {
        initTagsMaps(doc);
    }

    private void initTagsMaps(MethodDoc doc){
        ParamTag[] paramTags = doc.paramTags();
        for (ParamTag paramTag : paramTags){
            paramTagsMaps.put(paramTag.parameterName(),paramTag.parameterComment());
        }
    }

    /**
     * 查询某个参数的注释
     * @param paramName
     * @return
     */
    public String getComment(String paramName){
        return  paramTagsMaps.get(paramName);
    }

}
