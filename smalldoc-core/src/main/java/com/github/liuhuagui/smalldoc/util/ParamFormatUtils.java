package com.github.liuhuagui.smalldoc.util;

import com.alibaba.fastjson.JSONArray;
import com.github.liuhuagui.smalldoc.core.DefaultSmallDocletImpl;
import com.github.liuhuagui.smalldoc.core.storer.FieldDocStorer;
import com.github.liuhuagui.smalldoc.core.storer.ParamTagStorer;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.liuhuagui.smalldoc.core.constant.Constants.REQUEST_PARAM;

/**
 * 参数格式化工具 —— 是参数处理的核心类。
 *
 * @author KaiKang 799600902@qq.com
 */
public class ParamFormatUtils {
    private static Logger log = LoggerFactory.getLogger(ParamFormatUtils.class);

    public static final String GENERAL_PARAM_TOKEN = "@*";

    public static final String ENTITY_PARAM_TOKEN_REGEX = "([\\s\\S]*)@\\{(.*)}";

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理参数<br>
     * <b>Note</b>：如果你将‘实体数组’和‘集合类型’，直接作为API method参数，那么将抛出断言异常。因为Spring MVC并不支持这种方式的数据绑定
     * 例如：
     * <pre class="code">
     * ArrayList&lt;String> params0;
     * ArrayList&lt;实体类> params1;
     * List&lt;String> params2;
     * List&lt;实体类> params3;
     * Set&lt;String> params4;
     * Set&lt;实体类> params5;
     * 实体类[] params6;
     * </pre>
     * 但是如果你将这些类型的参数作为字段封装到某个实体类中，那么Spring MVC将会正确处理他们
     * <pre class="code">
     * public class ComplexEntityClass{
     * private ArrayList&lt;String> params0;
     * private ArrayList&lt;实体类> params1;
     * private List&lt;String> params2;
     * private List&lt;实体类> params3;
     * private Set&lt;String> params4;
     * private Set&lt;实体类> params5;
     * private 实体类[] params6;
     * }
     * </pre>
     * <pre class="code">
     * \@RequestMapping("action")
     * public Result test(ComplexEntityClass complexEntity) {
     * //logic
     * }
     * </pre>
     * 特殊的，如果参数是基本类型或String类型的‘集合类型’，可以使用<code>org.springframework.web.bind.annotation.RequestParam</code>使之生效<br>
     * 例如：
     * <pre class="code">
     * \@RequestParam ArrayList&lt;String> params0;
     * \@RequestParam List&lt;String> params2;
     * \@RequestParam Set&lt;String> params4;
     * </pre>
     *
     * @param parameterDoc
     * @param paramTag
     * @param paramsJSONArray
     */
    public static void formatParamDoc(DefaultSmallDocletImpl doclet, Parameter parameterDoc, ParamTag paramTag, JSONArray paramsJSONArray) {
        Type ptype = parameterDoc.type();
        addParamBean(doclet, ptype);
        //注意：如果数组类型ArrayTypeImpl中的元素类型是实体类型，那么该数组类型也是实体类型。
        if (TypeUtils.isEntity(ptype, doclet)) {
            formatEntityParamDoc(doclet, paramTag, paramsJSONArray, ptype);
        } else {
            formatNoEntityParamDoc(doclet, paramTag, paramsJSONArray, ptype, parameterDoc.annotations());
        }
    }

    /**
     * 不管参数是不是实体类型，一定要解析它的泛型参数
     *
     * @param doclet
     * @param type
     */
    private static void addParamBean(DefaultSmallDocletImpl doclet, Type type) {
        //先处理类型参数，后面再处理字段（保证TypeVariable的字段被处理）
        TypeUtils.getTypeArguments(type, doclet);
        if (TypeUtils.isEntity(type, doclet))
            TypeUtils.addBean(type, doclet);
    }

    private static void formatNoEntityParamDoc(DefaultSmallDocletImpl doclet, ParamTag paramTag, JSONArray paramsJSONArray, Type ptype, AnnotationDesc[] annotations) {
        if (TypeUtils.isCollection(ptype)) {
            Type typeArgument = ptype.asParameterizedType().typeArguments()[0];
            String currentMethodSignature = doclet.getCurrentMethodSignature();
            String parameterName = paramTag.parameterName();
            Assert.checkNot(TypeUtils.isEntity(typeArgument, doclet), "Method: %s, Param: %s, Spring MVC does not support collections parameter of entity type.", currentMethodSignature, parameterName);
            long count = Stream.of(annotations)
                    .filter(annotationDesc -> annotationDesc.annotationType().simpleTypeName().equals(REQUEST_PARAM))
                    .count();
            Assert.check(count == 1, "Method: %s, Param: %s, Spring MVC need @RequestParam annotation to support collections parameter of base or String type.", currentMethodSignature, parameterName);
        }
        extractGeneralParamTag(doclet, paramTag, ptype)
                .ifPresent(paramsJSONArray::add);
    }

    private static void formatEntityParamDoc(DefaultSmallDocletImpl doclet, ParamTag paramTag, JSONArray paramsJSONArray, Type ptype) {
        boolean array = Utils.isNotBlank(ptype.dimension());
        Assert.checkNot(array, "Method: %s, Param: %s, Spring MVC does not support entity arrays parameter.", doclet.getCurrentMethodSignature(), paramTag.parameterName());
        //注意：如果是数组类型ArrayTypeImpl，解析字段时默认跳过数组维度，默认得到的即是元素类型字段的映射。
        extractEntityParamTag(doclet, paramTag, ptype)
                .ifPresent(pStorer ->
                        paramsJSONArray.addAll(pStorer.getFieldParamStorers())
                );
    }

    /**
     * 抽取普通类型参数ParamTag信息并存储
     *
     * @param doclet
     * @param paramTag
     * @param type
     * @return
     */
    private static Optional<ParamTagStorer> extractGeneralParamTag(DefaultSmallDocletImpl doclet, ParamTag paramTag, Type type) {
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
     * Note：如果没有合适的字段标记<code>@{(*|**|f1[*])[,[-]f2[*]...]}</code>，将打印警告并返回null<br>
     * <ol>
     * <li>字段之间用“,”隔开。</li>
     * <li>字段后加“*”表示必须，无“*”表示否。</li>
     * <li>“*”单独出现表示展示所有非继承非实体字段，且是不必须。</li>
     * <li>“**”单独出现表示展示所有非继承非实体字段，且必须。</li>
     * <li>在花括号中，“*”和“**”不能同时出现，且必须第一个出现。</li>
     * <li>在“*”和“**”后出现的字段
     * <ol>
     * <li>如果以“-”开头，并且包含在非继承非实体字段内，则该字段不做展示。</li>
     * <li>如果以“-”开头，但是不包含在非继承非实体字段内，则断言异常发生。</li>
     * <li>如果没有以“-”开头，并且包含在非继承非实体字段内则复写它，通常用来改变必须性。</li>
     * <li>如果没有以“-”开头，并且没有包含在非继承非实体字段内同时包含在合法字段内，则增加显示该字段。</li>
     * <li>如果没有以“-”开头，并且没有包含在非继承非实体字段内但并不包含在合法字段内，则断言异常发生。</li>
     * </ol>
     * </li>
     * <li>集合或实体数组的字段会自动增加“[]”后缀，表示要采用数组方式提交</li>
     * </ol>
     *
     * @param doclet
     * @param paramTag
     * @param type
     * @return
     */
    private static Optional<ParamTagStorer> extractEntityParamTag(DefaultSmallDocletImpl doclet, ParamTag paramTag, Type type) {
        Pattern compile = Pattern.compile(ENTITY_PARAM_TOKEN_REGEX);
        Matcher matcher = compile.matcher(paramTag.parameterComment());
        if (matcher.find()) {
            String beanName = TypeUtils.inferBeanName(type);
            Map<String, FieldDocStorer> nameAndFieldMap0 = doclet.getNameAndFieldMap(beanName);
            Map<String, ParamTagStorer> paramTagStorerTempCache = new HashMap<>();

            String formatComment = matcher.group(2);
            if (formatComment.equals("*")) {
                paramTagStorerTempCache = createParamTagStorerTempCache(nameAndFieldMap0, false);
            } else if (formatComment.equals("**")) {
                paramTagStorerTempCache = createParamTagStorerTempCache(nameAndFieldMap0, true);
            } else {
                paramTagStorerTempCache = createParamTagStorerTempCache(doclet, paramTag, beanName, nameAndFieldMap0, paramTagStorerTempCache, formatComment);
            }

            return createFinalParamTagStorer(paramTag, matcher, paramTagStorerTempCache);
        }
        log.warn("Method: {}, The param tag @{(*|**|f1[*])[,[-]f2[*]...]} is expected when parameter {} is an entity type.", doclet.getCurrentMethodSignature(), paramTag.parameterName());
        return Optional.empty();
    }

    private static Optional<ParamTagStorer> createFinalParamTagStorer(ParamTag paramTag, Matcher matcher, Map<String, ParamTagStorer> paramTagStorerTempCache) {
        if (paramTagStorerTempCache.isEmpty())
            return Optional.empty();

        final LocalDateTime now = LocalDateTime.now();
        List<ParamTagStorer> fieldParamStorers = paramTagStorerTempCache.entrySet().stream().map(entry -> {
            ParamTagStorer paramTagStorer = entry.getValue();
            //默认示例值
            String example = "3";
            //先以类型推断示例值
            example = deriveExampleFromParamType(paramTagStorer, example, now);
            //特殊的，如果是文件类型，则不进行命名推断
            if (!example.equals(GENERAL_PARAM_TOKEN))
                //用命名推断示例值，命名推断会覆盖类型推断
                example = deriveExampleFromParamName(paramTagStorer.getName(), example, now);
            paramTagStorer.setExample(example);
            return paramTagStorer;
        }).collect(Collectors.toList());

        ParamTagStorer paramTagStorer = new ParamTagStorer(paramTag.parameterName());
        paramTagStorer.setComment(matcher.group(1));
        paramTagStorer.setFieldParamStorers(fieldParamStorers);
        return Optional.of(paramTagStorer);
    }

    private static String deriveExampleFromParamType(ParamTagStorer paramTagStorer, String example, LocalDateTime now) {
        String type = paramTagStorer.getType();
        example = deriveExampleFromType(type, example, now);
        if (TypeUtils.isCollection(type))
            example = deriveExampleFromType(paramTagStorer.getTypeArguments().get(0).getType(), example, now);
        return example;
    }

    private static String deriveExampleFromType(String type, String example, LocalDateTime now) {
        //如果是文件类型，做特殊标记
        if (TypeUtils.isFile(type))
            return GENERAL_PARAM_TOKEN;
        String type0 = type.toLowerCase();
        if (type0.contains("time")) {
            example = now.format(FORMATTER);
        } else if (type0.contains("date")) {
            example = now.toLocalDate().toString();
        } else if (type0.equals("string")) {
//                    example = "3";
        } else if (type0.equals("int") || type0.equals("integer")) {
//                    example = "3";
        } else if (type0.equals("long")) {
            example = "333333333";
        } else if (type0.equals("float")) {
            example = "3.0";
        } else if (type0.equals("double")) {
            example = "3.00";
        } else if (type0.equals("boolean")) {
            example = "true";
        } else if (type0.equals("short")) {
//                    example = "3";
        } else if (type0.equals("byte")) {
//                    example = "3";
        } else if (type0.equals("char") || type0.equals("character")) {
//                    example = "3";
        }
        return example;
    }

    private static String deriveExampleFromParamName(String paramName, String example, LocalDateTime now) {
        String name = paramName.toLowerCase();
        if (name.contains("time")) {
            example = now.format(FORMATTER);
        } else if (name.contains("date")) {
            example = now.toLocalDate().toString();
        } else if (name.contains("phone")) {
            example = "13271620008";
        } else if (name.contains("email")) {
            example = "799600902@qq.com";
        }
        return example;
    }

    private static Map<String, ParamTagStorer> createParamTagStorerTempCache(DefaultSmallDocletImpl doclet, ParamTag paramTag, String beanName, Map<String, FieldDocStorer> nameAndFieldMap0, Map<String, ParamTagStorer> paramTagStorerTempCache, String formatComment) {
        String[] formatFieldNames = formatComment.split(",");
        boolean allDeclaredLibraryField = false;
        int startIndex = 0;
        boolean all = formatComment.startsWith("*,");
        boolean allRequired = formatComment.startsWith("**,");

        if (all || allRequired) {
            allDeclaredLibraryField = true;
            startIndex = 1;
            paramTagStorerTempCache = createParamTagStorerTempCache(nameAndFieldMap0, allRequired);
        }

        for (int i = startIndex; i < formatFieldNames.length; i++) {
            String formatFieldName = formatFieldNames[i];
            String currentMethodSignature = doclet.getCurrentMethodSignature();
            String parameterName = paramTag.parameterName();
            Assert.checkNot(formatFieldName.equals("*") || formatFieldName.equals("**"), "Method: %s, Param: %s, FormatFieldName: %s, *|** can only appear as the first formatFieldName.", currentMethodSignature, parameterName, formatFieldName);
            boolean shouldBeEntity = formatFieldName.contains(".");
            boolean deleteDeclaredLibraryField = formatFieldName.startsWith("-");

            //'-' and '.' can't appear at the same time.
            Assert.checkNot(shouldBeEntity && deleteDeclaredLibraryField, "Method: %s, Param: %s, FormatFieldName: %s, '-' can only appear before the DeclaredLibraryField, while '.' represent Entity field, Please check your formatComment.", currentMethodSignature, parameterName, formatFieldName);

            //if '-' appear.
            if (deleteDeclaredLibraryField) {
                ActualField actualField = extractActualField(formatFieldName, 1);
                Assert.check(allDeclaredLibraryField, "Method: %s, Param: %s, FormatFieldName: %s, Only when *|** appears as the first formatFieldName, the remaining formatFieldNames can start with '-'.", currentMethodSignature, parameterName, formatFieldName);
                String name = actualField.getName();
                FieldDocStorer fieldDocStorer = nameAndFieldMap0.get(name);
                Assert.notNull(fieldDocStorer, "Method: %s, Param: %s, '-' can only appear before the DeclaredLibraryField, but field %s does not exist in %s.", currentMethodSignature, parameterName, name, beanName);
                Assert.check(fieldDocStorer.isDeclared(), "Method: %s, Param: %s, '-' can only appear before the DeclaredLibraryField, but field %s is inherited.", currentMethodSignature, parameterName, name);
                Assert.checkNot(fieldDocStorer.isEntity(), "Method: %s, Param: %s, '-' can only appear before the DeclaredLibraryField, but field %s is Entity.", currentMethodSignature, parameterName, name);
                if (paramTagStorerTempCache.get(name) != null)
                    paramTagStorerTempCache.remove(name);
            }
            //if '.' appear.
            if (shouldBeEntity) {
                ActualField actualField = extractActualField(formatFieldName, 0);
                ParamTagStorer fieldParamTagStorer = extractFieldParamTagStorer(doclet, beanName, actualField.getName(), actualField.isRequired());
                paramTagStorerTempCache.put(fieldParamTagStorer.getName(), fieldParamTagStorer);
            }

            //If neither '-' nor '.' appears.
            if (!shouldBeEntity && !deleteDeclaredLibraryField) {
                ActualField actualField = extractActualField(formatFieldName, 0);
                String name = actualField.getName();
                FieldDocStorer fieldDocStorer = nameAndFieldMap0.get(name);
                Assert.notNull(fieldDocStorer, "Method: %s, Param: %s, This field %s does not exist in %s.", currentMethodSignature, parameterName, name, beanName);
                Assert.checkNot(fieldDocStorer.isEntity(), "Method: %s, Param: %s, This field %s is an Entity Type, Please check your formatComment.", currentMethodSignature, parameterName, name);
                boolean required = actualField.isRequired();
                if (allDeclaredLibraryField && fieldDocStorer.isDeclared()) {
                    if (required != allRequired)
                        paramTagStorerTempCache.compute(name, (k, v) -> {
                            v.setRequired(required);
                            return v;
                        });
                } else {
                    paramTagStorerTempCache.put(fieldDocStorer.getName(), fieldDocStorer.build(required));
                }
            }
        }
        return paramTagStorerTempCache;
    }

    private static Map<String, ParamTagStorer> createParamTagStorerTempCache(Map<String, FieldDocStorer> nameAndFieldMap0, boolean allRequired) {
        return nameAndFieldMap0.entrySet().stream()
                .filter(entry -> {
                    FieldDocStorer fieldDocStorer = entry.getValue();
                    return fieldDocStorer.isDeclared() && !fieldDocStorer.isEntity();
                })
                .map(entry -> entry.getValue().build(allRequired))
                .collect(Collectors.toMap(pts -> pts.getName(), pts -> pts));
    }

    /**
     * 根据formatFieldName构造 {@link ParamTagStorer}
     *
     * @param doclet          {@link com.sun.javadoc.Doclet} 的实现类
     * @param beanName        参数类型的Bean {@link TypeUtils#inferBeanName(Type)}
     * @param formatFieldName 注释 @{(*|**|f1[*])[,[-]f2[*]...]} 中的f
     * @param required        是否必填，f后是否有*
     * @return
     */
    private static ParamTagStorer extractFieldParamTagStorer(DefaultSmallDocletImpl doclet, String beanName, String formatFieldName, boolean required) {
        Map<String, FieldDocStorer> nameAndFieldMap = doclet.getNameAndFieldMap(beanName);
        ParamTagStorer paramTagStorer = new ParamTagStorer("", required);
        FieldDocStorer fieldDocStorer;
        String name;
        StringTokenizer tokenizer = new StringTokenizer(formatFieldName, ".");
        boolean nameIsEmpty = true;
        String currentMethodSignature = doclet.getCurrentMethodSignature();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            name = token;
            fieldDocStorer = nameAndFieldMap.get(name);
            Assert.notNull(fieldDocStorer, "Method: %s, FormatFieldName: %s, This field %s does not exist in %s.", currentMethodSignature, formatFieldName, name, beanName);

            if (nameIsEmpty) {
                paramTagStorer.setName(name);
                nameIsEmpty = false;
            } else {
                paramTagStorer.setName(paramTagStorer.getName() + "." + name);
            }

            //如果没有包含下一层字段
            if (!tokenizer.hasMoreTokens()) {
                Assert.checkNot(fieldDocStorer.isEntity(), "Method: %s, FormatFieldName: %s, This field %s or its elements is an entity type. More fine-grained config is required.", currentMethodSignature, formatFieldName, name);
                paramTagStorer.setType(fieldDocStorer.getType());
                paramTagStorer.setTypeArguments(fieldDocStorer.getTypeArguments());
                paramTagStorer.setComment(fieldDocStorer.getComment());
                break;
            } else {
                Assert.check(fieldDocStorer.isEntity(), "Method: %s, FormatFieldName: %s, This field %s or its elements should be an entity type.", currentMethodSignature, formatFieldName, name);
                //元素类型是实体类型的集合或数组类型需要加“[]”后缀
                if (fieldDocStorer.isCollection() || fieldDocStorer.isArray())
                    paramTagStorer.setName(paramTagStorer.getName() + "[]");
                nameAndFieldMap = doclet.getNameAndFieldMap(fieldDocStorer.isCollection() ? fieldDocStorer.getEleName() : fieldDocStorer.getQtype());
            }
        }
        return paramTagStorer;
    }

    private static ActualField extractActualField(String formatFieldName, int beginIndex) {
        int i = formatFieldName.indexOf("*");
        if (i < 0)
            return ActualField.newField(formatFieldName.substring(beginIndex), false);
        return ActualField.newField(formatFieldName.substring(beginIndex, i), true);
    }

    private static class ActualField {
        private String name;
        private boolean required;

        public ActualField(String name, boolean required) {
            this.name = name;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }

        public static ActualField newField(String name, boolean required) {
            return new ActualField(name, required);
        }
    }
}
