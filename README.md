# Station Full-Text Search API

SQL Server の全文検索と位置情報検索を使った駅検索 API です。

## 機能

1. **CSV 取込（全件置換）**: CSV を受け取り、`stations_with_fulltext` を全削除して再登録します。
2. **キーワードのみ検索**: 完全一致・前方一致・部分一致（LIKE）の順でスコアが高くなるように評価します。
3. **キーワード＋位置情報検索**: 上記スコアに加えて、`geography` の距離スコアを加算します。

## 環境変数

| 環境変数 | 必須 | 説明 | 例 |
| --- | --- | --- | --- |
| `SUGGEST_DB_URL` | ○ | JDBC URL | `jdbc:sqlserver://server:1433;database=xxx;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;` |
| `SUGGEST_DB_USER` | ○ | DB ユーザー名 | `user@server` |
| `SUGGEST_DB_PASS` | ○ | DB パスワード | `your-password` |

例（Azure SQL）:
```bash
export SUGGEST_DB_URL="jdbc:sqlserver://recobook.database.windows.net:1433;database=nakashosqldb;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
export SUGGEST_DB_USER="your-user@recobook"
export SUGGEST_DB_PASS="your-password"
```

## データベースセットアップ

`docs/schema.sql` を SQL Server で実行してください。  
テーブル、全文検索カタログ/インデックス、空間インデックス、`fulltext_weight` 初期行を作成します。

```sql
-- docs/schema.sql を実行
```

## CSV 取込

**POST /api/stations/import**

```bash
curl -X POST "http://localhost:8080/api/stations/import" \
  -F "file=@/path/to/stations.csv"
```

CSV ヘッダー（必須）:
```csv
station_cd,station_name,station_name_k,station_name_r,address,lon,lat
```

レスポンス例:
```json
{"inserted": 9000}
```

## API

### 1) キーワード検索

**GET /api/stations/search**

```bash
curl "http://localhost:8080/api/stations/search?keyword=東京&limit=10"
```

レスポンス例:
```json
[
  {
    "stationCd": 1130101,
    "stationName": "東京",
    "stationNameK": "とうきょう",
    "stationNameR": "toukyou",
    "address": "東京都千代田区丸の内１丁目",
    "lon": 139.767125,
    "lat": 35.681236,
    "weight": 1.0,
    "score": 12345.0,
    "distanceKm": null
  }
]
```

### 2) キーワード＋位置情報検索

**GET /api/stations/search-geo**

```bash
curl "http://localhost:8080/api/stations/search-geo?keyword=中野&lat=35.68944&lon=139.70056&limit=11"
```

`distanceKm` が追加されます。

## スコア調整

`fulltext_weight` を更新してスコアの重みを調整できます。  
`id=1` の 1 行のみを更新してください。

例:
```sql
UPDATE fulltext_weight
SET exact_match_weight = 1000000,
    prefix_match_weight = 10000,
    partial_match_weight = 100,
    station_weight_weight = 1,
    distance_weight = 1,
    distance_km_scale = 1
WHERE id = 1;
```

## ビルド＆実行

```bash
./gradlew build
./gradlew bootRun
```

## 技術スタック

- Kotlin 2.2.21
- Spring Boot 4.0.3
- Spring Data JPA
- SQL Server / Azure SQL
