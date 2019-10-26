package com.github.liuhuagui.smalldoc.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.liuhuagui.smalldoc.core.DefaultSmallDocletImpl;
import com.github.liuhuagui.smalldoc.core.SmallDocContext;
import com.github.liuhuagui.smalldoc.properties.SmallDocProperties;
import com.github.liuhuagui.smalldoc.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SmallDocServlet extends HttpServlet {
    public static final String TITLE = "\\$\\{title}";
    public static final String DOCJSON = "\\$\\{docJSON}";
    public static final String DEFAULT_SERVLET_PATH = "smalldoc";

    private static Logger log = LoggerFactory.getLogger(SmallDocServlet.class);
    private String resourcePath = "smalldoc/support/http/resources";

    private SmallDocProperties smallDocProperties;
    private SmallDocContext smallDocContext;
    private CompletableFuture<JSONObject> docJSONFuture;

    public SmallDocServlet(SmallDocProperties smallDocProperties) {
        this.smallDocProperties = smallDocProperties;
        this.smallDocContext = new SmallDocContext(smallDocProperties);
    }

    @Override
    public void init() throws ServletException {
        this.docJSONFuture = CompletableFuture.supplyAsync(() -> {
            smallDocContext.execute(new DefaultSmallDocletImpl(smallDocContext));
            return smallDocContext.getDocsJSON();
        });
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String baseUrl = getURL(req);

        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String requestURI = req.getRequestURI();
        resp.setCharacterEncoding("utf-8");
        if (contextPath == null) { // root context
            contextPath = "";
        }
        String uri = contextPath + servletPath;
        String path = requestURI.substring(contextPath.length() + servletPath.length());

        if (path.equals("/")) {
            resp.setContentType("text/html; charset=utf-8");
            String filePath = getFilePath("/index.html");
            String text = Utils.readFromResource(filePath);
            text = text.replaceFirst(TITLE, smallDocProperties.getProjectName() == null ? DEFAULT_SERVLET_PATH : smallDocProperties.getProjectName());
            try {
                //做转义防止前端JSON解析失败
                String escapeStr = JSON.toJSONString(docJSONFuture.get().toString(), SerializerFeature.WriteSlashAsSpecial);
                //去除首尾引号
                escapeStr = escapeStr.substring(1,escapeStr.length()-1);
                text = text.replaceFirst(DOCJSON, escapeStr);
            } catch (InterruptedException | ExecutionException e) {
                log.error("", e);
            }
            resp.getWriter().write(text);
            return;
        }

        returnResourceFile(path, uri, resp);
    }

    private String getURL(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder(req.getScheme());
        sb.append("://");
        sb.append(req.getServerName());
        sb.append(":");
        sb.append(req.getServerPort());

        String contextPath = req.getContextPath();
        sb.append(contextPath);
        if (!contextPath.endsWith("/"))
            sb.append("/");

        String baseUrl = sb.toString();
        smallDocContext.getDocsJSON().put("url", baseUrl);
        return baseUrl;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        getURL(req);

        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String requestURI = req.getRequestURI();
        resp.setCharacterEncoding("utf-8");
        if (contextPath == null) { // root context
            contextPath = "";
        }
        String path = requestURI.substring(contextPath.length() + servletPath.length());

        if (path.equals("") || path.equals("/")) {
            resp.setContentType("application/json;charset=UTF-8");
            try {
                resp.getWriter().write(docJSONFuture.get().toJSONString());
            } catch (InterruptedException | ExecutionException e) {
                log.error("", e);
            }
            return;
        }
    }

    private String getFilePath(String fileName) {
        return resourcePath + fileName;
    }

    private void returnResourceFile(String fileName, String uri, HttpServletResponse response)
            throws ServletException,
            IOException {
        if (fileName.equals("")) {
            response.sendRedirect(uri + "/");
            return;
        }
        String filePath = getFilePath(fileName);

        if (filePath.endsWith(".html")) {
            response.setContentType("text/html; charset=utf-8");
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".ico")) {
            byte[] bytes = Utils.readByteArrayFromResource(filePath);
            if (bytes != null) {
                response.getOutputStream().write(bytes);
            }
            return;
        }

        String text = Utils.readFromResource(filePath);
        if (text == null) {
            response.sendRedirect(uri + "/");
            return;
        }
        if (fileName.endsWith(".css")) {
            response.setContentType("text/css;charset=utf-8");
        } else if (fileName.endsWith(".js")) {
            response.setContentType("text/javascript;charset=utf-8");
        }
        response.getWriter().write(text);
    }

}
