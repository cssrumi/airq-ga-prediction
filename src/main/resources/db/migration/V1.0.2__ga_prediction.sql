CREATE TABLE GA_PREDICTION
(
    id             BIGSERIAL PRIMARY KEY,
    timestamp      TIMESTAMPTZ,
    pm10_after_1h  DOUBLE PRECISION,
    pm10_after_24h DOUBLE PRECISION,
    lon            DOUBLE PRECISION,
    lat            DOUBLE PRECISION
);
