package models;

public class Url {
    private Request request;
    public Url(String url) {
        if (!url.contains("://"))
            throw new RuntimeException("Invalid URL, missing scheme: " + url);
        
        String[] parts = url.split("://");

        if (parts[0].contains("http"))
            request = new WebRequest(url);
        else if(parts[0].equals("data"))
            request = new DataRequest(url);
        else if (parts[0].equals("file"))
            request = new FileRequest(url);
        else
            throw new RuntimeException(String.format("Invalid scheme (%s) in url: %s", parts[0], url));
    }

    public void show() {
        request.request();

        if (request instanceof WebRequest && ((WebRequest) request).getViewSource()) {
            System.out.println(request.getBody());
        } else {
            boolean inTag = false;
            for (Character c : request.getBody().toCharArray()) {
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
}
