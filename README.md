# 참지마 모노레포

MVP 범위: 지도 표시 + "지금 급해!" 버튼(도보 시간순 정렬) + 급똥/약국 탭.
자세한 기획은 [`참지마_기획서.md`](./참지마_기획서.md) 참고.

## 구조

```
chamjima/
├── frontend/   React + TypeScript (Vite)
└── backend/    Spring Boot (Gradle, Java 25)
```

## 로컬 실행

### frontend (http://localhost:5173)

```
cd frontend
npm install
npm run dev
```

### backend (http://localhost:8080)

```
cd backend
./gradlew bootRun
```

헬스체크: `GET /api/health` → `{"status":"ok"}`

CORS는 로컬 개발 기준 `http://localhost:5173`만 허용하도록 설정되어 있음
(`backend/src/main/java/com/chamjima/backend/config/WebConfig.java`).

## 로컬 개발 환경 설정 (새 컴퓨터에서 처음 시작할 때)

### 1. MySQL

로컬에 MySQL을 설치하고 DB/계정을 만든다.

```sql
CREATE DATABASE chamjima;
CREATE USER 'chamjima'@'localhost' IDENTIFIED BY '원하는_비밀번호';
GRANT ALL PRIVILEGES ON chamjima.* TO 'chamjima'@'localhost';
```

### 2. 환경변수

셸 프로파일(`~/.zshrc` 등)에 추가:

```
export DB_PASSWORD=위에서_정한_비밀번호
export KAKAO_REST_API_KEY=...   # 카카오 개발자센터 REST API 키 (선택, 없으면 일부 기능 비활성)
export TMAP_APP_KEY=...          # 현재 미사용 (도보경로는 OSRM으로 대체됨)
export SEOUL_PHARMACY_API_KEY=... # 서울 열린데이터광장 인증키 (약국 데이터 임포트용)
```

`frontend/.env.local` (git에 안 올라감, 직접 생성):

```
VITE_KAKAO_MAP_KEY=카카오맵_JS_키
```

### 3. 화장실 데이터

DB의 화장실 데이터(서울시 공공데이터, 5,619건)는 git에 포함되어 있지 않다.
`backend/data/공중화장실정보_서울특별시.csv` 원본 파일을 구해서 같은 경로에 두고,
`application.properties`의 `app.import.restrooms.enabled=true`로 바꿔 한 번 실행하면
(`KAKAO_REST_API_KEY` 필요, geocoding용) 자동으로 적재된다. 이후 다시 `false`로 되돌릴 것.

### 4. 도보 경로 서버 (OSRM)

도보 시간/경로는 **자체 OSRM 인스턴스**로 계산한다. 없어도 앱은 뜨지만(Tmap → 카카오 →
직선거리로 폴백) 도보 시간이 부정확해진다.

공개 데모 서버(`router.project-osrm.org`)는 쓰지 않는다 — 개발용이라 서비스 트래픽을
보내면 안 되고, foot 프로필이 실제로는 자동차처럼 동작해 소요시간이 시속 34~54km로 나온다.

```bash
brew install osrm-backend osmium-tool

cd backend/data/osrm   # 없으면 mkdir -p
curl -SL -o south-korea.osm.pbf https://download.geofabrik.de/asia/south-korea-latest.osm.pbf

# 서울만 잘라낸다 (272MB -> 41MB). MVP가 서울 한정이라 전국 데이터가 필요없다.
osmium extract --bbox 126.70,37.35,127.25,37.78 -o seoul.osm.pbf south-korea.osm.pbf

# foot 프로필로 전처리 (MLD). 전체 15초 남짓, 피크 메모리 약 1.2GB.
osrm-extract -p /opt/homebrew/share/osrm/profiles/foot.lua seoul.osm.pbf
osrm-partition seoul.osrm
osrm-customize seoul.osrm

osrm-routed --algorithm mld --port 5001 seoul.osrm
```

- 서버 주소는 `OSRM_BASE_URL` 환경변수로 바꾼다 (기본값 `http://localhost:5001`).
- 산출물은 디스크 **407MB**, 구동 중 메모리 **약 280MB**. `backend/data/`는 gitignore라
  저장소에 올라가지 않으니 배포 환경에서는 위 전처리를 다시 돌리거나 산출물을 옮겨야 한다.
- OSM 데이터가 갱신되면 전처리부터 다시 한다.

### 5. 약국 데이터 ("전체" 레이어)

`application.properties`의 `app.import.pharmacies.enabled=true`로 바꿔 한 번 실행하면
(`SEOUL_PHARMACY_API_KEY` 필요) 서울시 약국 운영시간 정보 Open API(`TbPharmacyOperateInfo`)를
페이지네이션으로 전부 받아와 적재한다(좌표 포함이라 geocoding 불필요). 이후 다시 `false`로
되돌릴 것. "야간" 레이어(공공심야약국 39개소)는 API가 없어 수동 시드 데이터로 관리한다
(기획서 참고).
