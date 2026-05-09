package com.clubeve.cc.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core B&W palette ──────────────────────────────────────────────────────────
val Black         = Color(0xFF000000)
val White         = Color(0xFFFFFFFF)
val OffWhite      = Color(0xFFF5F5F5)
val LightGray     = Color(0xFFE8E8E8)
val MidGray       = Color(0xFF9E9E9E)
val DarkGray      = Color(0xFF3A3A3A)

// ── Backgrounds ───────────────────────────────────────────────────────────────
val BackgroundPrimary  = White
val BackgroundSurface  = OffWhite
val BackgroundElevated = LightGray

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary   = Black
val TextSecondary = DarkGray
val TextMuted     = MidGray

// ── Borders ───────────────────────────────────────────────────────────────────
val BorderDefault = Color(0xFFD0D0D0)
val BorderSubtle  = Color(0xFFEEEEEE)
val Divider       = Color(0xFFE0E0E0)

// ── Accent (still black) ──────────────────────────────────────────────────────
val AccentPrimary    = Black
val AccentGlow       = Color(0x14000000)
val PrimaryContainer = Color(0xFFE8E8E8)

// ── Status ────────────────────────────────────────────────────────────────────
val StatusSuccess = Color(0xFF1A7A3C)   // dark green — readable on white
val StatusWarning = Color(0xFF8A5A00)   // dark amber
val StatusError   = Color(0xFFB00020)   // dark red

// ── Legacy aliases (keep old references compiling) ────────────────────────────
val AccentPressed   = DarkGray
val AccentSecondary = StatusSuccess
val DarkBackground  = Black
val SurfaceColor    = OffWhite
val GlassWhite      = Color(0x1A000000)
val GlassBorder     = Color(0x33000000)
val PrimaryColor    = Black
val ErrorColor      = StatusError
