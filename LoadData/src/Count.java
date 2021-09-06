import net.sf.json.JSONObject;

import java.io.File;
import java.util.Scanner;

public class Count {
    public static void main(String args[]) {
        try {
            new Count();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Count() throws Exception {
        File file = new File("D:\\project\\Python-Memory-Leak\\result.json");
        Scanner scanner = new Scanner(file);
        StringBuffer buffer = new StringBuffer();
        while(scanner.hasNext())
            buffer.append(scanner.nextLine());

        JSONObject object = JSONObject.fromObject(buffer.toString());
        for(Object key : object.keySet()) {
            System.out.println(key + "\t" + object.getJSONArray((String) key).size());
        }
    }
}
