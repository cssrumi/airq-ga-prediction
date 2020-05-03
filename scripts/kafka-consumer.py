from kafka import KafkaConsumer

TOPIC = 'data.enriched'
KAFKA_SERVER = '10.1.1.51:9092'
GROUP = 'TEST_GROUP'


def consume(topic: str):
    consumer = KafkaConsumer(topic, bootstrap_servers=KAFKA_SERVER, group_id=GROUP)
    print("Consumer for {} created!".format(topic))
    for message in consumer:
        print(message)


def main():
    consume(TOPIC)


if __name__ == '__main__':
    main()
