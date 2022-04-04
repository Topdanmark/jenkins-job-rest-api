package dk.topdanmark;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Main {
    private static final int BUFFER_SIZE = 4096;
    private static final byte[] BUFFER = new byte[8192];
    private static final String JENKINS_HOST = "http://server.domain.local/jenkins";
    private static final String username = "aUserWithAdminPermissions";
    private static final String password = "relevantPassword";

    public static void main(String[] args) {
        String crumb = getCrumb();
        System.out.println("crumb: " + crumb);
        Path aPath = getRemoteJob(crumb);
        System.out.println("Path:\n" + aPath);
        updateRemoteJob(crumb);
    }

    private static String getCrumb() {
        String crumb = "";
        try {
            URL url = new URL(JENKINS_HOST + "/crumbIssuer/api/json");
            String authStr = username + ":" + password;
            String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + encoding);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JSONObject obj = new JSONObject(content.toString());
            crumb = obj.getString("crumb");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return crumb;
    }

    private static Path getRemoteJob(String crumb) {
        Path testProjectConfigPath = null;
        try {
            URL url = new URL(JENKINS_HOST + "/job/test/config.xml");
            String authStr = username + ":" + password;
            String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Jenkins-crumb", crumb);
            connection.setRequestProperty("Authorization", "Basic " + encoding);

            testProjectConfigPath = Files.createTempFile("test", "config.xml");
            InputStream inputStream = connection.getInputStream();
            OutputStream outputStream = Files.newOutputStream(testProjectConfigPath);
            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testProjectConfigPath;
    }

    private static void updateRemoteJob(String crumb) {
        try {
            URL url = new URL(JENKINS_HOST + "/job/test/config.xml");
            String authStr = username + ":" + password;
            String encoding = Base64.getEncoder().encodeToString(authStr.getBytes("utf-8"));
            Path configPath = Paths.get("config.xml"); // A path to the modified project configuration file.

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Jenkins-Crumb", crumb);
            connection.setRequestProperty("Authorization", "Basic " + encoding);

            try {
                InputStream inputStream = Files.newInputStream(configPath);
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                int c;
                while ((c = inputStream.read(BUFFER, 0, BUFFER.length)) > 0) {
                    wr.write(BUFFER, 0, c);
                }
            } catch (IOException exception) {
                System.out.println("IOException:\n" + exception.getMessage());
            }

            InputStream content = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
