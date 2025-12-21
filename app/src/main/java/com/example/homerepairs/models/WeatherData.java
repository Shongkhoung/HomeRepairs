package com.example.homerepairs.models;

public class WeatherData {
    private String temperature;
    private String condition;
    private String description;
    private String icon;
    private int iconResId;

    public WeatherData(String temperature, String condition, String description, String icon) {
        this.temperature = temperature;
        this.condition = condition;
        this.description = description;
        this.icon = icon;
        this.iconResId = getIconResourceId(condition);
    }

    public WeatherData(String temperature, String condition, String description, String icon, int iconResId) {
        this.temperature = temperature;
        this.condition = condition;
        this.description = description;
        this.icon = icon;
        this.iconResId = iconResId;
    }

    private int getIconResourceId(String condition) {
        switch (condition.toLowerCase()) {
            case "sunny":
            case "clear":
                return com.example.homerepairs.R.drawable.day;
            case "cloudy":
            case "clouds":
                return com.example.homerepairs.R.drawable.cloudy;
            case "rainy":
            case "rain":
            case "drizzle":
                return com.example.homerepairs.R.drawable.rainy_3;
            case "snowy":
            case "snow":
                // Cambodia doesn't have snow, use rainy icon instead
                return com.example.homerepairs.R.drawable.rainy_3;
            case "stormy":
            case "thunderstorm":
                return com.example.homerepairs.R.drawable.thunder;
            default:
                return com.example.homerepairs.R.drawable.day;
        }
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDisplayText() {
        return temperature + " " + condition;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    /**
     * Get Lottie animation JSON file name based on weather condition
     * Note: For rain, returns "day_rain" or "night_rain" based on time of day
     * Note: Uses sunny_weather.json for weather conditions (not sunny.json which is for time-based icons)
     * @param isDayTime true if current time is between 5 AM and 9 PM, false otherwise
     * @return Lottie JSON file name (e.g., "sunny_weather.json", "day_rain.json")
     */
    public String getLottieFileName(boolean isDayTime) {
        String conditionLower = condition.toLowerCase();
        switch (conditionLower) {
            case "sunny":
            case "clear":
                return "sunny_weather.json";
            case "cloudy":
            case "clouds":
                // Use sunny_weather.json for cloudy since cloudy.json doesn't exist
                return "sunny_weather.json";
            case "rainy":
            case "rain":
            case "drizzle":
                // Use day_rain.json or night_rain.json based on time of day
                return isDayTime ? "day_rain.json" : "night_rain.json";
            case "snowy":
            case "snow":
                // Cambodia doesn't have snow, map to rain based on time of day
                return isDayTime ? "day_rain.json" : "night_rain.json";
            case "stormy":
            case "thunderstorm":
                return "thunder.json";
            case "foggy":
            case "mist":
            case "fog":
                return "fog.json";
            default:
                return "sunny_weather.json";
        }
    }
    
    /**
     * Get Lottie animation JSON file name based on weather condition (without time check)
     * For backward compatibility - uses day_rain as default for rain
     * @return Lottie JSON file name
     */
    public String getLottieFileName() {
        return getLottieFileName(true); // Default to day time
    }
}

