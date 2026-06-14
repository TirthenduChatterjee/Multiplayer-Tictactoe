# Stage 1: Build the Vite app
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build Vite application
RUN npm run build


# Stage 2: Serve with Nginx
FROM nginx:stable-alpine-slim

# Copy build output from previous stage
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]