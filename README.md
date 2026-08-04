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
