import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.Callable;

public class Internet implements Callable<String> {

    String baseUrl;
    boolean addToken = true;

    public Internet(String s) {
        this.baseUrl = s;
    }

    public Internet(String s, boolean addToken) {
        this.baseUrl = s;
        this.addToken = addToken;
    }

    @Override
    public String call() throws Exception {
        baseUrl = baseUrl.replace(" ", "%20");
        HttpURLConnection connection = null;
        while (true) {
            try {
                InetSocketAddress addr = new InetSocketAddress("192.168.9.40", 1080);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, addr); // http 代理

                URL url = new URL(baseUrl + (addToken ? CommitInfo.token.get(Internet.getCount()) : ""));
                // connection = (HttpURLConnection) url.openConnection(proxy);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(2 * 60 * 1000);
                connection.setConnectTimeout(2 * 60 * 1000);

                int code = connection.getResponseCode();

                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(reader.readLine());
                    String s;
                    while ((s = reader.readLine()) != null)
                        buffer.append("\r\n" + s);
                    s = buffer.toString();
                    buffer = null;
                    connection.disconnect();
                    return s;
                } else if (code == 404) {
                    connection.disconnect();
                    System.out.println(url);
                    return null;
                } else if (code == 403) {
                    connection.disconnect();
                    Thread.sleep(5 * 60 * 1000);
                } else if (code == 502) {
                    connection.disconnect();
                    Thread.sleep(5 * 60 * 1000);
                } else if (code == 422) {
                    connection.disconnect();
                    System.out.println(url);
                    return null;
                } else {
                    connection.disconnect();
                    System.out.println(code + "  " + baseUrl);
                    Thread.sleep(20 * 1000);
                }
            } catch (Exception e) {
                if (connection != null)
                    connection.disconnect();
                e.printStackTrace();
                System.out.println(e.toString() + "  " + baseUrl);
                Thread.sleep(5 * 60 * 1000);
            }
        }
    }

    static int count = 0;

    public synchronized static int getCount() throws Exception {
        Thread.sleep(10);
        count = (count + 1) % CommitInfo.token.size();
        return count;
    }
}