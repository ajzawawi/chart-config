
| **PME ID** | **ISIN**     | **CUSIP** | **SEDOL** | **S3D\_ID** | **MLSEC\_NO** | **S3D\_NO\_MARKET** | **MARKET** | **MARKET CODE** | **MARKET ID** | **TRADE CURRENCY** |
| ---------- | ------------ | --------- | --------- | ----------- | ------------- | ------------------- | ---------- | --------------- | ------------- | ------------------ |
| PME12345   | US1234567890 | 123456789 | B0YBKL9   | S3D001      | ML001         | S3D001\_DE          | Germany    | DE              | MKT001        | EUR                |
|            |              |           |           | S3D002      | ML002         | S3D002\_EU          | Euroclear  | EUCL            | MKT002        | EUR                |
|            |              |           |           | S3D003      | ML003         | S3D003\_US          | USA        | US              | MKT003        | USD                |
|            |              |           | B1ZKJL8   | S3D004      | ML004         | S3D004\_FR          | France     | FR              | MKT004        | EUR                |
|            |              |           |           | S3D005      | ML005         | S3D005\_BE          | Belgium    | BE              | MKT005        | EUR                |


# 1. Canonical Hash (keyed by PME ID)

pme:PME12345 → HASH

```
HSET pme:PME12345 \
  isin "US1234567890" \
  cusip "123456789" \
  sedol "B0YBKL9,B1ZKJL8" \
  s3d_ids "S3D001,S3D002,S3D003,S3D004,S3D005" \
  mlsec_nos "ML001,ML002,ML003,ML004,ML005" \
  s3d_market_map '{
    "S3D001": { "mlsec": "ML001", "market": "Germany",   "code": "DE",    "id": "MKT001", "currency": "EUR" },
    "S3D002": { "mlsec": "ML002", "market": "Euroclear", "code": "EUCL",  "id": "MKT002", "currency": "EUR" },
    "S3D003": { "mlsec": "ML003", "market": "USA",       "code": "US",    "id": "MKT003", "currency": "USD" },
    "S3D004": { "mlsec": "ML004", "market": "France",    "code": "FR",    "id": "MKT004", "currency": "EUR" },
    "S3D005": { "mlsec": "ML005", "market": "Belgium",   "code": "BE",    "id": "MKT005", "currency": "EUR" }
  }' \
  mlsec_market_map '{
    "ML001": { "s3d": "S3D001", "market": "Germany",   "code": "DE",    "id": "MKT001", "currency": "EUR" },
    "ML002": { "s3d": "S3D002", "market": "Euroclear", "code": "EUCL",  "id": "MKT002", "currency": "EUR" },
    "ML003": { "s3d": "S3D003", "market": "USA",       "code": "US",    "id": "MKT003", "currency": "USD" },
    "ML004": { "s3d": "S3D004", "market": "France",    "code": "FR",    "id": "MKT004", "currency": "EUR" },
    "ML005": { "s3d": "S3D005", "market": "Belgium",   "code": "BE",    "id": "MKT005", "currency": "EUR" }
  }'
```
#2 Reverse Pointers
| **Pointer Key** | **Value**  | **Purpose**           |
| --------------- | ---------- | --------------------- |
| `mlsec:ML001`   | `PME12345` | Maps MLSEC to PME ID  |
| `mlsec:ML002`   | `PME12345` |                       |
| `mlsec:ML003`   | `PME12345` |                       |
| `mlsec:ML004`   | `PME12345` |                       |
| `mlsec:ML005`   | `PME12345` |                       |
| `s3d:S3D001`    | `PME12345` | Maps S3D ID to PME ID |
| `s3d:S3D002`    | `PME12345` |                       |
| `s3d:S3D003`    | `PME12345` |                       |
| `s3d:S3D004`    | `PME12345` |                       |
| `s3d:S3D005`    | `PME12345` |                       |


2. Reverse lookup for MLSEC_NO and S3D_ID → PME ID

```
mlsec:ML001 → PME12345
mlsec:ML002 → PME12345
...
s3d:S3D001  → PME12345
s3d:S3D002  → PME12345
...
```

🔍 Lookup by MLSEC_NO

```
GET mlsec:ML002 → PME12345
HGETALL pme:PME12345
```

🔍 Lookup by S3D_ID
```
GET s3d:S3D003 → PME12345
HGETALL pme:PME12345
```

🔍 Lookup by PME_ID directly
```
HGETALL pme:PME12345
```

| Lookup Field | Redis Key Pattern | Value Type | Purpose                |
| ------------ | ----------------- | ---------- | ---------------------- |
| PME ID       | `pme:<pmeid>`     | `HASH`     | Canonical record       |
| MLSEC\_NO    | `mlsec:<mlsec>`   | `STRING`   | Reverse pointer to PME |
| S3D\_ID      | `s3d:<s3d>`       | `STRING`   | Reverse pointer to PME |

# Example JSON

```json
{
  "pmeId": "PME12345",
  "isin": "US1234567890",
  "cusip": "123456789",
  "sedol": "B0YBKL9,B1ZKJL8",
  "s3d_ids": "S3D001,S3D002,S3D003,S3D004,S3D005",
  "mlsec_nos": "ML001,ML002,ML003,ML004,ML005",
  "s3d_market_map": {
    "S3D001": {
      "mlsec": "ML001",
      "market": "Germany",
      "code": "DE",
      "id": "MKT001",
      "currency": "EUR"
    },
    "S3D002": {
      "mlsec": "ML002",
      "market": "Euroclear",
      "code": "EUCL",
      "id": "MKT002",
      "currency": "EUR"
    },
    "S3D003": {
      "mlsec": "ML003",
      "market": "USA",
      "code": "US",
      "id": "MKT003",
      "currency": "USD"
    },
    "S3D004": {
      "mlsec": "ML004",
      "market": "France",
      "code": "FR",
      "id": "MKT004",
      "currency": "EUR"
    },
    "S3D005": {
      "mlsec": "ML005",
      "market": "Belgium",
      "code": "BE",
      "id": "MKT005",
      "currency": "EUR"
    }
  },
  "mlsec_market_map": {
    "ML001": {
      "s3d": "S3D001",
      "market": "Germany",
      "code": "DE",
      "id": "MKT001",
      "currency": "EUR"
    },
    "ML002": {
      "s3d": "S3D002",
      "market": "Euroclear",
      "code": "EUCL",
      "id": "MKT002",
      "currency": "EUR"
    },
    "ML003": {
      "s3d": "S3D003",
      "market": "USA",
      "code": "US",
      "id": "MKT003",
      "currency": "USD"
    },
    "ML004": {
      "s3d": "S3D004",
      "market": "France",
      "code": "FR",
      "id": "MKT004",
      "currency": "EUR"
    },
    "ML005": {
      "s3d": "S3D005",
      "market": "Belgium",
      "code": "BE",
      "id": "MKT005",
      "currency": "EUR"
    }
  }
}

```