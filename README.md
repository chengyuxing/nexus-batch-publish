# Nexus私服批量推送包程序

## 推送服务器

按顺序指定3个参数：

1. 将要推送的所有包的根目录（必须是包groupId分解为文件夹路径所在的根目录）；
2. 推送远程服务器地址；
3. 用户名密码（冒号分割）。

具体格式为：
```shell
$ java -jar nexus-batch-publish.jar [repositoryRootPath] [nexusServer] [username:password]
```
例如我自己的：
```shell
$ java -jar nexus-batch-publish.jar /Users/chengyuxing/Downloads/repository/ http://localhost:8081/repository/maven-releases/ admin:admin@123
```

## 客户机配置

默认编辑 `~/.m2/settings.xml`

```xml
<mirrors>
	<mirror>
    <id>nexus</id>
    <name>nexus maven</name>
    <url>http://localhost:8081/repository/maven-public/</url>
    <mirrorOf>*</mirrorOf>        
	</mirror>
</mirrors>

<profiles>
	<profile>
     <id>Nexus</id>
     <repositories>
       <repository>
        <id>nexus</id>
        <url>http://localhost:8081/repository/maven-public/</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
       </repository>
     </repositories>
     <pluginRepositories>
       <pluginRepository>
         	<id>nexus</id>
         	<url>http://localhost:8081/repository/maven-public/</url>
         	<releases>
          	<enabled>true</enabled>
         	</releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
       	</pluginRepository>
      </pluginRepositories>
    </profile>
</profiles>
  <!-- 激活 -->
<activeProfiles>
   <activeProfile>Nexus</activeProfile>
</activeProfiles>
```

