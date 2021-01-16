package cz.vse.miraql.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import com.github.owlcs.ontapi.Ontology;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
public class Util {

    private Util() {
        throw new IllegalAccessError("Utility class");
    }


    public static String getFileContent(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    public static String getOntologySourceString(Ontology ontology) throws OWLOntologyStorageException, UnsupportedEncodingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        ontology.saveOntology(new RDFXMLDocumentFormat(), os); // if this format is used, the engine will use it's own namespace prefixes
        ontology.saveOntology(os);
        return os.toString("UTF-8");
    }

    public static String getFileNameFromIri(String ontologyDomain, String iri) {
        String fileName = iri
                .replace(ontologyDomain, "")
                .replaceAll("/", "-")
                .replaceAll("#", "-");
        if (fileName.endsWith("-")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        fileName += ".owl";
        return fileName;
    }

    public static List<String> getImportIriMatches(String ontologyText) {
        Matcher m = Pattern.compile("<owl:imports rdf:resource=.*/>").matcher(ontologyText);
        List<String> matches = new ArrayList<>();
        while (m.find()) {
            String importIri = m.group()
                    .replace("<owl:imports rdf:resource=\"", "")
                    .replace("\"/>", "");
            matches.add(importIri);
        }
        return matches;
    }

    public static String fetchTextResponse(String uri) throws IOException {
        return sendGetRequest(uri, "");
    }
//    public static String fetchUserInfoTextResponse(String uri, String username, String password) throws IOException {
//        return sendGetRequest(uri, username, password, "");
//    }
    public static String fetchLibraryTextResponse(String uri) throws IOException {
        return sendGetRequest(uri, "application/rdf+xml");
    }
    private static String sendGetRequest(String uri, String mimeType) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        if (username.length() > 0 && password.length() > 0) {
//            String userCredentials = username + ":" + password;
//            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
//            connection.setRequestProperty("Authorization", basicAuth);
//        }
        connection.setRequestMethod("GET");
        if (mimeType.length() > 0) {
            connection.setRequestProperty("Accept", mimeType);
        }
        StringBuilder content;
        try (BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            content = new StringBuilder();
            while ((line = input.readLine()) != null) {
                // Append each line of the response and separate them
                content.append(line);
                content.append(System.lineSeparator());
            }
        } finally {
            connection.disconnect();
        }
        return content.toString();
    }

//    public static InputStream getFileInputStream(String path) throws FileNotFoundException {
//        return new FileInputStream(new File(path));
//    }
//
//    public static String getResourceFileContent(String fileName) throws IOException {
//        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
//        String f = Objects.requireNonNull(classLoader.getResource(fileName)).getFile();
//        File file = new File(f);
//        return new String(Files.readAllBytes(file.toPath()));
//    }

    public static String httpPostJson(String url, String payload, String bearerToken) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader(HttpHeaders.ACCEPT, "application/json");
        post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (StringUtils.isNotBlank(bearerToken)) {
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        StringEntity entity = new StringEntity(payload);
        post.setEntity(entity);

        CloseableHttpResponse response = client.execute(post);
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        client.close();
        return responseString;
    }

    public static String createPullRequest(String gitServicePrefix, String repositoryName, String newBranchName, String changeDescription, List<String> reviewers) throws IOException {
        return Util.httpPostJson(
                gitServicePrefix + "/" + repositoryName + "/pull-requests",
                "{\n" +
                "    \"title\": \"" + newBranchName + "\",\n" +
                "    \"description\": \"" + changeDescription.replaceAll("\"", "'") + "\",\n" +
                "    \"state\": \"OPEN\",\n" +
                "    \"open\": true,\n" +
                "    \"closed\": false,\n" +
                "    \"fromRef\": {\n" +
                "        \"id\": \"refs/heads/" + newBranchName + "\",\n" +
                "        \"repository\": {\n" +
                "            \"slug\": \"" + repositoryName + "\",\n" +
                "            \"name\": null,\n" +
                "            \"project\": {\n" +
                "                \"key\": \"PROJECT\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"toRef\": {\n" +
                "        \"id\": \"refs/heads/master\",\n" +
                "        \"repository\": {\n" +
                "            \"slug\": \"" + repositoryName + "\",\n" +
                "            \"name\": null,\n" +
                "            \"project\": {\n" +
                "                \"key\": \"PROJECT\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"locked\": false,\n" +
                "    \"reviewers\": [" +
                        reviewers.stream().map(r -> "{\"user\": {\"name\": \"" + r + "\"}}").collect(Collectors.joining(",")) +
                "    ],\n" +
                "    \"links\": {\n" +
                "        \"self\": [\n" +
                "            null\n" +
                "        ]\n" +
                "    }\n" +
                "}",
                "OTU5NTU5MjMwNjEyOm2KzszVt4eHIiGKC/fvtgP+5ZbQ"

        );
    }

//    public static String createPullRequestLink(String gitServicePrefix, String gitRepositoryName, String gitMainBranch, String newBranchName) {
//        return gitServicePrefix + "/" + gitRepositoryName + "/pull-requests?" +
//                "create&sourceBranch=refs/heads/" + newBranchName + "&targetBranch=refs/heads/" + gitMainBranch;
//    }

    public static String getShortUUID() {
        return UUID.randomUUID().toString().substring(24, 36);
    }
}
