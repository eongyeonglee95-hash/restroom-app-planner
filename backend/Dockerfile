# 백엔드(Spring Boot)와 도보 경로 서버(OSRM)를 한 이미지에 담는다. Cloud Run 무료
# 티어는 컨테이너 1개당 과금 기준이 잡히므로, 서비스를 둘로 쪼개면 그만큼 무료 한도
# 소진이 빨라진다 - 서울만 다루는 MVP 규모에서는 한 이미지가 더 유리하다.
#
# 최종 이미지 베이스를 OSRM 공식 이미지(ghcr.io/project-osrm/osrm-backend) 그대로 쓰는 이유:
# osrm-routed가 필요로 하는 공유 라이브러리(TBB 등)가 이미 정확히 맞춰져 있어서다. 바이너리만
# 뽑아 다른 베이스 이미지로 옮기면 라이브러리 버전이 어긋나 런타임에 실패할 위험이 있는데,
# 이 프로젝트는 로컬에 Docker가 없어 빌드해서 직접 검증할 수 없으므로 그 위험을 피한다.
# JRE는 Debian 저장소의 openjdk 패키지 유무를 확신할 수 없어(java 25는 아직 최신 버전이라
# 배포판 저장소 반영이 늦을 수 있음), Eclipse Adoptium 공식 API에서 타르볼로 직접 받는다
# (기존 백엔드 빌드가 쓰는 eclipse-temurin과 같은 배포처).

# ---- 1. Spring Boot jar 빌드 ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
RUN ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- 2. 전국 OSM 추출본에서 서울만 잘라내기 (272MB -> 41MB) ----
FROM ubuntu:24.04 AS osm-clip
RUN apt-get update && apt-get install -y --no-install-recommends \
      osmium-tool curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /osm
RUN curl -fSL -o south-korea.osm.pbf \
      https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
# 서울시 경계 bbox. MVP가 서울 한정이라(기획서 참고) 이 범위만 필요하다.
RUN osmium extract --bbox 126.70,37.35,127.25,37.78 \
      -o seoul.osm.pbf south-korea.osm.pbf

# ---- 3. OSRM 보행자(foot) 프로필로 전처리 (MLD) ----
# 프로필은 OSRM 공식 이미지 내부 경로를 추측하는 대신, 로컬에서 실제로 검증한 파일을
# backend/osrm-profile/ 에 그대로 복사해 커밋해 두고 그걸 쓴다.
FROM ghcr.io/project-osrm/osrm-backend:v26.8.0-amd64-debian AS osrm-prep
WORKDIR /data
COPY osrm-profile /data/profile
COPY --from=osm-clip /osm/seoul.osm.pbf /data/seoul.osm.pbf
RUN osrm-extract -p /data/profile/foot.lua /data/seoul.osm.pbf \
    && osrm-partition /data/seoul.osrm \
    && osrm-customize /data/seoul.osrm

# ---- 4. 최종 실행 이미지: OSRM 공식 베이스 + JRE 추가 ----
FROM ghcr.io/project-osrm/osrm-backend:v26.8.0-amd64-debian
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Eclipse Adoptium 공식 API로 Temurin JRE 25 타르볼을 받는다 (Debian 저장소의 openjdk
# 패키지 유무를 가정하지 않기 위함 - api.adoptium.net/v3/binary/latest/... 로 실제
# 호출해 200 응답과 정상 tar.gz를 확인했다).
RUN curl -fSL -o /tmp/jre.tar.gz \
      "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jre/hotspot/normal/eclipse" \
    && mkdir -p /opt/java \
    && tar -xzf /tmp/jre.tar.gz -C /opt/java --strip-components=1 \
    && rm /tmp/jre.tar.gz
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app
COPY --from=osrm-prep /data/seoul.osrm* /app/osrm-data/
COPY --from=build /app/build/libs/*.jar app.jar
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Cloud Run이 주입하는 $PORT로 Spring Boot가 뜬다(application.properties의
# server.port=${PORT:8080} 참고). management(actuator)는 8081 - 외부에 안 열림.
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
