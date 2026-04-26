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
# 把模板放到 /tmp，不使用官方的模板自动替换机制
COPY ruoyi-ui/nginx.conf.template /tmp/default.conf.template
EXPOSE 80
# 用 sed 将 __PORT__ 替换为真实端口（从环境变量 $PORT 读取，默认 80）
CMD ["/bin/sh", "-c", "sed \"s|__PORT__|${PORT:-80}|\" /tmp/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]