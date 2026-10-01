FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/client/browser ./

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
