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
  저장소에 올라가지 않는다 — **이건 로컬 개발용 절차**이고, 배포 시에는 `backend/Dockerfile`이
  같은 전처리를 빌드 단계에서 자동으로 돌리므로 이 절차를 따로 반복할 필요가 없다
  (아래 "배포" 항목 참고).
- OSM 데이터가 갱신되면 전처리부터 다시 한다.

### 5. 약국 데이터 ("전체" 레이어)

`application.properties`의 `app.import.pharmacies.enabled=true`로 바꿔 한 번 실행하면
(`SEOUL_PHARMACY_API_KEY` 필요) 서울시 약국 운영시간 정보 Open API(`TbPharmacyOperateInfo`)를
페이지네이션으로 전부 받아와 적재한다(좌표 포함이라 geocoding 불필요). 이후 다시 `false`로
되돌릴 것. "야간" 레이어(공공심야약국 39개소)는 API가 없어 수동 시드 데이터로 관리한다
(기획서 참고).

## 배포 (무료)

2026-08-10 기준 조사로 확정한 조합. 완전 무료지만 유휴 상태에서 첫 요청은 콜드 스타트가
걸린다(아래 참고) — "3초 안에"라는 제품 카피와는 타협이 있는 지점이다.

| | 선택 | 비고 |
|---|---|---|
| 프론트 | Render Static Site | 무료, 카드 불필요, 중지 없음 |
| 백엔드(API+OSRM) | Google Cloud Run | 월 2백만 요청 무료, **카드 등록 필수**, 한도 초과 시 자동 과금 |
| DB | Neon (Postgres) | 무료, 카드 불필요, 유휴 5분 후 슬립 → 다음 요청에 수백ms 만에 자동 기상 |

### 1. 프론트 — Render Static Site

Render 대시보드에서 **New + → Static Site** (Blueprint 아님 — Blueprint sync는 아래
`render.yaml`의 유료 백엔드 서비스도 함께 만들어버린다).

| 항목 | 값 |
|---|---|
| Root Directory | `frontend` |
| Build Command | `npm install && npm run build` |
| Publish Directory | `dist` |
| 환경변수 `VITE_KAKAO_MAP_KEY` | `frontend/.env.local`의 값을 그대로 |
| 환경변수 `VITE_API_BASE_URL` | 아래 3번에서 나온 Cloud Run URL |

배포된 도메인(`*.onrender.com`)을 **카카오 개발자센터 → 내 애플리케이션 → 앱 설정 →
플랫폼 → Web 플랫폼**에 등록해야 지도가 뜬다. 빠뜨리면 화면은 뜨는데 지도 자리만 비어 보인다.

### 2. DB — Neon (Postgres)

1. [neon.tech](https://neon.tech) 가입 (카드 불필요), 프로젝트 생성 — 리전은 서울에서 가장
   가까운 곳으로.
2. 대시보드의 Connection Details에서 host, database, username, password를 확인.
3. Spring 쪽 형식으로 조합:
   - `DB_URL` = `jdbc:postgresql://<host>/<database>?sslmode=require`
   - `DB_USERNAME` = Neon이 준 username
   - `DB_PASSWORD` = Neon이 준 password
4. 코드 변경 없음 — JDBC URL 스킴(`jdbc:postgresql:`)으로 드라이버가 자동 선택된다
   (`build.gradle`에 두 드라이버 모두 있음). 스키마는 `ddl-auto=update`가 첫 기동 때
   자동 생성한다.
5. 로컬 MySQL의 데이터를 옮기고 싶다면 `mysqldump`로 뜬 뒤 `pgloader` 등으로 이관 —
   MVP 단계에서는 그냥 Neon에서 비어있는 상태로 시작해 화장실/약국 데이터를 다시
   임포트하는 쪽이 더 간단하다(위 3, 5번 항목 참고).

### 3. 백엔드 — Google Cloud Run

`backend/Dockerfile`이 OSRM 전처리(서울 클리핑 + foot 프로필 빌드)까지 빌드 단계에서
전부 자동으로 한다 — 로컬에서 미리 만들어둔 산출물을 옮길 필요가 없다. 로컬에 Docker가
없어도 Cloud Build가 원격으로 이미지를 빌드하므로 `gcloud` CLI만 있으면 된다.

```bash
# 최초 1회
gcloud auth login
gcloud config set project <프로젝트ID>
gcloud services enable run.googleapis.com cloudbuild.googleapis.com

# 배포 (backend/ 디렉터리를 소스로 올려서 Dockerfile로 원격 빌드)
gcloud run deploy chamjima-backend \
  --source ./backend \
  --region asia-northeast3 \
  --memory 1Gi \
  --allow-unauthenticated \
  --set-env-vars DB_URL="jdbc:postgresql://<host>/<database>?sslmode=require" \
  --set-env-vars DB_USERNAME="<neon-username>" \
  --set-env-vars DB_PASSWORD="<neon-password>" \
  --set-env-vars KAKAO_REST_API_KEY="<카카오 REST API 키>" \
  --set-env-vars SEOUL_PHARMACY_API_KEY="<서울 열린데이터광장 키>" \
  --set-env-vars CORS_ALLOWED_ORIGINS="https://<프론트 도메인>" \
  --set-env-vars OSRM_BASE_URL="http://localhost:5001"
```

- `asia-northeast3` = 서울 리전(공식 확인).
- `--memory 1Gi` — JVM(약 170~300MB)과 OSRM(약 20~280MB, mmap이라 조회 패턴에 따라
  변동)을 한 컨테이너에서 같이 띄우므로 기본값 512MiB로는 부족하다.
- `OSRM_BASE_URL=http://localhost:5001` — OSRM이 같은 컨테이너 안에서 뜨므로(entrypoint.sh
  참고) 항상 localhost다. 다른 서비스로 분리하지 않는 이유는 위 배포 표 설명 참고.
- 첫 배포 후 나오는 `*.run.app` URL을 위 1번의 `VITE_API_BASE_URL`에 넣고 프론트를
  다시 배포해야 연결된다.

**콜드 스타트에 대해 솔직하게:** 로컬에서 실측한 값은 OSRM 기동 약 0.3초, Spring Boot
jar 기동 약 2.8초(맥북 기준, OS 파일 캐시가 남아있는 상태라 실제보다 빠를 수 있음).
클라우드의 약한 vCPU에서는 이보다 걸릴 수 있어, 15분 이상 방치 후 첫 요청은 최소
몇 초가 걸린다고 보는 게 정확하다. Render 무료의 콜드 스타트(약 1분)보다는 훨씬
낫지만 "3초 안에"를 완전히 지키지는 못한다 — 트래픽이 늘어 인스턴스가 계속 따뜻하게
유지될수록 이 문제는 자연히 옅어진다.

**비용 안전장치:** Cloud Run은 무료 한도(월 2백만 요청 / 180,000 vCPU-초)를 넘으면
카드로 자동 과금된다(공식 확인) — Render/Neon과 달리 하드 캡이 아니다. Google Cloud
콘솔에서 예산 알림을 걸어두는 걸 권장한다.
