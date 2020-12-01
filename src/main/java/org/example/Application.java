package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import org.apache.commons.lang3.ObjectUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Application extends AbstractHandler
{
    private static final int PAGE_SIZE = 3000;
    private static final String INDEX_HTML = loadIndex();
    private static final String DEFAULT_RESPONSE = "200";
    private static final String DEFAULT_DELAY = "0";
    private static final String RESPONSE_CODE_HEADER = "responseCode";
    private static final String DELAY_HEADER = "responseDelay";
    private static final String URL_HEADER = "urlHeader";
    private static final String CONTENT_TYPE = "contentType";
    private static final String RESPONSE_CONTENT = "responseContent";
    private static int MAX_RESPONSE_DELAY = 35000;

    private static String loadIndex() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Application.class.getResourceAsStream("/index.html")))) {
            final StringBuilder page = new StringBuilder(PAGE_SIZE);
            String line = null;

            while ((line = reader.readLine()) != null) {
                page.append(line);
            }

            return page.toString();
        } catch (final Exception exception) {
            return getStackTrace(exception);
        }
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter, true);
        throwable.printStackTrace(printWriter);

        return stringWriter.getBuffer().toString();
    }

    private static int getPort() {
        try {
            return Integer.parseInt(System.getenv().get("PORT"));
        } catch (Exception e) {
            return 5000;
        }
    }

    private void handleHttpRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle HTTP requests here.
        String pathInfo = request.getPathInfo();

        // Get response code
        String resourceURL = ObjectUtils.firstNonNull(request.getHeader(URL_HEADER),request.getParameter(URL_HEADER));
        String contentType = ObjectUtils.firstNonNull(request.getHeader(CONTENT_TYPE),request.getParameter(CONTENT_TYPE));
        String responseFromUrl = null;

        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        if (resourceURL != null) {

            try {
                com.squareup.okhttp.Request httpRequest = new com.squareup.okhttp.Request.Builder()
                        .url(resourceURL)
                        .build(); // defaults to GET

                Response httpResponse = client.newCall(httpRequest).execute();
                responseFromUrl = httpResponse.body().string();
                try {
                    JsonNode jsonNode = mapper.readValue(httpResponse.body().byteStream(), JsonNode.class);
                    responseFromUrl = jsonNode.toString();
                } catch (Exception e) {

                }

            } catch (Exception e){

            }
        } else {

                responseFromUrl = ObjectUtils.firstNonNull(request.getHeader(RESPONSE_CONTENT),request.getParameter(RESPONSE_CONTENT));
                try {
                    JsonNode jsonNode = mapper.readValue(responseFromUrl, JsonNode.class);
                    responseFromUrl = jsonNode.toString();
                } catch (Exception e) {

                }

        }

        response.getWriter().println(ObjectUtils.firstNonNull(responseFromUrl,INDEX_HTML));
        response.setContentType(ObjectUtils.firstNonNull(contentType,"text/html;charset=utf-8"));
    }

    private void handleCronTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle WorkerTier tasks here.
        response.getWriter().println("Process Task Here.");
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        baseRequest.setHandled(true);

        // Get response code
        String responseCode = ObjectUtils.firstNonNull(baseRequest.getHeader(RESPONSE_CODE_HEADER),baseRequest.getParameter(RESPONSE_CODE_HEADER),DEFAULT_RESPONSE);
        response.setStatus(Integer.valueOf(responseCode));

        // Apply delay
        String responseDelay = ObjectUtils.firstNonNull(baseRequest.getHeader(DELAY_HEADER),baseRequest.getParameter(DELAY_HEADER),DEFAULT_DELAY);
        try {
            if (Integer.valueOf(responseDelay)>MAX_RESPONSE_DELAY) {
                throw new IllegalArgumentException("Response delay can not be more than 35 seconds");
            }

            Thread.sleep(Integer.valueOf(responseDelay));
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Response delay must be an integer:\n" + e);
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo.equalsIgnoreCase("/crontask")) {
            handleCronTask(request, response);
        } else {
            handleHttpRequest(request, response);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Server server = new Server(getPort());
        server.setHandler(new Application());
        server.start();
        server.join();
    }
}
