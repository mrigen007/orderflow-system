#!/bin/bash

echo "========================================="
echo "   Day 13 - Final Verification Test"
echo "========================================="
echo ""

# Initialize
echo "🔧 Initializing inventory..."
curl -X POST http://localhost:8082/api/v1/inventory/initialize \
  -H "X-Tenant-Id: final-test" \
  -H "Content-Type: application/json"

echo ""
echo ""

# Create Order
echo "📦 Creating Order..."
ORDER=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: final-test" \
  -d '{
    "customerId": 999,
    "items": [
      {"productId": 101, "productName": "Laptop", "quantity": 1, "price": 1299.99},
      {"productId": 102, "productName": "Mouse", "quantity": 2, "price": 49.99}
    ]
  }')

echo "$ORDER"
ORDER_ID=$(echo "$ORDER" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo ""
echo "Order ID: $ORDER_ID"

sleep 5

echo ""
echo "========================================="
echo "   Observability Verification"
echo "========================================="
echo ""

# Check Metrics
echo "📊 1. PROMETHEUS METRICS"
echo ""
echo "Order Service - Total Requests:"
curl -s http://localhost:8080/actuator/prometheus | grep "http_server_requests_seconds_count" | wc -l

echo ""
echo "Inventory Service - Stock Reserved:"
curl -s http://localhost:8082/actuator/prometheus | grep "http_server_requests_seconds_count" | wc -l

echo ""
echo "Sample Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep 'http_server_requests_seconds_count.*POST.*orders' | head -2

echo ""
echo ""

# Check Traces
echo "🔍 2. DISTRIBUTED TRACING"
echo ""
TRACE_COUNT=$(curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=10" | grep -o "traceId" | wc -l)
echo "Traces in Zipkin: $TRACE_COUNT"
echo ""
echo "Latest Trace:"
curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=1" | grep -o '"name":"[^"]*"' | head -5

echo ""
echo ""

# Check Logs
echo "📝 3. STRUCTURED LOGS"
echo ""
echo "Order Service (JSON format):"
docker-compose logs order-service --tail=3 2>/dev/null | grep -E '"service":"order-service"' | head -1 || echo "Logs available via: docker-compose logs order-service"

echo ""
echo ""

# Check Health
echo "❤️  4. HEALTH CHECKS"
echo ""
echo "Order Service:"
curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | head -1

echo ""
echo "Inventory Service:"
curl -s http://localhost:8082/actuator/health | grep -o '"status":"[^"]*"' | head -1

echo ""
echo "Notification Service:"
curl -s http://localhost:8081/actuator/health | grep -o '"status":"[^"]*"' | head -1

echo ""
echo ""

# Verify Order Flow
echo "✅ 5. ORDER FLOW VERIFICATION"
echo ""
echo "Checking Inventory Stock (should show reserved):"
curl -s http://localhost:8082/api/v1/inventory/stock/101 \
  -H "X-Tenant-Id: final-test"

echo ""
echo ""
echo "Checking Notifications:"
curl -s http://localhost:8081/api/v1/notifications/order/$ORDER_ID \
  -H "X-Tenant-Id: final-test"

echo ""
echo ""

echo "========================================="
echo "   ✅ Day 13 Verification Complete!"
echo "========================================="
echo ""
echo "📊 Access Observability Stack:"
echo ""
echo "🔹 Zipkin UI (Distributed Tracing):"
echo "   http://localhost:9411"
echo "   → Click 'Run Query' to see request traces"
echo "   → See how requests flow: Order → Kafka → Inventory → Notification"
echo ""
echo "🔹 Prometheus Metrics:"
echo "   Order:        http://localhost:8080/actuator/prometheus"
echo "   Inventory:    http://localhost:8082/actuator/prometheus"
echo "   Notification: http://localhost:8081/actuator/prometheus"
echo ""
echo "🔹 Health Endpoints:"
echo "   Order:        http://localhost:8080/actuator/health"
echo "   Inventory:    http://localhost:8082/actuator/health"
echo "   Notification: http://localhost:8081/actuator/health"
echo ""
echo "🔹 Grafana Dashboard (optional):"
echo "   http://localhost:3000"
echo "   Default: admin/admin"
echo ""
echo "🔹 View Structured Logs:"
echo "   docker-compose logs order-service"
echo "   docker-compose logs inventory-service"
echo "   docker-compose logs notification-service"
echo ""
