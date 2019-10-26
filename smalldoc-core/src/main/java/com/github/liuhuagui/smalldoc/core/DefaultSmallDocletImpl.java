package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.liuhuagui.smalldoc.core.constant.Constants;
import com.github.liuhuagui.smalldoc.core.storer.MappingDescStorer;
import com.github.liuhuagui.smalldoc.core.storer.MethodParamsStorer;
import com.github.liuhuagui.smalldoc.core.storer.ParamTagStorer;
import com.github.liuhuagui.smalldoc.util.Assert;
import com.github.liuhuagui.smalldoc.util.TypeUtils;
import com.github.liuhuagui.smalldoc.util.Utils;
import com.sun.javadoc.*;

import java.util.ArrayList;


/**
 * 自定义Doclet。<br>
 * <b>注意：基于JDK1.8的Javadoc API，该API在JDK1.9被遗弃并由新的API支持，在不久的将来将被移除</b>
 *
 * @author KaiKang
 */
public class DefaultSmallDocletImpl extends SmallDoclet {

    public DefaultSmallDocletImpl(SmallDocContext smallDocContext) {
        super(smallDocContext);
        setDocLet(this);//将子类实例挂载到父类静态变量上
    }

    /**
     * 从{@link RootDoc}中解析文档信息
     *
     * @param root
     */
    @Override
    protected boolean process(RootDoc root) {
        handleClassDocs(root);
        return true;
    }

    /**
     * 处理所有类
     *
     * @param root
     */
    private void handleClassDocs(RootDoc root) {
        ClassDoc[] classes = root.classes();
        for (ClassDoc classDoc : classes) {
            if (!classDoc.name().endsWith(Constants.CONTROLLER))//只解析*Controller类
                continue;
            handleClassDoc(classDoc);
        }
    }

    /**
     * 处理单个类
     *
     * @param classDoc
     */
    private void handleClassDoc(ClassDoc classDoc) {
        JSONObject classJSON = new JSONObject();
        classJSON.put("name", classDoc.name());
        classJSON.put("comment", classDoc.commentText());
        classJSON.put("authors", getAuthorsInfo(classDoc.tags("@author")));
        //处理Mapping
        JSONObject classMappingInfo = getMappingInfo(classDoc);
        classJSON.put("mapping", classMappingInfo);
        classJSON.put("methods", getMehodDocsInfo(classDoc, classMappingInfo));//由于classJSON除方法信息外有额外信息，所以使用methods统一管理方法信息

        getClassesJSONArray().add(classJSON);

    }

    /**
     * 从@author标签中查询作者信息
     *
     * @param authorTags
     * @return
     */
    private JSONArray getAuthorsInfo(Tag[] authorTags) {
        JSONArray authorsJSONArray = new JSONArray();
        for (Tag tag : authorTags) {
            authorsJSONArray.add(tag.text());
        }
        return authorsJSONArray;
    }

    /**
     * 查询类中的方法信息
     *
     * @param classDoc         类文档
     * @param classMappingInfo 类的Mapping信息
     */
    private JSONArray getMehodDocsInfo(ClassDoc classDoc, JSONObject classMappingInfo) {
        JSONArray methodsJSONArray = new JSONArray();
        for (MethodDoc methodDoc : classDoc.methods()) {
            if (!methodDoc.isPublic())
                continue;
            handleMethodDoc(methodDoc, methodsJSONArray, classMappingInfo);
        }
        return methodsJSONArray;
    }

    /**
     * 处理单个方法
     *
     * @param methodDoc
     * @param methodsJSONArray
     * @param classMappingInfo
     */
    private void handleMethodDoc(MethodDoc methodDoc, JSONArray methodsJSONArray, JSONObject classMappingInfo) {
        setCurrentMethodSignature(methodDoc);//在上下文中设置当前解析的方法
        JSONObject methodMappingInfo = getMappingInfo(methodDoc);
        if (methodMappingInfo.isEmpty())//如果没有Mapping注解，则忽略此方法
            return;
        JSONObject methodJSON = new JSONObject();
        methodsJSONArray.add(methodJSON);

        methodJSON.put("name", methodDoc.name());
        methodJSON.put("comment", methodDoc.commentText());
        methodJSON.put("authors", getAuthorsInfo(methodDoc.tags("@author")));
        //处理Mapping
        methodJSON.put("mapping", handleMethodMappings(classMappingInfo, methodMappingInfo));
        //处理参数
        methodJSON.put("params", getParamDocsInfo(methodDoc));//由于methodJSON除参数信息外有额外信息，所以使用params统一管理参数信息
        //处理返回值
        methodJSON.put("returns", getReturnInfo(methodDoc));
    }

    /**
     * 根据Class的Mapping信息处理Method的Mapping
     *
     * @param classMappingInfo
     * @param methodMappingInfo
     * @return
     */
    private JSONObject handleMethodMappings(JSONObject classMappingInfo, JSONObject methodMappingInfo) {
        if (!classMappingInfo.isEmpty()) {
            String[] keys = {"method", "consumes", "produces"};
            String[] values;
            for (String key : keys) {
                if (Utils.isNotEmpty(values = (String[]) classMappingInfo.get(key)))
                    methodMappingInfo.put(key, values);
            }
        }
        methodMappingInfo.put("path", handleMappingPath((String[]) methodMappingInfo.get("path"), (String[]) classMappingInfo.get("path")));
        return methodMappingInfo;
    }

    /**
     * 根据Class的Mapping path得到Method最终的Mapping path
     *
     * @param methodMappingPaths
     * @param classMappingPaths  非空数组
     * @return
     */
    private ArrayList<String> handleMappingPath(String[] methodMappingPaths, String[] classMappingPaths) {
        ArrayList<String> finalPaths = new ArrayList<>();

        boolean methodPathsEmpty = Utils.isEmpty(methodMappingPaths);
        boolean classPathsEmpty = Utils.isEmpty(classMappingPaths);
        //如果类路径为空，直接使用方法路径（去除首部斜线）
        if (classPathsEmpty && !methodPathsEmpty) {
            for (String p1 : methodMappingPaths) {
                finalPaths.add(Utils.removeHeadSlashIfPresent(p1));
            }
        }

        //如果方法路径为空，用类路径作为最终路径（去除首部斜线）
        if (!classPathsEmpty && methodPathsEmpty) {
            for (String p0 : classMappingPaths) {
                finalPaths.add(Utils.removeHeadSlashIfPresent(p0));
            }
        }

        //如果类路径和方法路径都不为空
        if (!classPathsEmpty && !methodPathsEmpty) {
            for (String p0 : classMappingPaths) {
                //拼接类路径与方法路径作为最终路径
                for (String p1 : methodMappingPaths) {
                    finalPaths.add(Utils.unitePath(p0, p1));
                }
            }
        }
        return finalPaths;
    }


    private JSONObject getReturnInfo(MethodDoc methodDoc) {
        JSONObject returnJSON = new JSONObject();
        Type rtype = methodDoc.returnType();
        returnJSON.put("qtype", TypeUtils.inferBeanName(rtype));
        returnJSON.put("type", TypeUtils.getParamTypeWithDimension(rtype));//获取带维度的返回值
        returnJSON.put("typeArguments", TypeUtils.getTypeArguments(rtype, this));

        //如果包含返回标签，则解析返回标签的注释
        Tag[] returnTags = methodDoc.tags("@return");
        if (Utils.isNotEmpty((returnTags)))
            returnJSON.put("comment", returnTags[0].text());

        //如果不是库类型，保留字段
        if (TypeUtils.isEntity(rtype, this)) {
            TypeUtils.addBean(rtype, this);
        }
        return returnJSON;
    }

    /**
     * 查询方法的所有参数信息<br>
     * Note: 如果你的参数在方法注释中不存在对应的@param，那么你的参数将被忽略，这有时可能也成为你所期望的。
     * 如果你的方法注释中存在了某个@param，而方法中不存该参数，将或抛出异常提示。
     *
     * @param methodDoc
     * @return
     */
    private JSONArray getParamDocsInfo(MethodDoc methodDoc) {
        //处理参数
        JSONArray paramsJSONArray = new JSONArray();

        MethodParamsStorer methodParamsStorer = new MethodParamsStorer(methodDoc);
        ParamTag[] paramTags = methodDoc.paramTags();
        for (ParamTag paramTag : paramTags) {
            String paramName = paramTag.parameterName();
            handleParamDoc(methodParamsStorer.getParam(paramName), paramTag, paramsJSONArray);
        }
        return paramsJSONArray;
    }

    /**
     * 处理参数
     *
     * @param parameterDoc
     * @param paramTag
     * @param paramsJSONArray
     */
    private void handleParamDoc(Parameter parameterDoc, ParamTag paramTag, JSONArray paramsJSONArray) {
        Type ptype = parameterDoc.type();
        addParamBean(ptype);
        //注意：如果数组类型ArrayTypeImpl中的元素类型是实体类型，那么该数组类型也是实体类型。
        if (TypeUtils.isEntity(ptype, this)) {
            handleEntityParamDoc(paramTag, paramsJSONArray, ptype);
        } else {
            handleNoEntityParamDoc(paramTag, paramsJSONArray, ptype);
        }
    }

    /**
     * 不管参数是不是实体类型，一定要解析它的泛型参数
     *
     * @param type
     */
    private void addParamBean(Type type) {
        TypeUtils.getTypeArguments(type, this);
        if (TypeUtils.isEntity(type, this))
            TypeUtils.addBean(type, this);
    }

    private void handleNoEntityParamDoc(ParamTag paramTag, JSONArray paramsJSONArray, Type ptype) {
        if (TypeUtils.isCollection(ptype)) {
            Type typeArgument = ptype.asParameterizedType().typeArguments()[0];
            if (TypeUtils.isEntity(typeArgument, this)) {
                ParamTagStorer.extractEntityParamTag(this, paramTag, typeArgument, true)
                        .ifPresent(pStorer ->
                                paramsJSONArray.addAll(pStorer.getParamStorers())
                        );
                return;
            }
            //如果类型参数不是实体，continue直接处理该类型
        }
        //如果该类型不是集合，直接处理该类型
        ParamTagStorer.extractGeneralParamTag(this, paramTag, ptype)
                .ifPresent(paramsJSONArray::add);
    }


    private void handleEntityParamDoc(ParamTag paramTag, JSONArray paramsJSONArray, Type ptype) {
        boolean array = Utils.isNotBlank(ptype.dimension());
        //注意：如果是数组类型ArrayTypeImpl，解析字段时默认跳过数组维度，默认得到的即是元素类型字段的映射。
        ParamTagStorer.extractEntityParamTag(this, paramTag, ptype, array)
                .ifPresent(pStorer ->
                        paramsJSONArray.addAll(pStorer.getParamStorers())
                );
    }

    /**
     * 查询{@link ProgramElementDoc}的*Mapping注解信息
     *
     * @param elementDoc
     * @return
     */
    private JSONObject getMappingInfo(ProgramElementDoc elementDoc) {
        JSONObject mapping = new JSONObject();
        for (AnnotationDesc annotationDesc : elementDoc.annotations()) {
            MappingDescStorer mappingDescStorer = new MappingDescStorer(annotationDesc);
            String name = mappingDescStorer.name();
            if (name.endsWith("Mapping"))
                handleMappings(mapping, mappingDescStorer);
        }
        return mapping;
    }

    /**
     * 处理 @RequestMapping 信息
     *
     * @param mapping
     * @param mappingDescStorer
     */
    private void handleMappings(JSONObject mapping, MappingDescStorer mappingDescStorer) {
        mapping.put("method", getHttpMethod(mappingDescStorer));
        mapping.put("consumes", mappingDescStorer.getElementValue("consumes"));
        mapping.put("produces", mappingDescStorer.getElementValue("produces"));
        mapping.put("path", mappingDescStorer.getElementValue("value"));
    }

    /**
     * 获取Http Method
     *
     * @param mappingDescStorer
     * @return
     */
    private String[] getHttpMethod(MappingDescStorer mappingDescStorer) {
        String name = mappingDescStorer.name();
        if (Constants.REQUEST_MAPPING.equals(name)) {
            return mappingDescStorer.getElementValue("method");
        } else {
            return new String[]{name.substring(0, name.indexOf('M')).toUpperCase()};
        }
    }

}
