---
name: restroom-app-planner
description: >
  Maintain and evolve the living planning document (기획서) for Hoon's app project "참지마" —
  a restroom-and-pharmacy-finder app (formerly nicknamed 급똥맵, built for a husband with IBS
  and 7-year-old twins). Use this skill whenever Hoon shares a new idea, feature, GPT/ChatGPT
  output, brainstorm dump, or feedback related to this app, or asks things like "이거 기획서에
  추가해줘", "이 기능 넣으면 어때?", "지금까지 기획서 정리해줘", or otherwise discusses
  restroom-finding features, IBS-friendly navigation, public toilet ratings, kid-friendly
  "안심 화장실" filters, late-night pharmacy lookup, app positioning, or the 참지마 project's
  DB/auth design. Always check for an existing planning doc file before creating a new one —
  this skill's job is to keep ONE evolving document up to date, not to generate a fresh
  one-off document every time.
---

# 참지마 기획서 관리 (Restroom App Planning Doc Maintainer)

## Why this skill exists

Hoon is designing "참지마", an app that helps people in urgent situations find the nearest
usable restroom — and now also currently-open pharmacies — with his family's real needs as
the driving use case: a husband with IBS and 7-year-old twins who need bathrooms urgently and
often. Ideas arrive in bursts: a raw brainstorm dump, a pasted ChatGPT output, a one-line idea
mid-conversation, a competitive discovery ("어, 이런 앱이 이미 있네"), or a scope decision.
The point of this skill is to make sure none of that gets lost or duplicated: every new idea
should land in a single, continuously-updated 기획서 file, organized the same way each time.

## Project context (as of 2026-07)

Knowing where the project stands prevents re-litigating settled decisions:

- **Name**: 참지마 (확정). 급똥맵 and 급똥여지도 were rejected — existing services
  (toiletmap.co.kr's 급똥맵, the 대똥여지도 app) already own that naming space.
- **Scope**: restroom map + currently-open pharmacy map, in top-nav tabs [급똥]/[약국].
  Camping features were explicitly cut (scope reduction) — don't reintroduce them casually.
- **Positioning**: primary = urgency (급함), secondary = family reassurance (안심/아이와 함께
  모드). One marketing message, not two.
- **Architecture**: public-data base layer + user reviews on top; internal ids with
  external_id mapping; browse without login, JWT social login only for reviews/favorites.

If a new request contradicts one of these (e.g. adding camping back, renaming), don't refuse —
but point out the earlier decision recorded in the doc and confirm before changing it.

## Step 1: Find the existing doc

Before doing anything else, look for the existing planning doc. Check (in order):

1. The current conversation for a filename already mentioned.
2. The connected workspace folder (if one exists) for `참지마_기획서.md`, or any file matching
   `*기획서*.md`.
3. If nothing is found, ask Hoon directly — he may have a copy from a previous session —
   before assuming there isn't one. Don't silently create a duplicate doc.

If truly nothing exists yet, create a new one using `assets/template.md` as the starting
structure, save it as `참지마_기획서.md`, and tell Hoon where it was saved.

## Step 2: Figure out where new content belongs

When Hoon shares something new — a raw idea, a GPT dump, a piece of feedback — don't just
append it to the bottom. Read it, and figure out which section it belongs to (핵심 기능,
차별화 포인트, 포지셔닝, 경쟁 서비스 분석, 인증/데이터 설계, 화면 구조, 기술 스택...).

Hoon prefers a middle ground: don't silently guess on ambiguous cases, but don't interrupt
him for every trivial addition either. Use judgment —

- If it's obviously a new feature idea, or a clarification/expansion of something already in
  the doc, fold it into the right section yourself and briefly say where you put it.
- If it's genuinely ambiguous (could belong in two sections, contradicts a settled decision,
  or seems like it might replace rather than add to existing content), ask a short clarifying
  question before editing.
- Removals deserve extra care: when asked to cut a feature, remove exactly what was asked and
  leave adjacent mentions alone unless told otherwise — state what you kept and why, and
  invite correction. Record rejected options (like discarded names) with the reason, rather
  than deleting them without a trace; knowing why something was rejected prevents
  re-proposing it later.

When integrating, rewrite for consistency rather than pasting raw text — match the doc's
existing tone (short bullet points, emoji section markers, casual concrete examples like
`🚽 58초` rather than abstract descriptions). Remove true duplicates, but don't strip out
concrete details (names, numbers, examples) — those are what make the doc useful later.

## Step 3: Log what changed

Keep a `## 변경 이력` section at the bottom of the doc. Each time you update it, add one
short line: date + a one-sentence summary of what was added or changed. This lets Hoon (or
anyone else working on this later) see how the idea evolved without re-reading the whole
document.

## Step 4: Confirm and offer next steps

After updating, briefly tell Hoon what changed (1-3 sentences — don't recite the whole diff).
If the change was substantial, offer to also produce a `.docx` version for sharing (use the
docx skill for that conversion) — but don't convert automatically, since the working format
is markdown.

## Template structure

`assets/template.md` has the starting skeleton for a new doc. The sections are:

1. **한줄 소개 / 배경** — what the app is, why it exists (the family use case)
2. **MVP** — the minimum feature set to ship first
3. **핵심 기능** — numbered feature list, each with a short rationale (most new ideas land here)
4. **차별화 포인트** — what makes this different from existing map apps
5. **포지셔닝** — main vs. secondary marketing message
6. **경쟁 서비스 분석** — competitor list + the gap being targeted
7. **기술 스택** — tech choices and why
8. **인증 / 데이터 설계** — auth flow and DB table design
9. **화면 구조** — screen/navigation layout
10. **이름** — decided name plus rejected candidates with reasons
11. **변경 이력** — dated changelog (see Step 3)

Keep new sections out unless Hoon explicitly wants to restructure — consistency across
updates matters more than perfect categorization.
