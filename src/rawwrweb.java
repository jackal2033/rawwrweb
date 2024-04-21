import models.Url;
public class rawwrweb {
    public static void main(String[] args) {
        String site = "https://browser.engineering/examples/example1-simple.html";
        site = "file://data/testfile.txt";
        site = "data://text/html;base64,&lt;div&gt;hello&lt;/div&gt;";
        //site = "file://";
        if (args.length >= 1)
            site = args[0];

        Url url = new Url(site);
        url.show();
    }
}
