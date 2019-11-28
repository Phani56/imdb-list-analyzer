FROM clojure:openjdk-11-lein
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN lein uberjar && \
    useradd -m app_user && \
    chown -R app_user .
USER app_user
ENV JAVA_OPTS='-XX:+UseContainerSupport -Xmx300m -Xss512k'
ENV PORT=8080
EXPOSE $PORT
CMD java $JAVA_OPTS -jar target/imdb-list-analyzer-standalone.jar $PORT
