# Multi-stage build for TemplateGUI

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY delivery ./delivery
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install X11 libraries for GUI
RUN apt-get update && apt-get install -y \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libx11-6 \
    fontconfig \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Copy distribution
COPY --from=build /app/target/*-distribution.zip .
RUN unzip *-distribution.zip && rm *.zip && \
    mv TemplateGUI-* TemplateGUI

WORKDIR /app/TemplateGUI

# Set environment for GUI
ENV DISPLAY=:0

# Run the application
CMD ["java", "-cp", "lib/*", "de.cavdar.gui.view.MainView"]
