package org.example.ai_analyzer.controller;

import org.example.ai_analyzer.model.ReviewResponse;
import org.example.ai_analyzer.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "http://localhost:5173") // your React port
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * Analyze single uploaded file
     * Example: POST /api/review/file
     */
    @PostMapping("/file")
    public Map<String, Object> analyzeFile(@RequestParam("file") MultipartFile file) throws Exception {
        return reviewService.reviewCode(file);
    }

    /**
     * Analyze GitHub repository
     * Example: POST /api/review/github?repoUrl=https://github.com/user/repo
     */
    @PostMapping("/github")
    public Map<String, Object> analyzeGithubRepo(@RequestParam("repoUrl") String repoUrl) throws Exception {
        return reviewService.reviewGithubRepo(repoUrl);
    }
}

