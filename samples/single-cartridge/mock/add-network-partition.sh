echo "Adding network partition..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/network-partition.json' -kv -u admin:admin https://localhost:9443/api/networkPartitions