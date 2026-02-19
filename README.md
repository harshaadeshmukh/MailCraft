# MailCraft — AI-Powered Chrome Extension for Gmail ✉️🤖

> Refine selected text into clear, well-structured, and professional email responses inside Gmail — with a single click.

---

## 🎥 Live Demo

🔗 [Watch Demo on YouTube](https://www.youtube.com/watch?v=Eg_3R4pzB3o)

---

## 📌 Overview

**MailCraft** is an AI-powered Chrome extension that helps users generate professional emails instantly inside Gmail. It refines selected text into clear, well-structured, and professional email responses with a single click, saving time and improving productivity.

Whether you're dealing with a flood of work emails or just struggling to find the right words, MailCraft gives you a head start every time — without ever leaving Gmail.

---

## ✨ Features

- 🧩 **Chrome Extension** — Works directly inside Gmail, no tab switching needed
- 🧠 **AI-Powered Replies** — Uses Google Gemini API to generate intelligent, context-aware email responses
- 🎭 **Tone Selection** — Choose from tones like Professional, Friendly, Formal, Casual, and more
- ⚡ **One-Click Generation** — Select your draft text and get a polished reply instantly
- 🐳 **Docker Ready** — Fully containerized backend for easy local setup and deployment
- ☁️ **Deployed on Render** — Backend hosted on Render for reliable availability

---

## 🛠️ Tech Stack

| Layer | Technology | Why It Was Used |
|---|---|---|
| **Backend** | Java 21 + Spring Boot | Spring Boot is the gold standard for building production-grade REST APIs in Java. It offers auto-configuration, embedded server, and a massive ecosystem — perfect for quickly scaffolding a robust backend. |
| **AI Engine** | Google Gemini API | Gemini provides state-of-the-art language understanding and generation capabilities. It's free-tier friendly, fast, and produces high-quality text — ideal for generating natural-sounding email replies. |
| **Frontend** | HTML + CSS + JavaScript | A lightweight, dependency-free frontend keeps the project simple and fast. No framework overhead — just clean, functional UI that works everywhere without a build step. |
| **Containerization** | Docker | Docker ensures the app runs identically across all environments (local, staging, production). The Dockerfile packages the Spring Boot JAR into a portable image, eliminating "works on my machine" problems. |
| **Deployment** | Render | Render supports Docker-based deployments out of the box and offers a free tier for hobby projects. The `render.yaml` file enables Infrastructure-as-Code style deployments for easy CI/CD. |

---

## 📂 Project Structure

```
MailCraft/
├── src/
│   └── main/
│       ├── java/          # Spring Boot application code
│       │   └── ...        # Controllers, Services, Models
│       └── resources/
│           └── template/    # HTML, CSS, JS frontend files
├── .mvn/wrapper/          # Maven wrapper files
├── Dockerfile             # Docker image definition
├── render.yaml            # Render deployment configuration
├── pom.xml                # Maven dependencies & build config
├── mvnw / mvnw.cmd        # Maven wrapper scripts (Linux/Windows)
└── .gitignore
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+ (or 21)
- Maven (or use the included `mvnw` wrapper)
- Google Gemini API Key ([Get one here](https://aistudio.google.com/app/apikey))
- Docker (optional, for containerized setup)

---

### 🔧 Local Setup (Without Docker)

**1. Clone the repository**
```bash
git clone https://github.com/harshaadeshmukh/MailCraft.git
cd MailCraft
```

**2. Set your Gemini API Key**

Add your API key to `src/main/resources/application.properties`:
```properties
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
```
Or set it as an environment variable:
```bash
export GEMINI_API_KEY=your_key_here
```

**3. Build and run**
```bash
./mvnw spring-boot:run
```
> On Windows, use `mvnw.cmd spring-boot:run`

**4. Open the app**

Navigate to `http://localhost:8080` in your browser.

---

### 🐳 Local Setup (With Docker)

**1. Build the Docker image**
```bash
docker build -t mailcraft .
```

**2. Run the container**
```bash
docker run -p 8080:8080 -e GEMINI_API_KEY=your_key_here mailcraft
```

**3. Open the app**

Navigate to `http://localhost:8080` in your browser.

---

## 🌐 API Reference

### Generate Email Reply

**`POST /api/email/generate`**

**Request Body:**
```json
{
  "emailContent": "Hi, I wanted to follow up on our meeting from last week...",
  "tone": "Professional"
}
```

**Response:**
```json
{
  "reply": "Dear [Name],\n\nThank you for reaching out. I appreciate your follow-up regarding our recent meeting..."
}
```

---

## ☁️ Deployment on Render

This project includes a `render.yaml` for one-click deployment on [Render](https://render.com).

1. Push your code to GitHub
2. Connect your GitHub repo to Render
3. Add `GEMINI_API_KEY` as an environment variable in Render's dashboard
4. Render will auto-build the Docker image and deploy it

---

## 🔮 Future Improvements

- [ ] Support for Outlook and other email clients
- [ ] Multiple AI model support (OpenAI, Claude, etc.)
- [ ] User authentication and reply history
- [ ] Additional tone options (Empathetic, Assertive, Apologetic)
- [ ] Smart subject line suggestions
- [ ] Publish to Chrome Web Store

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 👨‍💻 Author

**Harsha Deshmukh**
- GitHub: [@harshaadeshmukh](https://github.com/harshaadeshmukh)

---

> ⭐ If you found this project useful, please consider giving it a star!
