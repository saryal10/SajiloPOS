# SajiloPOS — Android app

Offline-first Point of Sale for small retail businesses in Nepal. Runs the whole
shop counter on-device: catalog, cart, checkout, receipts and analytics work with
no network. Retail-only scope — no business-type switching.

## Features

**Sell**
- [x] One-tap **SCAN** hero: full-width gradient scan button, the merchant's primary action
- [x] Camera barcode scanning (CameraX + ZXing) with torch, manual entry, and demo barcodes
- [x] Product catalog with category color-coding, stock meters, low-stock / sold-out states
- [x] Search by name, Nepali name, or barcode, plus category filter chips
- [x] Floating cart bar with live total; dual-pane catalog + cart on tablets

**Checkout & payments**
- [x] Cash with quick-tender chips and live change calculation
- [x] eSewa / Fonepay / Khalti dynamic QR with gateway verification flow
- [x] Thermal receipt preview (58/80 mm), Android printing, share, and ESC/POS copy
- [x] Sequential daily invoice numbers (`INV-YYYYMMDD-0001`)

**Insights**
- [x] Revenue, orders, average ticket, gross profit and margin for Today / 7 / 30 days
- [x] Gap-free sales trend chart with peak-day marker, payment-mix donut, top sellers
- [x] Predictive restocking from 14-day sales velocity with one-tap restock

**Stock**
- [x] Add/edit products with quantity, cost price, and selling price
- [x] In-form barcode scanning that fills the SKU field straight from the camera
- [x] Low-stock alerts, quick stock adjusters, per-product margin %

**History, Settings & more**
- [x] Full sales history with search, payment filters, and receipt reprint
- [x] Business profile, PAN/VAT, VAT toggle, paper width, demo catalog reload
- [x] Fully offline: Room/SQLite ledger, bundled fonts, Nepali (Devanagari) names, `रू 1,25,000` grouping

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (BOM 2024.09.00) |
| Architecture | MVVM — `ViewModel` → `Repository` → Room `DAO` |
| Async | Coroutines + Flow (reactive Room queries) |
| Persistence | Room 2.7.0 (SQLite), versioned with an explicit migration |
| Camera / scanning | CameraX 1.5.0 + ZXing core 3.5.3 |
| Printing | Android `PrintManager`, WebView receipt, ESC/POS generation |
| QR rendering | Custom Compose `Canvas` renderer — no network, no ML Kit download |
| Build | AGP 9.1.1, KSP, Gradle Kotlin DSL, version catalog |
| Min / target SDK | 24 (Android 7.0) / 36 |
| Tests | JUnit 4, Robolectric — 73 unit tests green |

## Project structure

```
android_app/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt                  App shell: nav rail/bar, top bar, modals
│   ├── data/
│   │   ├── analytics/Analytics.kt       Pure analytics + restock maths
│   │   ├── local/                       Room DB, DAOs, projections, demo catalog
│   │   ├── model/PosModels.kt           Entities + domain models
│   │   └── repository/PosRepository.kt  Data access façade
│   └── ui/
│       ├── components/                  DesignSystem, Charts, scanner, payment, receipt, QR
│       ├── screens/                     Pos, Insights, Inventory, Transactions, Settings
│       ├── theme/                       Color, Type, Shape, Theme
│       ├── util/Format.kt               Money, dates and Nepali formatting
│       └── viewmodel/PosViewModel.kt    State, cart maths, orchestration
├── app/src/main/res/font/               Bundled Inter + Manrope (variable)
├── app/src/test/…                       8 test files, 73 tests
└── gradle/libs.versions.toml            Version catalog
```

## Docs in this folder

- [`instructions.md`](instructions.md) — run, build, and test the app step by step
- [`technical_design.html`](technical_design.html) — interactive architecture slideshow
  (open in a browser)
