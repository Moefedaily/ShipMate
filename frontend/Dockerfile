# ---- Base image for building and development ----
FROM node:20-alpine as dev
WORKDIR /app

# Install dependencies early to cache better
COPY package.json package-lock.json ./
RUN npm install

# Copy the rest of the app
COPY . .

# Expose the default Angular dev port
EXPOSE 4200

# Start Angular CLI dev server
CMD ["npm", "run", "start"]
