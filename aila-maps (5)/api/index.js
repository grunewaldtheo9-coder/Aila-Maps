const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');

const app = express();

// Enable Cross-Origin Resource Sharing (CORS) for local and mobile requests
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Title']
}));

app.use(express.json());

// Load Api keys from Environment variables on Vercel
const MAPTILER_KEY = process.env.MAPTILER_KEY || '';
const ORS_KEY = process.env.ORS_KEY || '';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const OPENROUTER_KEY = process.env.OPENROUTER_KEY || '';

// Logger Helper
function logRequest(apiName, details) {
  console.log(`[${new Date().toISOString()}] ${apiName} request: ${details}`);
}

// 1. Root diagnostic endpoint
app.get('/api', (req, res) => {
  res.status(200).json({
    name: "Aila Maps Secure API Proxy",
    status: "online",
    version: "1.0.0",
    configuredServices: {
      mapTiler: !!MAPTILER_KEY,
      openRouteService: !!ORS_KEY,
      geminiDirect: !!GEMINI_API_KEY,
      openRouterFallback: !!OPENROUTER_KEY
    }
  });
});

// 2. Weather route (Forwarding to Open-Meteo)
app.get('/api/weather', async (req, res) => {
  const { latitude, longitude } = req.query;
  if (!latitude || !longitude) {
    return res.status(400).json({ error: "latitude and longitude parameters are required" });
  }

  logRequest("Weather", `lat=${latitude}, lon=${longitude}`);
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current_weather=true`;

  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Open-Meteo returned status ${response.status}`);
    }
    const data = await response.json();
    res.status(200).json(data);
  } catch (error) {
    console.error("Open-Meteo Weather Forward Error:", error);
    res.status(500).json({ error: "Failed to fetch weather data", message: error.message });
  }
});

// 3. Reverse Geocode (Forwarding to Nominatim)
app.get('/api/reverse', async (req, res) => {
  const { lat, lon } = req.query;
  if (!lat || !lon) {
    return res.status(400).json({ error: "lat and lon parameters are required" });
  }

  logRequest("ReverseGeocode", `lat=${lat}, lon=${lon}`);
  const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=en`;

  try {
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'AilaMapsSecureProxy/1.0 (Vercel Node Serverless)'
      }
    });
    if (!response.ok) {
      throw new Error(`Nominatim returned status ${response.status}`);
    }
    const data = await response.json();
    res.status(200).json(data);
  } catch (error) {
    console.error("Nominatim Reverse Geocode Error:", error);
    res.status(500).json({ error: "Failed to reverse geocode", message: error.message });
  }
});

// 4. Map search / Geocode
app.get('/api/search', async (req, res) => {
  const { q, lat, lon } = req.query;
  if (!q) {
    return res.status(400).json({ error: "Query parameter 'q' is required" });
  }

  logRequest("Search", `q=${q}, contextBias=[${lat}, ${lon}]`);

  // Preferred MapTiler provider if key is set
  if (MAPTILER_KEY) {
    const encodedQ = encodeURIComponent(q);
    const url = `https://api.maptiler.com/geocoding/${encodedQ}.json?key=${MAPTILER_KEY}`;
    try {
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        return res.status(200).json(data);
      }
      console.warn(`MapTiler Geocoding API returned status ${response.status}. Falling back to Photon...`);
    } catch (e) {
      console.error("MapTiler geocode query error, trying Photon fallback:", e);
    }
  }

  // Fallback to free, public Photon Geocoder API (which doesn't require keys)
  const encodedQuery = encodeURIComponent(q);
  const biasLat = lat || '37.7651';
  const biasLon = lon || '-122.4932';
  const url = `https://photon.komoot.io/api/?q=${encodedQuery}&lat=${biasLat}&lon=${biasLon}&limit=10`;

  try {
    const response = await fetch(url, {
      headers: { 'User-Agent': 'AilaMapsSecureProxy/1.0 (Vercel Node Serverless)' }
    });
    if (!response.ok) {
      throw new Error(`Photon API returned status ${response.status}`);
    }
    const data = await response.json();
    res.status(200).json(data);
  } catch (err) {
    console.error("Photon Search Fallback Error:", err);
    res.status(500).json({ error: "Search failed on both MapTiler and Photon backends", message: err.message });
  }
});

// 5. OpenRouteService Directions Proxy
app.get('/api/directions', async (req, res) => {
  const { start, end } = req.query;
  if (!start || !end) {
    return res.status(400).json({ error: "start and end coordinates are required (format: lon,lat)" });
  }

  logRequest("Directions", `start=${start}, end=${end}`);

  // If local ORS_KEY is available, we try OpenRouteService
  if (ORS_KEY) {
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${ORS_KEY}&start=${start}&end=${end}`;
    try {
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        return res.status(200).json(data);
      }
      console.warn(`OpenRouteService API returned status ${response.status}. Falling back to OSRM...`);
    } catch (e) {
      console.error("OpenRouteService API request error, falling back to OSRM:", e);
    }
  }

  // Fallback to OSRM public router if ORS_KEY is empty or fails
  // Transpose "start=lon,lat" and "end=lon,lat" to "startLon,startLat;endLon,endLat"
  const startCoords = start.split(',');
  const endCoords = end.split(',');
  if (startCoords.length < 2 || endCoords.length < 2) {
    return res.status(400).json({ error: "Invalid coordinate format" });
  }

  const url = `https://router.project-osrm.org/route/v1/driving/${startCoords[0]},${startCoords[1]};${endCoords[0]},${endCoords[1]}?overview=full&geometries=geojson&steps=true`;

  try {
    const response = await fetch(url, {
      headers: { 'User-Agent': 'AilaMapsSecureProxy/1.0 (Vercel Node Serverless)' }
    });
    if (!response.ok) {
      throw new Error(`OSRM API returned status ${response.status}`);
    }
    const data = await response.json();
    res.status(200).json(data);
  } catch (err) {
    console.error("OSRM Route Fallback Error:", err);
    res.status(500).json({ error: "Routing services failed", message: err.message });
  }
});

// 6. Conversational AI Assistant & Review generator (Gemini + OpenRouter proxy)
app.post('/api/chat', async (req, res) => {
  const { contents, systemInstruction, generationConfig, messages, contextInfo } = req.body;
  
  logRequest("AIChat", `Request received with ${contents ? 'Gemini payload' : 'messages list'}`);

  // Direct Gemini API
  if (GEMINI_API_KEY && contents) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${GEMINI_API_KEY}`;
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contents, systemInstruction, generationConfig })
      });
      if (response.ok) {
        const data = await response.json();
        return res.status(200).json(data);
      }
      console.warn(`Direct Gemini API returned status ${response.status}. Trying fallbacks...`);
    } catch (e) {
      console.error("Direct Gemini API error:", e);
    }
  }

  // OpenRouter Fallback
  const finalOpenRouterKey = OPENROUTER_KEY;
  if (finalOpenRouterKey && messages) {
    const url = "https://openrouter.ai/api/v1/chat/completions";

    const messageList = messages.map(msg => ({
      role: msg.sender === 'USER' ? 'user' : 'assistant',
      content: msg.text
    }));

    const finalSystemPrompt = "You are Aila AI, an advanced, friendly, and expert Map and Route Assistant. " +
      `The user's current context is: ${contextInfo || 'Exploring'}. ` +
      "Always refer to their favorites, map zoom levels, and coordinates when answering. " +
      "Respond in the user's language (Portuguese if they speak in Portuguese, otherwise English). " +
      "Answer concisely with helpful routing details or recommendations.";

    messageList.unshift({ role: "system", content: finalSystemPrompt });

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${finalOpenRouterKey}`,
          'Content-Type': 'application/json',
          'X-Title': 'Aila Maps App'
        },
        body: JSON.stringify({
          model: 'google/gemini-2.5-flash',
          messages: messageList
        })
      });

      if (response.ok) {
        const data = await response.json();
        // Return standard response structured similarly for client ease
        return res.status(200).json({
          candidates: [{
            content: {
              parts: [{
                text: data.choices?.[0]?.message?.content || ""
              }]
            }
          }]
        });
      }
      console.warn(`OpenRouter API returned status ${response.status}`);
    } catch (e) {
      console.error("OpenRouter API error:", e);
    }
  }

  // Local procedural fallback if both keys are missing or failed
  return res.status(200).json({
    candidates: [{
      content: {
        parts: [{
          text: "I am Aila, your local companion. Please configure a GEMINI_API_KEY or OPENROUTER_KEY in your Vercel Environment Variables to activate complete Conversational AI features."
        }]
      }
    }]
  });
});

// Start listening if run locally (not in serverless mode)
if (process.env.NODE_ENV !== 'production') {
  const PORT = process.env.PORT || 3000;
  app.listen(PORT, () => {
    console.log(`Aila Secure Proxy running locally on port ${PORT}`);
  });
}

module.exports = app;
