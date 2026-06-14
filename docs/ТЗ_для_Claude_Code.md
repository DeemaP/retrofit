# ТЗ для разработки: программный комплекс автоматизации дооснащения ADAS

## Контекст проекта
Это магистерская диссертация. Нужна **альфа-версия** программного комплекса для управления процессом дооснащения автомобилей системами活ной безопасности (ADAS) в сервисных центрах. Срок — демонстрация на предзащите, поэтому цель: **работающий вертикальный срез**, а не полнота реализации.

Управление процессом строится на BPM-движке Camunda 7, встроенном в Spring Boot приложение. В корне проекта лежит файл `финальный_вариант.bpmn` (process id = `Process_1p16tp4`, isExecutable=true) — это полная бизнес-модель процесса дооснащения. Её нужно развернуть в Camunda целиком, но Java-делегаты писать только для happy-path (см. ниже).

## Технологический стек (обязательно соблюдать)
- Java 17 (LTS)
- Spring Boot 3.2+
- Camunda Platform 7.21+ (стартер `camunda-bpm-spring-boot-starter-webapp`, чтобы был доступен Camunda Cockpit)
- PostgreSQL 15
- Spring Data JPA / Hibernate 6.2
- Springdoc OpenAPI + Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
- Maven
- Docker Compose (PostgreSQL + приложение)
- JUnit 5 + Mockito + `camunda-bpm-junit5` для тестов процесса

## Структура пакетов
```
com.adas.retrofit
├── AdasRetrofitApplication.java        // @SpringBootApplication
├── controller                          // REST-контроллеры
│   ├── OrderController.java
│   └── TaskController.java
├── service                             // бизнес-логика
│   ├── OrderService.java
│   └── ActService.java
├── delegate                            // Java-делегаты для Camunda Service Task
│   ├── FormAcceptanceActDelegate.java
│   ├── CheckPartsAvailabilityDelegate.java
│   ├── SaveToDbDelegate.java
│   ├── GenerateDocumentationDelegate.java
│   └── StubDelegate.java               // универсальная заглушка-логгер для прочих веток
├── entity                              // JPA-сущности
│   ├── Order.java
│   ├── Vehicle.java
│   ├── Customer.java
│   └── ComplianceAct.java
├── repository                          // Spring Data JPA
│   ├── OrderRepository.java
│   ├── VehicleRepository.java
│   └── CustomerRepository.java
└── dto                                 // запросы/ответы REST
    ├── CreateOrderRequest.java
    └── OrderResponse.java
```

## Модель данных (минимум для альфы)
- **Customer**: id (UUID), fullName, phone, email
- **Vehicle**: id (UUID), vin, model, year, trimLevel
- **Order**: id (UUID), customer (ManyToOne), vehicle (ManyToOne), retrofitType (enum: ACC, BSA, AEB, LKA, COMBINED), status (enum: DRAFT, IN_PROGRESS, COMPLETED, CANCELLED), processInstanceId (String), createdAt
- **ComplianceAct**: id (UUID), order (OneToOne), documentNumber, issuedAt, summary (JSONB или TEXT для альфы)

Используй PostgreSQL JSONB для поля summary, если просто — можно TEXT.

## MVP happy-path сценарий (главное!)
Реализовать end-to-end так, чтобы это можно было показать вживую:

1. **POST /api/v1/orders** — создаёт Customer (если нужно), Vehicle, Order, и **запускает процесс Camunda** (`runtimeService.startProcessInstanceByKey("Process_1p16tp4", variables)`). В переменные процесса кладёт orderId. Сохраняет processInstanceId в Order. Возвращает OrderResponse с id и processInstanceId.

2. **GET /api/v1/orders/{id}** — возвращает заявку с её текущим статусом и активной задачей (если есть).

3. **GET /api/v1/tasks?orderId={id}** — список активных user task для заявки (через Camunda TaskService, фильтр по processInstanceId).

4. **POST /api/v1/tasks/{taskId}/complete** — завершает user task (`taskService.complete(taskId, variables)`), процесс движется дальше. Тело запроса — произвольные переменные процесса (Map).

5. Когда процесс доходит до service task **«Подготовка документации и актов»** (id=`Activity_1yuq8rv`) — делегат `GenerateDocumentationDelegate` создаёт ComplianceAct, привязанный к Order, проставляет documentNumber и issuedAt.

6. Когда процесс достигает endEvent `Event_Success` — Order переводится в статус COMPLETED (через execution listener или делегат).

## Маппинг Service Task → делегаты (happy-path)
В BPMN-модели проставь `camunda:delegateExpression` для этих Service Task (по id):
- `Activity_0iij30r` (Формирование акта о приёме) → `${formAcceptanceActDelegate}`
- `Activity_1wfd5h1` (Проверка наличия оборудования и запчастей) → `${checkPartsAvailabilityDelegate}`
- `Activity_1vm038x`, `Activity_0f4d1wp` (Сохранение в БД) → `${saveToDbDelegate}`
- `Activity_1auf6ys`, `Activity_1j7m77i` (Проверка БД) → `${checkPartsAvailabilityDelegate}` (переиспользуй)
- `Activity_0zitecv` (Составление списка отсутствующих) → `${stubDelegate}`
- `Activity_1yuq8rv` (Подготовка документации) → `${generateDocumentationDelegate}`
- `Activity_0vj13l3` (Пополнение БД) → `${saveToDbDelegate}`

**Все остальные Service Task** (если такие появятся в ветках демонтажа/возврата) → `${stubDelegate}` — он просто логирует "executed: <activityName>" и завершается. Это позволяет процессу не падать на нереализованных ветках.

## User Task — candidate groups
Проставь в BPMN `camunda:candidateGroups` для user task по ролям (для альфы аутентификацию можно не делать, группы — просто для наглядности в Cockpit):
- Приёмка, фотофиксация, фиксация ТЗ, передача авто → `ACCEPTOR`
- Проверка возможности дооснащения → `ENGINEER`
- Формирование списков, заказ → `ACCEPTOR`
- Выдача исполнителям, монтаж → `MOUNTER`
- Кодирование и калибровка → `ENGINEER` / `CALIBRATOR`
- Проверка системы и тест-драйв → `ACCEPTOR`

## Делегаты — что делает каждый (happy-path)
- **FormAcceptanceActDelegate**: логирует, кладёт в переменную процесса `acceptanceActNumber` сгенерированный номер.
- **CheckPartsAvailabilityDelegate**: имитирует проверку — всегда возвращает `partsAvailable=true` (заглушка диагностики). Логирует.
- **SaveToDbDelegate**: логирует факт сохранения, можно ничего не писать в БД для альфы или писать в простую таблицу-лог.
- **GenerateDocumentationDelegate**: создаёт ComplianceAct в БД, привязывает к Order по orderId из переменных процесса.
- **StubDelegate**: логирует `"[STUB] executed: " + execution.getCurrentActivityName()` и завершается. Используется для всех нереализованных веток.

## Docker Compose
- Сервис `postgres` (postgres:15), БД `adas`, проброс порта 5432
- Сервис `app` (Spring Boot), порт 8080, зависит от postgres
- `application.yml`: datasource на postgres, `spring.jpa.hibernate.ddl-auto=update`, Camunda auto-deploy BPMN из classpath, `camunda.bpm.admin-user` для входа в Cockpit

## Тесты (минимум)
- Один тест процесса через `camunda-bpm-junit5`: стартовать процесс, пройти happy-path, проверить что достигнут `Event_Success`. Service task замокать через Mockito где нужно.
- Один интеграционный тест REST: POST /orders → 201, GET /orders/{id} → 200.

## Чего НЕ делать в альфе
- Аутентификацию/авторизацию (Spring Security) — пропустить, оставить эндпоинты открытыми
- Реальную интеграцию с диагностическим оборудованием — все заглушки
- Полную реализацию веток демонтажа/возврата авто — только stub-делегаты
- Frontend — только Swagger UI как интерфейс

## Порядок работы
1. Сгенерируй каркас Maven-проекта с pom.xml (все зависимости).
2. Подними docker-compose с PostgreSQL, проверь что приложение стартует и Camunda разворачивает BPMN.
3. Сделай сущности + репозитории.
4. Реализуй OrderController.POST + OrderService — запуск процесса.
5. Реализуй TaskController — список и завершение задач.
6. Пропиши delegateExpression в BPMN, реализуй делегаты.
7. Проверь happy-path вручную через Swagger UI: создай заявку, пройди по задачам до конца, убедись что ComplianceAct создан и Order = COMPLETED.
8. Напиши два теста.

После каждого шага запускай сборку и проверяй, что приложение поднимается. Двигайся инкрементально.

## Критерий готовности альфы
Через Swagger UI можно: создать заявку (POST /orders), увидеть запущенный процесс, получить список задач, завершать задачи одну за другой, пройти весь happy-path, и в итоге получить ComplianceAct и статус заявки COMPLETED. Процесс при этом виден в Camunda Cockpit (http://localhost:8080/camunda).
