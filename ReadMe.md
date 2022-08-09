# Project 100

Stock Exchange - Price Time Priority Algorithm

[https://gitorko.github.io/](https://gitorko.github.io/)

### Postgres DB

```
docker run -p 5432:5432 --name pg-container -e POSTGRES_PASSWORD=password -d postgres:9.6.10
docker ps
docker run -it --rm --link pg-container:postgres postgres:9.6.10 psql -h postgres -U postgres
CREATE USER test WITH PASSWORD 'test@123';
CREATE DATABASE "test-db" WITH OWNER "test" ENCODING UTF8 TEMPLATE template0;
grant all PRIVILEGES ON DATABASE "test-db" to test;
```


To Run UI in DEV mode

```bash
cd project100/ui
yarn install
yarn build
yarn start
```

### Prod
To run as a single jar, both UI and backend are bundled to single uber jar.

Postgres DB is need to run the integration tests during build.

```bash
./gradlew cleanBuild
cd project100/build/libs
java -jar project100-1.0.0.jar
```

```bash
docker build -f docker/Dockerfile --force-rm -t project100:1.0.0 .
docker images |grep project100
docker tag project100:1.0.0 gitorko/project100:1.0.0
docker push gitorko/project100:1.0.0
docker-compose -f docker/docker-compose.yml up 
```
