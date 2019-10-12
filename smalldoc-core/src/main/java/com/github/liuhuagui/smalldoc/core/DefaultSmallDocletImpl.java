package com.github.liuhuagui.smalldoc.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.liuhuagui.smalldoc.core.constant.Constants;
import com.github.liuhuagui.smalldoc.core.storer.MappingDescStorer;
import com.github.liuhuagui.smalldoc.core.storer.MethodDocTagsStorer;
import com.github.liuhuagui.smalldoc.util.Utils;
import com.sun.javadoc.*;
import com.sun.tools.javac.code.TypeTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;


/**
 * 自定义Doclet。<br>
 * <b>注意：基于JDK1.8的Javadoc API，该API在JDK1.9被遗弃并由新的API支持，在不久的将来将被移除</b>
 *
 * @author KaiKang
 */
public class DefaultSmallDocletImpl extends SmallDoclet {
    private static final Logger log = LoggerFactory.getLogger(DefaultSmallDocletImpl.class);

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
        returnJSON.put("qtype", inferBeanName(rtype));
        returnJSON.put("type", getParamTypeWithDimension(rtype));//获取带维度的返回值
        returnJSON.put("typeArguments", getTypeArguments(rtype));

        //如果包含返回标签，则解析返回标签的注释
        Tag[] returnTags = methodDoc.tags("@return");
        if (Utils.isNotEmpty((returnTags)))
            returnJSON.put("comment", returnTags[0].text());

        //如果不是库类型，保留字段
        if (!Utils.isLibraryType(rtype, getLibraryTypePackages(), getLibraryTypeQualifiedNames())) {
            addBean(rtype);
        }
        return returnJSON;
    }

    /**
     * 查询方法的所有参数信息
     *
     * @param methodDoc
     * @return
     */
    private JSONArray getParamDocsInfo(MethodDoc methodDoc) {
        //处理参数
        JSONArray paramsJSONArray = new JSONArray();
        //用于存储方法的标签信息，方便快速查找
        MethodDocTagsStorer methodDocTagsStorer = new MethodDocTagsStorer(methodDoc);
        for (Parameter parameterDoc : methodDoc.parameters()) {
            handleParamDoc(parameterDoc, methodDocTagsStorer, paramsJSONArray);
        }
        return paramsJSONArray;
    }

    /**
     * 处理参数，如果你的参数在方法注释中不存在对应的@param，那么你的参数将被忽略，这有时可能也成为你所期望的。
     *
     * @param parameterDoc
     * @param methodDocTagsStorer
     * @param paramsJSONArray
     */
    private void handleParamDoc(Parameter parameterDoc, MethodDocTagsStorer methodDocTagsStorer, JSONArray paramsJSONArray) {
        String paramName = parameterDoc.name();
        String comment = methodDocTagsStorer.getComment(paramName);
        //如果你的参数在方法注释中不存在对应的@param，被忽略
        if (comment == null)
            return;

        JSONObject paramJSON = new JSONObject();
        paramsJSONArray.add(paramJSON);
        paramJSON.put("name", paramName);
        paramJSON.put("comment", comment);

        Type ptype = parameterDoc.type();
        paramJSON.put("qtype", inferBeanName(ptype));
        paramJSON.put("type", getParamTypeWithDimension(ptype));//获取带维度的参数
        paramJSON.put("typeArguments", getTypeArguments(ptype));

        //如果不是库类型，保留字段
        if (!Utils.isLibraryType(ptype, getLibraryTypePackages(), getLibraryTypeQualifiedNames())) {
            addBean(ptype);
        }
    }

    /**
     * 获取带维度的参数
     *
     * @param ptype
     * @return
     */
    private String getParamTypeWithDimension(Type ptype) {
        return ptype.typeName() + ptype.dimension();
    }

    /**
     * 增加到beans
     *
     * @param type
     */
    private void addBean(Type type) {
        JSONObject beanFieldsJSON = getBeanFieldsJSON();
        String beanName = inferBeanName(type);//推断BeanName
        //1. 单线程工作，不用加锁。
        //2. 判断该bean是否存在，避免循环引用造成的死循环
        if (Objects.nonNull(beanFieldsJSON.get(beanName)))
            return;
        Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap = null;
        if (type.asParameterizedType() != null) {
            typeVariableToTypeArgumentMap = typeVariableToTypeArgumentMap(type.asParameterizedType());
        }

        JSONArray fieldsJSONArray = new JSONArray();
        beanFieldsJSON.put(beanName, fieldsJSONArray);
        for (FieldDoc fieldDoc : getFieldDocs(type)) {
            Type ftype = fieldDoc.type();
            JSONObject fieldJSON = new JSONObject();
            //推断类型变量TypeVariable的实际类型
            inferTypeVariableActualType(typeVariableToTypeArgumentMap, ftype, fieldJSON);
            fieldJSON.put("typeArguments", getTypeArgumentsOnFields(ftype, typeVariableToTypeArgumentMap));
            fieldJSON.put("comment", fieldDoc.commentText());
            fieldJSON.put("name", fieldDoc.name());
            fieldsJSONArray.add(fieldJSON);

            //如果不是库类型，保留字段
            if (!Utils.isLibraryType(ftype, getLibraryTypePackages(), getLibraryTypeQualifiedNames())) {
                addBean(ftype);
            }
        }
    }

    /**
     * 获取字段集合。如果类实现了{@link java.io.Serializable} 或 {@link java.io.Externalizable}那么返回序列化字段集合，
     * 否则，获取所有字段集合，不管Access Modifier
     *
     * @param type
     * @return
     */
    private FieldDoc[] getFieldDocs(Type type) {
        if (Objects.isNull(type) || Utils.isLibraryType(type, getLibraryTypePackages(), getLibraryTypeQualifiedNames()))
            return new FieldDoc[0];
        ClassDoc classDoc = type.asClassDoc();
        FieldDoc[] fields = classDoc.serializableFields();
        if (Utils.isEmpty(fields))
            fields = classDoc.fields(false);//不管Access Modifier
        return Utils.addAll(fields, getFieldDocs(classDoc.superclassType()));
    }

    /**
     * 对于ParameterizedType（泛型调用），typeVariable的实际类型由传入的typeArgument决定，所以不同的typeArgments
     * 会产生不同的beanFields信息，为保证正确的映射关系，ParameterizedType的BeanName需要保留泛型信息，同时要注意去除
     * 数组维度。
     *
     * @param type
     * @return
     */
    private String inferBeanName(Type type) {
        //默认使用qualifiedTypeName做key
        String key = type.qualifiedTypeName();
        //typeArguments的传入会造成typeVariable字段的实际类型不同，
        //为了每次都能够解析到具体字段类型，使用toString()作为key（携带泛型信息）。
        ParameterizedType parameterizedType = type.asParameterizedType();//取出参数化类型做后续操作，防止数组维度造成的混乱。
        if (parameterizedType != null)
            key = parameterizedType.toString();
        return key;
    }

    /**
     * 不包含维度，但是包含完全限定和泛型参数信息
     *
     * @param type1
     * @return
     */
    private String getQualifierName(com.sun.tools.javac.code.Type type1) {
        return type1.hasTag(TypeTag.ARRAY) ? getQualifierName(((com.sun.tools.javac.code.Type.ArrayType) type1).elemtype) : type1.toString();
    }

    /**
     * 不包含完全限定，泛型，但是包含维度。
     *
     * @param type1
     * @param demision
     * @return
     */
    private String getName(com.sun.tools.javac.code.Type type1, int demision) {
        return type1.hasTag(TypeTag.ARRAY) ? getName(((com.sun.tools.javac.code.Type.ArrayType) type1).elemtype, ++demision) : type1.tsym.name.toString() + Utils.dimension(demision);
    }

    /**
     * 获得typeVariables与typeArguments的映射
     *
     * @param pType 参数化类型
     * @return
     */
    private Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap(ParameterizedType pType) {
        Field f = null;
        try {
            f = pType.getClass().getSuperclass().getDeclaredField("type");
        } catch (NoSuchFieldException e) {
            log.error("type fields not exist.", e);
        }
        com.sun.tools.javac.code.Type.ClassType o = null;
        try {
            f.setAccessible(true);
            o = (com.sun.tools.javac.code.Type.ClassType) f.get(pType);
        } catch (IllegalAccessException e) {
            log.error("fields can't be accessed", e);
        }

        List<com.sun.tools.javac.code.Type> typeArguments = o.typarams_field;
        List<com.sun.tools.javac.code.Type> typeVariables = ((com.sun.tools.javac.code.Type.ClassType) o.tsym.type).typarams_field;

        if (typeArguments.size() == 0) {
            log.warn("Failed to infer type, it is recommended to explicitly give the type parameter: {}.", pType.toString());
            return null;
        }
        if (typeArguments.size() != typeVariables.size())
            throw new IllegalArgumentException("This may be a bug，welcome to issue.");

        HashMap<String, com.sun.tools.javac.code.Type> map = new HashMap<>();
        for (int i = 0; i < typeVariables.size(); i++) {
            map.put(typeVariables.get(i).toString(), typeArguments.get(i));
        }
        return map;
    }


    /**
     * 获取泛型参数，并添加泛型信息到Beans。<br/>
     * 使用JSONArray存储返回值。如果使用JSONObject，泛型变量的个数将无法计算。
     *
     * @param ptype
     * @return
     */
    private JSONArray getTypeArguments(Type ptype) {
        JSONArray typeArgumentsJSONArray = new JSONArray();
        ParameterizedType parameterizedType;
        if (Objects.nonNull(parameterizedType = ptype.asParameterizedType())) {
            for (Type typeArgument : parameterizedType.typeArguments()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", getParamTypeWithDimension(typeArgument));
                jsonObject.put("typeArguments", getTypeArguments(typeArgument));
                jsonObject.put("qtype", inferBeanName(typeArgument));
                typeArgumentsJSONArray.add(jsonObject);

                //如果不是库类型，保留字段
                if (!Utils.isLibraryType(typeArgument, getLibraryTypePackages(), getLibraryTypeQualifiedNames())) {
                    addBean(typeArgument);
                }
            }
        }
        return typeArgumentsJSONArray;
    }

    /**
     * 获取字段Fields的泛型参数，并添加泛型信息到Beans。<br/>
     * <b>由于字段泛型参数可能是类型变量TypeVariable，所以需要单独处理。（对于方法或返回值中存在的类型变量TypeVariable，Controller接口应该去避免，所以不做解析支持。）</b>
     * 使用JSONArray存储返回值。如果使用JSONObject，泛型变量的个数将无法计算。
     *
     * @param ptype                         字段的类型
     * @param typeVariableToTypeArgumentMap 字段所属对象的类型参数TypeArgument与类型变量typeVariable的映射关系
     * @return
     */
    private JSONArray getTypeArgumentsOnFields(Type ptype, Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap) {
        JSONArray typeArgumentsJSONArray = new JSONArray();
        ParameterizedType parameterizedType;
        if (Objects.nonNull(parameterizedType = ptype.asParameterizedType())) {
            for (Type typeArgument : parameterizedType.typeArguments()) {
                JSONObject jsonObject = new JSONObject();
                //推断类型变量TypeVariable的实际类型
                inferTypeVariableActualType(typeVariableToTypeArgumentMap, typeArgument, jsonObject);
                jsonObject.put("typeArguments", getTypeArgumentsOnFields(typeArgument, typeVariableToTypeArgumentMap));
                typeArgumentsJSONArray.add(jsonObject);

                //如果不是库类型，保留字段
                if (!Utils.isLibraryType(typeArgument, getLibraryTypePackages(), getLibraryTypeQualifiedNames())) {
                    addBean(typeArgument);
                }
            }
        }
        return typeArgumentsJSONArray;
    }

    /**
     * 推断类型变量TypeVariable的实际类型
     *
     * @param typeVariableToTypeArgumentMap 字段所属对象的类型参数TypeArgument与类型变量typeVariable的映射关系
     * @param fieldOrFieldArgumentType      字段或字段参数类型
     * @param jsonObject                    数据存储对象
     */
    private void inferTypeVariableActualType(Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap, Type fieldOrFieldArgumentType, JSONObject jsonObject) {
        //字段是typeVariable并且typeArguments被显示声明，可推断。
        TypeVariable typeVariable = fieldOrFieldArgumentType.asTypeVariable();//取出类型变量做后续操作，防止数组维度造成的混乱。
        if (typeVariable != null && typeVariableToTypeArgumentMap != null) {
            com.sun.tools.javac.code.Type type1 = typeVariableToTypeArgumentMap.get(typeVariable.qualifiedTypeName());
            jsonObject.put("qtype", getQualifierName(type1));
            jsonObject.put("type", getName(type1, 0));
        } else {
            jsonObject.put("qtype", inferBeanName(fieldOrFieldArgumentType));
            jsonObject.put("type", getParamTypeWithDimension(fieldOrFieldArgumentType));
        }
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
