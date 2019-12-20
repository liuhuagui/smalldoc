package com.github.liuhuagui.smalldoc.core.storer;

import java.util.List;

/**
 * 字段文档存储器
 *
 * @author KaiKang 799600902@qq.com
 */
public class FieldDocStorer {
    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型的完全限定名
     */
    private String qtype;

    private String type;

    private String comment;

    private boolean entity;

    private boolean collection;

    private boolean array;

    private String eleName;

    private List<FieldDocStorer> typeArguments;

    public String getEleName() {
        return eleName;
    }

    public void setEleName(String eleName) {
        this.eleName = eleName;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public boolean isEntity() {
        return entity;
    }

    public void setEntity(boolean entity) {
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQtype() {
        return qtype;
    }

    public void setQtype(String qtype) {
        this.qtype = qtype;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<FieldDocStorer> getTypeArguments() {
        return typeArguments;
    }

    public void setTypeArguments(List<FieldDocStorer> typeArguments) {
        this.typeArguments = typeArguments;
    }
}
