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

## 배포 (무료) — 2026-08-10 실제 배포 완료

| | 선택 | 실제 URL / 프로젝트 |
|---|---|---|
| 프론트 | Firebase Hosting (Spark, 무료) | https://chamjima-frontend.web.app |
| 백엔드(API+OSRM) | Google Cloud Run | https://chamjima-backend-412068365628.asia-northeast3.run.app (프로젝트 `chamjima-app`) |
| DB | Neon (Postgres) | 프로젝트 `neondb`, 리전 us-east-2 |

**Render는 안 쓴다.** 처음엔 프론트를 Render Static Site로 계획했으나, Render의
Blueprint 배포 흐름이 무료 정적 사이트에도 결제 정보 등록을 요구해서(공식 문서상
"카드 불필요"와 실제 동작이 다름 — 2026-08-10 확인) Firebase Hosting Spark 플랜으로
바꿨다. Spark는 카드 자체를 요구하지 않는다(공식 확인). 이 프로젝트에 `render.yaml`이
없는 이유가 이거다.

**콜드 스타트:** 첫 실제 배포 후 헬스체크가 약 1초 만에 응답했다(로컬 실측 예상치
2.8초보다 빠름 — 클라우드 vCPU가 예상보다 나쁘지 않았던 것으로 보임). "3초 안에"
카피와는 여전히 타협이 있을 수 있으나, 우려했던 것만큼 심각하지 않다.

**비용 안전장치:** Cloud Run만 카드 등록이 필수고 무료 한도(월 2백만 요청/180,000
vCPU-초) 초과 시 자동 과금된다(공식 확인) — Firebase/Neon과 달리 하드 캡이 아니다.
Google Cloud 콘솔에서 예산 알림을 걸어두는 걸 권장.

### 재배포 방법

**프론트 업데이트:**
```bash
cd frontend && npm run build
cd .. && npx --yes firebase-tools deploy --only hosting
```
`VITE_API_BASE_URL`/`VITE_KAKAO_MAP_KEY`는 빌드 타임에 박히므로 `frontend/.env.local`에
미리 설정돼 있어야 한다(git에는 안 올라감).

**백엔드 업데이트:**
```bash
cd backend
export PATH="/opt/homebrew/share/google-cloud-sdk/bin:$PATH"
gcloud run deploy chamjima-backend --project=chamjima-app --source . \
  --region asia-northeast3 --memory 1Gi --allow-unauthenticated
```
환경변수만 바꿀 때는 이미지 재빌드 없이 더 빠르다:
```bash
gcloud run services update chamjima-backend --project=chamjima-app \
  --region asia-northeast3 --update-env-vars "KEY=VALUE"
```

### 처음부터 새로 설정할 때 (다른 컴퓨터/계정)

**1. DB — Neon (Postgres)**

1. [neon.tech](https://neon.tech) 가입 (카드 불필요), 프로젝트 생성.
2. Connection Details에서 host, database, username, password 확인 후 조합:
   `DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require` (Neon이 주는 문자열의
   `channel_binding=require`는 뺄 것 — pgjdbc는 이 파라미터를 `channelBinding`으로만
   인식해서 그대로 넣으면 안 된다. `sslmode=require`만으로 정상 연결 확인됨.)
3. 코드 변경 없음 — JDBC URL 스킴으로 드라이버 자동 선택(`build.gradle`에 MySQL·Postgres
   둘 다 있음). 스키마는 `ddl-auto=update`가 첫 기동 때 자동 생성.
4. 로컬 MySQL 데이터를 옮기려면(권장 — CSV 재임포트는 카카오 geocoding을 수천 번
   다시 호출하는 낭비): 테이블별로 MySQL에서 TSV 추출 → `psql \copy`로 적재.
   ```bash
   mysql -u chamjima chamjima -B -N -e "SELECT ... FROM restrooms" | \
     psql "$NEON_URL" -c "\copy restrooms (...) FROM STDIN WITH (FORMAT csv, DELIMITER E'\t', NULL 'NULL', QUOTE E'\x01')"
   ```
   bit(1) 컬럼은 `CAST(col AS UNSIGNED)`로 0/1 정수로 뽑으면 Postgres boolean이 그대로
   받는다. 옮긴 뒤 시퀀스를 맞출 것: `SELECT setval(pg_get_serial_sequence('restrooms','id'), (SELECT MAX(id) FROM restrooms));`
   (모든 테이블에 반복). 안 하면 다음 Hibernate insert가 기존 id와 충돌한다.

**2. 백엔드 — Google Cloud Run**

```bash
brew install --cask google-cloud-sdk
export PATH="/opt/homebrew/share/google-cloud-sdk/bin:$PATH"
gcloud auth login
gcloud projects create <프로젝트ID>
gcloud config set project <프로젝트ID>
gcloud billing projects link <프로젝트ID> --billing-account=<결제계정ID>  # console.cloud.google.com/billing 에서 먼저 결제계정 생성 필요 - 카드 등록 필수
gcloud services enable run.googleapis.com cloudbuild.googleapis.com

cd backend
gcloud run deploy chamjima-backend \
  --source . \
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
- `OSRM_BASE_URL=http://localhost:5001` — OSRM이 같은 컨테이너 안에서 뜨므로
  (`entrypoint.sh` 참고) 항상 localhost다.
- `backend/Dockerfile`이 OSRM 전처리(서울 클리핑 + foot 프로필 빌드)까지 빌드 단계에서
  전부 자동으로 한다. 로컬에 Docker가 없어도 Cloud Build가 원격으로 빌드하므로
  `gcloud` CLI만 있으면 된다.
- 결제 계정이 계정에 하나도 없으면 `gcloud billing accounts list`가 빈 목록을 반환한다
  — 이 경우 콘솔(console.cloud.google.com/billing)에서 직접 만들어야 하고, CLI로는
  대신할 수 없다(카드 입력은 브라우저에서만 받음).

**3. 프론트 — Firebase Hosting**

```bash
npx --yes firebase-tools login
npx --yes firebase-tools projects:create <프로젝트ID> --display-name "<이름>"
```
`.firebaserc`의 `default`를 그 프로젝트 ID로 바꾸고,
`frontend/.env.local`에 `VITE_API_BASE_URL`(위 Cloud Run URL)과 `VITE_KAKAO_MAP_KEY`를
채운 뒤:
```bash
cd frontend && npm run build && cd ..
npx --yes firebase-tools deploy --only hosting
```

배포된 도메인(`*.web.app`)을 **카카오 개발자센터 → 내 애플리케이션 → 앱 설정 →
플랫폼 → Web 플랫폼**에 등록해야 지도가 뜬다. 빠뜨리면 화면은 뜨는데 지도 자리만
비어 보인다 — 앱이 "카카오맵 SDK 로드 실패"로 명확히 알려준다.

마지막으로 백엔드 CORS를 이 도메인으로 업데이트:
```bash
gcloud run services update chamjima-backend --project=<프로젝트ID> \
  --region asia-northeast3 --update-env-vars "CORS_ALLOWED_ORIGINS=https://<프론트 도메인>"
```
