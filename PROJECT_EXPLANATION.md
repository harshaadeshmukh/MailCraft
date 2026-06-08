# MailCraft Backend - Architecture & Interview Guide

## 🌍 The Big Picture
MailCraft is a robust backend system built using **Java and Spring Boot**. Its core job is to sit on the internet, wait for someone (either via your website or your Chrome Extension) to send it a messy draft email, and then use Google's AI to rewrite it into a polished, professional response.

Because AI requests are expensive and time-consuming, the system is highly optimized to handle traffic efficiently without crashing, rate-limiting, or going bankrupt.

---

## 🚦 The Journey of a Request
When a user pastes an email into the Chrome Extension and clicks **"Generate Reply"**, the following steps occur:

#### 1. The Front Door (Controllers)
The request first hits the **`EmailGeneratorController`** (the API). This controller acts as the receptionist. It takes the JSON request (which contains the original email text and the desired tone, like "Professional") and hands it off to the backend services.

#### 2. The Bouncer (Rate Limiting)
Before the request even reaches the core logic, it is intercepted by the **`RateLimitInterceptor`**. 
* **Why we use this:** To stop hackers or abusers from spamming the API and exhausting the Google Gemini quota. 
* **What happens:** It checks the **Upstash Redis** database (a lightning-fast, remote memory database). The interceptor asks Redis, *"Has this IP address made more than 5 requests in the last minute?"* 
* If yes, the user is blocked and sees a custom **`error.html`** page. If no, the request is allowed through.

#### 3. The Memory Bank (Caching)
Now the request reaches the **`EmailGeneratorService`**. Before calling the AI, Spring Boot sees the `@Cacheable` annotation.
* **Why we use this:** Calling the AI takes a few seconds. If two users (or the same user clicking the button twice) ask for a reply to the exact same email, we shouldn't waste time asking the AI again.
* **What happens:** The app checks Upstash Redis. If that exact email was already processed recently, it instantly pulls the saved reply from Redis and sends it back to the user in milliseconds.

#### 4. The Load Balancer (API Key Rotator)
If the email wasn't in the cache, the app actually has to call the AI. But before it does, it needs an API key. 
* **Why we use this:** Google limits how many times a single free API key can be used per minute.
* **What happens:** The **`ApiKeyRotator`** uses the **Round-Robin** algorithm. It looks at the list of API keys provided in the environment variables, picks the next one in line, and hands it to the service. This distributes the traffic evenly so no single key gets overloaded.

#### 5. The Brain (Gemini AI & WebClient)
Now it's time to talk to Google.
* **Why we use this:** Spring's **`WebClient`** is a modern, non-blocking HTTP client. It is highly efficient at making external web requests compared to the older `RestTemplate`.
* **What happens:** `WebClient` packages the user's email into a JSON format, attaches the API key we just selected, and sends it to the AI model (**`gemini-2.5-flash`**). If Gemini is too busy and fails (or returns a 429), the code loop catches the error, asks the Rotator for a *different* key, and tries again.

#### 6. The Return Trip
Gemini sends back a complex JSON response. The **`ObjectMapper`** digs through that JSON, extracts just the pure text of the generated reply, saves a copy of it into Redis for next time, and finally sends it all the way back to the user's screen.

---

## ☁️ Deployment & Infrastructure
1. **`Dockerfile`**: This is the shipping container. It packages a lightweight Linux operating system, Java 17, and the compiled `.jar` file into one neat box using a **Multi-Stage Build**. This guarantees that the app will run identically anywhere in the world.
2. **`render.yaml`**: This is the deployment blueprint. When code is pushed to GitHub, Render reads this file, automatically pulls the latest code, securely injects the `GEMINI_API_KEYS` into the environment, builds the Docker container, and hosts it live on a public URL.

---
---

## 🎤 Technical Interview Questions & Answers

If you are presenting this project in an interview, here are the questions a recruiter or senior engineer will likely ask you. Each question includes a **Simple Answer** (to easily understand the concept) and a **Detailed Answer** (for deep technical explanations in an interview).

### Q1: Why did you use Spring `WebClient` instead of the standard `RestTemplate`?
**Simple Answer:** `WebClient` is faster because it doesn't wait around. It sends the request to the AI and does other work while waiting for the reply, whereas `RestTemplate` freezes and waits.
**Detailed Answer:** "I chose `WebClient` because it is highly efficient and supports non-blocking, reactive programming. Since AI requests to Google Gemini can take several seconds to complete, using a synchronous `RestTemplate` would block the server threads while waiting for the response, severely limiting how many concurrent users the app can handle. `WebClient` frees up the thread to do other work while waiting for the AI."

### Q2: How did you implement Load Balancing in your application?
**Simple Answer:** I used a "Round-Robin" technique. I made a list of API keys and my code just takes turns using them one by one (Key 1, then Key 2, then Key 3).
**Detailed Answer:** "Since free AI API tiers have strict rate limits, I implemented an application-level load balancer using the **Round-Robin** algorithm in my `ApiKeyRotator` class. It maintains a list of valid API keys and sequentially cycles through them for every new request. If a request fails, the service catches the exception and immediately retries the request using the *next* key in the rotation."

### Q3: How did you prevent API abuse and handle Rate Limiting?
**Simple Answer:** I track user IP addresses using a super-fast database called Redis. If an IP sends more than 5 requests in a minute, my code blocks them from generating emails.
**Detailed Answer:** "I implemented a custom Spring `HandlerInterceptor` that intercepts incoming HTTP requests. It extracts the user's IP address and uses a **Redis-backed counter** to track requests. If an IP exceeds 5 requests within a 60-second window, the interceptor short-circuits the request and returns a `429 Too Many Requests` status. Because it uses Redis instead of local server memory, this rate limiter works perfectly even if I scale my backend horizontally across multiple servers."

### Q4: Why did you choose Redis, and what exactly are you using it for?
**Simple Answer:** Redis is an extremely fast memory database. I use it to remember previous AI responses so I don't have to ask the AI twice (Caching), and to count user requests (Rate Limiting).
**Detailed Answer:** "I chose Redis (hosted on Upstash) because it's a blazingly fast, in-memory data store. I use it for two critical purposes:
1. **Caching (`@Cacheable`)**: To store the results of expensive AI generations. If the same email is requested twice, it serves the cached result in milliseconds instead of waiting 3 seconds for the AI.
2. **Distributed Rate Limiting**: To keep track of user IP addresses and prevent abuse globally across all instances of my application."

### Q5: What happens if the Gemini API goes down or times out?
**Simple Answer:** My app doesn't crash! It simply catches the error and automatically tries again using the next API key in the list.
**Detailed Answer:** "My application is built with fault tolerance in mind. The `EmailGeneratorService` loops through all available API keys. If an API request throws an exception (due to a timeout, 429, or 500 error from Google), the catch block logs the failure and automatically retries the generation using the next available API key in the rotation. It only throws a final `RuntimeException` if every single key fails."

### Q6: Can you explain your Dockerfile architecture?
**Simple Answer:** I used a "Multi-Stage" Dockerfile. Stage 1 builds the app using heavy tools, and Stage 2 throws away those tools to create a tiny, fast production package.
**Detailed Answer:** "I used a **Multi-Stage Dockerfile** to keep my production image as small and secure as possible. The first stage uses a heavy Maven image to compile the Java code and build the `.jar` file. The second stage uses a very lightweight Java Runtime Environment (JRE) image. It copies only the finished `.jar` file from the first stage and discards all the heavy Maven build tools and source code. This results in faster deployment times on Render."

### Q7: How did you implement caching, and how do you handle cache invalidation?
**Simple Answer:** I used Spring's `@Cacheable` to save AI responses to Redis. To prevent running out of memory, I set a timer (TTL) so old saved responses delete themselves after an hour.
**Detailed Answer:** "I used Spring's `@Cacheable` annotation backed by Redis. The cache key is dynamically generated by combining the original email content and the requested tone. If the exact same request comes in, the response is served from memory. To prevent the cache from growing infinitely and consuming all my Upstash memory limit, I configured a Time-To-Live (TTL) in my `application.properties` so old cache entries automatically expire and delete themselves after an hour."

### Q8: What is CORS, and why did you need to configure it?
**Simple Answer:** Browsers block websites (like your frontend) from secretly talking to different servers (like your backend) for security. CORS is the configuration where the backend tells the browser "It's okay, I trust this frontend".
**Detailed Answer:** "CORS (Cross-Origin Resource Sharing) is a browser security feature that prevents a website on one domain from making API requests to a different domain. Because my backend is hosted on Render (e.g., `mailcraft.onrender.com`), but requests are coming from a Chrome Extension or a local React frontend (like `localhost:3000`), the browser would block the requests. I had to explicitly configure a `WebMvcConfigurer` in Spring Boot to allow cross-origin requests from my specific frontend clients."

### Q9: How did you handle exceptions and error responses?
**Simple Answer:** I built a custom HTML error page. Instead of showing ugly Java code errors, it shows a friendly message that matches my app's design.
**Detailed Answer:** "Instead of letting Spring Boot show the default, ugly 'White-label Error Page' or dumping raw Java stack traces to the user, I created a custom `error.html` template using Thymeleaf. This intercepts 404 Not Found, 500 Internal Server Error, and 429 Too Many Requests errors. It dynamically displays the exact HTTP status code and a user-friendly message while maintaining my application's brand aesthetic."

### Q10: Why did you use `ObjectNode` and `ObjectMapper` instead of creating Java classes (POJOs) for the Gemini API request?
**Simple Answer:** The Gemini API has a very complicated JSON structure. Using `ObjectNode` let me build that JSON quickly without writing dozens of useless Java classes.
**Detailed Answer:** "The Google Gemini API has a deeply nested and somewhat complex JSON structure for its requests and responses. While creating Java classes (DTOs) is the standard approach, using `ObjectNode` allowed me to rapidly build the exact JSON structure I needed dynamically without cluttering my project with 5 or 6 nested record classes just to send a single text prompt."

### Q11: What would happen to your application if Redis went offline?
**Simple Answer:** My app would fail to process requests because it relies on Redis for rate limiting. In the future, I could add a fallback to ignore the rate limiter if Redis crashes.
**Detailed Answer:** "Currently, Redis is a hard dependency for both caching and rate limiting. If Upstash Redis goes offline, the `RateLimitInterceptor` will throw a connection exception, and the application will fail to process requests. If this were a critical enterprise app, I would implement a fallback mechanism (like a `try-catch` block around the Redis calls) that bypasses rate-limiting and caching entirely so the core email generation feature stays online in a degraded state."

### Q12: How would you scale this application to handle 10,000 concurrent users?
**Simple Answer:** Because I store all user data in Redis, the Java app is "stateless". I could just launch 10 more copies of my Java app on Render, and they would all share the same Redis database.
**Detailed Answer:** "The architecture is already designed for horizontal scaling. Because I used Redis for state management (caching and rate limiting), the Spring Boot application itself is completely stateless. To handle 10k users, I would simply spin up 5-10 more instances of the backend container on Render behind a load balancer. I would also need to upgrade my Gemini API tier or cycle through hundreds of API keys to handle the upstream AI limits."

### Q13: Why did you choose Spring Boot over other frameworks like Express.js or Python FastAPI?
**Simple Answer:** Spring Boot is extremely organized and provides powerful built-in tools for enterprise apps, making the code much easier to maintain as it gets bigger.
**Detailed Answer:** "Spring Boot excels at building enterprise-grade, highly structured applications. It provides fantastic out-of-the-box abstractions for things like caching (`@Cacheable`), dependency injection, and Interceptors. While Node.js or Python might be faster to prototype with, Spring's strict typing and built-in design patterns make the codebase much more maintainable as the project grows in complexity."

### Q14: Explain the difference between `@RestController` and `@Controller` in your project.
**Simple Answer:** `@RestController` is used to send raw data (JSON) back to the Chrome Extension. `@Controller` is used to send full HTML webpages (like the error page) to the browser.
**Detailed Answer:** "I used both! I used `@RestController` for my API endpoints (like `/api/email/generate`) because it automatically serializes the Java return objects directly into JSON for my Chrome Extension. I used `@Controller` for my Web views, because it tells Spring to look for a Thymeleaf HTML template (like `index.html` or `error.html`) to render and send back a full webpage to the user."

### Q15: How are you managing secrets, and how do you ensure API keys aren't pushed to GitHub?
**Simple Answer:** I never type my passwords in the code. I pull them from secure environment variables, and I use `.gitignore` to make sure they never get uploaded to GitHub.
**Detailed Answer:** "I strictly follow the Twelve-Factor App methodology. I never hardcode API keys in my Java code. Instead, I use `@Value(\"${gemini.api.keys}\")` to pull them from the application's environment. Locally, they are stored in my system environment variables, and in production, they are securely injected via Render's dashboard. My `.gitignore` file ensures no sensitive `.env` files are accidentally committed."

### Q16: Can you explain how a simple math bug could have crashed your `ApiKeyRotator`, and how you fixed it?
**Simple Answer:** In Java, a number counter can eventually go so high it turns into a negative number (overflow), which breaks the load balancer. I fixed it by mathematically wrapping the counter back to zero safely.
**Detailed Answer:** "My initial round-robin load balancer used `Math.abs(counter.getAndIncrement()) % keys.size()`. However, in Java, `AtomicInteger` eventually overflows to `Integer.MIN_VALUE` (a negative number). Taking the `Math.abs` of `Integer.MIN_VALUE` actually returns a negative number because it exceeds the maximum positive limit of a 32-bit integer! This would have caused an `ArrayIndexOutOfBoundsException`. I fixed it by changing the logic to safely wrap the counter back to zero using `compareAndSet`, ensuring mathematically perfect load balancing forever."
