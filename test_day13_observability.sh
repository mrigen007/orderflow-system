#!/bin/bash

echo "========================================="
echo "  Day 13: Observability Testing"
echo "========================================="
echo ""

# Test 1: Health Checks
echo "1️⃣ Testing Health Endpoints..."
echo ""
echo "Order Service:"
curl -s http://localhost:8080/actuator/health | jq .

echo ""
echo "Inventory Service:"
curl -s http://localhost:8082/actuator/health | jq .

echo ""
echo "Notification Service:"
curl -s http://localhost:8081/actuator/health | jq .

sleep 2
echo ""
echo "========================================="

# Test 2: Initialize & Create Order
echo "2️⃣ Creating Test Order (generates traces & metrics)..."
echo ""

curl -X POST http://localhost:8082/api/v1/inventory/initialize \
  -H "X-Tenant-Id: day13-test" \
  -H "Content-Type: application/json"

echo ""
sleep 2

ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: day13-test" \
  -d '{
    "customerId": 1001,
    "items": [
      {"productId": 101, "productName": "Laptop", "quantity": 2, "price": 1299.99}
    ]
  }')

echo "Order Created:"
echo "$ORDER_RESPONSE" | jq .

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')
echo ""
echo "Order ID: $ORDER_ID"

sleep 5
echo ""
echo "========================================="

# Test 3: Prometheus Metrics
echo "3️⃣ Checking Prometheus Metrics..."
echo ""

echo "Order Service - HTTP Request Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep "http_server_requests_seconds_count" | head -3

echo ""
echo "Order Service - JVM Memory:"
curl -s http://localhost:8080/actuator/prometheus | grep "jvm_memory_used_bytes{" | head -3

echo ""
echo "Inventory Service - HTTP Metrics:"
curl -s http://localhost:8082/actuator/prometheus | grep "http_server_requests_seconds_count" | head -3

sleep 2
echo ""
echo "========================================="

# Test 4: Check Structured Logs
echo "4️⃣ Checking Structured JSON Logs..."
echo ""

echo "Order Service Logs (last 5 JSON entries):"
docker-compose logs order-service --tail=5 | grep -E '\{.*"service".*\}' | tail -3

echo ""
echo "Inventory Service Logs (last 5 JSON entries):"
docker-compose logs inventory-service --tail=5 | grep -E '\{.*"service".*\}' | tail -3

sleep 2
echo ""
echo "========================================="

# Test 5: Zipkin Traces
echo "5️⃣ Distributed Tracing with Zipkin..."
echo ""

echo "Checking Zipkin API for traces..."
TRACES=$(curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=1")

if [ "$TRACES" != "[]" ]; then
    echo "✅ Traces found in Zipkin!"
    echo "$TRACES" | jq '.[0] | {traceId: .[0].traceId, duration: .[0].duration, spans: (. | length)}'
else
    echo "⚠️  No traces yet (may take a moment to appear)"
fi

echo ""
echo "📊 Open Zipkin UI: http://localhost:9411"
echo "   - Click 'Run Query' to see traces"
echo "   - Look for 'order-service' traces"
echo "   - Click trace to see request flow: Order → Inventory → Notification"

echo ""
echo "========================================="
echo ""

# Summary
echo "✅ Day 13 Observability Test Complete!"
echo ""
echo "📊 Access Points:"
echo ""
echo "Health Checks:"
echo "  - Order: http://localhost:8080/actuator/health"
echo "  - Inventory: http://localhost:8082/actuator/health"
echo "  - Notification: http://localhost:8081/actuator/health"
echo ""
echo "Prometheus Metrics:"
echo "  - Order: http://localhost:8080/actuator/prometheus"
echo "  - Inventory: http://localhost:8082/actuator/prometheus"
echo "  - Notification: http://localhost:8081/actuator/prometheus"
echo ""
echo "Distributed Tracing:"
echo "  - Zipkin UI: http://localhost:9411"
echo ""
echo "Logs:"
echo "  - docker-compose logs order-service"
echo "  - docker-compose logs inventory-service"
echo "  - docker-compose logs notification-service"
echo ""
