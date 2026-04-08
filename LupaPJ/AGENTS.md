# LupaPJ Working Notes

## Current Scope
- This week focuses on a buildable demo frontend for `루파루파`.
- Primary flow: `메인 로딩 화면 -> 로그인/회원가입 팝업 -> 기본 방 화면`.
- The room screen includes only the agreed room objects, button A, button B, bottom nav placeholder, and inventory sheet.
- Backend is not ready yet, so mock repositories and local state drive the current demo.

## Project Structure
- `app/src/main/java/com/example/lupapj/app`: app root and simple container
- `app/src/main/java/com/example/lupapj/data`: demo models, repository interfaces, mock implementations
- `app/src/main/java/com/example/lupapj/viewmodel`: `AppUiState`, `AppViewModel`
- `app/src/main/java/com/example/lupapj/ui`: theme, common components, loading screen, room screen

## Run
- Open in Android Studio and run the `app` configuration on API 26+ emulator/device.
- CLI check used this week: `./gradlew :app:compileDebugKotlin`

## Do-Not Rules
- Do not add email/password auth UI or general sign-up forms.
- Do not wire full Kakao SDK, real store behavior, screenshot, contacts, or gallery integrations.
- Do not add extra furniture, extra menus, settings, or dashboard-style layouts.
- Do not over-introduce DI, domain layers, or speculative abstractions beyond this week’s demo slice.

## Integration Notes
- `AuthRepository.loginWithKakao(kakaoAccessToken: String)` is the future SDK/server bridge.
- `RoomRepository.getRoom()` maps to future `GET /room`.
- `RoomRepository.performObjectAction(...)` and `RoomRepository.placeFood(...)` are the future `POST /room/action` bridge points.
- Keep TODOs explicit where mock logic stands in for Kakao auth and API transport.
