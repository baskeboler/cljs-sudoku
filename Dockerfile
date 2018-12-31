FROM nginx
COPY public/. /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/nginx.conf
