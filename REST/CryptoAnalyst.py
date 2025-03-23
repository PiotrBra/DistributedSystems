from fastapi import FastAPI, HTTPException, Query
import os
from datetime import datetime
from dotenv import load_dotenv
from starlette.staticfiles import StaticFiles
import httpx

app = FastAPI()
load_dotenv()

COINGECKO_API_URL = "https://api.coingecko.com/api/v3"
NEWS_API_URL = "https://newsapi.org/v2/everything"
NEWS_API_KEY = os.getenv("NEWS_API_KEY")

# static/index.html
app.mount("/static", StaticFiles(directory="static", html=True), name="static")


async def fetch_crypto_prices(crypto_id: str, from_date: str, to_date: str):
    try:
        start_date = datetime.strptime(from_date, "%Y-%m-%d")
        end_date = datetime.strptime(to_date, "%Y-%m-%d")
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid date format. Use YYYY-MM-DD.")

    if start_date > end_date:
        raise HTTPException(status_code=400, detail="Start date must be earlier than end date.")

    start_timestamp = int(start_date.timestamp())
    end_timestamp = int(end_date.timestamp())

    url = f"{COINGECKO_API_URL}/coins/{crypto_id}/market_chart/range"
    params = {
        "vs_currency": "usd",
        "from": start_timestamp,
        "to": end_timestamp
    }
    async with httpx.AsyncClient() as client:
        response = await client.get(url, params=params)
        if response.status_code != 200:
            try:
                error_message = response.json().get("error", "Unknown error")
            except Exception:
                error_message = "Unknown error"
            raise HTTPException(
                status_code=response.status_code,
                detail=f"Error fetching data from CoinGecko: {error_message}"
            )
        return response.json()


def calculate_statistics(prices):
    if not prices:
        raise HTTPException(status_code=404, detail="No price data available for the given date range.")

    price_values = [price[1] for price in prices]
    return {
        "current_price": price_values[-1],
        "average_price": sum(price_values) / len(price_values),
        "max_price": max(price_values),
        "min_price": min(price_values)
    }


async def fetch_crypto_news(crypto_id: str):
    params = {
        "q": crypto_id,
        "apiKey": NEWS_API_KEY,
        "pageSize": 20,
        "sortBy": "publishedAt",
        "language": "en"
    }
    async with httpx.AsyncClient() as client:
        response = await client.get(NEWS_API_URL, params=params)
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail="Error fetching news data.")
        articles = response.json().get("articles", [])
        articles_data = [{"title": article["title"], "url": article["url"]} for article in articles]
        return {"crypto": crypto_id, "articles": articles_data}


@app.get("/crypto/stats/{crypto_id}")
async def get_crypto_stats(
        crypto_id: str,
        from_date: str = Query(..., description="Start date (YYYY-MM-DD)"),
        to_date: str = Query(..., description="End date (YYYY-MM-DD)")
):
    data = await fetch_crypto_prices(crypto_id, from_date, to_date)
    if "prices" not in data:
        raise HTTPException(status_code=404, detail="Price data not found in API response.")
    statistics = calculate_statistics(data["prices"])
    return statistics


@app.get("/crypto/news/{crypto_id}")
async def get_crypto_news(crypto_id: str):
    news_data = await fetch_crypto_news(crypto_id)
    return news_data
