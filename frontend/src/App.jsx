import React, { useState } from "react";
import axios from "axios";
import "./index.css";

function App() {
  const [file, setFile] = useState(null);
  const [repoUrl, setRepoUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  const handleFileChange = (e) => setFile(e.target.files[0]);

  const handleFileUpload = async () => {
    if (!file) return alert("Please upload a file first.");
    setLoading(true);
    setError("");
    setResult(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await axios.post("http://localhost:8080/api/review/file", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      handleApiResponse(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to analyze file. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleRepoAnalyze = async () => {
    if (!repoUrl) return alert("Please enter a GitHub repository URL.");
    setLoading(true);
    setError("");
    setResult(null);

    try {
      const res = await axios.post("http://localhost:8080/api/review/github", null, {
        params: { repoUrl },
      });
      handleApiResponse(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to analyze repository. Please check the URL or repo visibility.");
    } finally {
      setLoading(false);
    }
  };

  const handleApiResponse = (data) => {
    try {
      if (typeof data === "object") {
        setResult(data);
        return;
      }

      let cleaned = data.replace(/```json|```/g, "").trim();
      const parsed = JSON.parse(cleaned);
      setResult(parsed);
    } catch (err) {
      console.error("Failed to parse AI output:", err);
      setError("Failed to parse AI output. Please check backend logs.");
      setResult({ raw: data });
    }
  };

  return (
    <div className="container">
      <h1 className="title">🤖 AI Code Reviewer</h1>

      {/* ===== Upload Section ===== */}
      <div
        className="upload-box"
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          setFile(e.dataTransfer.files[0]);
        }}
      >
        <input
          type="file"
          id="fileInput"
          accept=".java,.js,.py,.cpp,.ts"
          onChange={handleFileChange}
          className="file-input"
        />
        <label htmlFor="fileInput" className="upload-label">
          {file ? (
            <span className="file-name">{file.name}</span>
          ) : (
            <>
              <span className="upload-icon">📂</span>
              <p>
                Drag & drop your file here, or{" "}
                <span className="highlight">browse</span>
              </p>
            </>
          )}
        </label>
      </div>

      <button
        onClick={handleFileUpload}
        disabled={!file}
        className="upload-btn"
      >
        🚀 Upload File
      </button>

      <div className="or-divider">or</div>

      <div className="repo-section">
        <input
          type="text"
          placeholder="https://github.com/username/repo"
          value={repoUrl}
          onChange={(e) => setRepoUrl(e.target.value)}
          className="repo-input"
        />
        <button onClick={handleRepoAnalyze} className="repo-btn">
          🔍 Analyze Repo
        </button>
      </div>

      {loading && (
        <div className="loading">
          <div className="spinner"></div>
          <p>Analyzing your code with AI...</p>
        </div>
      )}

      {error && <div className="error">{error}</div>}

      {result && (
        <div className="result-card">
          <h2>📋 Summary</h2>
          <p>{result.summary || "No summary available."}</p>

          <div className="insight-section">
            <div className="insight-card">
              <h4>🧠 Architecture Insight</h4>
              <p>
                The code follows a layered structure with controllers, services, and entities,
                ensuring modularity. However, dependency injection patterns and logging could
                be standardized for maintainability.
              </p>
            </div>
            <div className="insight-card">
              <h4>🧩 Complexity Insight</h4>
              <p>
                Detected a moderate level of control flow complexity. Consider simplifying
                deeply nested conditions and reusing common logic in utility classes.
              </p>
            </div>
            <div className="insight-card">
              <h4>🔐 Security Insight</h4>
              <p>
                CORS configurations, exception handling, and input validation should be
                tightened to prevent exposure to injection and open access vulnerabilities.
              </p>
            </div>
          </div>

          <h3>⚠️ Issues</h3>
          {result.issues && Array.isArray(result.issues) ? (
            <ul className="issue-list">
              {result.issues.map((issue, i) => (
                <li key={i} className={`issue-card ${issue.type?.toLowerCase() || "general"}`}>
                  <b>{issue.file || "General"}</b> ({issue.type || "General"}):{" "}
                  {issue.message || "No description provided."}
                </li>
              ))}
            </ul>
          ) : (
            <pre>{result.raw || JSON.stringify(result.issues, null, 2)}</pre>
          )}

          <h3>🏆 Code Quality Score</h3>
          <div className="score-wrapper">
            <div className="score-bar">
              <div
                className="score-fill"
                style={{ width: `${result.score || 0}%` }}
              ></div>
            </div>
            <p className="score-text">{result.score || 0}/100</p>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
