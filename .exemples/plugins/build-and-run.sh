#!/bin/bash

set -e

echo "======================================"
echo "PF4J Spring Boot Starter - Quick Start"
echo "======================================"
echo ""

# Step 1: Install the starter
echo "[1/4] Installing PF4J Spring Boot Starter..."
cd ../../
mvn clean install -q -DskipTests
echo "✓ Starter installed"
echo ""

# Step 2: Build the plugin
echo "[2/4] Building greeting plugin..."
cd .exemples/plugins/plugin
mvn clean package -q
echo "✓ Plugin built: target/greeting-plugin-1.0.0.jar"
echo ""

# Step 3: Setup plugins directory
echo "[3/4] Setting up plugins directory..."
cd ../app
mkdir -p plugins
cp ../plugin/target/greeting-plugin-1.0.0.jar plugins/
echo "✓ Plugin copied to app/plugins/"
echo ""

# Step 4: Run the application
echo "[4/4] Starting application..."
echo "======================================"
echo ""
echo "Application will start on http://localhost:8080"
echo ""
echo "Try these commands:"
echo "  curl http://localhost:8080/api/plugins"
echo "  curl http://localhost:8080/api/greet?name=John"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""
echo "======================================"
echo ""

mvn spring-boot:run
