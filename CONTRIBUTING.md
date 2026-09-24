# Contributing to RagKit

Thank you for your interest in contributing to RagKit! We welcome bug reports, feature proposals, and pull requests from everyone.

---

## 🛠️ Development Setup

1. Fork and clone the repository:
   ```bash
   git clone https://github.com/Gurkan26/ragkit-android.git
   cd ragkit-android
   ```
2. Open the project in the latest **Android Studio (Koala / Ladybug or newer)**.
3. Ensure JDK 17+ is configured as the Gradle JVM.
4. Run Gradle test to verify your local environment:
   ```bash
   ./gradlew test
   ```

---

## 🌿 Branching Model & Commits

- Create a feature branch off `main`:
  ```bash
  git checkout -b feat/your-feature-name
  ```
- Write clear, conventional commit messages:
  - `feat: add hybrid keyword-vector search`
  - `fix: prevent memory leak during onTrimMemory`
  - `docs: update quickstart instructions in README`
  - `test: add unit tests for CosineSimilarity`

---

## 🧪 Testing Guidelines

- Always write JUnit 5 unit tests for new business logic in `ragkit-core`.
- If modifying storage or database models, add or update Room instrumented tests.
- Verify everything passes before submitting:
  ```bash
  ./gradlew test
  ./gradlew lint
  ```

---

## 📝 Pull Request Checklist

- [ ] Code follows Kotlin official style guide (`kotlin.code.style=official`).
- [ ] Public API methods are documented with complete KDoc comments.
- [ ] Unit tests are written and passing.
- [ ] PR description clearly explains the motivation and technical changes.
