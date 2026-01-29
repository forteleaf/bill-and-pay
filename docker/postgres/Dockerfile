FROM postgres:18-alpine

# Install ltree extension
RUN apk add --no-cache postgresql-contrib

# Copy initialization scripts
COPY init-db.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/init-db.sh

# Set default timezone
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 5432
