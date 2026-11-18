FROM clojure:lein-2.9.10

WORKDIR /usr/src/app/

COPY project.clj /usr/src/app
RUN lein deps

COPY . /usr/src/app

EXPOSE 9090
ENV DBHOST=mongo

ENTRYPOINT ["lein", "run"]
