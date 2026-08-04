interface Window {
  kakao: any
}

interface ImportMetaEnv {
  readonly VITE_KAKAO_MAP_KEY: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
