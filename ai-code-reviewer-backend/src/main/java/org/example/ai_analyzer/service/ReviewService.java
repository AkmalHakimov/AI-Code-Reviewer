package org.example.ai_analyzer.service;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_analyzer.model.ReviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ReviewService {
    private final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final String OPENAI_API_KEY = "YOUR_KEY";

    private final WebClient webClient = WebClient.builder()
            .baseUrl(OPENAI_API_URL)
            .defaultHeader("Authorization", "Bearer " + OPENAI_API_KEY)
            .defaultHeader("Content-Type", "application/json")
            .build();

    /**
     * Analyze a single uploaded file
     */
    public Map<String, Object> reviewCode(MultipartFile file) throws Exception {
        String allCode = new String(file.getBytes(), StandardCharsets.UTF_8);
        return analyzeCodeWithAI(allCode, detectLanguageFromFilename(file.getOriginalFilename()));
    }

    /**
     * Analyze a public GitHub repository.
     */
    public Map<String, Object> reviewGithubRepo(String repoUrl) throws Exception {
        // Convert to downloadable zip URL (handle main or master)
        String baseUrl = repoUrl.replace(".git", "");
        String zipUrl = baseUrl + "/archive/refs/heads/main.zip";

        // fallback for repos that still use 'master'
        boolean foundAny = false;
        StringBuilder allCode = new StringBuilder();

        try (InputStream inputStream = new URL(zipUrl).openStream();
             ZipInputStream zipIn = new ZipInputStream(inputStream)) {

            allCode.append(extractSupportedFiles(zipIn));
            foundAny = allCode.length() > 0;

        } catch (FileNotFoundException e) {
            // try master
            zipUrl = baseUrl + "/archive/refs/heads/master.zip";
            try (InputStream inputStream = new URL(zipUrl).openStream();
                 ZipInputStream zipIn = new ZipInputStream(inputStream)) {

                allCode.append(extractSupportedFiles(zipIn));
                foundAny = allCode.length() > 0;

            } catch (FileNotFoundException ex2) {
                throw new FileNotFoundException("Repository not found or private. Please ensure it’s public.");
            }
        }

        if (!foundAny) {
            throw new Exception("No supported code files (.java, .py, .js, .ts, .cpp, .cs) found in the repository.");
        }

        // Detect primary language (based on file extensions)
        String language = detectLanguageFromRepo(allCode.toString());
        return analyzeCodeWithAI(allCode.toString(), language);
    }

    /**
     * Shared method: Send code to OpenAI and parse the structured result.
     */
    private Map<String, Object> analyzeCodeWithAI(String allCode, String language) {
        String prompt = buildEnhancedPrompt(allCode, language);

        try {
            Map<String, Object> body = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            Map<String, Object> response = webClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String aiText = extractAIResponse(response);

            // Clean markdown fences
            String cleaned = aiText.replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            JSONObject json = new JSONObject(cleaned);
            return json.toMap();

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summary", "AI returned unstructured response or parsing failed.");
            fallback.put("issues", List.of(Map.of("type", "General", "message", e.getMessage())));
            fallback.put("score", 0);
            return fallback;
        }
    }

    /**
     * Build the enhanced, language-aware prompt.
     */
    private String buildEnhancedPrompt(String allCode, String language) {
        return """
You are an experienced senior software engineer and code quality reviewer.

""" +
                "Analyze the following codebase written primarily in " + language + ".\n" +
                """
                It may contain multiple files and modules.
                
                ⚠️ Rules:
                - Return ONLY a valid JSON object — no markdown, no explanations, no ```json fences.
                - Be objective, detailed, and professional.
                - If certain sections don’t apply, omit them rather than leaving empty placeholders.
                
                The JSON must strictly follow this structure:
                {
                  "summary": "<3–5 sentence summary describing what the code does, its structure, and general design quality>",
                  "insights": {
                    "architecture": "<evaluate modularity, design patterns, and separation of concerns>",
                    "complexity": "<evaluate control flow, maintainability, and readability>",
                    "security": "<identify potential vulnerabilities or weak configurations>"
                  },
                  "issues": [
                    {"file": "<filename or 'General'>", "type": "<Performance|Security|Readability|Maintainability|BestPractice>", "message": "<detailed explanation>"}
                  ],
                  "recommendations": [
                    "<improvement 1>", "<improvement 2>", "<improvement 3>"
                  ],
                  "score": <integer 0–100>
                }
                
                Analyze this codebase in detail:
                """ + allCode;

    }

    /**
     * Detect the language based on file extension.
     */
    private String detectLanguageFromFilename(String filename) {
        if (filename == null) return "Unknown";
        filename = filename.toLowerCase();
        if (filename.endsWith(".java")) return "Java";
        if (filename.endsWith(".py")) return "Python";
        if (filename.endsWith(".js") || filename.endsWith(".ts")) return "JavaScript";
        if (filename.endsWith(".cpp") || filename.endsWith(".h")) return "C++";
        if (filename.endsWith(".cs")) return "C#";
        return "Unknown";
    }

    /**
     * Estimate dominant language based on text patterns.
     */
    private String detectLanguageFromRepo(String code) {
        if (code.contains("import java") || code.contains("@SpringBootApplication")) return "Java";
        if (code.contains("def ") || code.contains("import os")) return "Python";
        if (code.contains("function ") || code.contains("const ")) return "JavaScript";
        if (code.contains("#include")) return "C++";
        if (code.contains("using System")) return "C#";
        return "Unknown";
    }

    /**
     * Extract text from supported files within a ZIP.
     */
    private String extractSupportedFiles(ZipInputStream zipIn) throws IOException {
        StringBuilder allCode = new StringBuilder();
        ZipEntry entry;

        while ((entry = zipIn.getNextEntry()) != null) {
            if (!entry.isDirectory() &&
                    (entry.getName().endsWith(".java")
                            || entry.getName().endsWith(".py")
                            || entry.getName().endsWith(".js")
                            || entry.getName().endsWith(".ts")
                            || entry.getName().endsWith(".cpp")
                            || entry.getName().endsWith(".cs"))) {

                allCode.append("\n--- File: ").append(entry.getName()).append(" ---\n");
                allCode.append(readFileFromZip(zipIn)).append("\n\n");
            }
        }

        return allCode.toString();
    }

    /**
     * Extract text content from an OpenAI response.
     */
    private String extractAIResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return message.get("content").toString();
        } catch (Exception e) {
            return "Failed to parse AI response";
        }
    }

    /**
     * Read the contents of a file inside a ZIP.
     */
    private String readFileFromZip(InputStream zipInputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

}
