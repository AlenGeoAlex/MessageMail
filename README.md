# MessageMail: Android Message Forwarder

A complete monorepo project featuring a native Android application that forwards device events (like SMS and battery status) to a serverless API running on Cloudflare Workers, which then sends formatted email notifications.

[![Deploy Worker CI](https://github.com/alenalex/MessageMail/actions/workflows/deploy-worker.yml/badge.svg)](https://github.com/alenalex/MessageMail/actions/workflows/deploy-worker.yml)
[![Build & Release Android](https://github.com/alenalex/MessageMail/actions/workflows/release-android.yml/badge.svg)](https://github.com/alenalex/MessageMail/actions/workflows/release-android.yml)

## 🚀 About The Project

This project demonstrates a modern, full-stack development approach using a single repository (monorepo) to manage both a mobile application and its backend API. The core functionality is to capture events on an Android device and securely forward them as nicely formatted emails, all powered by the Cloudflare Edge network.

### Key Features

-   **Monorepo Structure:** Manages the API and mobile app in one place for unified versioning and simplified dependency management using `pnpm` workspaces.
-   **Serverless API:** A lightweight, high-performance API built with [Hono](https://hono.dev/) and deployed globally on [Cloudflare Workers](https://workers.cloudflare.com/).
-   **Native Android App:** A simple Android application built to capture system broadcasts and communicate with the backend.

## 🛠️ Tech Stack

This project is built with a modern, efficient technology stack:

| Area          | Technology                                                                          |
| :------------ | :---------------------------------------------------------------------------------- |
| **API**       | [Cloudflare Workers](https://workers.cloudflare.com/), [Hono](https://hono.dev/), TypeScript, [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/) |
| **Mobile App**| Native Android (Java), Android SDK, Gradle                                          |
| **Tooling**   | [pnpm](https://pnpm.io/) Workspaces, Git, GitHub Actions                            |

## 📂 Monorepo Structure

The project is organized into distinct applications within the `apps` directory, promoting separation of concerns while enabling code sharing.

```
MessageMail/
├── .github/
│   └── workflows/
│       ├── deploy-worker.yml      # CI/CD for the Cloudflare API
│       └── release-android.yml    # CI/CD for building the Android release
├── apps/
│   ├── api-cloudflare-worker/     # The Hono-based Cloudflare Worker API
│   └── app-android/               # The native Android application project
├── .gitignore
├── package.json                   # Root package definition
└── pnpm-workspace.yaml            # pnpm workspace configuration
```

## 🏁 Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

-   [Node.js](https://nodejs.org/) (v18 or later)
-   [pnpm](https://pnpm.io/installation) (`npm install -g pnpm`)
-   [Android Studio](https://developer.android.com/studio) (for the mobile app)
-   A Cloudflare account

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/alenalex/MessageMail.git
    cd MessageMail
    ```

2.  **Install dependencies:**
    This command will install dependencies for all projects in the monorepo.
    ```bash
    pnpm install
    ```

### Running the API Locally

1.  **Navigate to the worker directory:**
    ```bash
    cd apps/api-cloudflare-worker
    ```

2.  **Set up local environment variables:**
    Create a file named `.dev.vars` in the `apps/api-cloudflare-worker` directory. This file is for local development secrets and is ignored by Git.
    ```ini
    # .dev.vars
    TARGET_EMAIL="your-recipient-email@example.com"
    FROM_EMAIL="noreply@your-domain.com"
    ```

3.  **Start the local development server:**
    ```bash
    pnpm dev
    ```
    Your API will be available at `http://localhost:8787`.

### Running the Android App

1.  **Open the Android project:**
    In Android Studio, select "Open" and navigate to the `MessageMail/apps/app-android` directory.

2.  **Configure the API Endpoint:**
    You will need to update the API URL in the Android app's source code to point to your deployed Cloudflare Worker's URL (for production) or `http://localhost:8787` (for local testing with an emulator).

3.  **Build and Run:**
    Use Android Studio to build and run the app on an emulator or a physical device.

## ⚙️ Cloudflare Configuration (Production)

For the deployed worker to function, you must add your secrets to the Cloudflare dashboard.

1.  Navigate to **Workers & Pages** in your Cloudflare dashboard.
2.  Select your `my-awesome-api` worker.
3.  Go to **Settings** > **Variables**.
4.  Add the following **Environment Variables** and encrypt them for security:
    -   `TARGET_EMAIL`: The email address(es) that will receive the forwarded messages.
    -   `FROM_EMAIL`: The address the emails will be sent from (must be a verified address for most email providers).

## 🔄 CI/CD Automation

This project uses GitHub Actions for automated building and deployment.

-   **`deploy-worker.yml`**: This workflow is triggered on any push to `master` that includes changes in the `apps/api-cloudflare-worker` directory. It automatically deploys the latest version of the API to Cloudflare.
-   **`release-android.yml`**: Triggered on changes to `apps/app-android`, this workflow builds, versions, and signs a release-ready APK. It then creates a new GitHub Release and attaches the APK for easy distribution.

## 📄 License

Distributed under the MIT License. See `LICENSE.txt` for more information.