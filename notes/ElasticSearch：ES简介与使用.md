## 1.了解ES

### 1.1.为什么学习ES

1、在当前软件行业中，搜索是一个软件系统或平台的基本功能， 学习ElasticSearch就可以为相应的软件打造出良好的搜索体验。

2、其次，ElasticSearch具备非常强的大数据分析能力。虽然Hadoop也可以做大数据分析，但是ElasticSearch的分析能力非常高，具备Hadoop不具备的能力。比如有时候用Hadoop分析一个结果，可能等待的时间比较长。

3、ElasticSearch可以很方便的进行使用，可以将其安装在个人的笔记本电脑，也可以在生产环境中，将其进行水平扩展。

4、国内比较大的互联网公司都在使用，比如小米、滴滴、携程等公司。另外，在腾讯云、阿里云的云平台上，也都有相应的ElasticSearch云产品可以使用。

5、在当今大数据时代，掌握近实时的搜索和分析能力，才能掌握核心竞争力，洞见未来。



### 1.2.什么是ES

ElasticSearch是一款非常强大的、基于Lucene的开源搜索及分析引擎。

它被用作**全文检索**、**结构化搜索**、**分析**以及这三个功能的组合：

- *Wikipedia* 使用 Elasticsearch 提供带有高亮片段的全文搜索，还有 search-as-you-type 和 did-you-mean 的建议。

- *卫报* 使用 Elasticsearch 将网络社交数据结合到访客日志中，为它的编辑们提供公众对于新文章的实时反馈。

- *Stack Overflow* 将地理位置查询融入全文检索中去，并且使用 more-like-this 接口去查找相关的问题和回答。

- *GitHub* 使用 Elasticsearch 对1300亿行代码进行查询。

- ...

除了搜索，结合Kibana、Logstash、Beats开源产品，Elastic Stack（简称ELK）还被广泛运用在大数据近实时分析领域，包括：**日志分析**、**指标监控**、**信息安全**等。它可以帮助你**探索海量结构化、非结构化数据，按需创建可视化报表，对监控数据设置报警阈值，通过使用机器学习，自动识别异常状况**。

ElasticSearch是基于Restful WebApi，使用Java语言开发的搜索引擎库类，并作为Apache许可条款下的开放源码发布，是当前流行的企业级搜索引擎。其客户端在Java、C#、PHP、Python等许多语言中都是可用的。



### 1.3.ELK技术栈

elasticsearch结合kibana、Logstash、Beats，也就是elastic stack（ELK）。被广泛应用在日志数据分析、实时监控等领域：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/8b43f578-df1a-49d2-8b5d-ff773732ce9c.png "")

而elasticsearch是elastic stack的核心，负责存储、搜索、分析数据。

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/c1020375-5a85-4c70-8329-c3844304324e.png "")

### 1.4.倒排索引

倒排索引的概念是基于MySQL这样的正向索引而言的。

1.4.1.正向索引

那么什么是正向索引呢？例如给下表（tb_goods）中的id创建索引：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/215a0926-5be2-4c92-8daa-53198d86416d.png "")

如果是根据id查询，那么直接走索引，查询速度非常快。

但如果是基于title做模糊查询，只能是逐行扫描数据，流程如下：

1）用户搜索数据，条件是title符合"%手机%""%手机%"

2）逐行获取数据，比如id为1的数据

3）判断数据中的title是否符合用户搜索条件

4）如果符合则放入结果集，不符合则丢弃。回到步骤1

逐行扫描，也就是全表扫描，随着数据量增加，其查询效率也会越来越低。当数据量达到数百万时，就是一场灾难。

1.4.2.倒排索引

倒排索引中有两个非常重要的概念：

- 文档（Document）：用来搜索的数据，其中的每一条数据就是一个文档。例如一个网页、一个商品信息）：用来搜索的数据，其中的每一条数据就是一个文档。例如一个网页、一个商品信息

- 词条（Term）：对文档数据或用户搜索数据，利用某种算法分词，得到的具备含义的词语就是词条。例如：我是中国人，就可以分为：我、是、中国人、中国、国人这样的几个词条）：对文档数据或用户搜索数据，利用某种算法分词，得到的具备含义的词语就是词条。例如：我是中国人，就可以分为：我、是、中国人、中国、国人这样的几个词条

    - 创建倒排索引**是对正向索引的一种特殊处理，流程如下：

- 将每一个文档的数据利用算法分词，得到一个个词条

- 创建表，每行数据包括词条、词条所在文档id、位置等信息

- 因为词条唯一性，可以给词条创建索引，例如hash表结构索引

如图：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/d98863c2-a768-496e-a4fb-eef7fd8aa881.png "")

倒排索引的搜索流程搜索流程如下（以搜索"华为手机"为例）：如下（以搜索"华为手机"为例）：

1）用户输入条件"华为手机""华为手机"进行搜索。进行搜索。

2）对用户输入内容分词分词，得到词条：，得到词条：华为华为、、手机手机。。

3）拿着词条在倒排索引中查找，可以得到包含词条的文档id：1、2、3。

4）拿着文档id到正向索引中查找具体文档。

如图：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/e3f16713-c3ee-4478-ab15-e2ba985ced9e.png "")

虽然要先查询倒排索引，再查询倒排索引，但是无论是词条、还是文档id都建立了索引，查询速度非常快！无需全表扫描。

1.4.3.正向和倒排

那么为什么一个叫做正向索引，一个叫做倒排索引呢？

- 而倒排索引倒排索引则相反，是先找到用户要搜索的词条，根据词条得到保护词条的文档的id，然后根据id获取文档。是则相反，是先找到用户要搜索的词条，根据词条得到保护词条的文档的id，然后根据id获取文档。是根据词条找文档的过程根据词条找文档的过程。。

是不是恰好反过来了？

那么两者方式的优缺点是什么呢？

- 正向索引**：

- 优点：

- 可以给多个字段创建索引

- 根据索引字段搜索、排序速度非常快

    - 倒排索引**：

- 缺点：

- 根据非索引字段，或者索引字段中的部分词条查找时，只能全表扫描。

- 优点：

- 根据词条搜索、模糊搜索时，速度非常快

- 缺点：

- 只能给词条创建索引，而不是字段

- 无法根据字段做排序



### 1.5.算分规则

es查询出的每个结果默认都有一个文档分数，查询出的结果默认根据这个分数倒序排序，相关性越大，评分越高。

可以手动提高某个词的分数权重，平常逛淘宝时的广告商品往往排在比较前面，就是因为提升了该商品的算分权重。

**1.ES5之前，默认评分规则是TF-IDF，这是信息检索领域最重要的发明**

TF（Term Frequency）词频，检索词在一篇文档中出现的频次，检索词个数除以一篇文档总字数，频次越高，得分越高

DF （Document Frequency），检索词在所有文档中出现的频次

IDF （Inverse Document Frequency）逆向文档率，文档个数除以检索词出现过的文档数，频次越高，得分越低

**2.ES5开始，默认使用BM25**

传统的TF数值越大，得分越高

BM25在TF的数值大到某一区间，得分会趋近一个值，而不是无限增大



### 1.6.ES概念和DB概念对比

![](https://tcs-devops.aliyuncs.com/storage/112m5e46b8ea0653df51fc6f7f0d414f7219?Signature=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJBcHBJRCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9hcHBJZCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9vcmdhbml6YXRpb25JZCI6IiIsImV4cCI6MTY4OTU3NTUxNywiaWF0IjoxNjg4OTcwNzE3LCJyZXNvdXJjZSI6Ii9zdG9yYWdlLzExMm01ZTQ2YjhlYTA2NTNkZjUxZmM2ZjdmMGQ0MTRmNzIxOSJ9.Kr_gmhD4d3khe_xhmQCG6_7YeLTHfImhMUP8-L8-JQc&download=image.png "")

### 1.7.基本概念

1. **NRT（Near Realtime）：近实时**

   两方面：
   写入数据时，过1 秒才会被搜索到，因为内部在分词、录入索引。
   es搜索时：搜索和分析数据只需要秒级出结果。

2. **Cluster：集群**

   包含一个或多个启动着es实例的机器群。
   通常一台机器起一个es实例。
   同一网络下，集名一样的多个es实例自动组成
   集群，自动均衡分片等行为。默认集群名为“elasticsearch”。

3. **Node：节点**

   每个es实例称为一个节点。节点名自动分配，也可以手动配置。

4. **Index：索引**

   包含一堆有相似结构的文档数据。
    索引创建规则：

> 仅限小写字母
>  不能包含\、 /、  *、 ?、 "、 <、 >、 | 、 #以及空格符等特殊符号
>  从7.0版本开始不再包含冒号
>  不能以-、 _或+开头
>  不能超过255个字节（注意它是字节，因此多字节字符将计入255个限制）

5. **Document：文档**

   es中的最小数据单元。一个document就像数据库中的一条记录。通常以json格式显示。多个document存储于一个索引（Index）中。

```dart
book document
{
"book_i d": "1",
"book_name": "编程思想",
"book_desc": "从基础语法到最高级特性 ",
"category_i d": "2",
"category_name": "code"
}
```

6. **Field:字段**

   就像数据库中的列（Columns），定义每个document应该有的字段。

7. **Type：类型**

   每个索引里都可以有一个或多个type， type是index中的一个逻辑数据分类，一个type下的document，都有相同的field。
   注意： 6.0之前的版本有type（类型）概念， type相当于关系数据库的表， ES官方将在ES9.0版本中彻底删除type。这里type都为_doc。

8. **shard：分片**

   index数据过大时，将index里面的数据，分为多个shard，分布式的存储在各个服务器上面。可以支持海量数据和高并发，提升性能和吞吐量，充分利用多台机器的cpu。

9. **replica：副本**

   在分布式环境下，任何一台机器都会随时宕机，如果宕机， index的一个分片没有，导致此index不能搜索。所以，为了保证数据的安全，我们会将每个index的分片经行备份，存储在另外的机器上。保证少数机器宕机es集群仍可以搜索。
   能正常提供查询和插入的分片我们叫做主分片（primary shard），其余的我们就管他们叫做备份的分片（replica shard）。



## 2.索引库操作

### 2.1.mapping映射属性

mapping是对索引库中文档的约束，常见的mapping属性包括：

- type：字段数据类型，常见的简单类型有：

- 字符串：text（可分词的文本）、keyword（精确值，例如：品牌、国家、ip地址）

- 数值：long、integer、short、byte、double、float、

- 布尔：boolean

- 日期：date

- 对象：object

- index：是否创建索引，默认为true

- analyzer：使用哪种分词器

- properties：该字段的子字段

例如下面的json文档：

```text
{
    "age": 21,
    "weight": 52.1,
    "isMarried": false,
    "info": "redis持久化的方式有哪些",
    "email": "m1307123fs@163.com",
    "score": [99.1, 99.5, 98.9],
    "name": {
        "firstName": "云",
        "lastName": "赵"
    }
}
```

对应的每个字段映射（mapping）：

- age：类型为 integer；参与搜索，因此需要index为true；无需分词器

- weight：类型为float；参与搜索，因此需要index为true；无需分词器

- isMarried：类型为boolean；参与搜索，因此需要index为true；无需分词器

- info：类型为字符串，需要分词，因此是text；参与搜索，因此需要index为true；分词器可以用ik_smart

- email：类型为字符串，但是不需要分词，因此是keyword；不参与搜索，因此需要index为false；无需分词器

- score：虽然是数组，但是我们只看元素的类型，类型为float；参与搜索，因此需要index为true；无需分词器

- name：类型为object，需要定义多个子属性

- name.firstName；类型为字符串，但是不需要分词，因此是keyword；参与搜索，因此需要index为true；无需分词器

- name.lastName；类型为字符串，但是不需要分词，因此是keyword；参与搜索，因此需要index为true；无需分词器

### 2.2.索引库的CRUD

2.2.1.创建索引库和映射

- 基本语法：**

- 请求方式：PUT

- 请求路径：/索引库名，可以自定义

- 请求参数：mapping映射

格式：

```text
PUT /索引库名称
{
  "mappings": {
    "properties": {
      "字段名":{
        "type": "text",
        "analyzer": "ik_smart"
      },
      "字段名2":{
        "type": "keyword",
        "index": "false"
      },
      "字段名3":{
        "properties": {
          "子字段": {
            "type": "keyword"
          }
        }
      },
      // ...略
    }
  }
}
```

```text
PUT /subject
{
  "mappings": {
    "properties": {
      "id":{
        "type": "keyword"
      },
      "email":{
        "type": "keyword",
        "index": "true",
        "analyzer": "ik_smart"
      }
    }
  }
}
```

2.2.2.查询索引库

- 基本语法**：

- 请求方式：GET

    - 格式**：

- 请求路径：/索引库名

- 请求参数：无

```text
GET /索引库名
```

2.2.3.修改索引库

倒排索引结构虽然不复杂，但是一旦数据结构改变（比如改变了分词器），就需要重新创建倒排索引，这简直是灾难。因此索引库一旦创建，无法修改mapping一旦创建，无法修改mapping。。

虽然无法修改mapping中已有的字段，但是却允许添加新的字段到mapping中，因为不会对倒排索引产生影响。

```text
PUT /索引库名/_mapping
{
  "properties": {
    "新字段名":{
      "type": "integer"
    }
  }
}
```

2.2.4.删除索引库

- 语法：**

- 请求方式：DELETE

    - 格式：**

- 请求路径：/索引库名

- 请求参数：无

```text
DELETE /索引库名
```

## 3.文档操作

### 3.1.新增文档

```text
POST /索引库名/_doc/文档id
{
    "字段1": "值1",
    "字段2": "值2",
    "字段3": {
        "子属性1": "值3",
        "子属性2": "值4"
    },
    // ...
}
```

```text
POST /subject/_doc/1
{
    "id": "1",
    "stem": "redis持久化的方式有哪些",
}
```

### 3.2.查询文档

根据rest风格，新增是post，查询应该是get，不过查询一般都需要条件，这里我们把文档id带上。

```text
GET /{索引库名称}/_doc/{id}
```

```text
GET /subject/_doc/1
```

### 3.3.删除文档

删除使用DELETE请求，同样，需要根据id进行删除：

```text
DELETE /{索引库名}/_doc/id值
```

```text
# 根据id删除数据
DELETE /subject/_doc/1
```

### 3.4.修改文档

修改有两种方式：

- 全量修改：直接覆盖原来的文档

- 增量修改：修改文档中的部分字段

3.4.1.全量修改

全量修改是覆盖原来的文档，其本质是：

- 根据指定的id删除文档

- 新增一个相同id的文档

    - 注意**：如果根据id删除时，id不存在，第二步的新增也会执行，也就从修改变成了新增操作了。

    - 语法：**

```text
PUT /{索引库名}/_doc/文档id
{
    "字段1": "值1",
    "字段2": "值2",
    // ... 略
}
```

```text
PUT /subject/_doc/1
{
    "id": "1",
    "stem": "spring依赖注入的方式"
}
```

3.4.2.增量修改

增量修改是只修改指定id匹配的文档中的部分字段。

```text
POST /{索引库名}/_update/文档id
{
    "doc": {
         "字段名": "新的值",
    }
}
```

```text
POST /heima/_update/1
{
  "doc": {
    "stem": "spring依赖注入的方式"
  }
}
```

### 3.5.分词

需要先安装ik分词器，具体步骤在安装文档中。

IK分词器包含两种模式：

- ik_smart：最少切分：最少切分

- ik_max_word：最细切分：最细切分

```text
GET /_analyze
{
  "analyzer": "ik_smart",
  "text": "redis持久化的方式有哪些"
}
```

3.5.1.扩展词词典

随着互联网的发展，“造词运动”也越发的频繁。出现了很多新的词语，在原有的词汇列表中并不存在。比如：“奥力给”，“传智播客” 等。

所以我们的词汇也需要不断的更新，IK分词器提供了扩展词汇的功能。

1）打开IK分词器config目录：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/c78f4a49-2403-4e87-b242-094aca75a924.png "")

2）在IKAnalyzer.cfg.xml配置文件内容添加：

```text
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典 *** 添加扩展词典-->
        <entry key="ext_dict">ext.dic</entry>
</properties>
```

3）新建一个 ext.dic，可以参考config目录下复制一个配置文件进行修改，直接输入自己要加入的词即可，一行一个词

4）重启elasticsearch

```text
docker restart es

# 查看 日志
docker logs -f elasticsearch
```

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/w5VLqXW3EyGZnX19/img/e1c51b61-ed8f-4834-a95c-5c83b4467d99.png "")

日志中已经成功加载ext.dic配置文件

注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

3.5.2.停用词词典

在互联网项目中，在网络间传输的速度很快，所以很多语言是不允许在网络上传递的，如：关于宗教、政治等敏感词语，那么我们在搜索时也应该忽略当前词汇。

IK分词器也提供了强大的停用词功能，让我们在索引时就直接忽略当前的停用词汇表中的内容。

1）IKAnalyzer.cfg.xml配置文件内容添加：

```text
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典-->
        <entry key="ext_dict">ext.dic</entry>
         <!--用户可以在这里配置自己的扩展停止词字典  *** 添加停用词词典-->
        <entry key="ext_stopwords">stopword.dic</entry>
</properties>
```

3）在 stopword.dic 添加停用词

4）重启elasticsearch

```text
# 重启服务
docker restart elasticsearch
docker restart kibana

# 查看 日志
docker logs -f elasticsearch
```

日志中已经成功加载stopword.dic配置文件

注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

## 4.springboot整合ES

以上介绍的都是通过dsl语句在Kibana中操作ES，我们实际开发中则需要通过代码来操作。spring提供了RestClient来操作ES。

先引入依赖

```xml
<!--ElasticSearch-->
<dependency>
   <groupId>org.elasticsearch.client</groupId>
   <artifactId>elasticsearch-rest-high-level-client</artifactId>
</dependency>
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

在application.properties中配置

```properties
spring.elasticsearch.rest.uris=ip:port
```

### 4.1.建立连接

创建ES工具类，写init方法，使用@PostConstruct注解表示在servlet容器初始化前执行，此方法中就是获取到配置的ip端口，然后进行拆分创建ES连接。

```java
@Value("${spring.elasticsearch.rest.uris}")
private String uris;

private RestHighLevelClient restHighLevelClient;

public static final String SUBJECT_INDEX = "subject";

/**
 * 在Servlet容器初始化前执行
 */
@PostConstruct
private void init() {
    try {
        if (restHighLevelClient != null) {
            restHighLevelClient.close();
        }
        if (StringUtils.isBlank(uris)) {
            log.error("spring.elasticsearch.rest.uris is blank");
            return;
        }
        //解析yml中的配置转化为HttpHost数组
        String[] uriArr = uris.split(",");
        HttpHost[] httpHostArr = new HttpHost[uriArr.length];
        int i = 0;
        for (String uri : uriArr) {
            if (StringUtils.isEmpty(uris)) {
                continue;
            }
            try {
                //拆分出ip和端口号
                String[] split = uri.split(":");
                String host = split[0];
                String port = split[1];
                HttpHost httpHost = new HttpHost(host, Integer.parseInt(port), "http");
                httpHostArr[i++] = httpHost;
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        RestClientBuilder builder = RestClient.builder(httpHostArr);
        restHighLevelClient = new RestHighLevelClient(builder);
    } catch (IOException e) {
        log.error(e.getMessage());
    }
}
```

另一种建立连接的方式，通过配置类：

```java
@Configuration
@RequiredArgsConstructor
public class ElasticsearchClientConfig {

    private final EsProperties esProperties;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (Utils.notEmpty(esProperties.getUsername()) && Utils.notEmpty(esProperties.getPassword())) {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esProperties.getUsername(), esProperties.getPassword()));
        }
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(esProperties.getHost(), esProperties.getPort(), esProperties.getScheme()))
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        return new RestHighLevelClient(restClientBuilder);
    }
}
```

### 4.2.索引库操作

4.2.1.创建索引库

isIndexExist方法是判断索引库是否存在，在下方讲解

```java
/**
 * 创建索引
 *
 * @param index 索引名，类似Mysql库名
 * @param mappingBuilder 库的创建信息，类似详细的建表语句
 * @param type 类型名，类似Mysql表名
 * @return 创建是否成功
 */
public boolean createIndex(String index, XContentBuilder mappingBuilder, String type) throws IOException {
    if (isIndexExist(index)) {
        log.error("Index is  exits!");
        return false;
    }
    //1.创建索引请求
    CreateIndexRequest request = new CreateIndexRequest(index);
    request.mapping(type, mappingBuilder);
    //2.执行客户端请求
    CreateIndexResponse response = restHighLevelClient.indices()
            .create(request, RequestOptions.DEFAULT);
    return response.isAcknowledged();
}
```

调用此方法时需要传入XContentBuilder：

```java
@Test
public void test14() throws IOException {
    XContentBuilder mappingBuilder = JsonXContent.contentBuilder()
            .startObject().startObject("properties") // 对应DSL语句的mapping properties
                .startObject("id") // 创建ID字段开始
                .field("type","keyword") // 类型指定
                .endObject() // 创建ID字段完毕

                .startObject("stem") // 创建题干字段开始
                .field("type", "text") // 类型指定
                .field("index", "true") // 加入反向索引，因为需要分词
                .field("analyzer", "ik_smart") // 分词器设置
                .endObject() // 创建题干字段结束
            .endObject().endObject(); // 创建结束
    // 调用方法
    esUtils.createIndex(ElasticSearchUtils.SUBJECT_INDEX, mappingBuilder, "_doc");
}
```

4.2.2.判断索引库是否存在

```java
/**
 * 判断索引是否存在
 *
 * @param index
 * @return
 */
public boolean isIndexExist(String index) throws IOException {
    GetIndexRequest request = new GetIndexRequest(index);
    return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
}
```

```java
/**
 * 删除索引
 *
 * @param index
 * @return
 */
public boolean deleteIndex(String index) throws IOException {
    if (!isIndexExist(index)) {
        log.error("Index is not exits!");
        return false;
    }
    DeleteIndexRequest request = new DeleteIndexRequest(index);
    AcknowledgedResponse delete = restHighLevelClient.indices()
            .delete(request, RequestOptions.DEFAULT);
    return delete.isAcknowledged();
}
```

### 4.3.文档操作

4.3.1.批量插入

此功能可用于导入Mysql数据。

传入的objects集合类型字段需设置成和索引库的相同，包括顺序。

```java
/**
 * 批量插入false成功
 *
 * @param index   索引，类似数据库
 * @param objects 数据
 * @param ids 用来设置ES的ID，用来设置ES里这条记录的ID，这条记录里有两个属性，id和stem，内部的id是题目id。
 *            为了方便，这里调用的时候就直接使用题目id来作为ES记录的ID
 * @return
 */
public boolean bulkPost(String index, List<?> objects, String[] ids) {
    BulkRequest bulkRequest = new BulkRequest();
    BulkResponse response = null;
    //最大数量不得超过20万
    for (int i = 0; i < objects.size(); i++) {
        IndexRequest request = new IndexRequest(index);
        request.source(JSON.toJSONString(objects.get(i)), XContentType.JSON);
        request.id(ids[i]);
        bulkRequest.add(request);
    }
    try {
        response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null != response && response.hasFailures();
}
```

调用方法：

```java
@Test
public void test15() {
    // 查询mysql数据库题目表所有数据
    List<Subject> subjects = subjectDao.find();
    List<SubjectDoc> subjectDocs = new ArrayList<>(subjects.size());
    String[] ids = new String[subjects.size()];
    for (int i = 0; i < subjects.size(); i++) {
        // 转换为文档类型SubjectDoc
        SubjectDoc doc = new SubjectDoc(subjects.get(i).getIexasubject(), subjects.get(i).getStem());
        subjectDocs.add(doc);
        ids[i] = ""+subjects.get(i).getIexasubject();
    }
    esUtils.bulkPost("subject", subjectDocs, ids);
}
```

4.3.2.新增/更新单条数据

```java
/**
 * 新增/更新数据
 *
 * @param object 要新增/更新的数据
 * @param index  索引，类似数据库
 * @param id     数据ID
 * @return
 */
public String submitData(Object object, String index, String id) throws IOException {
    if (null == id) {
        // 新增数据，没给数据ID，随机一个
        return addData(object, index);
    }
    if (existsById(index, id)) {
        // ID存在，修改数据
        return updateDataByIdNoRealTime(object, index, id);
    } else {
        // 新增数据
        return addData(object, index, id);
    }
}
```

```java
/**
 * 新增数据，自定义id
 *
 * @param object 要增加的数据
 * @param index  索引，类似数据库
 * @param id     ES数据ID
 * @return
 */
public String addData(Object object, String index, String id) throws IOException {
    if (null == id) {
        return addData(object, index);
    }
    if (this.existsById(index, id)) {
        return updateDataByIdNoRealTime(object, index, id);
    }
    //创建请求
    IndexRequest request = new IndexRequest(index);
    request.id(id);
    request.timeout(TimeValue.timeValueSeconds(1));
    //将数据放入请求 json
    request.source(JSON.toJSONString(object), XContentType.JSON);
    //客户端发送请求
    IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
    log.info("添加数据成功 索引为: {}, response 状态: {}, id为: {}", index, response.status().getStatus(), response.getId());
    return response.getId();
}
```

```java
/**
 * 通过ID 更新数据,保证实时性
 *
 * @param object 要增加的数据
 * @param index  索引，类似数据库
 * @param id     数据ID
 * @return
 */
public String updateDataByIdNoRealTime(Object object, String index, String id) throws IOException {
    //更新请求
    UpdateRequest updateRequest = new UpdateRequest(index, id);
    //保证数据实时更新
    updateRequest.setRefreshPolicy("wait_for");
    updateRequest.timeout("1s");
    updateRequest.doc(JSON.toJSONString(object), XContentType.JSON);
    //执行更新请求
    UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    log.info("索引为: {}, id为: {},updateResponseID：{}, 实时更新数据成功", index, id, updateResponse.getId());
    return updateResponse.getId();
}
```

4.3.3.根据ID删除数据

```java
/**
 * 通过ID删除数据
 *
 * @param index 索引，类似数据库
 * @param id    数据ID
 * @return
 */
public String deleteDataById(String index, String id) throws IOException {
    DeleteRequest request = new DeleteRequest(index, id);
    DeleteResponse deleteResponse = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
    return deleteResponse.getId();
}
```

4.3.4.查询并分页

```java
/**
 * 查询并分页
 *
 * @param index          索引名称
 * @param query          查询条件
 * @param highlightField 高亮字段
 * @return
 */
public List<Map<String, Object>> searchListData(String index,
                                                SearchSourceBuilder query,
                                                String highlightField) throws IOException {
    SearchRequest request = new SearchRequest(index);

    //高亮
    HighlightBuilder highlight = new HighlightBuilder();
    highlight.field(highlightField);
    //关闭多个高亮
    highlight.requireFieldMatch(false);
    highlight.preTags("<span style='color:red'>");
    highlight.postTags("</span>");
    query.highlighter(highlight);
    //不返回源数据。只有条数之类的数据。
    //builder.fetchSource(false);
    request.source(query);
    SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
    if (response.status().getStatus() == 200) {
        // 解析对象
        return setSearchResponse(response, highlightField);
    }
    return null;
}
```

setSearchResponse方法是解析高亮用的，不详细看了。调用方法需要传入SearchSourceBuilder：

```java
public List<SubjectDoc> getLikeStemES(String stem) throws IOException {
    // 创建查询
    SearchSourceBuilder searchSourceBuilder = getSearchSourceBuilder(stem);
    // 调用es工具类查询方法
    List<Map<String, Object>> list = esUtils.searchListData(ElasticSearchUtils.SUBJECT_INDEX, searchSourceBuilder, stem);
    // 转换查询结果类型
    List<SubjectDoc> res = new ArrayList<>(list.size());
    for (Map<String, Object> map : list) {
        SubjectDoc subjectDoc = JSONObject.parseObject(JSONObject.toJSONString(map), SubjectDoc.class);
        res.add(subjectDoc);
    }
    return res;
}

private SearchSourceBuilder getSearchSourceBuilder(String stem) {
    // 创建BoolQueryBuilder对象
    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
    // 设置boolQueryBuilder条件
    MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("stem", stem);
    // 子boolQueryBuilder条件条件，用来表示查询条件or的关系
    BoolQueryBuilder childBoolQueryBuilder = new BoolQueryBuilder()
        .should(matchQueryBuilder);
    // 添加查询条件到boolQueryBuilder中
    boolQueryBuilder.must(childBoolQueryBuilder);
    // 创建并设置SearchSourceBuilder对象
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    // 查询条件--->生成DSL查询语句
    searchSourceBuilder.query(boolQueryBuilder);
    // 第几页
    searchSourceBuilder.from(0);
    // 每页多少条数据
    searchSourceBuilder.size(5);
    return searchSourceBuilder;
}
```

