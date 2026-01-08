#!/bin/bash
# StandardMDIGUI Docker Starter for Linux
# ========================================

set -e

echo ""
echo "========================================"
echo " StandardMDIGUI Docker Launcher"
echo "========================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    echo "Please start Docker first: sudo systemctl start docker"
    exit 1
fi

# Allow X11 connections from Docker
echo "Allowing X11 connections from Docker..."
xhost +local:docker > /dev/null 2>&1 || true

# Export DISPLAY
export DISPLAY=${DISPLAY:-:0}

# Change to project root
cd "$(dirname "$0")/.."

echo "Starting PostgreSQL and Application..."
echo ""

# Build and start containers
docker-compose up --build -d

echo ""
echo "Waiting for services to start..."
sleep 5

# Show status
echo ""
echo "========================================"
echo " Container Status:"
echo "========================================"
docker-compose ps

echo ""
echo "========================================"
echo " Application Logs (Ctrl+C to exit):"
echo "========================================"
docker-compose logs -f app
