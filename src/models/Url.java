package models;

public class Url {
    private Request request;
    public Url(String url) {
        String scheme = null;
        for (int right = 0; right < url.length(); right++) {
            if (url.charAt(right) == ':') {
                scheme = url.substring(0, right);
                break;
            }
        }
        if (scheme == null)
            throw new RuntimeException("Unable to find scheme within: " + url);

        if (scheme.equals("http") || scheme.equals("https"))
            request = new WebRequest(url);
        else if(scheme.equals("data"))
            request = new DataRequest(url);
        else if (scheme.equals("file"))
            request = new FileRequest(url);
    }

    public void show() {
        request.request();

        boolean inTag = false;
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
    }
}
