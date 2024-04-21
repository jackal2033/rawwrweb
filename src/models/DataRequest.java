package models;

public class DataRequest implements Request {
    private final String mediatype;
    private final boolean encoded;
    private final String data;
    public DataRequest(String url) {
        // data://mediatype;encoded,data
        String[] parts = url.split("://");
        if (!parts[0].equals("data"))
            throw new RuntimeException("Invalid scheme of: " + parts[0]);

        // Split on comma to find options and data
        parts = parts[1].split(",", 2);
        // Decode tags
        data = parts[1].replace("&gt;", ">")
                .replace("&lt;", "<");


        if (parts[0].isEmpty()) {
            mediatype = "";
            encoded = false;
        } else {
            if (parts[0].contains(";")) {
                parts = parts[0].split(";");
                mediatype = parts[0];
                // I don't think it matters what text is present in encoding
                // if it's there... it's base64
                encoded = true;
            } else {
                mediatype = parts[0];
                encoded = false;
            }
        }

        System.out.format("%s %s %s\n", mediatype, encoded, data);
    }
    @Override
    public void request() {

    }

    @Override
    public String getBody() {
        return this.data;
    }

    @Override
    public String getContent() {
        return this.data;
    }
}
