FROM clojure:openjdk-11-lein
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN lein uberjar
ENV JAVA_OPTS='-XX:+UseContainerSupport -Xmx300m -Xss512k'
CMD java $JAVA_OPTS -jar target/imdb-list-analyzer-standalone.jar $PORT
