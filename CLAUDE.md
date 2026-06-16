# CLAUDE.md

Инструкция для Claude Code по проекту. Читается автоматически в начале каждой сессии. Цель — **полная реализация программы по утверждённой BPMN-модели**, быстро и оперативно.

## Проект

Программный комплекс для автоматизации и контроля процесса дооснащения автомобилей системами активной безопасности (ADAS) в сервисных центрах. Магистерская диссертация.

Ядро — BPM-движок Camunda 7, встроенный в Spring Boot. Бизнес-процесс дооснащения задан BPMN-моделью (файл `src/main/resources/processes/process.bpmn`, process id = `Process_1p16tp4`), движок её исполняет. Модель утверждена научным руководителем — **менять её структуру нельзя**, можно только проставлять технические атрибуты (delegateExpression, candidateGroups, formKey, условия на flow).

Цель этапа: реализовать **всю модель целиком** — все user task с формами/эндпоинтами, все service task с делегатами, все ветвления, включая сценарии демонтажа и возврата авто в исходное состояние при неудаче.

## Стек (фиксирован, major-версии не менять)

- Java 17 (LTS) — не использовать фичи Java 21+
- Spring Boot 3.2+
- Camunda Platform 7.21+ (`camunda-bpm-spring-boot-starter-webapp` — нужен Cockpit)
- PostgreSQL 15 (через Docker Compose)
- Spring Data JPA / Hibernate 6.2
- Springdoc OpenAPI + Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
- Maven (не Gradle)
- JUnit 5 + Mockito + `camunda-bpm-junit5`
- Lombok (для DTO/entity boilerplate)

Camunda строго 7, НЕ 8 — у них несовместимая архитектура (встраиваемый движок vs внешние воркеры).

## Команды

```bash
mvn clean install                       # сборка
mvn test                                # все тесты
mvn test -Dtest=ClassName#method        # один тест
mvn spring-boot:run                     # запуск приложения локально
docker compose up -d                    # поднять PostgreSQL + приложение
docker compose up -d postgres           # только база (app из IDE)
docker compose logs -f app              # логи
docker compose down                     # остановить
docker compose down -v                  # остановить + удалить тома (сброс БД Camunda)
```

После запуска:
- Swagger UI: http://localhost:8080/swagger-ui.html
- Camunda Cockpit: http://localhost:8080/camunda (admin-user из application.yml)

## Структура

```
src/main/java/com/adas/retrofit/
├── AdasRetrofitApplication.java
├── controller    — REST (тонкие, без бизнес-логики)
├── service       — бизнес-логика (@Service)
├── delegate      — Java-делегаты Camunda для Service Task (@Component)
├── entity        — JPA-сущности
├── repository    — Spring Data JPA
├── dto           — запросы/ответы REST (records)
├── mapper        — entity ↔ dto
└── config        — конфигурация (OpenAPI, Camunda, и т.п.)

src/main/resources/
├── processes/process.bpmn
└── application.yml

src/test/java/     — зеркалит main
```

## Архитектурные правила

- Слои однонаправленные: controller → service → repository. Контроллер не лезет в репозиторий и не содержит бизнес-логики.
- Делегаты вызывают сервисный слой, а не репозитории напрямую. Делегат — тонкая обёртка: достать переменные процесса → вызвать сервис → положить результат обратно.
- Состояние процесса живёт в Camunda, бизнес-данные — в наших таблицах. Связь через `processInstanceId` в сущности Order.
- Транзакционность: операции, меняющие и бизнес-данные, и состояние процесса, — в одной @Transactional-границе. Делегаты Camunda по умолчанию выполняются в транзакции движка — учитывать это.
- DTO отдельно от entity. Не отдавать JPA-entity из контроллера напрямую (ленивые связи, циклы). Маппинг явный.

## Конвенции

- Java records для DTO. Lombok (`@Getter/@Setter/@Builder`) для entity.
- Конструкторная инъекция (`@RequiredArgsConstructor` + `final`), не `@Autowired` на полях.
- Имя делегата в BPMN: `${camelCaseDelegate}` = имя Spring-бина (класс с маленькой буквы).
- UUID как id сущностей.
- Имена классов/методов/переменных — английские; логи и комментарии — можно русские.
- Не плодить абстракции на будущее. Простое работающее решение лучше «гибкого».

## BPMN — карта элементов (process id = Process_1p16tp4)

Модель утверждена. При реализации проставить `camunda:delegateExpression` для service task и `camunda:candidateGroups` для user task по таблицам ниже. Не менять id элементов — на них завязаны делегаты и тесты.

### Service Task → делегат

| id задачи | Назначение | delegateExpression |
|-----------|-----------|--------------------|
| Activity_0iij30r | Формирование акта о приёме, предв. заказа-наряда | `${formAcceptanceActDelegate}` |
| Activity_1wfd5h1 | Проверка наличия оборудования и запчастей | `${checkPartsAvailabilityDelegate}` |
| Activity_1vm038x | Сохранение списка оборудования в БД | `${saveToDbDelegate}` |
| Activity_0f4d1wp | Сохранение списка запчастей в БД | `${saveToDbDelegate}` |
| Activity_1auf6ys | Проверка БД на наличие списка запчастей | `${checkDbStockDelegate}` |
| Activity_1j7m77i | Проверка БД на наличие списка оборудования | `${checkDbStockDelegate}` |
| Activity_0zitecv | Составление списка отсутствующих позиций | `${composeMissingListDelegate}` |
| Activity_1yuq8rv | Подготовка документации и актов для клиента | `${generateDocumentationDelegate}` |
| Activity_0vj13l3 | Пополнение БД информацией о проекте с фото | `${saveProjectToDbDelegate}` |

### User Task → роль (candidateGroups)

| id задачи | Назначение | candidateGroups |
|-----------|-----------|-----------------|
| Activity_0gzebr4 | Первичная приёмка автомобиля | ACCEPTOR |
| Activity_0g5gyr1 | Фотофиксация повреждений | ACCEPTOR |
| Activity_0f2hjgl | Фиксация ТЗ от клиента | ACCEPTOR |
| Activity_15i5z4l | Проверка возможности дооснащения + диагностика | ENGINEER |
| Activity_0pa8xap | Формирование списка запчастей | ENGINEER |
| Activity_0gx280h | Формирование списка оборудования | ENGINEER |
| Activity_0a02b3u | Заказ недостающих запчастей и оборудования | ACCEPTOR |
| Activity_1a1msli | Выдача запчастей исполнителям | MOUNTER |
| Activity_1asctpd | Монтажные и слесарные работы | MOUNTER |
| Activity_0xl0n6f | Кодирование и калибровка | CALIBRATOR |
| Activity_0p38zs6 | Проверка системы и тест-драйв | ENGINEER |
| Activity_1rt6csz | Передача автомобиля клиенту | ACCEPTOR |
| Activity_0kgksar | Выявление причины неисправности | ENGINEER |
| Activity_05mqhxj | Демонтаж установленной системы | MOUNTER |
| Activity_08k31yn | Возврат авто в изначальное состояние | CALIBRATOR |

### Делегаты — поведение

Реализовать все. Каждый: достаёт нужные переменные процесса (`execution.getVariable(...)`), вызывает сервис, кладёт результат обратно.
- `formAcceptanceActDelegate` — формирует акт о приёме, сохраняет, кладёт `acceptanceActNumber` в процесс.
- `checkPartsAvailabilityDelegate` — проверяет доступность оборудования/запчастей (на этом этапе можно эмулировать наличие справочником в БД), ставит булевы переменные процесса для шлюзов.
- `saveToDbDelegate` — сохраняет список оборудования/запчастей заявки в БД.
- `checkDbStockDelegate` — проверяет БД на наличие позиций, ставит переменную для exclusive gateway (`allInStock` true/false).
- `composeMissingListDelegate` — формирует список отсутствующих позиций.
- `generateDocumentationDelegate` — создаёт ComplianceAct, привязывает к Order, проставляет documentNumber/issuedAt.
- `saveProjectToDbDelegate` — сохраняет полную информацию о проекте (фото, результаты) в БД.

### Промежуточные события

В модели есть `intermediateThrowEvent` (3) и `intermediateCatchEvent` (1) — это **link events** (имя ссылки `Return`, «Выдача автомобиля клиенту»), а не message events. Все три throw-точки (дооснащение невозможно; отказ в поставке; демонтаж/возврат завершён) — это внутренние goto к одному catch перед подготовкой документации (`Activity_1yuq8rv`): в любом сценарии завершения авто возвращается клиенту с документами. Link events обрабатываются движком Camunda внутри одного экземпляра процесса — внешняя корреляция и эндпоинт доставки сообщений **не нужны**.

### Шлюзы

8 exclusive + 4 parallel. Условия на исходящих flow exclusive-шлюзов прописать через `${переменная}` (например, `${allInStock}`, `${diagnosticPassed}`, `${testDrivePassed}`). Переменные ставят соответствующие делегаты или завершение user task. Parallel-шлюзы парные (split + join) — не трогать балансировку, она в модели уже выстроена.

## Модель данных

- **Customer**: id (UUID), fullName, phone, email
- **Vehicle**: id (UUID), vin, model, year, trimLevel
- **Order**: id (UUID), customer (ManyToOne), vehicle (ManyToOne), retrofitType (enum ACC/BSA/AEB/LKA/COMBINED), status (enum), processInstanceId, businessKey, createdAt
- **Component**: id, order (ManyToOne), partNumber, name, type (PART/EQUIPMENT), inStock (bool), installed (bool)
- **ProgrammingRecord**: id, order, ecuName, swBefore, swAfter, activatedFeatures (JSONB)
- **CalibrationRecord**: id, order, type (STATIC/DYNAMIC), parameters (JSONB), passed (bool)
- **ComplianceAct**: id, order (OneToOne), documentNumber, issuedAt, summary (JSONB)
- **User**: id, login, fullName, role (enum ACCEPTOR/MOUNTER/ENGINEER/CALIBRATOR/ADMIN)

JSONB-поля мапить через Hibernate (`@JdbcTypeCode(SqlTypes.JSON)` в Hibernate 6) или hypersistence-utils.

## REST API (минимальный набор)

- `POST /api/v1/orders` — создать заявку + `startProcessInstanceByKey("Process_1p16tp4", businessKey=orderId, vars)`; вернуть order с processInstanceId.
- `GET /api/v1/orders/{id}` — заявка с текущим статусом и активной задачей.
- `GET /api/v1/orders` — список с фильтром по статусу.
- `GET /api/v1/tasks?orderId=` или `?group=` — активные user task (TaskService).
- `POST /api/v1/tasks/{taskId}/complete` — завершить задачу с переменными.
- `POST /api/v1/orders/{id}/components` — добавить компоненты.
- `GET /api/v1/orders/{id}/act` — акт соответствия.

## Definition of Done

- Вся BPMN-модель развёрнута и исполняется; пройден полный happy-path И хотя бы один сценарий с ветвлением (нет на складе → заказ; неудача теста → выявление причины → демонтаж/возврат).
- Все 9 делегатов реализованы и вызываются.
- Link events («Выдача автомобиля клиенту») корректно сходятся к подготовке документации во всех сценариях завершения.
- `mvn test` зелёный: минимум — 1 process-тест happy-path, 1 process-тест ветки исключения, по 1 тесту на нетривиальный делегат и на OrderController.
- Через Swagger можно провести заявку по процессу; всё видно в Cockpit.

## Рабочий процесс (важно для скорости)

- Двигаться инкрементально и вертикальными срезами: сначала запустить скелет + развернуть BPMN + поднять окружение, затем happy-path целиком, затем ветки исключений.
- После каждого осмысленного шага — `mvn test` и проверка, что приложение стартует. Не копить непроверенные изменения.
- При ошибке (делегат не найден, BPMN не деплоится, gateway не находит переменную) — сначала читать логи и точечно чинить, не переписывать всё.
- Менять BPMN — аккуратной правкой XML атрибутов или в Camunda Modeler, сохраняя валидность и id. После изменения процесса при странном поведении — `docker compose down -v` для сброса таблиц ACT_* и чистого передеплоя.
- Не коммитить с падающими тестами.
- Не добавлять в этот этап: Spring Security/аутентификацию (эндпоинты открытые, роли — через candidateGroups для наглядности), реальную интеграцию с железом (эмулировать), фронтенд (интерфейс — Swagger UI).
