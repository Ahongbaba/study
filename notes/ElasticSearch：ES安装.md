# 1.windows安装

es官网：[__https://www.elastic.co/cn/__](https://www.elastic.co/cn/)

下载地址（zip压缩包，直接复制到浏览器就会开始下载，可以改版本号下载指定版本，kibana和es版本要保持一致）：

[__https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.10.2-windows-x86_64.zip__](https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.10.2-windows-x86_64.zip)

[__https://artifacts.elastic.co/downloads/kibana/kibana-7.10.2-windows-x86_64.zip__](https://artifacts.elastic.co/downloads/kibana/kibana-7.10.2-windows-x86_64.zip)

## 1.1.ES：

![](https://tcs-devops.aliyuncs.com/storage/112l7987b78cb72621fc017295e701129bc6?Signature=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJBcHBJRCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9hcHBJZCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9vcmdhbml6YXRpb25JZCI6IiIsImV4cCI6MTY4OTU3NTU0NywiaWF0IjoxNjg4OTcwNzQ3LCJyZXNvdXJjZSI6Ii9zdG9yYWdlLzExMmw3OTg3Yjc4Y2I3MjYyMWZjMDE3Mjk1ZTcwMTEyOWJjNiJ9.VbDL_OCKfk0dzhrttzL_XIp9mj-0TyuK_crJBcs3jgo&download=image.png "")

解压后进入bin文件夹，运行elasticsearch.bat即可，如果运行失败，可尝试打开config文件夹下的elasticsearch.yml文件，在最后一行加上 `xpack.ml.enabled: false`

es的默认端口是9200，在浏览器输入[__http://localhost:9200/__](http://localhost:9200/)，出现以下页面表示es启动成功

![](https://tcs-devops.aliyuncs.com/storage/112l884b0d27870bd45b3816ad4f5174b488?Signature=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJBcHBJRCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9hcHBJZCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9vcmdhbml6YXRpb25JZCI6IiIsImV4cCI6MTY4OTU3NTU0NywiaWF0IjoxNjg4OTcwNzQ3LCJyZXNvdXJjZSI6Ii9zdG9yYWdlLzExMmw4ODRiMGQyNzg3MGJkNDViMzgxNmFkNGY1MTc0YjQ4OCJ9.RgIc7yjIJpy3ATAEh8I3Fy0kPOSarwZ0syw6Y2vPzxY&download=image.png "")

## 1.2.Kibana

![](https://tcs-devops.aliyuncs.com/storage/112l35bc3ea28a2173eac1810f54869ba349?Signature=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJBcHBJRCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9hcHBJZCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9vcmdhbml6YXRpb25JZCI6IiIsImV4cCI6MTY4OTU3NTU0NywiaWF0IjoxNjg4OTcwNzQ3LCJyZXNvdXJjZSI6Ii9zdG9yYWdlLzExMmwzNWJjM2VhMjhhMjE3M2VhYzE4MTBmNTQ4NjliYTM0OSJ9.j9w8BfNNbon08EChg0bWkhX4JMSm22W7ZXcCHnrPFL0&download=image.png "")

进入config文件夹，打开kibana.yml文件，在最后加上es的配置：`elasticsearch.hosts: ["http://localhost:9200"]`

进入bin文件夹，运行kibana.bat

kibana默认端口是5601，在浏览器输入[__http://localhost:5601/__](http://localhost:9200/)，出现以下页面表示kibana启动成功

![](https://tcs-devops.aliyuncs.com/storage/112la456a5f9ca842b45ec8d6d12f9ac7d6f?Signature=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJBcHBJRCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9hcHBJZCI6IjVlNzQ4MmQ2MjE1MjJiZDVjN2Y5YjMzNSIsIl9vcmdhbml6YXRpb25JZCI6IiIsImV4cCI6MTY4OTU3NTU0NywiaWF0IjoxNjg4OTcwNzQ3LCJyZXNvdXJjZSI6Ii9zdG9yYWdlLzExMmxhNDU2YTVmOWNhODQyYjQ1ZWM4ZDZkMTJmOWFjN2Q2ZiJ9.6jsD-6rotnU_D7FBNpYm4N2KWo1Mfw4mwUTJO0f2vxI&download=image.png "")



# 2.部署单点es

## 2.1.创建网络

因为我们还需要部署kibana容器，因此需要让es和kibana容器互联。这里先创建一个网络：

```text
docker network create es-net
```

## 2.2.加载镜像

这里我们采用elasticsearch的7.12.1版本的镜像，这个镜像体积非常大，接近1G。

镜像的tar包：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/df8931c7-0aa9-4ef5-9740-e2c37c003225.png "")

上传到虚拟机中，然后运行命令加载即可：

```text
# 导入数据
docker load -i es.tar
```

同理还有kibana的tar包也需要这样做。

## 2.3.运行

运行docker命令，部署单点es：

```text
docker run -d \
	--name es \
    -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
    -e "discovery.type=single-node" \
    -v es-data:/usr/share/elasticsearch/data \
    -v es-plugins:/usr/share/elasticsearch/plugins \
    --privileged \
    --network es-net \
    -p 9200:9200 \
    -p 9300:9300 \
elasticsearch:7.12.1
```

命令解释：

- -e "cluster.name=es-docker-cluster"：设置集群名称

- -e "http.host=0.0.0.0"：监听的地址，可以外网访问

- -e "ES_JAVA_OPTS=-Xms512m -Xmx512m"：内存大小

- -e "discovery.type=single-node"：非集群模式

- -v es-data:/usr/share/elasticsearch/data：挂载逻辑卷，绑定es的数据目录

- -v es-logs:/usr/share/elasticsearch/logs：挂载逻辑卷，绑定es的日志目录

- -v es-plugins:/usr/share/elasticsearch/plugins：挂载逻辑卷，绑定es的插件目录

- --privileged：授予逻辑卷访问权

- --network es-net ：加入一个名为es-net的网络中

- -p 9200:9200：端口映射配置

在浏览器中输入：http://192.168.150.101:9200   即可看到elasticsearch的响应结果：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/2279bbef-2448-479f-8d73-8bac28371145.png "")

# 3.部署kibana

kibana可以给我们提供一个elasticsearch的可视化界面，便于我们学习。

## 3.1.部署

运行docker命令，部署kibana

```text
docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \
--network=es-net \
-p 5601:5601  \
kibana:7.12.1
```

- --network es-net ：加入一个名为es-net的网络中，与elasticsearch在同一个网络中

- -e ELASTICSEARCH_HOSTS=http://es:9200"：设置elasticsearch的地址，因为kibana已经与elasticsearch在一个网络，因此可以用容器名直接访问elasticsearch

- -p 5601:5601：端口映射配置

kibana启动一般比较慢，需要多等待一会，可以通过命令：

```text
docker logs -f kibana
```

查看运行日志，当查看到下面的日志，说明成功：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/0543f3ad-c3e1-447c-9673-a4efdd42f3d7.png "")

此时，在浏览器输入地址访问：http://192.168.150.101:5601，即可看到结果

## 3.2.DevTools

kibana中提供了一个DevTools界面：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/28731db0-eaaa-4452-9320-a6b81b336e2f.png "")

这个界面中可以编写DSL来操作elasticsearch。并且对DSL语句有自动补全功能。

# 4.安装IK分词器

## 4.1.在线安装ik插件（较慢）

```text
# 进入容器内部
docker exec -it elasticsearch /bin/bash

# 在线下载并安装
./bin/elasticsearch-plugin  install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.12.1/elasticsearch-analysis-ik-7.12.1.zip

#退出
exit
#重启容器
docker restart elasticsearch
```

## 4.2.离线安装ik插件（推荐）

### 1）查看数据卷目录

安装插件需要知道elasticsearch的plugins目录位置，而我们用了数据卷挂载，因此需要查看elasticsearch的数据卷目录，通过下面命令查看:

```text
docker volume inspect es-plugins
```

显示结果：

```text
[
    {
        "CreatedAt": "2022-05-06T10:06:34+08:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/es-plugins/_data",
        "Name": "es-plugins",
        "Options": null,
        "Scope": "local"
    }
]
```

说明plugins目录被挂载到了：/var/lib/docker/volumes/es-plugins/_data 这个目录中。

### 2）解压缩分词器安装包

ik分词器解压缩，重命名为ik

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/3f5e1139-e044-4b9f-98a2-750f186259c2.png "")

### 3）上传到es容器的插件数据卷中

也就是/var/lib/docker/volumes/es-plugins/_data ：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/a3fec9b3-e455-49c8-b030-b3048191926f.png "")

### 4）重启容器

```text
# 4、重启容器
docker restart es
```

```text
# 查看es日志
docker logs -f es
```

### 5）测试：

IK分词器包含两种模式：

- ik_smart：最少切分

- ik_max_word：最细切分

```text
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "程序员学习java太棒了"
}
```

结果：

```text
{
  "tokens" : [
    {
      "token" : "程序员",
      "start_offset" : 2,
      "end_offset" : 5,
      "type" : "CN_WORD",
      "position" : 1
    },
    {
      "token" : "程序",
      "start_offset" : 2,
      "end_offset" : 4,
      "type" : "CN_WORD",
      "position" : 2
    },
    {
      "token" : "员",
      "start_offset" : 4,
      "end_offset" : 5,
      "type" : "CN_CHAR",
      "position" : 3
    },
    {
      "token" : "学习",
      "start_offset" : 5,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 4
    },
    {
      "token" : "java",
      "start_offset" : 7,
      "end_offset" : 11,
      "type" : "ENGLISH",
      "position" : 5
    },
    {
      "token" : "太棒了",
      "start_offset" : 11,
      "end_offset" : 14,
      "type" : "CN_WORD",
      "position" : 6
    },
    {
      "token" : "太棒",
      "start_offset" : 11,
      "end_offset" : 13,
      "type" : "CN_WORD",
      "position" : 7
    },
    {
      "token" : "了",
      "start_offset" : 13,
      "end_offset" : 14,
      "type" : "CN_CHAR",
      "position" : 8
    }
  ]
}
```

## 4.3 扩展词词典

随着互联网的发展，“造词运动”也越发的频繁。出现了很多新的词语，在原有的词汇列表中并不存在。

所以我们的词汇也需要不断的更新，IK分词器提供了扩展词汇的功能。

1）打开IK分词器config目录：

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/eea4e32b-ecb6-4d65-83e1-d732d10f6b60.png "")

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

3）新建一个 ext.dic，可以参考config目录下复制一个配置文件进行修改

```text
冰墩墩
雪容融
```

4）重启elasticsearch

```text
docker restart es

# 查看 日志
docker logs -f elasticsearch
```

![](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oMAeqxdDoDZKq8j9/img/d3522170-6a1f-42b3-b3b7-c281cf69c733.png "")

日志中已经成功加载ext.dic配置文件

5）测试效果：

```text
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "好想要冰墩墩和雪容融啊！"
}
```

注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

## 4.4 停用词词典

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

```text
习大大
```

4）重启elasticsearch

```text
# 重启服务
docker restart elasticsearch
docker restart kibana

# 查看 日志
docker logs -f elasticsearch
```

日志中已经成功加载stopword.dic配置文件

5）测试效果：

```text
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "习大大非常推荐冰墩墩和雪容融啊！"
}
```

注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

