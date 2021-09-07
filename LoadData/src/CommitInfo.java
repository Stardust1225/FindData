import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommitInfo {

    static List<String> token, func_list;

    public static void main(String args[]) {
        try {
            token = Files.readAllLines(Paths.get("access_token.txt"));
            func_list = Files.readAllLines(Paths.get("function_list.txt"));

            new CommitInfo();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public CommitInfo() throws Exception {
        List<String> repoList = Files.readAllLines(Paths.get("repo_list.txt"));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        ExecutorCompletionService<String> service = new ExecutorCompletionService<>(pool);

        int count = 0;
        for (String s : repoList) {
            service.submit(new LoadCommit(s));
            count++;
        }

        for (int i = 0; i < count; i++)
            System.out.println(new Date() + "\t" + service.take().get());

        pool.shutdown();
    }

    private class LoadCommit implements Callable<String> {

        String repo;

        public LoadCommit(String s) {
            this.repo = s;
        }

        @Override
        public String call() throws Exception {
            ArrayList<ArrayList<String>> commitList = new ArrayList<>();
            int page = 1, count = 0;
            String baseUrl = "https://api.github.com/repos/" + repo + "/commits?page=";

            ExecutorService pool = Executors.newFixedThreadPool(20);
            ExecutorCompletionService<ArrayList<String>> service = new ExecutorCompletionService<>(pool);

            JSONArray lastPageCommits = new JSONArray();
            while (true) {
                JSONArray commits = JSONArray.fromObject(new Internet(baseUrl + page, false).call());
                if (commits.size() <= 0)
                    break;
                if (page % 100 == 0)
                    System.out.println(new Date() + "\t" + repo + "\tpage\t" + page);
                page++;

                for (int i = 0; i < commits.size(); i++) {
                    String message = commits.getJSONObject(i).getJSONObject("commit").getString("message").toLowerCase();

                    if (message.contains("leak") || message.contains("ref count") || message.contains("reference count")) {
                        if (i > 0)
                            service.submit(new LoadCommitInfo(commits.getJSONObject(i).getString("url"),
                                    commits.getJSONObject(i - 1).getString("url")));
                        else if (lastPageCommits.size() > 0)
                            service.submit(new LoadCommitInfo(commits.getJSONObject(i).getString("url"),
                                    lastPageCommits.getJSONObject(lastPageCommits.size() - 1).getString("url")));
                        count++;
                        break;
                    }
                }

                lastPageCommits.clear();
                lastPageCommits.addAll(commits);
            }

            for (int i = 0; i < count; i++) {
                ArrayList<String> onecommit = service.take().get();
                if (onecommit.size() > 0)
                    commitList.add(onecommit);
                if (i % 1000 == 0)
                    System.out.println(new Date() + "\t" + repo + "\tcommit\t" + i);
            }

            File resultFile = new File(repo.replace("/", "-") + "_commit_info");
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(resultFile));
            outputStream.writeObject(commitList);
            outputStream.close();

            pool.shutdown();

            return repo;
        }
    }

    private class LoadCommitInfo implements Callable<ArrayList<String>> {
        String url, parent;

        public LoadCommitInfo(String s, String s1) {
            this.url = s;
            this.parent = s1;
        }

        @Override
        public ArrayList<String> call() throws Exception {
            JSONObject commit = JSONObject.fromObject(new Internet(url, false).call());
            JSONArray files = commit.getJSONArray("files");
            ArrayList<String> strings = new ArrayList<>();

            int flag = 0, i = 0;

            for (; i < files.size(); i++) {
                String filename = files.getJSONObject(i).getString("filename");
                if (!filename.endsWith(".py") || !filename.endsWith("cpp")
                        || !filename.endsWith("c") || !filename.endsWith("CXX"))
                    continue;

                if (files.getJSONObject(i).containsKey("patch") && files.getJSONObject(i).containsKey("raw_url")) {
                    String[] patch = files.getJSONObject(i).getString("patch").split("\n");
                    for (String oneline : patch) {
                        if (!oneline.startsWith("+") && !oneline.startsWith("-"))
                            continue;

                        for (String one_func : func_list)
                            if (oneline.contains(one_func)) {
                                flag = func_list.indexOf(one_func) + 1;
                                break;
                            }

                        if (flag != 0)
                            break;
                    }

                    if (flag != 0)
                        break;
                }
            }

            if (flag == 0 || i >= files.size())
                return strings;

            strings.add(commit.getJSONObject("commit").getString("message"));
            strings.add(commit.getString("html_url"));
            strings.add(url);
            strings.add(parent);
            strings.add(func_list.get(flag - 1));
            strings.add(files.getJSONObject(i).getString("filename"));
            strings.add(files.getJSONObject(i).getString("patch"));
            strings.add(files.getJSONObject(i).getString("raw_url"));
            return strings;
        }
    }
}