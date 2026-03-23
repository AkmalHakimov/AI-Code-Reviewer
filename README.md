# AI Code Reviewer

A GitHub Action that automatically reviews your pull requests using OpenAI's GPT-4. Instead of waiting on a teammate to catch issues, this runs the moment a PR is opened and leaves comments directly on the diff — pointing out bugs, style inconsistencies, and anything else worth a second look.

---

## How it works

When a pull request is opened or updated, the action fetches the diff, breaks it into chunks, and sends each one to the OpenAI API. GPT-4 reviews the changes and posts inline comments on the PR with its feedback. Files you don't want reviewed (like lock files or auto-generated code) can be excluded with a simple pattern.

---

## Getting started

You'll need an OpenAI API key. If you don't have one yet, you can get it at [platform.openai.com](https://platform.openai.com).

Once you have the key, add it to your repository secrets as `OPENAI_API_KEY`. Then create the following workflow file:

```yaml
# .github/workflows/code-review.yml

name: AI Code Reviewer

on:
  pull_request:
    types:
      - opened
      - synchronize

permissions:
  write-all

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repo
        uses: actions/checkout@v3

      - name: AI Code Reviewer
        uses: AkmalHakimov/AI-Code-Reviewer@main
        with:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          OPENAI_API_MODEL: "gpt-4"        # optional, defaults to gpt-4
          exclude: "**/*.json, **/*.md"    # optional, comma-separated patterns
```

That's it. The action will start reviewing new PRs automatically.

---

## Configuration

| Input | Required | Default | Description |
|---|---|---|---|
| `GITHUB_TOKEN` | yes | — | Provided automatically by GitHub Actions |
| `OPENAI_API_KEY` | yes | — | Your OpenAI API key |
| `OPENAI_API_MODEL` | no | `gpt-4` | The model to use for reviews |
| `exclude` | no | — | File patterns to skip, comma-separated |

The `exclude` field accepts standard glob patterns. For example, `**/*.json, **/*.md, **/dist/**` would skip JSON files, Markdown, and anything in a dist folder.

---

## What it reviews

The reviewer looks at the actual diff for each PR, so it only comments on what changed. It tends to flag things like potential bugs or logic errors, unclear variable names, missing error handling, code that could be simplified, and security concerns in common patterns.

It won't catch everything, and it's not meant to replace a real human review — but it's useful for catching low-hanging fruit before anyone else has to look at the code.

---

## Contributing

Pull requests are welcome. If you run into something unexpected or have an idea for improving the prompts or the action itself, feel free to open an issue.

---

## License

MIT
