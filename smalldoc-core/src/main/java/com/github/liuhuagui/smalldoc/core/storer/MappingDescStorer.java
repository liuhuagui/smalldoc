package com.github.liuhuagui.smalldoc.core.storer;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping存储器，方便取回关于Mapping的有效信息。
 *
 * @author KaiKang 799600902@qq.com
 */
public class MappingDescStorer {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private String name;
    private Map<String, String[]> valuesMap = new HashMap<>();

    public MappingDescStorer(AnnotationDesc annotationDesc) {
        this.name = annotationDesc.annotationType().name();
        initValues(annotationDesc);
    }

    private void initValues(AnnotationDesc annotationDesc) {
        for (AnnotationDesc.ElementValuePair elementValuePair :
                annotationDesc.elementValues()) {
            valuesMap.put(elementValuePair.element().name(), analyseValue(elementValuePair));
        }
    }


    /**
     * 解析value值，去除首尾空白字符
     *
     * @param elementValuePair
     * @return
     */
    private String[] analyseValue(AnnotationDesc.ElementValuePair elementValuePair) {
        String name = elementValuePair.element().name();
        Object value = elementValuePair.value().value();
        if (name.equals("name"))
            return new String[]{value.toString().trim()};//去除首尾空白字符
        AnnotationValue[] values = (AnnotationValue[]) value;
        String[] strings = new String[values.length];
        int i = 0;
        for (AnnotationValue v : values) {
            String s = v.value().toString().trim();//去除首尾空白字符
            if (name.equals("method")) {
                strings[i++] = s.substring(s.lastIndexOf(".") + 1, s.length());
            } else {
                strings[i++] = s;
            }
        }
        return strings;
    }

    public String name() {
        return name;
    }

    public String[] getElementValue(String name) {
        String[] value = valuesMap.get(name);
        if (value == null) {
            //value为空时，取path
            if (name.equals("value"))
                value = valuesMap.get("path");
            //path为空时，取value
            if (name.equals("path"))
                value = valuesMap.get("value");
        }
        return value == null ? EMPTY_STRING_ARRAY : value;//为null时返回空字符串数组
    }
}
