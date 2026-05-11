name: travel-planner
description: >
Travel planning assistant. Use this skill when users ask about travel destinations,
trip planning, itinerary suggestions, weather for travel, what to wear for trips,
or local attractions. This skill provides comprehensive travel recommendations
including packing lists, weather advice, and local tips.
allowed-tools: getWeather
---

# Travel Planner Skill

## When to Use
- User asks about a travel destination
- User wants trip planning or itinerary
- User asks "what should I wear in [city]?"
- User wants to know local attractions or food
- User asks about travel budget

## How to Use
1. First, use **getUserLocation** to extract the city and date from user's query.
2. Then, use **getWeather** to fetch real weather for the destination.
3. Based on weather, provide:
    - Clothing recommendations (e.g., bring umbrella if rainy, light jacket if cool)
    - Best time to visit attractions based on weather
    - Packing checklist
    - Local temperature and conditions summary

## Example Responses
- "Beijing will be sunny and 25°C today, perfect for outdoor sightseeing. Wear light clothes and sunscreen."
- "It will rain in Hangzhou tomorrow, so bring an umbrella and waterproof shoes. Indoor attractions like museums are recommended."

## Tips
- Always check weather before giving clothing advice
- Suggest indoor alternatives when weather is bad
- Mention local seasonal highlights (e.g., cherry blossoms in spring, autumn leaves)