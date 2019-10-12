package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

import java.util.List;

public abstract class SmallDoclet extends Doclet{
    private SmallDocContext smallDocContext;

    public SmallDoclet(SmallDocContext smallDocContext) {
        this.smallDocContext = smallDocContext;
    }

    protected static SmallDoclet doclet;

    /**
     * Generate documentation in here.This method is required for all doclets.
     *
     * @param root
     * @return
     */
    public static boolean start(RootDoc root) {
        return doclet.process(root);
    }

    /**
     * 返回 {@link LanguageVersion#JAVA_1_5} 避免获取结构信息时泛型擦除（即使Compiler Tree API已经处理了泛型）
     *
     * @return
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    /**
     * process {@link RootDoc} root
     * @param root
     * @return
     */
    protected abstract boolean process(RootDoc root);

    protected void setDocLet(SmallDoclet doclet){
        SmallDoclet.doclet = doclet;
    }

    protected JSONArray getClassesJSONArray() {
        return smallDocContext.getClassesJSONArray();
    }

    protected JSONObject getBeanFieldsJSON() {
        return smallDocContext.getBeanFieldsJSON();
    }

    protected List<String> getLibraryTypePackages(){
        return smallDocContext.getSmallDocProperties().getLibraryTypePackages();
    }
    protected List<String> getLibraryTypeQualifiedNames(){
        return smallDocContext.getSmallDocProperties().getLibraryTypeQualifiedNames();
    }
}
