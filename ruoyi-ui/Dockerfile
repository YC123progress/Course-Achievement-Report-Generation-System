# ruoyi-ui/Dockerfile
FROM node:18-alpine AS builder
ARG CACHEBUST=20260424-1
RUN echo "Cache busted at $CACHEBUST"
WORKDIR /app
COPY ruoyi-ui/package*.json ./
RUN npm install
COPY ruoyi-ui/ .
RUN echo "Cache busted at $CACHEBUST" > /tmp/cache_buster.txt
RUN npm run build:prod

FROM nginx:stable-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# 注意这里：复制 .template 文件
COPY ruoyi-ui/nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]