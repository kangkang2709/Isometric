# GitHub Copilot Custom Instructions for RPG Vocabulary Game Project

## 🧠 Project Overview
This is a turn-based RPG game built with **Java 17** and **LibGDX**, designed to help players memorize English vocabulary.  
It integrates **WordNet** to generate semantic-based questions (e.g., multiple choice, fill-in-the-blank, synonyms, hypernyms).  
The game includes typical RPG systems like quests, combat, achievements, and inventory, all themed around language learning.

---

## 🏗️ Architectural Guidelines
- **Follow the MVC pattern strictly**:
    - `Model`: game data (e.g., word entries, player stats, enemy configs)
    - `View`: UI rendering using LibGDX Scene2D or 3D scenes
    - `Controller`: input handling, logic coordination, game state transitions
- No game logic inside UI widgets or views
- Decouple systems where possible for testability and reusability

---

## 🧑‍💻 Coding Style
- Use **Java 17 syntax and features**
- Avoid magic numbers and hardcoded strings
    - Use enums, constants, config files (JSON/properties), or asset references
- Prefer clarity and readability over cleverness
- Use meaningful class, method, and variable names in English

---

## 🎨 UI & HUD Style Guidelines
When generating UI or HUD components (e.g., Player Info, Inventory, Combat HUD), **always use visual inspiration from**:

### 🕹️ Primary UI Inspiration:
- **Final Fantasy VII Remake**:
    - Semi-transparent panels with blurred background
    - Soft glow borders, elegant shadows
    - Animated HP/MP bars, slide transitions, minimal but dynamic feel

### 🎨 Secondary UI Inspiration (when applicable):
- **Octopath Traveler II** (for menu and pixel aesthetic when needed)
- **Pokemon series** (for layout clarity and friendly design)

---
## 🌐 Language & Responses
- When Copilot suggests documentation, explanations, or UI labels, prefer **Vietnamese** if the context allows
- Code comments can be in **English**, unless localization is needed

---


## 📦 Dependencies
- Java 17
- LibGDX (2D/3D)
- WordNet (via JWNL or custom parser)
- Optional: Universal Tween Engine, VisUI, TiledMap for levels

---

