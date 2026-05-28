# Руководство по настройке Firebase для Deals Recorder (voice-recorder)

Вы успешно создали проект Firebase с именем **voice-recorder**. Ниже приведена подробная пошаговая инструкция по активации и деплою всех серверных ресурсов (Cloud Functions, Security Rules, База данных, Хранилище, App Check).

---

## 1. НАСТРОЙКА СЕРВИСОВ В Firebase Console

Откройте свой проект в [панели управления Firebase Console](https://console.firebase.google.com/) и выполните следующие действия:

### А. Firebase Authentication (Вход пользователей)
1. В левом меню перейдите в **Build** -> **Authentication**, нажмите кнопку **Get Started**.
2. Вкладка **Sign-in method**:
   - Нажмите на **Email/Password** и включите его (Enable).
   - Нажмите **Add new provider** -> Выберите **Google**:
     - Включите его (Enable).
     - Укажите почту поддержки проекта.
     - Нажмите **Save**.

### Б. Cloud Firestore (База данных)
1. В левом меню нажмите **Build** -> **Firestore Database** -> **Create Database**.
2. Установите ближайший гео-регион (например, `europe-west3` для Европы или любой другой по вкусу).
3. Нажмите **Start in dynamic test mode** (мы заменим правила безопасности на боевые позже).
4. Нажмите **Create**.

### В. Firebase Storage (Хранение аудио)
1. В левом меню выберите **Build** -> **Storage** -> **Get Started**.
2. Выберите режим **Start in test mode**.
3. Установите тот же регион, что и для Firestore. Нажмите **Done**.

### Г. Firebase Cloud Messaging (Push-уведомления)
1. Откройте шестеренку (Project Settings) в левом верхнем углу -> вкладка **Cloud Messaging**.
2. Убедитесь, что **Firebase Cloud Messaging API (V1)** имеет статус *Enabled*. 
3. Здесь же, при интеграции с Android, вы получите сертификаты, если решите загрузить APNs / FCM ключи.

---

## 2. ПОДКЛЮЧЕНИЕ Firebase App Check (Play Integrity)

Для защиты Cloud Functions от чужих вызовов и накрутки расходов ИИ:
1. Выберите в левом меню **Build** -> **App Check** -> Нажмите **Get Started**.
2. Во вкладке **Apps** нажмите **Register** для вашего Android-приложения.
3. Выберите **Play Integrity**.
4. Введите SHA-256 fingerprint вашего приложения. (Вы можете получить SHA-256 из отладочного ключа на вашем компьютере или Firebase CLI).
5. Задайте время жизни токена (по умолчанию 1 час) и сохраните.
6. Во вкладке **APIs** найдите `Cloud Functions` и `Cloud Firestore` и нажмите для них **Enforce** (Принудительно проверять токены App Check).

---

## 3. ПОЛУЧЕНИЕ И УСТАНОВКА `google-services.json`

1. Зайдите в **Project Settings** (настройки проекта) -> вкладка **General**.
2. Внизу в разделе *Your apps* нажмите **Add app** -> выберите значок **Android**.
3. Заполните данные:
   - **Android package name (Обязательно)**: `com.aistudio.dealsrecorder.qvnpxv` (скопировано из файла `app/build.gradle.kts`).
   - **App nickname**: `Deals Recorder`.
   - **Debug signing certificate SHA-1**: *Необязательно (но желательно для входа через Google)*.
4. Нажмите **Register app**.
5. Скачайте файл `google-services.json`.
6. Положите скачанный файл в папку `/app` вашего экспортированного Android-проекта:
   - Путь в локальной структуре: `ваш_проект/app/google-services.json`.

---

## 4. ДЕПЛОЙ СЕРВЕРНОЙ ЧАСТИ ЧЕРЕЗ FIREBASE CLI

Все необходимые файлы конфигурации и функций уже созданы в папке `/firebase` вашего репозитория.

### Подготовка окружения на вашем компьютере:
Убедитесь, что у вас установлен Node.js (версии 18 или 20+) и Firebase CLI. Если CLI не установлен, выполните:
```bash
npm install -g firebase-tools
```

### Шаги деплоя:
1. Откройте терминал в корневой папке проекта на вашем компьютере.
2. Перейдите в каталог `firebase`:
   ```bash
   cd firebase
   ```
3. Войдите в свой Firebase-аккаунт:
   ```bash
   firebase login
   ```
4. Инициализируйте проект и свяжите его с вашим облачным проектом `voice-recorder`:
   ```bash
   firebase use --add
   ```
   выберите из списка `voice-recorder` (или впишите его ID).

5. Установите ваш API-ключ Google Gemini в облачный Secret Manager сервиса Cloud Functions, чтобы функция безопасно получала к нему доступ (функции защищены политикой IAM):
   ```bash
   firebase functions:secrets:set GEMINI_API_KEY="ВАШ_КЛЮЧ_GEMINI_ИЗ_AI_STUDIO"
   ```
   *(Данный ключ никогда не утечет на клиентские устройства!)*

6. Запустите деплой всех правил бэкенда и функций:
   ```bash
   firebase deploy
   ```
   Этот шаг автоматически загрузит:
   - Правила доступа к Cloud Firestore (`firestore.rules`)
   - Правила доступа к Firebase Storage (`storage.rules`)
   - Обе Cloud Functions: `processMeeting` (обработка речи и ИИ-анализ) и заготовку `enrollVoice`.

---

## 5. ПОЧЕМУ ТАКАЯ АРХИТЕКТУРА — ЛУЧШАЯ?

- **Безопасность API-ключа**: Ключ Gemini находится в Cloud Secret Manager, доступном только для Google Cloud Functions. Клиент вызывает сетевой метод `processMeeting`, не зная ключа.
- **Безопасность файлов**: Клиент загружает аудиозапись во временную изолированную папку Storage, к которой доступ имеет только он сам. После обработки Cloud Function удаляет тяжелый файл — экономится место в Storage и гарантируется полная приватность (записи не хранятся на сервере).
- **Разделение прав Firestore**: Правила Firestore (`firestore.rules`) защищают структуру: клиент может менять только свои "контакты", профили или информацию о встречах. Записывать в аналитику или добавлять сегменты текстовой расшифровки напрямую с телефона ЗАПРЕЩЕНО — это делает только проверенный бэкенд на Cloud Functions.
- **App Check**: Гарантирует, что запросы к Cloud Functions шлет только официальная версия вашего Android-приложения, исключая ботов и несанкционированные API-запросы.
