package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.commons.lang3.ObjectUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

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
    //RestTemplate restTemplate = new RestTemplate();

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
        // Get response code
        String resourceURL = request.getHeader(URL_HEADER);
        String responseFromUrl = null;

        if (resourceURL != null) {
            //ResponseEntity<String> responseCall = restTemplate.getForEntity(resourceURL, String.class);
            //responseFromUrl = responseCall.getBody();
        }

        response.getWriter().println(ObjectUtils.firstNonNull(responseFromUrl,"Sample Response"));
    }

    private void handleCronTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle WorkerTier tasks here.
        response.getWriter().println("Process Task Here.");
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        //response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        // Get response code
        String responseCode = ObjectUtils.firstNonNull(baseRequest.getHeader(RESPONSE_CODE_HEADER),DEFAULT_RESPONSE);
        response.setStatus(Integer.valueOf(responseCode));

        // Apply delay
        String responseDelay = ObjectUtils.firstNonNull(baseRequest.getHeader(DELAY_HEADER),DEFAULT_DELAY);
        try {
            Thread.sleep(Integer.valueOf(responseDelay));
        } catch (InterruptedException e) {
            e.printStackTrace();
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
