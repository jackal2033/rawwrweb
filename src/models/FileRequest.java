package models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileRequest implements Request {
    private final String path;
    private String content;
    public FileRequest(String url) {
        String[] parts = url.split("://", 2);
        if (!parts[0].equals("file"))
            throw new RuntimeException("Invalid scheme of: " + parts[0]);
        path = parts[1];
    }
    @Override
    public void request() {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(String.format("%s\n", line));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        content = builder.toString();
    }

    @Override
    public String getBody() {
        return content;
    }

    @Override
    public String getContent() {
        return content;
    }
}
