package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.liuhuagui.smalldoc.core.storer.FieldDocStorer;
import com.github.liuhuagui.smalldoc.properties.SmallDocProperties;
import com.github.liuhuagui.smalldoc.util.Utils;
import com.sun.tools.javadoc.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程不安全，但是文档结构，只在工程启动时生成一次。<br>
 * 参考链接：<a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDIBDDD">https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#CHDIBDDD</a><br>
 * <b>注意：基于JDK1.8的Javadoc API，该API在JDK1.9被遗弃并由新的API支持，以后将被移除</b>
 *
 * @author KaiKang 799600902@qq.com
 */
public class SmallDocContext {
    private static final Logger log = LoggerFactory.getLogger(SmallDocContext.class);
    /**
     * 默认的sourcepath
     */
    private static final String DEFAULT_SOURCE_PATH = System.getProperty("user.dir") + "\\src\\main\\java";

    /**
     * 文档结构，包含工程以外的信息
     */
    private final JSONObject docsJSON;

    /**
     * 所有类的文档结构
     */
    private final JSONArray classesJSONArray;

    /**
     * 装载bean的字段信息
     */
    private final Map<String,List<FieldDocStorer>> beanFieldsMap;

    /**
     * 存储实体类型，字段名与字段信息的映射关系
     */
    private final Map<String,Map<String,FieldDocStorer>> entityAndFieldMap;

    /**
     * 源文件路径
     */
    private List<String> paths;

    /**
     * 扫描的包及它的子包
     */
    private List<String> packages;

    private SmallDocProperties smallDocProperties;

    public SmallDocContext(SmallDocProperties smallDocProperties) {
        this.docsJSON = new JSONObject();
        this.classesJSONArray = new JSONArray();
        this.beanFieldsMap = new HashMap<>();
        this.entityAndFieldMap = new HashMap<>();

        this.smallDocProperties = smallDocProperties;
        this.paths = smallDocProperties.getSourcePaths();
        this.packages = smallDocProperties.getPackages();
        //默认已添加当前项目源码路径——user.dir
        this.paths.add(DEFAULT_SOURCE_PATH);
        //如果没有指定扫描的包，将扫描源码路径下所有包，建议给出指定包名，提升解析速度
        if (packages.isEmpty())
            this.packages.add("/");

        this.docsJSON.put("projectName", smallDocProperties.getProjectName());
        this.docsJSON.put("jdkVersion", System.getProperty("java.version"));
        this.docsJSON.put("osName", System.getProperty("os.name"));
        this.docsJSON.put("encoding", System.getProperty("file.encoding"));
        this.docsJSON.put("support", "https://github.com/liuhuagui/smalldoc");
        this.docsJSON.put("classes", this.classesJSONArray);
        this.docsJSON.put("beans", this.beanFieldsMap);
    }

    /**
     * 执行文档解析
     */
    public void execute(SmallDoclet doclet) {
        String sourcepath = Utils.join(paths, ";");
        docsJSON.put("sourcepath", sourcepath);
        log.info("-sourcepath is {}", sourcepath);

        String subpackages = Utils.join(packages, ":");
        docsJSON.put("subpackages", subpackages);
        log.info("-subpackages is {}", subpackages);

        //执行javadoc 命令
        Main.execute(new String[]{
                "-doclet", doclet.getClass().getName(),
                "-encoding", System.getProperty("file.encoding"),
                "-quiet",
                "-sourcepath", sourcepath,
                "-subpackages", subpackages}
        );
    }

    public SmallDocProperties getSmallDocProperties() {
        return smallDocProperties;
    }

    public JSONObject getDocsJSON() {
        return docsJSON;
    }

    public JSONArray getClassesJSONArray() {
        return classesJSONArray;
    }

    public Map<String, List<FieldDocStorer>> getBeanFieldsMap() {
        return beanFieldsMap;
    }

    public Map<String, Map<String, FieldDocStorer>> getEntityAndFieldMap() {
        return entityAndFieldMap;
    }

    public List<String> getPaths() {
        return paths;
    }

    public List<String> getPackages() {
        return packages;
    }
}