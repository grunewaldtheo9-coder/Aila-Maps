# 🚀 Aila Maps Vercel Secure API Proxy Gateway

To secure your sensitive API keys (Gemini, OpenRouter, MapTiler, OpenRouteService) from being embedded in your Android client-side bundle and decompiled, you can host a secure Serverless Proxy backend on **Vercel** with **one click**.

This repository is now fully structured as a **hybrid workspace** so that Vercel will automatically detect and deploy it as a high-performance NodeJS Serverless API backend, whilst Gradle continues to build your Android APK without interference.

---

## 📁 Implemented Proxy Structure

We have added the following configuration files to the workspace root:
1.  **`package.json`**: Declares standard lightweight proxy dependencies (`express`, `cors`, `node-fetch`).
2.  **`vercel.json`**: Directs all inbound `/api/*` requests automatically to our unified serverless index handler.
3.  **`api/index.js`**: A high-performance Express-like routing proxy controller.

---

## 🔑 Environment Variables to Configure on Vercel

When importing your project on Vercel, set up these Environment Variables in your Vercel Dashboard project settings:

| Name | Description | Source |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Direct Google Gemini flash generation tasks | Google AI Studio |
| `OPENROUTER_KEY` | Chatbot, context routing & recommendations | OpenRouter |
| `MAPTILER_KEY` | Elegant topographic search & vector tile map | MapTiler |
| `ORS_KEY` | Real-time driving turn-by-turn routing steps | OpenRouteService |

---

## ⚡ How to Deploy to Vercel (Step-by-Step)

### 1. Push Code to GitHub
You can push this repository directly to your GitHub account (this is available in your AI Studio editor settings menu).

### 2. Connect to Vercel
1.  Go to [Vercel.com](https://vercel.com) and sign in.
2.  Click **Add New...** -> **Project**.
3.  Import your GitHub repository.
4.  Expand the **Environment Variables** section and add the keys (listed in the table above).
5.  Click **Deploy**!

Vercel will successfully build and host your custom proxy gateway at:
`https://<your-vercel-project-name>.vercel.app/api`

---

## 🛰️ Proxy Endpoint Reference Guide

Once deployed, your backend will securely offer the following REST API endpoints:

### 🌞 Get Weather
*   **Method**: `GET`
*   **Path**: `/api/weather`
*   **Query Parameters**: `latitude=<double>&longitude=<double>`
*   **Behavior**: Proxies data from Open-Meteo securely.

### 📍 Reverse Geocoding
*   **Method**: `GET`
*   **Path**: `/api/reverse`
*   **Query Parameters**: `lat=<double>&lon=<double>`
*   **Behavior**: Calls Nominatim reverse location search securely with dedicated custom headers.

### 🔍 Map Search / Autocomplete
*   **Method**: `GET`
*   **Path**: `/api/search`
*   **Query Parameters**: `q=<query_string>&lat=<user_lat>&lon=<user_lon>`
*   **Behavior**: If `MAPTILER_KEY` is present, crawls high-speed MapTiler search coordinates. Otherwise, automatically drops back to high-grade public Photon geocoder.

### 🚗 Turn-by-Turn Directions
*   **Method**: `GET`
*   **Path**: `/api/directions`
*   **Query Parameters**: `start=<lon,lat>&end=<lon,lat>`
*   **Behavior**: Fetches ORS route pathing with `ORS_KEY` securely. Fails over to modern municipal OSRM open routing engine with custom speed calculations if no key exists.

### 💬 Deep Conversational Chat / Route Advisor
*   **Method**: `POST`
*   **Path**: `/api/chat`
*   **Body (JSON)**: Traditional Gemini payload OR OpenRouter companion message-list context.
*   **Behavior**: Executes generative content calls through direct Gemini Flash or OpenRouter using your private variables.
