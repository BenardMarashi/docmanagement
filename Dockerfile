FROM openjdk:17-jdk-alpine

# Install Tesseract OCR
RUN apk add --no-cache tesseract-ocr

# Ensure tessdata directory exists and download English language data manually
RUN mkdir -p /usr/share/tessdata && \
    wget -O /usr/share/tessdata/eng.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

# Set the environment variable for Tesseract data
ENV TESSDATA_PREFIX=/usr/share/tessdata

WORKDIR /app
COPY target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
