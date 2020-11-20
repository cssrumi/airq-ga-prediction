package pl.airq.prediction.ga.integration;

class DBConstant {

    static final String CREATE_PREDICTION_TABLE = "CREATE TABLE PREDICTION\n" +
            "(\n" +
            "    id             BIGSERIAL PRIMARY KEY,\n" +
            "    timestamp      TIMESTAMPTZ,\n" +
            "    config         JSONB,\n" +
            "    stationid      VARCHAR,\n" +
            "    value          DOUBLE PRECISION\n" +
            ")";

    static final String DROP_PREDICTION_TABLE = "DROP TABLE IF EXISTS PREDICTION";
}
