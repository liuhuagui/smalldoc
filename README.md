### 项目
- [https://github.com/liuhuagui/smalldoc](https://github.com/liuhuagui/smalldoc) 一个基于Java标准注释的 RESTful API 文档工具
-  [**smalldoc-antd-react-ui**](https://github.com/liuhuagui/smalldoc-antd-react-ui)（https://github.com/liuhuagui/smalldoc-antd-react-ui）
### 为什么要造轮子？
- 强迫症患者，接受不了**Swagger**的各式注解对代码的入侵造成的冗杂，更渴望清洁的代码；
- 注解的使用需要一定的学习成本；
- 随后尝试使用**Apidoc**，尽管Apidoc是基于注释生成文档，但是学习成本并没有降低，你需要学习额外的注释**Tag**，同时你不得不使用这些特殊的**Tag**将你所需接口的相关信息手动写出来，感觉并没有大幅度降低书写文档的工作量；
- 也有一些基于Java标准注释生成文档的项目，但是有的无法支持实体参数、泛型变量、多模块依赖，有的Bug太多，UI界面不够友好，使用方式过于复杂，甚至逻辑处理上存在问题。
### 更新日志
#### 2.3.1
- 修复并优化 **source-paths** 与 **packages** 配置
- 递归解析返回参数
- 支持列表或分页接口返回值中List元素结构的解析
- 修复`*Mapping注解`解析异常。
- 采用注释的方式支持参数是否必须，支持List，Set，数组，和实体参数
- 优化参数名展示
- 增加大量断言
- 支持离线文档
详情可以了解：[smalldoc-2.3.1更新日志](https://github.com/liuhuagui/smalldoc/doc/blob/master/smalldoc-2.3.1.md)
### 特性
- 基于Java源码、标准注释以及Tag生成文档，无代码入侵，保证代码整洁，同时保证开发人员的注释习惯与注释规范
- 提供了友好的默认UI
- 提供了文档RESTEful API，支持实现自定义UI
- 提供规范配置方式，使用更方便
- 提供相应的spring-boot-starter，同时支持传统spring与spring-boot
- 支持关联实体参数
- 支持泛型
- 支持忽略某个参数
- 支持忽略解析指定包或指定类型参数的数据结构
- 支持多模块项目（从指定Jar包中解析源码或数据结构）
### 实现方式
- 解析源码文件，通过源码及其中的注释生成文档信息。目前为止注释中使用的Tag都是Java注释的标准Tag，后续可能会添加一些必要的自定义Tag，甚至有可能提供Tag扩展机制 —— 由使用者自定义Tag，同时自定义Tag的处理方式。
- 一想到生成Java RESTful API文档，首先就是想到Java API文档是如何生成的，所以解析源码的方式没有选择使用`com.github.javaparser » javaparser-core`或者`com.thoughtworks.qdox » qdox`，而是选择JDK自己的[Javadoc Tool](https://docs.oracle.com/en/java/javase/12/tools/javadoc.html) （https://docs.oracle.com/en/java/javase/12/tools/javadoc.html），Javadoc Tool对应的API是[Javadoc API](https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/module-summary.html)（https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/module-summary.html）
- 由于作者还在使用**Java8**，所以该项目的实现完全是基于**Javadoc API 旧版**
  - **Package [com.sun.tools.javadoc](https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/com/sun/tools/javadoc/package-summary.html)**   (https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/com/sun/tools/javadoc/package-summary.html)
  - **Package [com.sun.javadoc](https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/com/sun/javadoc/package-summary.html)** (https://docs.oracle.com/en/java/javase/12/docs/api/jdk.javadoc/com/sun/javadoc/package-summary.html)

  其中：
  >**Module** jdk.javadoc
**Package** com.sun.tools.javadoc
This package and its contents are deprecated and may be removed in a future release. See javax.tools.**ToolProvider.getSystemDocumentationTool** and **javax.tools.DocumentationTool** for replacement functionality.
  
  >**Module** jdk.javadoc
**Package** com.sun.javadoc
**Note:** The declarations in this package have been superseded by those in the package **jdk.javadoc.doclet**. For more information, see the Migration Guide in the documentation for that package.
  ```java
  @Deprecated(since="9",forRemoval=true)
  public class Main extends Object
  ```
  可以看出，旧版Javadoc API自 **Java9** 已经被标记遗弃，在不久的将来将被移除，但是值得庆幸，直到最新的大版本 **Java12** 该API还未移除，所以使用 **Java12** 及以前版本的用户可以放心使用，后续作者会提供新版API支持。
 - UI界面是基于 **create-react-app** 与 **antd** 开发的single page application —— 
  [**smalldoc-antd-react-ui**](https://github.com/liuhuagui/smalldoc-antd-react-ui)（https://github.com/liuhuagui/smalldoc-antd-react-ui）
### 使用
示例为spring-boot项目，使用 **application.yml** 做为配置文件
##### 引入依赖
```xml
<dependency>
    <groupId>com.github.liuhuagui</groupId>
    <artifactId>smalldoc-spring-boot-starter</artifactId>
    <version>2.3</version>
</dependency>
```
##### 配置
接口文档通常在开发时使用，只需要保证文档配置在开发环境下生效 —— `spring.profiles.active=dev`
```yml
server: 
  port: 8080
  servlet:
    context-path: /my-project
spring: 
  profiles:
    active: dev
---
spring:
  profiles: dev
smalldoc:
  source-paths: #额外的源码路径（项目的源码路径默认已经包含在内，不需要再添加）
    - 'D:\Workspaces\myBeanProject\my-bean\src\main\java'
    - 'D:\Maven\Repositories\repository\com\aliyun\aliyun-java-sdk-core\3.5.0'
  packages:
    - quantity.knowledgebase
    - my.bean
    - com.aliyuncs.auth.sts
  project-name: 我的文档
  enabled: true #默认为true
  url-pattern: /smalldoc/* #默认为/smalldoc/*
```
##### 访问地址
- **URL:** `http://192.168.1.76:8080/my-project/smalldoc/` 
- **METHOD:** **GET**
##### 接口源码
```java
/**
 * 文章的创建，编辑，发布，自定义
 * @author KaiKang 799600902@qq.com
 */
@RestController
@RequestMapping("w")
public class WriteArticleController {
    /**
     * 原创文章在编辑中保存
     * @param content 内容
     * @param oaCopy  原创文章副本
     * @return data-草稿ID
     * @author KaiKang 799600902@qq.com
     */
    @PostMapping(path = "o/save_draft",produces = {"text/plain", "application/json;charset=UTF-8"},consumes = "application/x-www-form-urlencoded")
    public Result<Long> saveOriginalDraft(String content, OriginalArticleCopy oaCopy, HttpServletRequest request) {
        return writeArticleService.saveOriginalDraft(content, oaCopy);
    }

    /**
     * 这只是一个测试接口
     * @param content 内容
     * @return 返回数据
     * @author KaiKang 799600902@qq.com
     */
    @GetMapping(path = "o/save",produces = {"text/plain", "application/json;charset=UTF-8"})
    public Result<OriginalArticle> save(String content, HttpServletRequest request) {
        return null;
    }
}
```
##### 接口文档
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191016163729604.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019101616375367.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191016163816357.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
##### 文档API（用来实现自定义UI）
- **URL:** `http://192.168.1.76:8080/my-project/smalldoc/` 
- **METHOD:** POST
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191016155016143.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
### 注意
- **source-paths** 配置项表示额外的源码路径（比如多模块项目下，其它源码路径），项目的源码路径默认已经包含在内，不需要额外添加，只需要指定扫描的包，比如：`my.project.controller`
- **packages** 如果没有指定扫描的包，默认“/”，将扫描源码路径下所有包，建议给出指定包名，提升解析速度。
- 程序只会解析类名为`*Controller`的源代码中的接口信息（规范）
- 程序暂未支持Linux环境，在项目打包部署之前，记得关闭文档功能，关闭方式多种多样，比如：
  1. `spring.profiles.active=*`(**\*** 只要不是dev即可)，不再激活开发环境配置
  2. `smalldoc.enabled=false`，关闭启用
  3. 修改依赖作用域为`test`后再进行打包，这样连**smalldoc**的jar包都不会被打包进去（推荐）
     ```xml
      <dependency>
			<groupId>com.github.liuhuagui</groupId>
			<artifactId>smalldoc-spring-boot-starter</artifactId>
			<version>2.3</version>
			<scope>test</scope>
      </dependency>
       ```
### 社区
如果在使用过程中需要帮助或者希望项目增加某些功能，欢迎issue —— [https://github.com/liuhuagui/smalldoc/issues](https://github.com/liuhuagui/smalldoc/issues)