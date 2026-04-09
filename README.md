# 🤖 AI Debugging System (Project Iseng :v)

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**AI Debugging System** adalah asisten debugging otomatis untuk aplikasi Spring Boot yang menggunakan **Artificial Intelligence** untuk menganalisis error dan memberikan solusi secara real-time langsung di console IDE.

---

## ✨ Fitur Utama

### 🔥 Level 0 - Base Features
| Fitur | Deskripsi |
|-------|-----------|
| **Auto Error Detection** | Otomatis menangkap semua exception di aplikasi |
| **Source Code Extraction** | Menampilkan kode sumber dengan marker 👉 di baris error |
| **AI-Powered Analysis** | Analisis error menggunakan AI (Groq/OpenAI/Gemini) |
| **Bahasa Indonesia** | Output analisis dalam Bahasa Indonesia |

### 🟢 Level 1 - Easy Features
| Fitur | Deskripsi |
|-------|-----------|
| **Multi-Provider AI** | Dukungan Groq (gratis), OpenAI, dan Gemini |
| **Error History** | Menyimpan log error ke file untuk referensi |
| **Clipboard Auto-Copy** | Solusi otomatis tersalin ke clipboard |
| **Colorful Console** | Output berwarna untuk memudahkan membaca |

### 🟡 Level 2 - Medium Features (Masih tahap Develop)
| Fitur | Deskripsi |
|-------|-----------|
| **Memory Session** | AI mengingat percobaan fix sebelumnya, tidak ulangi solusi gagal |
| **Discord/Slack Notification** | Notifikasi real-time ke channel team |
| **Unit Test Generator** | Generate JUnit test otomatis untuk method yang error |
| **Auto-Fix** | One-click apply fix dengan backup otomatis |

### 🔮 Level 3 - Advanced (Coming Soon)
- Web Dashboard monitoring
- IntelliJ IDEA Plugin
- Learning System
- Performance Impact Analysis

---

## 🚀 Quick Start

### Prasyarat
- Java 17+
- Maven 3.6+
- API Key dari salah satu provider AI (Gratis: [Groq](https://console.groq.com))

### 1. Clone Repository
```bash
git clone https://github.com/sidik219/ai-debugging-system.git
cd ai-debugging-system
```

---

## 📄 Set Environment Variable
Windows (Command Prompt)
- set GROQ_API_KEY=gsk_your_api_key_here

Atau di IntelliJ IDEA:
- Run → Edit Configurations → Environment variables
- GROQ_API_KEY=gsk_your_api_key_here

## ⭐ Test Endpoint
Bisa dengan membuka swagger:
- http://localhost:8080/swagger-ui/index.html#/
  
Atau Curl:
- curl http://localhost:8080/api/debug/test-error
- curl http://localhost:8080/api/debug/test-divide
- curl http://localhost:8080/api/debug/test-array

## 🛠️ Tech Stack
| Kategori | Teknologi |
|-------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.4.4 |
| **AI** | Groq (Llama 3.3), OpenAI, Gemini |
| **HTTP Client** | WebClient (Spring WebFlux) |
| **Testing** | JUnit 5, Mockito, MockMvc |
| **Notification** | Discord/Slack Webhook |
| **Build Tool** | Maven |
| **Utilities** | Lombok, Jackson, Java Reflection |

## 🤝 Kontribusi
Kontribusi selalu diterima! Silakan:

    Fork repository

    Buat branch fitur (git checkout -b feature/AmazingFeature)

    Commit perubahan (git commit -m 'Add some AmazingFeature')

    Push ke branch (git push origin feature/AmazingFeature)

    Buka Pull Request

## 📝 Lisensi
Distributed under the MIT License. See LICENSE for more information.

## ⭐ Support
Jika project ini bermanfaat, berikan ⭐ di GitHub!
Made with ❤️ by Sidik
