package com.github.liuhuagui.smalldoc.core.storer;

import com.github.liuhuagui.smalldoc.core.DefaultSmallDocletImpl;
import com.github.liuhuagui.smalldoc.util.Assert;
import com.github.liuhuagui.smalldoc.util.TypeUtils;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数信息存储器
 *
 * @author KaiKang 799600902@qq.com
 */
public class ParamTagStorer {
    private static Logger log = LoggerFactory.getLogger(ParamTagStorer.class);

    public static final String GENERAL_PARAM_TOKEN = "@*";

    public static final String ENTITY_PARAM_TOKEN_REGEX = "([\\s\\S]*)@\\{(.*)}";

    private String name;

    private String type;

    private List<FieldDocStorer> typeArguments;

    private String comment;
    /**
     * 参数是否必须
     */
    private boolean required;

    private List<ParamTagStorer> fieldParamStorers;

    public ParamTagStorer(String name) {
        this.name = name;
    }

    public ParamTagStorer(String name, boolean required) {
        this.name = name;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public boolean isRequired() {
        return required;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<FieldDocStorer> getTypeArguments() {
        return typeArguments;
    }

    public void setTypeArguments(List<FieldDocStorer> typeArguments) {
        this.typeArguments = typeArguments;
    }

    public List<ParamTagStorer> getFieldParamStorers() {
        return fieldParamStorers;
    }

    public void setFieldParamStorers(List<ParamTagStorer> fieldParamStorers) {
        this.fieldParamStorers = fieldParamStorers;
    }

    public void addFieldParam(ParamTagStorer paramStorer) {
        this.fieldParamStorers.add(paramStorer);
    }

    public void addFieldParam(DefaultSmallDocletImpl doclet,
                              String beanName,
                              String prefixName,
                              String fieldName,
                              boolean required) {
        addFieldParam(ParamTagStorer.createFieldParamTagStorer(doclet, beanName, prefixName, fieldName, required));
    }

    /**
     * 抽取普通类型参数ParamTag信息并存储
     *
     * @param doclet
     * @param paramTag
     * @param type
     * @return
     */
    public static Optional<ParamTagStorer> extractGeneralParamTag(DefaultSmallDocletImpl doclet, ParamTag paramTag, Type type) {
        ParamTagStorer paramTagStorer = new ParamTagStorer(paramTag.parameterName());
        paramTagStorer.setType(TypeUtils.getParamTypeWithDimension(type));
        paramTagStorer.setTypeArguments(TypeUtils.getTypeArguments(type, doclet));

        String comment = paramTag.parameterComment();
        if (comment.endsWith(GENERAL_PARAM_TOKEN)) {
            paramTagStorer.setRequired(true);
            paramTagStorer.setComment(comment.substring(0, comment.lastIndexOf(GENERAL_PARAM_TOKEN)));
        } else {
            paramTagStorer.setRequired(false);
            paramTagStorer.setComment(comment);
        }
        return Optional.of(paramTagStorer);
    }

    /**
     * 抽取实体类型参数ParamTag信息并存储。<br>
     * Note：如果没有合适的字段标记<code>@{f1[*][,f2[*]...]}</code>，将打印警告并返回null
     *
     * @param doclet
     * @param paramTag
     * @param type
     * @param prefix   元素类型是实体类型的集合或数组类型需要加“[]”后缀
     * @return
     */
    public static Optional<ParamTagStorer> extractEntityParamTag(DefaultSmallDocletImpl doclet, ParamTag paramTag, Type type, boolean prefix) {
        Pattern compile = Pattern.compile(ENTITY_PARAM_TOKEN_REGEX);
        Matcher matcher = compile.matcher(paramTag.parameterComment());
        if (matcher.find()) {
            ParamTagStorer paramTagStorer = new ParamTagStorer(paramTag.parameterName());
            paramTagStorer.setFieldParamStorers(new ArrayList<>());
            paramTagStorer.setComment(matcher.group(1));
            String beanName = TypeUtils.inferBeanName(type);
            String prefixName = prefix ? paramTag.parameterName() : null;

            StringTokenizer tokenizer = new StringTokenizer(matcher.group(2), "*,", true);
            String prevFieldName = null;
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                switch (token) {
                    case ",":
                        if (prevFieldName != null) {
                            paramTagStorer.addFieldParam(doclet, beanName, prefixName, prevFieldName, false);
                            prevFieldName = null;
                        }
                        break;
                    case "*":
                        if (prevFieldName != null) {
                            paramTagStorer.addFieldParam(doclet, beanName, prefixName, prevFieldName, true);
                            prevFieldName = null;
                        }
                        break;
                    default://这要么是第一个，要么前面肯定有一个,或*，所以prevFieldName一定为null
                        if (tokenizer.hasMoreTokens()) {
                            prevFieldName = token;
                        } else {
                            paramTagStorer.addFieldParam(doclet, beanName, prefixName, token, false);
                        }
                }
            }
            if (!paramTagStorer.getFieldParamStorers().isEmpty())
                return Optional.of(paramTagStorer);
            paramTagStorer.setFieldParamStorers(null);//清除空的List对象
        }
        log.warn("Method: {}, The param tag @{f1[*][,f2[*]...]} is expected when parameter {} is an entity type.", doclet.getCurrentMethodSignature(), paramTag.parameterName());
        return Optional.empty();
    }

    /**
     * 根据fieldName构造 {@link ParamTagStorer}
     *
     * @param doclet     {@link com.sun.javadoc.Doclet} 的实现类
     * @param beanName   参数类型的Bean {@link TypeUtils#inferBeanName(Type)}
     * @param prefixName 如果不为null，需要在字段前包含“prefixName[].”
     * @param fieldName  注释 @{f1[*][,f2[*]...]} 中的f
     * @param required   是否必填，f后是否有*
     * @return
     */
    public static ParamTagStorer createFieldParamTagStorer(DefaultSmallDocletImpl doclet, String beanName, String prefixName, String fieldName, boolean required) {
        Map<String, FieldDocStorer> nameAndFieldMap = doclet.getNameAndFieldMap(beanName);
        ParamTagStorer paramTagStorer = new ParamTagStorer("", required);
        FieldDocStorer fieldDocStorer;
        String name;
        StringTokenizer tokenizer = new StringTokenizer(fieldName, ".");
        boolean nameIsEmpty = true;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            name = token;
            fieldDocStorer = nameAndFieldMap.get(name);
            Assert.notNull(fieldDocStorer, "Method: %s, Comment: %s, This field %s does not exist in %s", doclet.getCurrentMethodSignature(), fieldName, name, beanName);

            if (nameIsEmpty) {
                paramTagStorer.setName(name);
                nameIsEmpty = false;
            } else {
                paramTagStorer.setName(paramTagStorer.getName() + "." + name);
            }

            //如果没有包含下一层字段
            if (!tokenizer.hasMoreTokens()) {
                Assert.check(!fieldDocStorer.isEntity(), "Method: %s, Comment: %s, This field %s or its elements is an entity type. More fine-grained config is required.", doclet.getCurrentMethodSignature(), fieldName, name);
                if (prefixName != null)
                    paramTagStorer.setName(prefixName + "[]." + paramTagStorer.getName());
                paramTagStorer.setType(fieldDocStorer.getType());
                paramTagStorer.setTypeArguments(fieldDocStorer.getTypeArguments());
                paramTagStorer.setComment(fieldDocStorer.getComment());
                break;
            } else {
                Assert.check(fieldDocStorer.isEntity(), "Method: %s, Comment: %s, This field %s or its elements should be an entity type.", doclet.getCurrentMethodSignature(), fieldName, name);
                //元素类型是实体类型的集合或数组类型需要加“[]”后缀
                if (fieldDocStorer.isCollection() || fieldDocStorer.isArray())
                    paramTagStorer.setName(paramTagStorer.getName() + "[]");
                nameAndFieldMap = doclet.getNameAndFieldMap(fieldDocStorer.isCollection() ? fieldDocStorer.getEleName() : fieldDocStorer.getQtype());
            }
        }

        return paramTagStorer;
    }
}
