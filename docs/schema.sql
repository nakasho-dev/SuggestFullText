-- stations table
CREATE TABLE stations_with_fulltext (
    station_cd BIGINT NOT NULL,
    station_name NVARCHAR(200) NOT NULL,
    station_name_k NVARCHAR(200) NOT NULL,
    station_name_r NVARCHAR(200) NOT NULL,
    address NVARCHAR(500) NOT NULL,
    lon FLOAT NOT NULL,
    lat FLOAT NOT NULL,
    geog AS geography::Point(lat, lon, 4326) PERSISTED,
    weight FLOAT NOT NULL CONSTRAINT DF_stations_weight DEFAULT (1.0),
    CONSTRAINT PK_stations_with_fulltext PRIMARY KEY (station_cd)
);

-- fulltext weight (single row)
CREATE TABLE fulltext_weight (
    id INT NOT NULL CONSTRAINT PK_fulltext_weight PRIMARY KEY,
    fulltext_rank_weight FLOAT NOT NULL CONSTRAINT DF_fulltext_rank_weight DEFAULT (1.0),
    exact_match_weight FLOAT NOT NULL CONSTRAINT DF_exact_match_weight DEFAULT (1000000.0),
    prefix_match_weight FLOAT NOT NULL CONSTRAINT DF_prefix_match_weight DEFAULT (10000.0),
    partial_match_weight FLOAT NOT NULL CONSTRAINT DF_partial_match_weight DEFAULT (100.0),
    station_weight_weight FLOAT NOT NULL CONSTRAINT DF_station_weight_weight DEFAULT (1.0),
    distance_weight FLOAT NOT NULL CONSTRAINT DF_distance_weight DEFAULT (1.0),
    distance_km_scale FLOAT NOT NULL CONSTRAINT DF_distance_km_scale DEFAULT (1.0),
    CONSTRAINT CK_fulltext_weight_single CHECK (id = 1)
);

INSERT INTO fulltext_weight (id) VALUES (5);

-- full-text index
CREATE FULLTEXT CATALOG ft_station AS DEFAULT;
CREATE FULLTEXT INDEX ON stations_with_fulltext (
    station_name LANGUAGE 1041,
    station_name_k LANGUAGE 1041,
    station_name_r LANGUAGE 1041
)
KEY INDEX PK_stations_with_fulltext
WITH STOPLIST = OFF;

CREATE SPATIAL INDEX IX_stations_with_fulltext_geog
ON stations_with_fulltext(geog);
