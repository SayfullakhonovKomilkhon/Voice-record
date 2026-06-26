<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/AI-Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/React-TypeScript-61DAFB?style=for-the-badge&logo=react&logoColor=black"/>

# 🎙️ Deals Recorder

### Умный протокол переговоров с ИИ-транскрипцией

*Записывайте деловые встречи, автоматически разделяйте спикеров и получайте готовые протоколы за секунды*

[Открыть в AI Studio](https://ai.studio/apps/f46837b5-b2b7-4356-bd2d-d659e54cfe2e) • [Сообщить об ошибке](https://github.com/SayfullakhonovKomilkhon/Voice-record/issues)

</div>

---

## ✨ Возможности

| Функция | Описание |
|---|---|
| 🎤 **Запись переговоров** | Высококачественная запись через MediaRecorder API с поддержкой паузы и возобновления |
| 🌊 **Визуализация звука** | Живая спектрограмма в реальном времени с анимированными волнами |
| 🤖 **ИИ-транскрипция** | Автоматическое разделение реплик по спикерам с помощью Google Gemini |
| 💾 **Локальный архив** | Все встречи сохраняются в IndexedDB прямо в браузере — без сервера |
| ⬇️ **Авто-загрузка** | Аудиофайл автоматически скачивается после остановки записи |
| ▶️ **Воспроизведение** | Прослушивание записей прямо из архива |
| 📋 **Диалоговые транскрипты** | Разбивка разговора по спикерам в формате чата |

---

## 📱 Интерфейс

```
┌─────────────────────────────┐
│  🎙️ Deals Recorder          │  
│  Smart Protocol Engine  ●ЭФИР│
├─────────────────────────────┤
│  Тема: Переговоры по        │
│        инвестициям          │
│  Спикер А: Дмитрий          │
│  Спикер Б: Александр        │
├─────────────────────────────┤
│       00:04:32              │
│  ▁▃▅▇▅▃▂▄▆▇▅▃▁▂▄▆▇▄▂▁      │
├─────────────────────────────┤
│    ⏸️        ⏹️              │
└─────────────────────────────┘
```

---

## 🚀 Быстрый старт

### Требования

- [Android Studio](https://developer.android.com/studio) (последняя версия)
- Ключ [Gemini API](https://aistudio.google.com/app/apikey)
- Android-устройство или эмулятор

### Установка

```bash
# 1. Клонируйте репозиторий
git clone https://github.com/SayfullakhonovKomilkhon/Voice-record.git

# 2. Откройте папку в Android Studio
# File → Open → выберите директорию проекта

# 3. Создайте файл .env в корне проекта
cp .env.example .env
```

```env
# .env
GEMINI_API_KEY=ваш_ключ_здесь
```

### Запуск

1. Откройте проект в **Android Studio**
2. Дождитесь синхронизации Gradle
3. В файле `app/build.gradle.kts` удалите строку:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
4. Запустите на эмуляторе или физическом устройстве (`Shift + F10`)

---

## 🛠️ Технологический стек

```
┌─────────────────────────────────────────┐
│              DEALS RECORDER             │
├──────────────────┬──────────────────────┤
│   Android (Kotlin)│   Web Component     │
│  ─────────────── │ ──────────────────── │
│  • MediaRecorder │ • React + TypeScript │
│  • Gemini AI SDK │ • Web Audio API      │
│  • Jetpack       │ • IndexedDB          │
│  • Gradle KTS    │ • Canvas API         │
└──────────────────┴──────────────────────┘
```

| Слой | Технология |
|---|---|
| **Мобильное приложение** | Kotlin, Jetpack, Android SDK |
| **ИИ-движок** | Google Gemini API (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`) |
| **Веб-компонент** | React 18, TypeScript |
| **Аудио** | MediaRecorder API, Web Audio API, AnalyserNode |
| **Хранилище** | IndexedDB, localStorage |
| **Визуализация** | HTML5 Canvas с градиентными столбцами |
| **Сборка** | Gradle Kotlin DSL |

---

## 🏗️ Архитектура

```
Voice-record/
├── app/                    # Android-приложение (Kotlin)
│   └── src/main/
│       ├── java/           # Основная логика
│       └── res/            # Ресурсы и макеты
├── firebase/               # Конфигурация Firebase
├── docs/                   # Документация
├── MeetingRecorderComponent.tsx  # Веб-компонент React
├── build.gradle.kts        # Конфигурация сборки
├── metadata.json           # Метаданные приложения
└── .env.example            # Пример переменных окружения
```

---

## 🔑 Переменные окружения

| Переменная | Обязательная | Описание |
|---|---|---|
| `GEMINI_API_KEY` | ✅ Да | Ключ Google Gemini API для ИИ-транскрипции |

> **Получить ключ:** [Google AI Studio](https://aistudio.google.com/app/apikey) → Create API key

---

## 💡 Как это работает

```
Пользователь нажимает ▶
       ↓
Запрос доступа к микрофону
       ↓
MediaRecorder захватывает аудио (каждые 250мс)
       ↓
AnalyserNode → Canvas рисует волны в реальном времени
       ↓
Пользователь нажимает ⏹
       ↓
AudioBlob сохраняется в IndexedDB
       ↓
Файл автоматически скачивается (.webm)
       ↓
Gemini AI генерирует транскрипт с разделением спикеров
```

---

## 🤝 Вклад в проект

1. Сделайте Fork репозитория
2. Создайте ветку для фичи: `git checkout -b feature/amazing-feature`
3. Сделайте коммит: `git commit -m 'Add amazing feature'`
4. Запушьте ветку: `git push origin feature/amazing-feature`
5. Откройте Pull Request

---

## 📄 Лицензия

Распространяется под лицензией MIT. Подробнее см. `LICENSE`.

---

<div align="center">

Создано с ❤️ · Powered by [Google Gemini](https://ai.google.dev/) · Запустить в [AI Studio](https://ai.studio/apps/f46837b5-b2b7-4356-bd2d-d659e54cfe2e)

</div>
