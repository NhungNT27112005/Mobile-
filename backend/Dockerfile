FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts
COPY src ./src
COPY migrations ./migrations
COPY seeds ./seeds
USER node
EXPOSE 8000
CMD ["node", "src/server.mjs"]

FROM runtime AS test
COPY tests ./tests
CMD ["npm", "test"]
