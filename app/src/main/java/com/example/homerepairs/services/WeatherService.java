package com.example.homerepairs.services;

import android.util.Log;

import com.example.homerepairs.models.WeatherData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String API_KEY = "86f0a838a65c35c5019be3c51a32791c";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    private OkHttpClient client;
    private Gson gson;

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }

    public WeatherService() {
        // Configure OkHttpClient with timeouts and NO CACHE to ensure fresh data
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cache(null) // Disable caching completely
                .build();
        this.gson = new Gson();
    }

    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        // Add timestamp and random component to prevent caching
        long timestamp = System.currentTimeMillis();
        long random = (long)(Math.random() * 1000000); // Add random component for uniqueness
        String url = BASE_URL + "?lat=" + latitude + "&lon=" + longitude + 
                    "&appid=" + API_KEY + "&units=metric" + "&_t=" + timestamp + "&_r=" + random;
        
        Log.d(TAG, "Fetching weather from: " + url.replace(API_KEY, "API_KEY_HIDDEN"));
        Log.d(TAG, "Request timestamp: " + timestamp + ", random: " + random);
        
        // Create cache control that forces fresh data
        CacheControl cacheControl = new CacheControl.Builder()
                .noCache()
                .noStore()
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .cacheControl(cacheControl)
                .header("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("If-Modified-Since", "0") // Prevent conditional requests
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Weather API call failed", e);
                callback.onError("Failed to fetch weather: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "Weather API error: " + response.code() + " - " + errorBody);
                    
                    // Check for specific error codes
                    if (response.code() == 401) {
                        Log.e(TAG, "Invalid API key. Please get a valid API key from https://openweathermap.org/api");
                        callback.onError("Invalid API key. Please configure a valid OpenWeatherMap API key.");
                    } else if (response.code() == 429) {
                        Log.e(TAG, "API rate limit exceeded");
                        callback.onError("Too many requests. Please try again later.");
                    } else {
                        callback.onError("Weather API error: " + response.code());
                    }
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Weather API Response: " + responseBody);
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    
                    // Extract weather data
                    JsonObject main = jsonObject.getAsJsonObject("main");
                    double temp = main.get("temp").getAsDouble();
                    int tempInt = (int) Math.round(temp);
                    
                    // Log the timestamp from API response to verify freshness
                    long apiTimestamp = jsonObject.has("dt") ? jsonObject.get("dt").getAsLong() * 1000 : 0;
                    long currentTime = System.currentTimeMillis();
                    long dataAge = currentTime - apiTimestamp;
                    
                    Log.d(TAG, "API Response timestamp: " + apiTimestamp + " (" + new java.util.Date(apiTimestamp) + ")");
                    Log.d(TAG, "Current time: " + currentTime + " (" + new java.util.Date(currentTime) + ")");
                    Log.d(TAG, "Data age: " + (dataAge / 1000) + " seconds");
                    Log.d(TAG, "Raw temperature from API: " + temp + "°C");
                    Log.d(TAG, "Rounded temperature: " + tempInt + "°C");
                    
                    JsonObject weather = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();
                    String condition = weather.get("main").getAsString();
                    String description = weather.get("description").getAsString();
                    String icon = weather.get("icon").getAsString();
                    
                    Log.d(TAG, "Weather condition: " + condition + ", Description: " + description + ", Icon code: " + icon);
                    
                    // Convert condition to display format
                    String displayCondition = formatCondition(condition);
                    
                    // Get icon resource ID based on API icon code (more accurate than condition)
                    int iconResId = getIconResourceIdFromIconCode(icon, condition);
                    
                    Log.d(TAG, "Display condition: " + displayCondition + ", Icon resource ID: " + iconResId);
                    
                    String displayText = tempInt + "°C " + displayCondition;
                    Log.d(TAG, "Final weather display text: " + displayText);
                    
                    WeatherData weatherData = new WeatherData(
                            tempInt + "°C",
                            displayCondition,
                            description,
                            icon,
                            iconResId
                    );
                    
                    callback.onSuccess(weatherData);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing weather response", e);
                    Log.e(TAG, "Exception details: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                    callback.onError("Error parsing weather data: " + e.getMessage());
                }
            }
        });
    }

    private String formatCondition(String condition) {
        switch (condition.toLowerCase()) {
            case "clear":
                return "Sunny";
            case "clouds":
                return "Cloudy";
            case "rain":
                return "Rainy";
            case "drizzle":
                return "Drizzle";
            case "thunderstorm":
                return "Stormy";
            case "snow":
                // Cambodia doesn't have snow, map to Rainy instead
                return "Rainy";
            case "mist":
            case "fog":
                return "Foggy";
            default:
                return condition;
        }
    }

    /**
     * Get icon resource ID based on OpenWeatherMap icon code
     * Icon codes: 01d/01n (clear), 02d/02n (few clouds), 03d/03n (scattered clouds),
     *             04d/04n (broken clouds), 09d/09n (shower rain), 10d/10n (rain),
     *             11d/11n (thunderstorm), 13d/13n (snow), 50d/50n (mist)
     */
    private int getIconResourceIdFromIconCode(String iconCode, String condition) {
        Log.d(TAG, "getIconResourceIdFromIconCode called with iconCode: '" + iconCode + "', condition: '" + condition + "'");
        
        if (iconCode == null || iconCode.isEmpty()) {
            Log.d(TAG, "Icon code is null or empty, falling back to condition-based mapping");
            return getIconResourceIdFromCondition(condition);
        }
        
        // Use the first 2 characters of icon code (e.g., "01", "02", "03")
        String iconPrefix = iconCode.length() >= 2 ? iconCode.substring(0, 2) : iconCode;
        
        Log.d(TAG, "Mapping icon code: '" + iconCode + "' (prefix: '" + iconPrefix + "') to resource");
        
        int resourceId;
        switch (iconPrefix) {
            case "01": // Clear sky
                resourceId = com.example.homerepairs.R.drawable.day;
                Log.d(TAG, "Icon code 01 (Clear sky) -> day drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "02": // Few clouds
            case "03": // Scattered clouds
            case "04": // Broken clouds
                resourceId = com.example.homerepairs.R.drawable.cloudy;
                Log.d(TAG, "Icon code " + iconPrefix + " (Clouds) -> cloudy drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "09": // Shower rain
            case "10": // Rain
                resourceId = com.example.homerepairs.R.drawable.rainy_3;
                Log.d(TAG, "Icon code " + iconPrefix + " (Rain) -> rainy_3 drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "11": // Thunderstorm
                resourceId = com.example.homerepairs.R.drawable.thunder;
                Log.d(TAG, "Icon code 11 (Thunderstorm) -> thunder drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "13": // Snow
                resourceId = com.example.homerepairs.R.drawable.snowy_3;
                Log.d(TAG, "Icon code 13 (Snow) -> snowy_3 drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "50": // Mist/Fog
                resourceId = com.example.homerepairs.R.drawable.cloudy; // Use cloudy icon for mist/fog
                Log.d(TAG, "Icon code 50 (Mist/Fog) -> cloudy drawable (resource ID: " + resourceId + ")");
                return resourceId;
            default:
                // Fallback to condition-based mapping
                Log.w(TAG, "Unknown icon code prefix: '" + iconPrefix + "', falling back to condition: '" + condition + "'");
                return getIconResourceIdFromCondition(condition);
        }
    }
    
    /**
     * Fallback method to get icon resource ID based on condition string
     */
    private int getIconResourceIdFromCondition(String condition) {
        Log.d(TAG, "getIconResourceIdFromCondition called with condition: '" + condition + "'");
        
        if (condition == null) {
            Log.d(TAG, "Condition is null, using default day drawable");
            return com.example.homerepairs.R.drawable.day;
        }
        
        String conditionLower = condition.toLowerCase();
        int resourceId;
        
        switch (conditionLower) {
            case "sunny":
            case "clear":
                resourceId = com.example.homerepairs.R.drawable.day;
                Log.d(TAG, "Condition '" + condition + "' -> day drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "cloudy":
            case "clouds":
                resourceId = com.example.homerepairs.R.drawable.cloudy;
                Log.d(TAG, "Condition '" + condition + "' -> cloudy drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "rainy":
            case "rain":
            case "drizzle":
                resourceId = com.example.homerepairs.R.drawable.rainy_3;
                Log.d(TAG, "Condition '" + condition + "' -> rainy_3 drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "snowy":
            case "snow":
                resourceId = com.example.homerepairs.R.drawable.snowy_3;
                Log.d(TAG, "Condition '" + condition + "' -> snowy_3 drawable (resource ID: " + resourceId + ")");
                return resourceId;
            case "stormy":
            case "thunderstorm":
                resourceId = com.example.homerepairs.R.drawable.thunder;
                Log.d(TAG, "Condition '" + condition + "' -> thunder drawable (resource ID: " + resourceId + ")");
                return resourceId;
            default:
                Log.w(TAG, "Unknown condition: '" + condition + "', using default day drawable");
                resourceId = com.example.homerepairs.R.drawable.day;
                return resourceId;
        }
    }
}

