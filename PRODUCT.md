# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Existing Spring Boot 3.4.1 application with Thymeleaf templates, vanilla HTML/CSS/JavaScript, WebSocket support, PixiJS, and Live2D Cubism runtime assets.

## Users

Primary user is inferred from the current interface and Chinese-language copy: a person who wants to talk with and spend time with a configurable Live2D character in a browser.

## Product Purpose

SekaiForm provides a browser-based virtual companion space. The user can load a character, interact with its motion and expressions, chat with an AI companion, and develop character stats through interaction.

## Positioning

The product combines a visible, touchable Live2D character with conversational assistance and lightweight character progression. The character is the primary interface, not a decorative avatar beside a separate chat product.

## Operating Context

The product is used as an immersive personal browser surface. The main interaction is centered on the character and the lower chat input. Supporting controls include model selection, model upload and deletion, character stats, a chat history window, and diagnostic access.

## Capabilities and Constraints

- Preserve the existing routes `/`, `/login`, and `/live2d`.
- Preserve the existing API paths, form field names, and DOM IDs used by the current JavaScript runtime, except for removed image-chat endpoints and controls.
- Preserve Live2D model loading, switching, upload, deletion, dragging, expressions, motions, chat, stats, affection, EXP, and toast feedback.
- Existing model assets are stored under `SekaiForm/models/2d` and currently include `haru_ja` and `hiyori_en`.
- The diagnostic page must continue to expose PixiJS and Live2D runtime load status.
- The frontend is server-rendered and has no npm build pipeline in the current project.
- Accessibility assumptions to preserve and improve: keyboard access to forms and controls, visible focus states, readable contrast, and a reduced-motion path for nonessential visual loops.

## Brand Commitments

The existing visible product name is `DivaStage`. The product voice is friendly, casual, and character-led. The Live2D character artwork is the primary brand asset and should remain visually dominant.

## Evidence on Hand

- `SekaiForm/src/main/resources/templates/login.html`
- `SekaiForm/src/main/resources/templates/live2d.html`
- `SekaiForm/src/main/resources/templates/diag.html`
- `SekaiForm/src/main/resources/static/css/app.css`
- `SekaiForm/src/main/resources/static/js/live2d-app.js`
- Live2D model and texture assets under `SekaiForm/models/2d`

No verified customer claims, performance metrics, pricing, testimonials, or external brand guidelines are present. Do not invent them.

## Product Principles

- The character remains the focal point.
- Conversation should feel immediate and understandable.
- Supporting controls should stay available without competing with the character.
- Feedback should explain what the system is doing.
- Visual polish must not break existing model and chat behavior.

## Accessibility & Inclusion

The interface should remain usable with keyboard focus, high-contrast text and controls, clear form labels, and reduced motion for decorative animation. Character interaction should have visible non-motion feedback where possible.
