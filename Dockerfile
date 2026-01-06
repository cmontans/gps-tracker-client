# Dockerfile for GPS Tracker Server (Koyeb Deployment)
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY server/package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy server source code
COPY server/ ./

# Expose port (Koyeb will override with $PORT)
EXPOSE 8080

# Start the server
CMD ["npm", "start"]
