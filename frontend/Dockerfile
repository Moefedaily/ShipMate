# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies early to cache better
COPY package.json package-lock.json ./
RUN npm install

# Copy the rest of the app
COPY . .
RUN npm run build -- --configuration production

# ---- Runtime stage ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]