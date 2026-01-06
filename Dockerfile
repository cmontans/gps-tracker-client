# Dockerfile for GPS Tracker Server (Koyeb Deployment)
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY server/package.json server/package-lock.json ./

# Install dependencies
RUN npm ci --omit=dev

# Copy server source code
COPY server/ ./

# Expose port (Koyeb will override with $PORT)
EXPOSE 8080

# Start the server
CMD ["npm", "start"]
