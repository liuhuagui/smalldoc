package com.github.liuhuagui.smalldoc.util;

import com.github.liuhuagui.smalldoc.core.DefaultSmallDocletImpl;
import com.github.liuhuagui.smalldoc.core.storer.FieldDocStorer;
import com.sun.javadoc.*;
import com.sun.tools.javac.code.TypeTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 类型处理工具
 *
 * @author KaiKang 799600902@qq.com
 */
public class TypeUtils {
    private static Logger log = LoggerFactory.getLogger(TypeUtils.class);

    /**
     * 判断类型是否为库类型。默认基本类型，java包，javax包，spring包为库类型。
     *
     * @param ptype
     * @return
     */
    public static boolean isLibraryType(Type ptype) {
        String qualifiedTypeName;
        return ptype.isPrimitive() ||
                (qualifiedTypeName = ptype.qualifiedTypeName()).startsWith("java.") ||
                qualifiedTypeName.startsWith("javax.") ||
                qualifiedTypeName.startsWith("org.springframework.");
    }

    /**
     * 判断类型是否为库类型。默认基本类型，java包，javax包，spring包和配置类型。
     *
     * @param ptype
     * @return
     */
    public static boolean isLibraryType(Type ptype, DefaultSmallDocletImpl doclet) {
        String qualifiedTypeName = ptype.qualifiedTypeName();
        List<String> libraryTypePackages = doclet.getLibraryTypePackages();
        if (Utils.isNotEmpty(libraryTypePackages)) {
            for (String libraryTypePackage : libraryTypePackages) {
                if (qualifiedTypeName.startsWith(libraryTypePackage))
                    return true;
            }
        }
        List<String> libraryTypeQualifiedNames = doclet.getLibraryTypeQualifiedNames();
        if (Utils.isNotEmpty(libraryTypeQualifiedNames)) {
            for (String libraryTypeQualifiedName : libraryTypeQualifiedNames) {
                if (qualifiedTypeName.equals(libraryTypeQualifiedName))
                    return true;
            }
        }
        return isLibraryType(ptype);
    }

    /**
     * 非库类型即实体类型
     *
     * @param type
     * @return
     * @see #isLibraryType(Type, DefaultSmallDocletImpl)
     */
    public static boolean isEntity(Type type, DefaultSmallDocletImpl doclet) {
        return !isLibraryType(type, doclet);
    }

    public static boolean isCollection(Type ptype) {
        String typeName = ptype.typeName();
        return typeName.equals("Set") || typeName.equals("List");
    }

    /**
     * 获取带维度的参数
     *
     * @param ptype
     * @return
     */
    public static String getParamTypeWithDimension(Type ptype) {
        return ptype.typeName() + ptype.dimension();
    }

    /**
     * 增加到beans
     *
     * @param type
     * @param doclet
     */
    public static void addBean(Type type, DefaultSmallDocletImpl doclet) {
        String beanName = inferBeanName(type);//推断BeanName
        //1. 单线程工作，不用加锁。
        //2. 判断该bean是否存在，避免循环引用造成的死循环
        Map<String, List<FieldDocStorer>> beanFieldsMap = doclet.getBeanFieldsMap();
        if (Objects.nonNull(beanFieldsMap.get(beanName)))
            return;

        //如果该类型是参数化类型，去推断类型变量
        Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap = null;
        if (type.asParameterizedType() != null) {
            typeVariableToTypeArgumentMap = typeVariableToTypeArgumentMap(type.asParameterizedType());
        }

        List<FieldDocStorer> fieldDocStorers = new ArrayList<>();
        beanFieldsMap.put(beanName, fieldDocStorers);

        Map<String, Map<String, FieldDocStorer>> entityAndFieldMap = doclet.getEntityAndFieldMap();
        Map<String, FieldDocStorer> nameAndFieldMap = new HashMap<>();
        entityAndFieldMap.put(beanName, nameAndFieldMap);

        for (FieldDoc fieldDoc : getFieldDocs(type, doclet)) {
            Type ftype = fieldDoc.type();
            FieldDocStorer fieldDocStorer = new FieldDocStorer();

            //推断类型变量TypeVariable的实际类型
            inferTypeVariableActualType(typeVariableToTypeArgumentMap, ftype, fieldDocStorer);
            fieldDocStorer.setTypeArguments(getTypeArgumentsOnFields(ftype, typeVariableToTypeArgumentMap, doclet));
            fieldDocStorer.setComment(fieldDoc.commentText());
            fieldDocStorer.setName(fieldDoc.name());
            fieldDocStorer.setCollection(TypeUtils.isCollection(ftype));
            fieldDocStorer.setArray(Utils.isNotBlank(ftype.dimension()));

            //如果是List或Set
            if (fieldDocStorer.isCollection()) {
                Type typeArgument = ftype.asParameterizedType().typeArguments()[0];
                fieldDocStorer.setEleName(inferBeanName(typeArgument));
                fieldDocStorer.setEntity(isEntity(typeArgument, doclet));
            }

            nameAndFieldMap.put(fieldDocStorer.getName(), fieldDocStorer);
            fieldDocStorers.add(fieldDocStorer);
            //如果不是库类型，保留字段
            if (isEntity(ftype, doclet)) {//数组类型是否是库类型，与其元素类型一致
                fieldDocStorer.setEntity(true);
                addBean(ftype, doclet);
            }
        }
    }

    /**
     * 获取字段集合。如果类实现了{@link java.io.Serializable} 或 {@link java.io.Externalizable}那么返回序列化字段集合，
     * 否则，获取所有字段集合，不管Access Modifier <br>
     * 注意：如果是数组类型ArrayTypeImpl，则跳过数组维度，默认得到的即是元素类型字段的集合。
     *
     * @param type
     * @param doclet
     * @return
     */
    public static FieldDoc[] getFieldDocs(Type type, DefaultSmallDocletImpl doclet) {
        if (Objects.isNull(type) || isLibraryType(type, doclet))
            return new FieldDoc[0];
        ClassDoc classDoc = type.asClassDoc();//如果是数组类型ArrayTypeImpl，则跳过数组维度
        FieldDoc[] fields = classDoc.serializableFields();
        if (Utils.isEmpty(fields))
            fields = classDoc.fields(false);//不管Access Modifier
        return Utils.addAll(fields, getFieldDocs(classDoc.superclassType(), doclet));
    }

    /**
     * 对于ParameterizedType（泛型调用），typeVariable的实际类型由传入的typeArgument决定，所以不同的typeArgments
     * 会产生不同的beanFields信息，为保证正确的映射关系，ParameterizedType的BeanName需要保留泛型信息，同时要注意去除
     * 数组维度。
     *
     * @param type
     * @return
     */
    public static String inferBeanName(Type type) {
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
    public static String getQualifierName(com.sun.tools.javac.code.Type type1) {
        return type1.hasTag(TypeTag.ARRAY) ? getQualifierName(((com.sun.tools.javac.code.Type.ArrayType) type1).elemtype) : type1.toString();
    }

    /**
     * 不包含完全限定，泛型，但是包含维度。
     *
     * @param type1
     * @param demision
     * @return
     */
    public static String getName(com.sun.tools.javac.code.Type type1, int demision) {
        return type1.hasTag(TypeTag.ARRAY) ? getName(((com.sun.tools.javac.code.Type.ArrayType) type1).elemtype, ++demision) : type1.tsym.name.toString() + Utils.dimension(demision);
    }

    /**
     * 获得typeVariables与typeArguments的映射
     *
     * @param pType 参数化类型
     * @return
     */
    public static Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap(ParameterizedType pType) {
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
     * @param doclet
     * @return
     */
    public static List<FieldDocStorer> getTypeArguments(Type ptype, DefaultSmallDocletImpl doclet) {
        List<FieldDocStorer> typeArgumentStorers = new ArrayList<>();
        ParameterizedType parameterizedType;
        if (Objects.nonNull(parameterizedType = ptype.asParameterizedType())) {
            for (Type typeArgument : parameterizedType.typeArguments()) {
                FieldDocStorer typeArgumentStorer = new FieldDocStorer();
                typeArgumentStorer.setType(getParamTypeWithDimension(typeArgument));
                typeArgumentStorer.setTypeArguments(getTypeArguments(typeArgument, doclet));
                typeArgumentStorer.setQtype(inferBeanName(typeArgument));
                typeArgumentStorers.add(typeArgumentStorer);

                //如果不是库类型，保留字段
                if (isEntity(typeArgument, doclet)) {
                    addBean(typeArgument, doclet);
                }
            }
        }
        return typeArgumentStorers;
    }

    /**
     * 获取字段Fields的泛型参数，并添加泛型信息到Beans。<br/>
     * <b>由于字段泛型参数可能是类型变量TypeVariable，所以需要单独处理。（对于方法或返回值中存在的类型变量TypeVariable，Controller接口应该去避免，所以不做解析支持。）</b>
     * 使用JSONArray存储返回值。如果使用JSONObject，泛型变量的个数将无法计算。
     *
     * @param ptype                         字段的类型
     * @param typeVariableToTypeArgumentMap 字段所属对象的类型参数TypeArgument与类型变量typeVariable的映射关系
     * @param doclet
     * @return
     */
    public static List<FieldDocStorer> getTypeArgumentsOnFields(Type ptype, Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap, DefaultSmallDocletImpl doclet) {
        List<FieldDocStorer> typeArgumentStorers = new ArrayList<>();
        ParameterizedType parameterizedType;
        if (Objects.nonNull(parameterizedType = ptype.asParameterizedType())) {
            for (Type typeArgument : parameterizedType.typeArguments()) {
                FieldDocStorer typeArgumentStorer = new FieldDocStorer();
                //推断类型变量TypeVariable的实际类型
                inferTypeVariableActualType(typeVariableToTypeArgumentMap, typeArgument, typeArgumentStorer);
                typeArgumentStorer.setTypeArguments(getTypeArgumentsOnFields(typeArgument, typeVariableToTypeArgumentMap, doclet));
                typeArgumentStorers.add(typeArgumentStorer);

                //如果不是库类型，保留字段
                if (isEntity(typeArgument, doclet)) {
                    addBean(typeArgument, doclet);
                }
            }
        }
        return typeArgumentStorers;
    }

    /**
     * 推断类型变量TypeVariable的实际类型
     *
     * @param typeVariableToTypeArgumentMap 字段所属对象的类型参数TypeArgument与类型变量typeVariable的映射关系
     * @param fieldOrFieldArgumentType      字段或字段参数类型
     * @param fieldDocStorer                字段信息存储对象
     */
    public static void inferTypeVariableActualType(Map<String, com.sun.tools.javac.code.Type> typeVariableToTypeArgumentMap, Type fieldOrFieldArgumentType, FieldDocStorer fieldDocStorer) {
        //字段是typeVariable并且typeArguments被显示声明，可推断。
        TypeVariable typeVariable = fieldOrFieldArgumentType.asTypeVariable();//取出类型变量做后续操作，防止数组维度造成的混乱。
        if (typeVariable != null && typeVariableToTypeArgumentMap != null) {
            com.sun.tools.javac.code.Type type1 = typeVariableToTypeArgumentMap.get(typeVariable.qualifiedTypeName());
            fieldDocStorer.setQtype(getQualifierName(type1));
            fieldDocStorer.setType(getName(type1, 0));
        } else {
            fieldDocStorer.setQtype(inferBeanName(fieldOrFieldArgumentType));
            fieldDocStorer.setType(getParamTypeWithDimension(fieldOrFieldArgumentType));
        }
    }
}
