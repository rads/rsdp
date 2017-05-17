curl -X POST -H "content-type: application/vnd.kafka.v2+json" \
  --data '{"name": "i72", "format": "binary", "auto.offset.reset": "earliest"}' \
  http://localhost:8082/consumers/$1

sleep 2

curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["t72"]}' \
  http://localhost:8082/consumers/$1/instances/i72/subscription

