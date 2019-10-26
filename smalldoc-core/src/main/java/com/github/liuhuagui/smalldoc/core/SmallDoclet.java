package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.github.liuhuagui.smalldoc.core.storer.FieldDocStorer;
import com.github.liuhuagui.smalldoc.util.Assert;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

import java.util.List;
import java.util.Map;

public abstract class SmallDoclet extends Doclet {
    private SmallDocContext smallDocContext;

    /**
     * 当前正在解析的方法的签名
     */
    private String currentMethodSignature;

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
     *
     * @param root
     * @return
     */
    protected abstract boolean process(RootDoc root);

    protected void setDocLet(SmallDoclet doclet) {
        SmallDoclet.doclet = doclet;
    }

    protected JSONArray getClassesJSONArray() {
        return smallDocContext.getClassesJSONArray();
    }

    public Map<String, List<FieldDocStorer>> getBeanFieldsMap() {
        return smallDocContext.getBeanFieldsMap();
    }

    public Map<String, Map<String, FieldDocStorer>> getEntityAndFieldMap() {
        return smallDocContext.getEntityAndFieldMap();
    }

    public Map<String, FieldDocStorer> getNameAndFieldMap(String beanName) {
        Map<String, FieldDocStorer> nameAndFieldMap = smallDocContext.getEntityAndFieldMap().get(beanName);
        Assert.notNull(nameAndFieldMap, "The fields information of %s does not exist. Check if the source configuration is correct.", beanName);
        return nameAndFieldMap;
    }

    public List<String> getLibraryTypePackages() {
        return smallDocContext.getSmallDocProperties().getLibraryTypePackages();
    }

    public List<String> getLibraryTypeQualifiedNames() {
        return smallDocContext.getSmallDocProperties().getLibraryTypeQualifiedNames();
    }

    public String getCurrentMethodSignature() {
        return currentMethodSignature;
    }

    public void setCurrentMethodSignature(MethodDoc currentMethodDoc) {
        this.currentMethodSignature = currentMethodDoc.qualifiedName() + currentMethodDoc.flatSignature();
    }
}
