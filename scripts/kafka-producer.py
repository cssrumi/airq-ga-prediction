import json

from kafka import KafkaProducer

TOPIC = 'data.enriched'
KAFKA_SERVER = '10.1.1.51:9092'
GROUP = 'TEST_GROUP'
TEST_EVENT = {
    "dateTime": 1588510198.421893000, "payload": {
        "enrichedData": [
            {
                "timestamp": 1588507200.000000000, "pm10": 20.675, "pm25": None, "temp": 285.92, "wind": 5.7, "windDirection": 300.0,
                "humidity": 76.0,
                "pressure": 1012.0, "lon": 18.635283, "lat": 54.353336, "provider": "GIOS", "station": "AM1 Gdańsk Śródmieście"
            },
            {
                "timestamp": 1588507200.000000000, "pm10": 28.4911, "pm25": 22.3114, "temp": 285.86, "wind": 5.7, "windDirection": 300.0,
                "humidity": 76.0,
                "pressure": 1012.0, "lon": 18.620274, "lat": 54.38028, "provider": "GIOS", "station": "AM8 Gdańsk Wrzeszcz"
            },
            {
                "timestamp": 1588507200.000000000, "pm10": 9.64694, "pm25": None, "temp": 285.83, "wind": 5.7, "windDirection": 300.0,
                "humidity": 76.0,
                "pressure": 1012.0, "lon": 18.579721, "lat": 54.431667, "provider": "GIOS", "station": "AM6 Sopot"
            },
            {
                "timestamp": 1588507200.000000000, "pm10": 12.9561, "pm25": None, "temp": 286.04, "wind": 5.7, "windDirection": 300.0,
                "humidity": 76.0,
                "pressure": 1012.0, "lon": 18.46491, "lat": 54.46576, "provider": "GIOS", "station": "AM9 Gdynia Dąbrowa"
            },
            {
                "timestamp": 1588507200.000000000, "pm10": 39.5612, "pm25": None, "temp": 285.89, "wind": 5.7, "windDirection": 300.0,
                "humidity": 76.0,
                "pressure": 1012.0, "lon": 18.657497, "lat": 54.400833, "provider": "GIOS", "station": "AM3 Gdańsk Nowy Port"
            }
        ]
    }, "eventType": "DataEnriched"
}


def produce(topic: str, event: dict):
    producer = KafkaProducer(bootstrap_servers=KAFKA_SERVER)

    future = producer.send(topic, json.dumps(event).encode())
    future.get(timeout=2)


if __name__ == '__main__':
    produce(TOPIC, TEST_EVENT)
