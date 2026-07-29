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
