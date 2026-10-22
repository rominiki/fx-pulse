from pathlib import Path

import pandas as pd
import requests


API_URL = "https://api.frankfurter.dev/v2/providers/ecb/rates"

PARAMS = {
    "from": "2025-01-01",
    "to": "2025-01-31",
    "base": "eur",
    "quotes": "usd",
}


def main():

    print("Fetching historical EUR/USD rates...")

    response = requests.get(
        API_URL,
        params=PARAMS,
        timeout=30,
    )
    response.raise_for_status()

    rows = response.json()

    if not isinstance(rows, list) or not rows:
        raise ValueError("API did not return a non-empty list")

    df = pd.DataFrame(rows)

    required = {"date", "base", "quote", "rate"}
    if not required.issubset(df.columns):
        raise ValueError("Unexpected API response structure")

    df = df[["date", "base", "quote", "rate"]].copy()

    df["date"] = pd.to_datetime(df["date"], errors="raise")
    df["rate"] = pd.to_numeric(df["rate"], errors="raise")

    df = df.sort_values("date").reset_index(drop=True)

    if df[["date", "base", "quote", "rate"]].isna().any().any():
        raise ValueError("Missing required data")

    if (df["rate"] <= 0).any():
        raise ValueError("Invalid non-positive rate")

    if not ((df["base"] == "EUR") & (df["quote"] == "USD")).all():
        raise ValueError("Unexpected currency pair")

    if df["date"].duplicated().any():
        raise ValueError("Duplicate observation dates")

    df["return_pct"] = (
        df["rate"].pct_change(fill_method=None) * 100
    )

    df["mean_5"] = df["rate"].rolling(
        window=5,
        min_periods=5,
    ).mean()

    print("\nEUR/USD analytics:")
    print(
        df[["date", "rate", "return_pct", "mean_5"]]
        .to_string(index=False)
    )

    assert pd.isna(df.loc[0, "return_pct"])
    assert df["mean_5"].iloc[:4].isna().all()

    if len(df) >= 5:
        expected_mean = df["rate"].iloc[:5].mean()
        actual_mean = df.loc[4, "mean_5"]

        assert abs(expected_mean - actual_mean) < 1e-10

    output_path = (
        Path(__file__).resolve().parent
        / "eur_usd_jan_2025.csv"
    )

    df.to_csv(
        output_path,
        index=False,
        date_format="%Y-%m-%d",
    )

    print(f"\nSaved {len(df)} records to:")
    print(output_path)
    print("\nAll checks passed!")


if __name__ == "__main__":
    main()
