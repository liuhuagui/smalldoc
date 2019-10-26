---
- 项目地址：[https://github.com/liuhuagui/smalldoc](https://github.com/liuhuagui/smalldoc)
- 快速使用：[https://github.com/liuhuagui/smalldoc#user-content-%E4%BD%BF%E7%94%A8](https://github.com/liuhuagui/smalldoc#user-content-%E4%BD%BF%E7%94%A8)
---
很高兴 **smalldoc** 能够帮助 **Java Web** 开发人员解决文档书写的麻烦，将你们从 **swagger** 的繁琐注解中解救出来，也感谢使用者提出的 **issues** 帮助 **smalldoc** 变得更完善更便捷。

<kbd>smalldoc-2.3.1</kbd>根据 **issues**更新如下：
### 修复并优化 **source-paths** 与 **packages** 配置
 **source-paths** 默认已经给出当前项目源码路径（即，引入该smalldoc依赖的项目的源码路径 —— `System.getProperty("user.dir")`， `2.3.1`修复了不配置路径的空指针错误。
   - 只有当你需要第三方jar包源码
    - 或者你的项目是多模块项目需要引入其他模块的源码，才有必要配置 **source-paths**。
  
  **packages** 配置`Controller类`所在的包，会自动递归它们子包。如果没有指定，默认为`/`，将扫描源码路径下所有包，建议给出指定包名，提升解析速度。
### 递归解析返回参数
   无论你的返回对象有几层，都可以显示在返回参数表格中，如下图
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/2019102617262937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
### 支持列表或分页接口返回值中List元素结构的解析
   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20191026172831104.png)
   ![在这里插入图片描述](https://img-blog.csdnimg.cn/20191026172904601.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
### 修复`*Mapping注解`解析异常。
 ```xml
  java.lang.ClassCastException: java.lang.Boolean cannot be cast to [Lcom.sun.javadoc.AnnotationValue;
  ```
### 采用注释的方式支持参数是否必须，支持List，Set，数组，和实体参数
- **普通参数** ，有且仅在注释后添加`@*`表示必须，否则为可选参数。包括基本类型，基本类型的包装类型，字符串，以及它们的数组，List，Set，同时还有一些`库类型` —— 例如 **File** ， **MultipartFile**
- **实体参数** ，实体类中的所有字段都可能作为参数被传递，而且每个接口所需要传递字段的要求不尽相同，所以我们不可能在 **DTO实体** 中做标记，这样不仅有代码侵入性，同时也不能满足接口传参的多样性。
实体参数的注释，可以使用 `@{f1[*],[f2[*],...]}` 这种形式来写，要么代替整个注释，要么放在注释最后。
     - 其中`f`表示实体类的某个字段名，通过它 ，**smalldoc** 可以去你的实体类源码中搜寻参数的注释。
     - 字段名后添加`*`表示必须，否则为可选参数。
     - 如果实体类中的字段没有出现在`@{}`内，该字段将不会作为参数。
     - 如果在`@`之前还有其它注释内容，将被忽略。
     - 如果你的参数是实体参数，注释结尾却不包含该形式，那么将会打印警告日志，帮你预先定位该问题。
  
 示例如下。
 ### 优化参数名展示
 优化过后的参数名支持复杂数据结构，比如关联对象，关联集合，Set，List或数组，可直接作为实际参数名进行接口调用。
  
 示例代码
 ```java
 /**
     * 测试接口
     * @param file 文件
     * @param bb saddas
     * @param cc CCCC
     * @param pp h哈哈是@*
     * @param cca  擦擦擦黑@{authorId*}
     * @param content 内容@*
     * @param oaCopyArray  @{authorId*,originalArticleId,categoryId*,paragraph.content}
     * @param oaCopy  @{authorId*,originalArticleId,categoryId*,paragraph.content}
     * @return data-草稿ID
     */
    @RequestMapping("test_path/action2")
    public Result<Long> test(MultipartFile file, Long[] bb , Long cc, List<String> pp, String content, List<OriginalArticleCopy> cca, OriginalArticleCopy[] oaCopyArray, OriginalArticleCopy oaCopy, HttpServletRequest request) {
        return null;
    }
 ```
 文档显示
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20191026192124876.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191026192205903.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191026192222944.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMyMzMxMDcz,size_16,color_FFFFFF,t_70)
### 增加大量断言
如果你的注释不规范，无法生成合理文档，**smalldoc** 将打印警告或直接提示异常
### 支持离线文档
最初的[ **smalldoc-antd-react-ui** 【https://github.com/liuhuagui/smalldoc-antd-react-ui】](https://github.com/liuhuagui/smalldoc-antd-react-ui)，采用 `React+Fetch` 的形式获得文档结构，新版本改用
`React+模板引擎` 写法，使支持离线文档，你只需要在浏览器中打开文档UI界面，然后 `Ctrl+S` 保存离线文件。

---
### 下个版本将会更新
- 优化父类字段存储方式，减少请求数据
- 为满足微服务多包接口的便捷性，增加包名导航（可选择关闭或开启，默认关闭）
- 希望能够给Java开发者带来更多帮助，更多更新期待你们的 **issue**
