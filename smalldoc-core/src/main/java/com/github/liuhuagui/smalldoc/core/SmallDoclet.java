package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.liuhuagui.smalldoc.core.storer.FieldDocStorer;
import com.github.liuhuagui.smalldoc.util.Assert;
import com.sun.javadoc.*;
import com.sun.tools.javac.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.liuhuagui.smalldoc.core.constant.Constants.CLASSES;

public abstract class SmallDoclet extends Doclet {
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
     *
     * @param root
     * @return
     */
    protected abstract boolean process(RootDoc root);

    protected void setDocLet(SmallDoclet doclet) {
        SmallDoclet.doclet = doclet;
    }

    protected abstract JSONObject handleClassDoc(ClassDoc classDoc);

    protected void addClassDoc(ClassDoc classDoc) {
        PackageDoc packageDoc = classDoc.containingPackage();
        String packageName = packageDoc.toString();

        JSONObject packagesJSON = smallDocContext.getPackagesJSON();
        JSONObject packageJSON = packagesJSON.getJSONObject(packageName);
        if (packageJSON != null) {
            JSONArray classes = packageJSON.getJSONArray(CLASSES);
            classes.add(handleClassDoc(classDoc));
        } else {
            JSONArray classes = new JSONArray();
            classes.add(handleClassDoc(classDoc));
            packageJSON = new JSONObject();
            packageJSON.put(CLASSES, classes);

            putPackageComment(packageDoc, packageName, packageJSON);
            putPackageUrl(packageDoc, packageJSON);

            packagesJSON.put(packageName, packageJSON);
        }
    }

    private void putPackageUrl(PackageDoc packageDoc, JSONObject packageJSON) {
        Tag[] urls = packageDoc.tags("url");
        if (urls != null && urls.length > 0) {
            String url = urls[0].text();
            packageJSON.put("url", url.endsWith("/") ? url : url + "/");
        }
    }

    private void putPackageComment(PackageDoc packageDoc, String packageName, JSONObject packageJSON) {
        String comment = packageDoc.commentText();
        if (comment != null && comment.trim().length() != 0)
            packageJSON.put("comment", comment);
        else
            packageJSON.put("comment", packageName);
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

    public String nameRegex() {
        return smallDocContext.getSmallDocProperties().getNameRegex();
    }

    public String getCurrentMethodSignature() {
        return smallDocContext.getCurrentMethodSignature();
    }

    /**
     * Set the current handling method in the context.
     *
     * @param currentMethodDoc
     */
    public void setCurrentMethodSignature(MethodDoc currentMethodDoc) {
        smallDocContext.setCurrentMethodSignature(currentMethodDoc.qualifiedName() + currentMethodDoc.flatSignature());
    }
}
