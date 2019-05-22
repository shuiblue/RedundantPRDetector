package main.Util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shuruiz on 10/19/17.
 */
public class IO_Process {
    String token;
    String user, pwd, myUrl, ghtpwd, ghtUrl;
    static String github_api_repo = "https://api.github.com/repos/";
    static String github_url = "https://github.com/";
    String current_dir = System.getProperty("user.dir");
    static String working_dir, pr_dir, output_dir, clone_dir,graph_dir;
    final int batchSize = 500;
    HashSet<String> stopFileSet = new HashSet<>();
    HashSet<String> sourceCodeSuffix = new HashSet<>();

    public IO_Process() {

        try {
            String[] paramList = readResult(current_dir + "/input/dir-param.txt").split("\n");
            working_dir = paramList[0];
            pr_dir = working_dir + "queryGithub/";
            output_dir = working_dir + "ForkData/";
            graph_dir = output_dir + "ClassifyCommit_new/";
            clone_dir = output_dir + "clones/";
            myUrl = paramList[1];
            user = paramList[2];
            pwd = paramList[3];
            ghtpwd = paramList[4];
            ghtUrl = paramList[5];
            token = readResult(current_dir + "/input/token.txt").trim();
            stopFileSet.addAll(Arrays.asList(readResult(current_dir + "/input/StopFiles.txt").split("\n")));
            sourceCodeSuffix.addAll(Arrays.asList(readResult(current_dir + "/input/sourceCode.txt").split("\n")));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void writeTofile(String content, String filepath) {
        try {
            File file = new File(filepath);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rewriteFile(String content, String filepath) {

        try {
            File file = new File(filepath);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * this function read the content of the file from filePath, and ready for comparing
     *
     * @param filePath file path
     * @return content of the file
     * @throws IOException e
     */
    public String readResult(String filePath) throws IOException {
        BufferedReader result_br = new BufferedReader(new FileReader(filePath));
        String result;
        try {
            StringBuilder sb = new StringBuilder();
            String line = result_br.readLine();

            while (line != null) {

                sb.append(line);
                sb.append(System.lineSeparator());

                line = result_br.readLine();
            }
            result = sb.toString();
        } finally {
            result_br.close();
        }
        return result;
    }

    public List<List<String>> readCSV(String filePath) {
        File csvFile = new File(filePath);
        return readCSV(csvFile);
    }

    public List<List<String>> readCSV(File csvFile) {
        List<List<String>> rows = new ArrayList<>();
        try (InputStream in = new FileInputStream(csvFile);) {
            CSV csv = new CSV(true, ',', in);
            while (csv.hasNext()) {
                List<String> fields = new ArrayList<>(csv.next());
                rows.add(fields);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public void deleteDir(File file) throws IOException {

        FileUtils.deleteDirectory(file);
    }

    public static void fileCopy(String sourceFile, String destinationFile) throws IOException {
        Path source = Paths.get(sourceFile);
        Path destination = Paths.get(destinationFile);

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }




    public boolean isStopFile(String fileName) {
        for (String file : stopFileSet) {
            if (fileName.toLowerCase().contains(file)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSourceCode(String fileName) {
        for (String suffix : sourceCodeSuffix) {
            if (fileName.toLowerCase().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDuplicatePR(String comment) {

        Pattern p1 = Pattern.compile("(clos(e|ed|ing)|dup(licate(d)?|e)?|super(c|s)ee?ded?|obsoleted?|replaced?|redundant|better (implementation|solution)" +
                "|solved|fixed|done|going|addressed|already)[ ]{1,}(by|in|with|as|of|in favor of|at|since|via)?[ ]{1,}" +
                "(#|http(s)?://github.com/([\\w\\.-]+/){2}(pull|issues)(#(\\w)+)?/)(\\d+)");  // remove merged, landed
        Matcher m1 = p1.matcher(comment);
        if (m1.find()) {
            System.out.println(m1.group());
            System.out.println("match 1.");
            return true;
        }
        Pattern p2 = Pattern.compile("(#|http(s)?://github.com/([\\w\\.-]+/){2}(pull|issues)(#(\\w)+)?/)(\\d+).{1,}" +
                "(better (implementation|solution)|dup(licate(d)?|e)?|(fixe(d|s)?|obsolete(d|s)?|replace(d|s)?))");
        Matcher m2 = p2.matcher(comment);
        if (m2.find()) {
            System.out.println(m2.group());
            System.out.println("match 2.");
            return true;
        }
        Pattern p3 = Pattern.compile("dup(?:licated?)?:?[ ]{1,}(#|http(s)?://github.com/([\\w\\.-]+/){2}(pull|issues)(#(\\w)+)?/)(\\d+)");
        Matcher m3 = p3.matcher(comment);
        if (m3.find()) {
            System.out.println(m3.group());
            System.out.println("match 3.");
            return true;
        }
        return false;
    }
    public boolean isDuplicateComment(String comment) {
        Pattern p1 = Pattern.compile("(dup(licate(d)?|e)?|super(c|s)(ee)?(ded)?(edes)?|obsoleted?|replaced?|redundant|better (implementation|solution)" +
                "|solved|going|addressed|already)[ ]{1,}(by|in|with|as|of|in favor of|at|since|via)?");
        //      "|solved|fixed|done|going|addressed|already)[ ]{1,}(by|in|with|as|of|in favor of|at|since|via)?");// remove merged, landed, fixed, done, clos(e|ed|ing)|
        Matcher m1 = p1.matcher(comment);
        if (m1.find()) {
            System.out.println(m1.group());
            System.out.println("match 1.");
            return true;
        }
        Pattern p2 = Pattern.compile(
//                "(better (implementation|solution)|dup(licate(d)?|e)?|(fixe(d|s)?|obsolete(d|s)?|replace(d|s)?))"); //todo remove fixes for now
                "(better (implementation|solution)|dup(licate(d)?|e)?|obsolete(d|s)?|replace(d|s)?)"); //todo remove fixes for now
        Matcher m2 = p2.matcher(comment);
        if (m2.find()) {
            System.out.println(m2.group());
            System.out.println("match 2.");
            return true;
        }
        Pattern p3 = Pattern.compile("dup(?:licated?)?:?[ ]{1,}");
        Matcher m3 = p3.matcher(comment);
        if (m3.find()) {
            System.out.println(m3.group());
            System.out.println("match 3.");
            return true;
        }
        return false;
    }







}




