# Multi-Tenant Social Connector Engine (Meta / Facebook Graph API)

A modular, enterprise-ready Spring Boot microservice designed to manage social channel onboarding, OAuth token exchange, and automated cross-channel publishing to Meta Graph API v20.0 using dynamic database-driven multi-tenancy and the Strategy Design Pattern.

---

## 📑 Table of Contents

- [Architectural Overview](#-architectural-overview)
- [Key Architectural Patterns](#-key-architectural-patterns)
- [Domain Model & Database Schema](#-domain-model--database-schema)
- [Meta Graph API Publishing Flow](#-meta-graph-api-publishing-flow)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Quick Start & Setup](#-quick-start--setup)
- [End-to-End Testing & Verification Guide (PowerShell)](#-end-to-end-testing--verification-guide-powershell)
  - [1. OAuth Token Exchange & Page Onboarding](#1-oauth-token-exchange--page-onboarding)
  - [2. Multi-Channel Post Publishing (Text & Media)](#2-multi-channel-post-publishing-text--media)
  - [3. Messaging App Webhook Ingestion](#3-messaging-app-webhook-ingestion)
- [Production Security & Best Practices](#-production-security--best-practices)
- [License](#-license)

---

## 🏛 Architectural Overview

In multi-tenant social media management systems, organizations must publish content across disparate social networks (Facebook Pages, Instagram Business, LinkedIn) on behalf of hundreds of unique clients without hardcoding API credentials.

This service acts as an abstraction and orchestration bridge:
1. **Dynamic Tenant Metadata:** Client credentials, managed pages, and encrypted page access tokens are stored as JSON payloads in relational storage (`InfoSocial`, `TokenSocial`).
2. **Strategy Pattern Routing:** A unified interface (`Channels`) allows runtime selection of concrete platform connectors (`@Component("FACEBOOK")`) based on database configuration.
3. **Smart Meta Graph API Routing:** Automatically determines whether to hit `/{page-id}/photos` (for image attachments) or `/{page-id}/feed` (for text-only updates).

```text
       [ Web Dashboard / Messaging Chat Webhook (Telegram/WhatsApp) ]
                                    │
                                    ▼ (HTTP REST)
                     [ PostPublishController ]
                                    │
                                    ▼
                     [ PublishPostService ]
                    (Resolve Tenant & Channels)
                                    │
             ┌──────────────────────┴──────────────────────┐
             ▼                                             ▼
  [ ClienteSocialRepository ]                    [ Map<String, Channels> ]
  (Check enabled platforms)                     (Strategy Pattern Lookup)
                                                           │
                                                           ▼
                                                [ FacebookConnector ]
                                               (@Component("FACEBOOK"))
                                                           │
                                        ┌──────────────────┴──────────────────┐
                                        ▼                                     ▼
                           [ InfoSocialRepository ]              [ TokenSocialRepository ]
                           (Read active pages JSON)              (Read page tokens JSON)
                                        │                                     │
                                        └──────────────────┬──────────────────┘
                                                           ▼
                                            [ Meta Graph API v20.0 ]
                                         - /{page-id}/photos (Media)
                                         - /{page-id}/feed   (Status)