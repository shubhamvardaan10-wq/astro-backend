# 🪐 Vedic & Western Astrological Observatory & Enterprise Backend

An autonomous, high-precision astrological calculation engine and AI consultation platform combining **Java 21 (Spring Boot 3.2.5)** with an isolated **Python 3 Swiss Ephemeris & PyJHora worker runtime**.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.10-blue.svg)](https://www.python.org/)
[![Swiss Ephemeris](https://img.shields.io/badge/Ephemeris-Swiss%20pyswisseph-purple.svg)](https://www.astro.com/swisseph/)
[![Tests](https://img.shields.io/badge/Tests-279%20Java%20%7C%2064%20Python%20Passing-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🏛️ System Architecture

The application implements a decoupled, high-throughput dual-runtime architecture designed for sub-millisecond mathematical calculations and non-blocking AI consultations:

```
                                  ┌───────────────────────────────┐
                                  │      Client Applications      │
                                  │  - Observatory UI (index.html)│
                                  │  - API Sandbox (docs.html)    │
                                  │  - WhatsApp / Telegram Bots   │
                                  └───────────────┬───────────────┘
                                                  │ HTTP / JSON
                                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                Java 21 Spring Boot Application                                  │
│                                                                                                 │
│  ┌───────────────────────┐   ┌───────────────────────────────┐   ┌────────────────────────────┐ │
│  │   AstroController     │──▶│    AstroExpansionService      │──▶│   In-Memory Vector Store   │ │
│  │   (80+ REST Endpoints)│   │  (Pipeline & Orchestration)   │   │  (Classical Jyotish RAG)   │ │
│  └───────────────────────┘   └──────────────┬────────────────┘   └────────────────────────────┘ │
│                                             │                                                   │
│                                             │ Process Pipe (stdin / stdout)                     │
│                                             ▼                                                   │
│  ┌───────────────────────────────────────────────────────────────────────────────────────────┐  │
│  │                         Isolated Python 3 Calculation Worker                              │  │
│  │                                                                                           │  │
│  │  - pyswisseph (Swiss Ephemeris 2.10.3.2)       - PyJHora (Shadbala & D-Charts)            │  │
│  │  - lunar-python (BaZi & Solar Terms)           - timezonefinder (Offline IANA Geocoding)  │  │
│  │  - Jaimini Chara Dasha & Karakamsha            - Krishnamurti Paddhati 249 Sub-Lords      │  │
│  │  - South Indian Dasa Koota (10-Porutham)       - Mathematical Yantra Magic Squares (3x3)  │  │
│  │  - Astrocartography GeoJSON Vector Lines       - W.D. Gann Square of 9 Financial Timing   │  │
│  └───────────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                                 │
│  ┌───────────────────────┐   ┌───────────────────────────────┐   ┌────────────────────────────┐ │
│  │   AstroCacheService   │   │       AstroKafkaService       │   │    Local Ollama Client     │ │
│  │   (Redis Fallback)    │   │  (Telemetry & Event Streams)  │   │   (Non-Technical Synthesizer)│ │
│  └───────────────────────┘   └───────────────────────────────┘   └────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🌐 Built-in Interactive Web Interfaces

The application serves zero-dependency frontend clients directly out of the box:

1. **Astrological Observatory & AI Consultation Workbench (`/index.html`)**:
   - **Real-Time Cosmic Ticker**: Live Panchang limbs (Tithi, Nakshatra, Vara, Yoga, Karana), current Hora lord, diurnal/nocturnal Choghadiya, and active Rahu Kaal window.
   - **Multi-Style Vector SVG Visualizer**: Real-time rendering of North Indian Diamond, South Indian Box, and Western 360° Circular Wheel with aspect chords.
   - **Lo Shu Magic Square Grid**: Interactive 3x3 Chinese Astro-Numerology magic square with Mulank, Bhagyank, Kua number, and Arrows of Strength.
   - **Prashna Horary Divination**: KP seed (1–249) query assistant with Karya Siddhi probability readout (0–100%).
   - **Ayur-Jyotish Tridosha Bars**: Dynamic animated Vata, Pitta, and Kapha constitution percentages with herbal prescriptions.
   - **Bilingual Switcher**: Instant one-click toggle between authentic Hindi and English.

2. **Developer Documentation & Interactive API Sandbox (`/docs.html`)**:
   - Categorizes all 80+ endpoints into functional modules.
   - Built-in JSON request body editors with pre-populated fixtures.
   - Direct browser-based `fetch()` execution against the live API with status badges and syntax-highlighted response trees.

---

## 🚀 Complete API Endpoint Catalog (80+ Endpoints)

### 1. Vedic Natal & Divisional Varga Calculations
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/v1/vedic` | Legacy fixed-IST Parashari natal chart with planetary degrees, rashis, and houses. |
| `POST` | `/api/astro/v2/natal` | High-precision Swiss Ephemeris natal calculation with explicit IANA timezone and Ayanamsa. |
| `POST` | `/api/astro/v2/divisional` | Divisional Vargas from D1 to D60 (D9 Navamsha, D10 Dashamsha, D7 Saptamsha, D60 Shashtiamsha). |
| `POST` | `/api/astro/v2/shadbala` | 6-fold planetary strength calculation (Sthana, Kala, Dig, Cheshta, Naisargika, Drik Bala). |
| `POST` | `/api/astro/v2/ashtakavarga` | Bhinnashtakavarga (BAV) and Sarvashtakavarga (SAV) unreduced and reduced bindu matrices. |

### 2. Dasha Timelines & Predictive Life Horizons
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/v1/dasha` | 120-year Vimshottari Dasha intervals partitioning Mahadasha, Antardasha, and Pratyantardasha. |
| `POST` | `/api/astro/jaimini-chara-dasha` | Sign-based Jaimini Chara Dasha sequence with dual direct/reverse progressions and Karakamsha blueprint. |
| `POST` | `/api/astro/varshaphala` | Tajika annual solar return chart with Muntha, Varsheshwara, Mudda Dasha, and Harsha Bala. |
| `POST` | `/api/astro/sade-sati-timeline` | Saturn 7.5-year Sade Sati (Rising, Peak, Setting) and Dhaiya (Kantaka & Ashtama) periods. |
| `POST` | `/api/astro/calendar-feed` | Generates RFC 5545 `.ics` iCalendar subscription feed for dasha transitions and Shubh Muhurtas. |

### 3. Krishnamurti Paddhati (KP) & Horary Divination
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/kp-significators` | 249 KP Sub-Lord divisions, Placidus Cuspal Sub Lords (CSL 1–12), 4-step theory, and Ruling Planets. |
| `POST` | `/api/astro/prashna-horary` | Classical Vedic & KP Horary divination evaluating Karyesha, Tajika yogas, and Karya Siddhi probability. |
| `POST` | `/api/astro/birth-time-rectification` | Algorithmic birth time rectification assistant evaluating micro-degree Lagna boundaries and Tattva Shodhana. |

### 4. Synastry, Porutham & Compatibility Matrix
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/v1/matchmaking` | Classical 36-Guna Ashta Koota compatibility (Varna, Vashya, Tara, Yoni, Maitri, Gana, Bhakoot, Nadi). |
| `POST` | `/api/astro/dasa-koota-matching` | South Indian 10-Porutham marital match with strict Rajju Dosha (Shiro/Kantha/Udara) evaluation. |
| `POST` | `/api/astro/dosha-cancellation` | Deep cancellation matrix evaluating 24 classical Sanskrit exceptions for Manglik and Nadi Doshas. |
| `POST` | `/api/astro/synastry-composite` | Western Synastry Ptolemaic aspect chords, Midpoint Composite chart, and Davison relationship chart. |

### 5. Chakras, Occult Grids & Astro-Numerology
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/sarvatobhadra-chakra` | 81-square (9x9) occult grid calculating 4-directional Vedha (Sammukha, Vama, Dakshina, Kona). |
| `POST` | `/api/astro/kota-chakra` | Concentric 4-zone fortress siege defense chart (Stambha, Madhya, Prakara, Bahya) and Kota Swami. |
| `POST` | `/api/astro/lo-shu-grid` | Classical 3x3 Lo Shu Magic Square grid evaluating Mulank, Bhagyank, Kua number, and 8 Planes. |
| `POST` | `/api/astro/tara-bala-calendar` | Navatara Chakra 9-Tara daily transit grid across 3 Paryayas with Chandra-Ashtama warnings. |

### 6. Sacred Remedies, Yantras & Lineage Karma
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/remedies-yantra-mantra` | Mathematical 3x3 magic square planetary Yantras (Surya=15 to Ketu=39), Beeja mantras, and vector SVGs. |
| `POST` | `/api/astro/ancestral-lineage` | Astro-Genealogy analyzer evaluating 14 Pitru/Matru Dosha configurations and 6 Ancestral Rinas (Debts). |
| `POST` | `/api/astro/gemstone-rudraksha` | Anukul Graha gemstone prescriptions (Jeeva, Punya, Bhagya Ratna) and 1–14 Mukhi Rudraksha beads. |
| `POST` | `/api/astro/lal-kitab` | Fixed-house Lal Kitab chart (Aries 1st House), 35-year cycle, sleeping houses, and practical upayas. |

### 7. Western Systems, Progressions & Astrocartography
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/v1/western` | Tropical Placidus chart with planetary aspect chords (Conjunction, Trine, Square, Opposition, Sextile). |
| `POST` | `/api/astro/draconic-chart` | Western Draconic Chart anchored to True North Node (0° Aries) projecting Higher Self soul contracts. |
| `POST` | `/api/astro/progressions-directions` | Secondary Progressions (day-for-a-year) and Solar Arc directions evaluating progressed Moon phases. |
| `POST` | `/api/astro/solar-lunar-return` | Western Solar & Lunar precision returns with relocated angular cusps. |
| `POST` | `/api/astro/astrocartography/geojson`| Generates RFC 7946 GeoJSON LineStrings for planetary lines on Ascendant, Midheaven, Descendant, and IC. |

### 8. Panchangam, Muhurta & Planetary Clock
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/panchangam` | Complete daily 5 limbs (Tithi, Vara, Nakshatra, Yoga, Karana), sunrise/sunset ephemeris, and festivals. |
| `POST` | `/api/astro/planetary-clock` | Real-time planetary clock calculating rising Ascendant, current Hora lord, Choghadiya, and Rahu Kaal. |
| `POST` | `/api/astro/electional-muhurta` | Classical Vedic electional assistant identifying Abhijit Muhurta and eliminating 16 classical doshas. |
| `POST` | `/api/astro/widget/daily-summary`| Unified Daily Cosmic Dashboard Widget API synthesizing Panchang, Hora, Choghadiya, and sentiment. |

### 9. Medical Ayur-Jyotish & Astro-Finance
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/astro/medical-ayurveda` | Medical astrology engine calculating Vata/Pitta/Kapha percentages, Dhatu tissue vulnerabilities, and herbs. |
| `POST` | `/api/astro/market-gann` | Financial astrology & W.D. Gann Square of 9 timing engine tracking planetary harmonics for crypto and stocks. |
| `POST` | `/api/astro/ashtakavarga-kaksha` | Ashtakavarga transit heatmap dividing signs into 8 micro-Kakshas (3°45') tracking bindu execution. |
| `POST` | `/api/astro/famous-horoscopes` | Benchmark database of historical and celebrity horoscopes indexed by classical yogas. |

### 10. AI Consultation, Vector Search & Vault
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/ai/agent/chat` | Multi-turn conversational ReAct agent consultation memory connected to calculation tools. |
| `POST` | `/api/astro/v2/ai/report` | 12-chapter comprehensive narrative report synthesis with strict evidence grounding and data minimization. |
| `POST` | `/api/astro/chart-svg` | Generates high-resolution North Indian, South Indian, and Western 360° SVG vector charts. |
| `GET`  | `/api/astro/vault/list` | Lists all saved client and family horoscope profiles from the concurrent in-memory vault repository. |
| `POST` | `/api/astro/vault/save` | Saves a new horoscope profile to the vault repository. |

---

## 🔬 Mathematical & Ephemeris Verification Standards

* **Astronomical Accuracy**: Computations use the **Swiss Ephemeris (`pyswisseph` 2.10.3.2)** and official IANA Timezone databases (`timezonefinder`).
* **Ashtakavarga Invariants**: All Bhinnashtakavarga unreduced row sums strictly equal `48, 49, 39, 54, 56, 52, 39`; Sarvashtakavarga sum strictly equals `337`.
* **Vimshottari Dasha Partitioning**: 120-year lifespans are partitioned without time gaps or roundoff drift using 365.25-day Julian orbital years.
* **Yantra Mathematical Verification**: Every planetary Yantra 3x3 magic square strictly satisfies:
  $$\sum \text{Row}_i = \sum \text{Col}_j = \sum \text{Diag}_k = \text{MagicSum}$$
  where $\text{Surya}=15$, $\text{Chandra}=18$, $\text{Mangal}=21$, $\text{Budha}=24$, $\text{Guru}=27$, $\text{Shukra}=30$, $\text{Shani}=33$, $\text{Rahu}=36$, $\text{Ketu}=39$.

---

## 🛠️ Local Development & Build

### Prerequisites
* **Java 21** (OpenJDK / Eclipse Temurin)
* **Maven 3.9+**
* **Python 3.10+** (in `target/engine-venv`)

### Build & Test
```bash
# Run Java integration test suite (279 tests)
mvn -o -B -ntp test

# Run Python worker regression test suite (64 tests)
target/engine-venv/bin/python -m unittest discover -s worker -p 'test_*.py' -v

# Run application locally (default port 18080)
java -jar target/astro-backend-1.0.0.jar --server.port=18080
```

---

## 🐳 Docker & Cloud Deployment

The repository includes a production multi-stage [`Dockerfile`](Dockerfile) bundling both **Java 21** and the **Python 3 calculation worker**:

```bash
# Build container image
docker build -t astro-backend:latest .

# Run container image
docker run -d -p 8080:8080 --name astro astro-backend:latest
```

### One-Click Cloud Deployment
* **Render.com**: Connect this GitHub repository as a **Web Service (Docker)**. Render will auto-detect the Dockerfile and deploy seamlessly.
* **Hugging Face / OCI / Fly.io / Koyeb**: Fully supported via container entrypoint handling dynamic `$PORT` and `$SERVER_PORT`.
