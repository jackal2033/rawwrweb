package models;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class WebRequest implements Request {
    public static void main(String[] args) {

        String site = "https://browser.engineering/examples/example1-simple.html";
        //site = "file://";
        if (args.length >= 1)
            site = args[0];

        WebRequest request = new WebRequest(site);
        request.request();

        request.processContent();
        boolean inTag = false;
        System.out.println();
        for (Character c: request.getBody().toCharArray()) {
            switch (c) {
                case '<':
                    inTag = true;
                    break;
                case '>':
                    inTag = false;
                    break;
                default:
                    if (!inTag)
                        System.out.print(c);
            }
        }
        System.out.println();
        System.out.println(request.getContent());
    }
    private final String url;
    private final int port;
    private final String host;
    private final String scheme;
    private final String path;
    private ArrayList<String> responseContent;
    private String responseBody;
    private String request;
    private ArrayList<String> requestHeaders;

    // Status Information about Request
    private String httpVersion;
    private int statusCode;
    private String statusDesc;

    // Headers sent within Response
    private HashMap<String, String> responseHeaders;
    private final boolean viewSource;
    public WebRequest(String url) {
        // Inefficient way of finding parts; but finals and loops don't mix

        /*
        currently dealing with (view-source):http(s)://example.com:8063/path/fi.le
        split on :// to get scheme
         */
        String[] parts = url.split("://", 2);
        if (parts[0].contains(":")) {
            String[] tmp = parts[0].split(":",2);
            if (tmp[0].equals("view-source"))
                viewSource = true;
            else
                throw new RuntimeException("Invalid scheme-prefix of: " + tmp[0]);
            this.scheme = tmp[1];
        } else {
            this.scheme = parts[0];
            viewSource = false;
        }
        assert this.scheme.equals("http") || this.scheme.equals("https");

        /*
        dealing with example.com:8063/path/fi.le
        split on / to get host and path
         */
        parts = parts[1].split("/", 2);
        this.url = parts[1];
        if (parts[0].contains(":")) {
            // there's a port specified
            parts = parts[0].split(":");
            this.host = parts[0];
            this.port = Integer.parseInt(parts[1]);
        } else {
            // There's no port specified, so set port based on scheme
            this.host = parts[0];
            this.port = this.scheme.equals("http") ? 22 : 443;
        }
        this.path = "/" + this.url;

        // Default Request Headers
        // It's required that our headers end in a newline
        requestHeaders = new ArrayList<>();
        requestHeaders.add(String.format("GET %s HTTP/1.1", this.path));
        requestHeaders.add(String.format("Host: %s", this.host));
        requestHeaders.add("Connection: Close");
        requestHeaders.add("User-Agent: rawwrweb");
        requestHeaders.add("");
    }

    @Override
    public void request() {
        /*
        Makes the web request and fetches any content.
        Uses the scheme to determine what socket type should be used.
         */
        responseContent = new ArrayList<>();
        request = String.join("\r\n", requestHeaders);
        responseHeaders = new HashMap<>();

        if (this.scheme.equals("https"))
            this.makeHttpsRequest(request);
        else if (this.scheme.equals("http"))
            makeHttpRequest(request);
        else
            throw new RuntimeException("Invalid scheme of: " + this.scheme);

        this.processContent();
    }

    private void makeHttpRequest(String request) {
        try (
                Socket sock = new Socket(this.host, 80);
                PrintWriter printWriter = new PrintWriter(sock.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        ) {
            printWriter.println(request);
            // System.out.println(request);
            String line;
            while ((line = reader.readLine()) != null)
                responseContent.add(line);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void makeHttpsRequest(String request) {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(this.host, this.port);
             PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            socket.startHandshake();
            printWriter.println(request);
            // System.out.println(request);
            String line;
            while ((line = reader.readLine()) != null)
                responseContent.add(line);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processContent() {
        // Status information comes first
        // httpVersion statusCode statusDesc (sep by spaces)
        String line = responseContent.getFirst();
        String[] parts = line.split(" ");
        httpVersion = parts[0];
        statusCode = Integer.parseInt(parts[1]);
        statusDesc = parts[2];

        // After the status info comes the response headers
        // then a newline
        // and then our body
        StringBuilder body = new StringBuilder();
        boolean isHeader = true;
        for (int i = 1; i < responseContent.size(); i++) {
            line = responseContent.get(i);
            if (isHeader) {
                if (line.isBlank() || line.equals("\r\n"))
                    isHeader = false;
                else {
                    // Header K/V are seperated by ": "
                    parts = line.split(": ", 2);
                    responseHeaders.put(parts[0], parts[1]);
                }
            } else {
                if (viewSource)
                    body.append(String.format("%s\n", line));
                else
                    body.append(String.format("%s\n", line.strip()));
            }
        }
        this.responseBody = body.toString();
    }
    @Override
    public String getBody() {
        return this.responseBody;
    }

    @Override
    public String  getContent() {
        return this.responseContent.toString();
    }

    public boolean getViewSource() {
        return this.viewSource;
    }
}
