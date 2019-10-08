FROM clojure
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN lein uberjar
CMD java -jar target/imdb-list-analyzer-standalone.jar $PORT
