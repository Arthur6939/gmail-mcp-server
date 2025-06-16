# Gmail MCP Server

一个基于Model Context Protocol (MCP)的服务器，提供Gmail邮件删除功能。

## 功能
- 通过MCP协议暴露Gmail邮件删除工具
- 支持通过邮件ID删除指定用户的邮件
- 提供邮件搜索功能，支持按条件查询邮件
- 支持批量删除指定用户的多个邮件

## 环境要求
- Java 17或更高版本
- Maven 3.6+或Gradle 7.0+
- Gmail API credentials.json文件

## 快速开始

### 1. 准备Gmail API凭据
1. 前往[Google Cloud Console](https://console.cloud.google.com/)
2. 创建新项目并启用Gmail API
3. 创建OAuth 2.0客户端ID凭据
4. 下载credentials.json文件并保存到`src/main/resources/`目录

### 2. 构建项目
```bash
./mvnw clean install
```

### 3. 运行服务器
```bash
./mvnw spring-boot:run
```

## 配置
配置文件位于`src/main/resources/application.properties`，可修改以下参数：
- `server.port`: 服务器端口，默认8080
- `spring.ai.mcp.server.enabled`: 是否启用MCP服务器，默认true
- `gmail.credentials.path`: Gmail凭据文件路径，默认classpath:credentials.json

## 使用方法
通过MCP客户端连接服务器后，可调用以下工具：
- `deleteEmail`: 删除指定ID的邮件
  - 参数: userId(用户邮箱), messageId(邮件ID)
- `searchEmails`: 搜索符合条件的邮件
  - 参数: userId(用户邮箱), query(搜索条件), maxResults(最大结果数)
  - 返回值: 邮件列表，包含邮件ID和基本信息
- `batchDeleteEmails`: 批量删除邮件
  - 参数: userId(用户邮箱), messageIds(邮件ID列表)
  - 返回值: 操作结果状态
## MCP server 配置
{
  "mcpServers": {
    "gmail-mcp-server": {
    "command": "java",
    "args": [
      "-Dspring.ai.mcp.server.stdio=true",
      "-Dspring.main.web-application-type=none",
      "-Dlogging.pattern.console=",
      "-jar",
      "/Users/arthur/.m2/repository/arthur/mcp/gmail-mcp-server/0.0.1-SNAPSHOT/gmail-mcp-server-0.0.1-SNAPSHOT.jar"
    ]
    }
  }
}