package arthur.mcp.gmail.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GmailService {
    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final String APPLICATION_NAME = "Gmail MCP Server";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String authorize_user_id = "me";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = List.of(GmailScopes.MAIL_GOOGLE_COM);

    @Value("${gmail.local.port}")
    private int localServerPort;

    @Value("${gmail.token.path}")
    private String gmailTokenPath;

    private Gmail gmailService;

    public GmailService() {
        // 空构造函数，用于Spring依赖注入
    }

    @PostConstruct
     public void initializeGmailService() {
        try {
            log.info("start init Gmail service");
            // Build a new authorized API client service.
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Profile profile = gmailService.users().getProfile("me").execute();
            log.info("Gmail service initialized successfully, user email: {}", profile.getEmailAddress());

            String result = searchEmails("me", "after:2025/06/10 before:2025/06/16", 10);
            log.info("searchEmails result: {}", result);

        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize Gmail service: {}", e.getMessage(), e);
            throw new RuntimeException("Gmail service initialization failed", e);
        }
    } 

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));


        log.info("gmailTokenPath: {}", gmailTokenPath);
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(gmailTokenPath)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(localServerPort).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(authorize_user_id);
    }


    @Tool(description = "删除指定ID的Gmail邮件")
    public String deleteEmail(
            @ToolParam(description = "用户邮箱地址，如example@gmail.com") String userId,
            @ToolParam(description = "要删除的邮件ID") String messageId) {
        try {
            gmailService.users().messages().delete(userId, messageId).execute();
            return "邮件删除成功: " + messageId;
        } catch (IOException e) {
            return "邮件删除失败: " + e.getMessage();
        }
    }

    @Tool(description = "批量删除Gmail邮件")
    public String batchDeleteEmails(
            @ToolParam(description = "用户邮箱地址，如example@gmail.com") String userId,
            @ToolParam(description = "要删除的邮件ID列表，用逗号分隔") String messageIds) {
        try {
            String[] ids = messageIds.split(",");
            com.google.api.services.gmail.model.BatchDeleteMessagesRequest request = new com.google.api.services.gmail.model.BatchDeleteMessagesRequest();
            request.setIds(java.util.Arrays.asList(ids));
            gmailService.users().messages().batchDelete(userId, request).execute();
            return "批量删除成功，共删除 " + ids.length + " 封邮件";
        } catch (IOException e) {
            return "批量删除失败: " + e.getMessage();
        }
    }

    @Tool(description = "查询Gmail邮件")
    public String searchEmails(
            @ToolParam(description = "用户邮箱地址，如example@gmail.com") String userId,
            @ToolParam(description = "查询条件，如'in:inbox is:unread'，留空则返回收件箱所有邮件") String query,
            @ToolParam(description = "返回结果数量，默认10") Integer maxResults) {
        try {
            if (maxResults == null) maxResults = 10;
            Gmail.Users.Messages.List request = gmailService.users().messages().list(userId);
            if (query != null && !query.isEmpty()) {
                request.setQ(query);
            }
            com.google.api.services.gmail.model.ListMessagesResponse response = request.setMaxResults((long) maxResults).execute();
            StringBuilder result = new StringBuilder();
            result.append("找到 ").append(response.getMessages() != null ? response.getMessages().size() : 0).append(" 封邮件:\n");
            List<String> msgIDs = new ArrayList<>();
            if (response.getMessages() != null) {
                for (com.google.api.services.gmail.model.Message message : response.getMessages()) {
                    msgIDs.add(message.getId());
                }
            }
            result.append("msgIDs:").append(String.join(", ", msgIDs));
            return result.toString();
        } catch (IOException e) {
            return "查询邮件失败: " + e.getMessage();
        }
    }

    
}