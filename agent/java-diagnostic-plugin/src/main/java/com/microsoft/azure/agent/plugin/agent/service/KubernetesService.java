package com.microsoft.azure.agent.plugin.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.agent.plugin.agent.UrlConfig;
import com.microsoft.azure.agent.plugin.agent.entity.PodInfo;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubernetesService {

    public volatile static String defaultPodName = null;
    public volatile static String defaultContainerName = null;
    public volatile static String defaultNamespace = "default";
    static ApiClient client = null;
    static CoreV1Api api = null;

    public static void initializeClient() {
        try {
            client = Config.defaultClient();
            io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
            api = new CoreV1Api();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Kubernetes client");
        }
    }

    public static void resetClient() {
        client = null;
        api = null;
        initializeClient();
    }

    public static List<String> listNamespaces() {
        try {
            List<String> namespaces = api.listNamespace(null, null, null, null, null, null, null, null, null, null, null).getItems().stream().map(n -> n.getMetadata().getName()).toList();
            return namespaces;
        } catch (Exception e) {
            System.out.println("error + " + e.getMessage());
            return new ArrayList<String>();
        }
    }

    public static List<PodInfo> getAllPods() throws Exception {
        try {
            List<PodInfo> podList = new ArrayList<>();
            V1PodList pods = getPods(defaultNamespace);
            if (pods == null) {
                return podList;
            }
            List<String> attachInfo = getAttachInfo();
            Map<String, String> attachMap = attachInfo.stream()
                    .map(info -> info.split(" "))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

            for (V1Pod pod : pods.getItems()) {
                String podName = pod.getMetadata().getName();
                String podIP = pod.getStatus().getPodIP();
                String podPhase = pod.getStatus().getPhase();
                String isAttach = attachMap.getOrDefault(podName, "Unknown");

                podList.add(new PodInfo(podName, podIP, podPhase, isAttach));
            }
            return podList;
        } catch (Exception e) {
            System.out.println("error + " + e.getMessage());
            return new ArrayList<PodInfo>();
        }
    }

    private static List<String> getAttachInfo() {
        try {
            Pair<Integer, String> response = callGetUrl(UrlConfig.getAttachInfoUrl() + "?namespace=" + defaultNamespace, 10000);
            if (response.getKey() == HttpStatus.SC_OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.getValue().trim(), List.class);
            }
            System.err.println("Failed to get attach info: " + response.getValue());

        } catch (Exception e) {
            System.err.println("Error occurred while getting attach info: " + e);
        }
        return new ArrayList<String>();
    }

    public static List<String> getContainerNames(String podName) {
        try {
            V1Pod pod = getPodByName(podName, defaultNamespace);
            if (pod == null) {
                return new ArrayList<String>();
            }
            return pod.getSpec().getContainers().stream().map(c -> c.getName()).toList();
        } catch (Exception e) {
            System.out.println("error + " + e.getMessage());
            return new ArrayList<String>();
        }
    }


    public static boolean attachAgent(String podName, String containerName) throws Exception {
        String requestBody = "{"
                + "\"podName\": \"" + podName + "\","
                + "\"containerName\": \"" + containerName + "\","
                + "\"namespace\": \"" + defaultNamespace + "\""
                + "}";

        // sometime, it need to pull diagnostic agent image first
        Pair<Integer, String> response = callPostUrl(UrlConfig.getAttachUrl(), requestBody, 20000);
        if (response.getKey() == HttpStatus.SC_OK) {
            return true;
        }
        throw new RuntimeException(response.getValue());
    }

    public static boolean addLog(String podName, String containerName, String className, String methodName) {
        String requestBody = "{"
                + "\"podName\": \"" + podName + "\","
                + "\"containerName\": \"" + containerName + "\","
                + "\"namespace\": \"" + defaultNamespace + "\","
                + "\"className\": \"" + className + "\","
                + "\"methodName\": \"" + methodName + "\""
                + "}";
        try {

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while adding around log: " + e.getMessage());
        }
        Pair<Integer, String> response = callPostUrl(UrlConfig.getAddLogsUrl(), requestBody, 5000);
        if (response.getKey() == HttpStatus.SC_OK) {
            return true;
        }
        throw new RuntimeException(response.getValue());
    }


    public static boolean removeLog(String podName, String containerName) {
        String requestBody = "{"
                + "\"podName\": \"" + podName + "\","
                + "\"containerName\": \"" + containerName + "\","
                + "\"namespace\": \"" + defaultNamespace + "\","
                + "\"deleteAll\": " + true
                + "}";
        Pair<Integer, String> response = callPostUrl(UrlConfig.getRemoveLogsUrl(), requestBody, 5000);
        if (response.getKey() == HttpStatus.SC_OK) {
            return true;
        }
        throw new RuntimeException(response.getValue());
    }

    private static Pair<Integer, String> callGetUrl(String url, int timeout) {
        try {
            java.net.URL obj = new java.net.URL(url);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
            con.setReadTimeout(timeout);
            con.setConnectTimeout(timeout);
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            // Determine the correct stream to read based on response code
            BufferedReader in;
            if (responseCode >= 200 && responseCode < 300) {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            // Read the response message
            String inputLine;
            StringBuilder responseMessage = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseMessage.append(inputLine).append("\n");
            }
            in.close();

            return Pair.of(responseCode, responseMessage.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException( "Error occurred: " + e.getMessage());
        }
    }


    private static Pair<Integer, String> callPostUrl(String url, String requestBody, int timeout) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Set the request method to POST
            con.setRequestMethod("POST");
            con.setDoOutput(true); // Allow sending data in the body
            con.setRequestProperty("Content-Type", "application/json"); // Set the content type to JSON
            con.setReadTimeout(timeout);
            con.setConnectTimeout(timeout);

            // Send the request data
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code and handle the response
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Check if the response code is 2xx (success)
            if (responseCode >= 200 && responseCode < 300) {
                // Read the response body
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    StringBuilder response = new StringBuilder();
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return Pair.of(responseCode, response.toString());
                }
            } else {
                // Read the error response body (4xx, 5xx)
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    StringBuilder response = new StringBuilder();
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse the error response JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> errorMap = objectMapper.readValue(response.toString(), Map.class);
                    String message = errorMap.getOrDefault("message", "Unknown error").toString();
                    return Pair.of(responseCode, message);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException( "Error occurred: " + e.getMessage());
        }
    }

    private static V1PodList getPods(String namespace) {
        try {
            V1PodList podList = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null, null);
            return podList;
        } catch (ApiException e) {
            System.err.println("ApiException when calling Kubernetes API: " + e.getResponseBody());
        } catch (Exception e) {
            System.err.println("Exception when initializing Kubernetes client: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static V1Pod getPodByName(String name, String namespace) {
        try {
            V1Pod pod = api.readNamespacedPod(name, namespace, null);
            return pod;

        } catch (ApiException e) {
            System.err.println("ApiException when calling Kubernetes API: " + e.getResponseBody() + " podName: " + name + " namespace: " + namespace);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception when initializing Kubernetes client: " + e.getMessage() + " podName: " + name + " namespace: " + namespace);
            e.printStackTrace();
        }
        return null;
    }
}