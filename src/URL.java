import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class URL {
    private final String host;
    private final String scheme;
    private final String url;
    private final String path;
    private final int port;
    private ArrayList<String> body;
    private ArrayList<String> requestBody;
    private String httpVersion;
    private int statusCode;
    private String statusDesc;
    private ArrayList<String> requestHeaders;
    private boolean dateEncoded = false;
    private String dataMediaType;

    private HashMap<String, String> headers;
    public static void main(String[] args) {
        String site = "https://browser.engineering/examples/example1-simple.html";
        //site = "file://";
        if (args.length >= 1)
            site = args[0];

        URL url = new URL(site);
        url.request();

        //url.processContent();
        url.show();
        System.out.println(url.getRequestStatus());
    }
    public URL(String url) {
        System.out.println(url);

        // Grab the Scheme (http/https)
        // scheme://host:port/url
        String[] parts = url.split("://", 2);
        this.scheme = parts[0];

        // Find the Address and Path of request
        // host:port/url
        if (this.scheme.equals("file") || this.scheme.equals("data")) {
            this.port = -1;
            this.url = "";
            this.host = "localhost";

        } else if (scheme.startsWith("http")){
            parts = parts[1].split("/", 2);
            if (parts[1].indexOf(':') != -1) {
                // scheme://host:port/url
                this.host = parts[0];
                parts = parts[1].split(":");
                this.url = parts[0];
                this.port = Integer.parseInt(parts[1]);
            } else {
                this.host = parts[0];
                this.url = parts[1];
                // HTTP uses Port 22; HTTPS uses Port 8080

                if (scheme.equals("http"))
                    port = 22;
                else if (scheme.equals("https"))
                    port = 443;
                else
                    port = -1;
            }
        } else {
            throw new RuntimeException(String.format("Invalid protocol: %s", this.scheme));
        }
        this.path = "/" + this.url;

        // Only populate requestHeaders if we're making a WEB request
        requestHeaders = new ArrayList<>();
        if (this.scheme.equals("http") || this.scheme.equals("https")) {
            requestHeaders.add("");
            requestHeaders.add(String.format("GET %s HTTP/1.1", this.path));
            requestHeaders.add(String.format("Host: %s", this.host));
            requestHeaders.add("Connection: Close");
            requestHeaders.add("User-Agent: rawwrweb");
            requestHeaders.add("");
        }
        System.out.format("Scheme: %s\nHost: %s\nURL: %s\nPath: %s\nPort: %s\n", this.scheme, this.host, this.url, this.path, this.port);
    }

    public void request() {
        requestBody = new ArrayList<>();
        String request = String.join("\r\n", requestHeaders);
        switch (this.scheme) {
            case "http":
                makeHttpRequest(request);
                break;
            case "https":
                makeHttpsRequest(request);
                break;
            case "file":
                getFile();
                break;
            case "data":
                throw new RuntimeException("Data request not yet implemented");
            default:
                throw new RuntimeException(String.format("Invalid scheme: %s", this.scheme));
        }
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
                requestBody.add(line);

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
                requestBody.add(line);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void processContent() {
        body = new ArrayList<>();
        if (scheme.startsWith("http"))
            processWebContent();
        else if(scheme.equals("file"))
            processFileContent();
    }

    public void processWebContent() {
        // HTTPversion StatusCode StatusDesc
        String statusLine = requestBody.getFirst();
        String[] statusParts = statusLine.split(" ");
        httpVersion = statusParts[0];
        statusCode = Integer.parseInt(statusParts[1]);
        statusDesc = statusParts[2];


        // After the statusDescription comes the responseHeaders
        String[] headerParts;
        headers = new HashMap<>();

        boolean isHeader = true;
        String line;
        for (int i = 1; i < requestBody.size(); i++) {
            line = requestBody.get(i);
            if (isHeader) {
                if (line.isEmpty() || line.equals("\r\n") || line.equals("\n")) {
                    isHeader = false;
                } else {
                    headerParts = line.split(": ", 2);
                    headers.put(headerParts[0].toLowerCase(), headerParts[1]);
                }
            } else {
                if (!line.isBlank())
                    body.add(line.strip());
            }
        }
    }

    public void processFileContent() {
        try(
                BufferedReader reader = new BufferedReader(new FileReader(path))
                ) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                body.add(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void show() {
        this.processContent();
        boolean inTag = false;
        for (String line: body) {
            for (Character c: line.toCharArray()) {
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
        }
    }

    private void getFile() {

    }

    public HashMap<String, String> getHeaders() {
        return this.headers;
    }

    public ArrayList<String> getRequestHeaders() {
        return this.requestHeaders;
    }
    public String getRequestStatus() {
        return String.format("%s %s %s", this.httpVersion, this.statusCode, this.statusDesc);
    }
}
