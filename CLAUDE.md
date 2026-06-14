# CLAUDE.md

Руководство для Claude Code при работе с этим репозиторием.

## Что это за проект

Альфа-версия программного комплекса автоматизации **дооснащения автомобилей системами ADAS**
(активной безопасности) в сервисных центрах. Магистерская диссертация — цель не полнота, а
**рабочий вертикальный срез happy-path** для демонстрации на предзащите.

Управление процессом — на BPM-движке **Camunda 7**, встроенном в Spring Boot. Полная бизнес-модель
процесса лежит в `src/main/resources/bpmn/retrofit.bpmn` (process id = `Process_1p16tp4`,
`isExecutable=true`) и разворачивается в Camunda целиком; Java-логика (делегаты) написана только для
happy-path, остальные ветки работают через универсальную заглушку `stubDelegate`, чтобы процесс не падал.

Подробное ТЗ: `docs/ТЗ_для_Claude_Code.md`.

## Стек (соблюдать строго)

- **Java 17** (LTS) — проект пишется и собирается под JDK 17. На машине разработчика также стоит JDK 25
  (рабочий), перед сборкой нужно переключиться на JDK 17. Spring Boot 3.2 / Camunda 7 валидированы на 17–21.
- Spring Boot 3.2.x
- Camunda Platform 7.21+ — стартер `camunda-bpm-spring-boot-starter-webapp` (доступен Camunda Cockpit)
- PostgreSQL 15
- Spring Data JPA / Hibernate 6.2
- Springdoc OpenAPI + Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
- Maven (на машине: `C:\Users\pazinichdv\apache-maven-3.9.9`), wrapper отсутствует
- JUnit 5 + Mockito + `camunda-bpm-junit5`
- Docker Compose (PostgreSQL + приложение)

Базовый пакет — **`com.adas.retrofit`**. В `pom.xml` `groupId` остаётся `org.polytech` (вузовский),
artifact `adas-retrofit` (groupId и пакет различаются намеренно).

## Команды

> Перед сборкой убедиться, что активен **JDK 17** (`java -version`).

```bash
mvn clean package                 # сборка + тесты
mvn test                          # только тесты
mvn spring-boot:run               # локальный запуск (нужен поднятый PostgreSQL)

docker compose up -d postgres     # только база
docker compose up --build         # база + приложение + MinIO
```

Точки доступа после запуска:
- **Пульт процесса (кастомный UI)** — http://localhost:8080/ (`src/main/resources/static/index.html`):
  создать заявку, заполнять **формы user task**, видеть статус/акт/списки снабжения/галерею фото, ссылка в Cockpit.
- Swagger UI — http://localhost:8080/swagger-ui.html
- Camunda Cockpit / Tasklist — http://localhost:8080/camunda (логин из `camunda.bpm.admin-user`, по умолч. admin/admin)
- MinIO Console — http://localhost:9001 (minioadmin/minioadmin), S3 API на :9000, бакет `adas-photos`

**Сценарий живого демо:** открыть `/` → создать заявку (процесс стартует) → открыть Cockpit по ссылке
(токен стоит на первой user task) → возвращаться на пульт, заполнять формы задач (приёмка, ТЗ, фото,
диагностика, списки запчастей/оборудования), на остальных жать «Завершить»; service task проходят сразу,
токен в Cockpit перепрыгивает к следующей user task. В конце — статус COMPLETED, сформированы списки
запчастей/оборудования и создан акт.

## Архитектура

Слои в `com.adas.retrofit`:
- `controller` — REST (`OrderController`, `TaskController`, `TaskFormController`)
- `service` — бизнес-логика (`OrderService`, `ActService`, `SupplyService`, `SpecCatalogService`, `PhotoStorageService`)
- `delegate` — Java-делегаты для Camunda Service Task
- `config` — `MinioConfig` (бин `MinioClient`)
- `entity` — JPA-сущности (`Customer`, `Vehicle`, `Order`, `ComplianceAct`, `Part`, `Equipment`, `DamagePhoto`, `PartSpec`, `EquipmentSpec` + enum `RetrofitType`, `OrderStatus`, `SupplyOrderStatus`)
- `repository` — Spring Data JPA
- `dto` — `CreateOrderRequest`, `OrderResponse`, формы ЮТ (`AcceptanceRequest`, `RequirementsRequest`, `FeasibilityRequest`, `SupplyListRequest`/`SupplyItem`, `PhotoView`)

Связь приложения и движка: `OrderService` создаёт заявку и стартует процесс
(`runtimeService.startProcessInstanceByKey("Process_1p16tp4", vars)`), кладёт `orderId` в переменные
процесса и сохраняет `processInstanceId` в `Order`. Делегаты получают `orderId` из переменных процесса.

## MVP happy-path (главный сценарий демо)

1. `POST /api/v1/orders` — создаёт Customer/Vehicle/Order, стартует процесс, возвращает `OrderResponse`.
2. `GET /api/v1/orders/{id}` — заявка + текущий статус + активная задача.
3. `GET /api/v1/tasks?orderId={id}` — активные user task заявки (Camunda TaskService по processInstanceId).
4. `POST /api/v1/tasks/{taskId}/complete` — завершает user task (тело — Map переменных), процесс идёт дальше.
5. На service task `Activity_1yuq8rv` («Подготовка документации и актов») делегат
   `GenerateDocumentationDelegate` создаёт `ComplianceAct`, привязанный к Order.
6. На endEvent `Event_Success` execution listener переводит Order в статус `COMPLETED`.

Критерий готовности: через Swagger UI создать заявку, пройти задачи до конца, получить `ComplianceAct`
и `Order.status = COMPLETED`; процесс виден в Camunda Cockpit.

## Конвенции BPMN-модели

Модель изначально нарисована как «голый флоу» без исполняемых атрибутов. При наполнении соблюдаем:

**Запчасти vs оборудование (важно, в ТЗ были склеены):** *запчасти* (`Part`) — ставятся в авто
(радары, датчики, кронштейны, разъёмы); *оборудование* (`Equipment`) — оснастка цеха (компьютеры с ПО,
специнструмент, калибровочные стенды). Это раздельные сущности с раздельными ветками, делегатами и
переменными процесса.

**Service Task → `camunda:delegateExpression`:**
| Activity | Делегат |
|---|---|
| `Activity_0iij30r` (акт о приёме) | `${formAcceptanceActDelegate}` |
| `Activity_1wfd5h1` (наличие на складе) | `${checkStockDelegate}` (ставит `partsAvailable`, `equipmentAvailable`) |
| `Activity_1auf6ys` (проверка БД: запчасти) | `${checkPartsListDelegate}` (ставит `partsListInDb` по справочнику модель+год+тип; если есть — разворачивает в список заявки) |
| `Activity_1j7m77i` (проверка БД: оборудование) | `${checkEquipmentListDelegate}` (ставит `equipmentListInDb`; аналогично) |
| `Activity_0f4d1wp` (сохранение списка запчастей) | `${savePartsListDelegate}` (разворачивает справочник в список заявки) |
| `Activity_1vm038x` (сохранение списка оборудования) | `${saveEquipmentListDelegate}` (аналогично) |
| `Activity_0vj13l3` (пополнение БД о проекте) | `${saveToDbDelegate}` |
| `Activity_0zitecv` (список отсутствующих) | `${stubDelegate}` |
| `Activity_1yuq8rv` (документация) | `${generateDocumentationDelegate}` |
| любой прочий service task | `${stubDelegate}` |

**Справочник списков по конфигурации авто (`SpecCatalogService`):** требуемые списки хранятся как
`PartSpec`/`EquipmentSpec`, ключ — **модель + год выпуска + тип дооснащения**. Логика этапов:
*проверка БД* (`partsListInDb`/`equipmentListInDb`) = есть ли справочник под конфигурацию; *формирование*
(форма ЮТ) сохраняет справочник; *сохранение* (делегат) разворачивает справочник в `Part`/`Equipment`
под конкретную заявку. Если справочника нет, он засеивается из `RetrofitCatalog` (чтобы happy-path и
завершение из Cockpit работали). Для второй заявки той же конфигурации формирование пропускается —
список берётся из справочника. Просмотр списков заявки: `GET /api/v1/orders/{id}/supply`.

## Формы user task (`TaskFormController`)

Фронтенд выбирает форму по `taskDefinitionKey` активной задачи (он отдаётся в `TaskView`). Каждый
эндпоинт формы сохраняет доменные данные шага и, если передан `taskId`, завершает соответствующую
задачу процесса. Для задач без формы — общий `POST /api/v1/tasks/{taskId}/complete`.

| ЮТ (taskDefinitionKey) | Эндпоинт | Что сохраняет |
|---|---|---|
| `Activity_0gzebr4` (первичная приёмка) | `POST /orders/{id}/acceptance` | `Order.mileage`, `Vehicle.licensePlate`, `Vehicle.stsNumber` |
| `Activity_0f2hjgl` (фиксация ТЗ) | `POST /orders/{id}/requirements` | `Order.clientRequirements` |
| `Activity_0g5gyr1` (фотофиксация) | `POST /orders/{id}/photos` (multipart `files`), `GET .../photos`, `GET .../photos/{photoId}/content` | `DamagePhoto` (байты в MinIO, метаданные в БД) |
| `Activity_15i5z4l` (возможность дооснащения) | `POST /orders/{id}/feasibility` | `Order.feasibilityComment`; ставит переменную `retrofitPossible` |
| `Activity_0pa8xap` (список запчастей) | `GET /orders/{id}/parts/suggestion`, `POST /orders/{id}/parts` | `PartSpec` (модель+год+тип) + разворот в `Part` заявки |
| `Activity_0gx280h` (список оборудования) | `GET /orders/{id}/equipment/suggestion`, `POST /orders/{id}/equipment` | `EquipmentSpec` + разворот в `Equipment` |
| `Activity_0a02b3u` (заказ недостающего) | `GET /orders/{id}/order-supply`, `POST /orders/{id}/order-supply/advance` | `Order.supplyOrderStatus`: TO_ORDER→ORDERED→RECEIVED |
| `Activity_1a1msli` (выдача исполнителям) | `POST /orders/{id}/issue` | `Part.issued`/`Equipment.issued` (списание со склада) |

Форма запчастей/оборудования **префиллится** подсказкой из справочника (или `RetrofitCatalog`) и при
сохранении пишет **справочник** по конфигурации авто, затем разворачивает его в список заявки.

**Заказ недостающего (статусы в одной ЮТ):** форма показывает отсутствующие позиции (`inStock == false`)
и текущий статус; кнопки продвигают `supplyOrderStatus` (TO_ORDER → ORDERED → RECEIVED). Переход в
`RECEIVED` помечает позиции поступившими (`inStock=true`) и завершает задачу. Эта ЮТ — на «плохой» ветке
(`partsAvailable==false || equipmentAvailable==false`); чтобы её показать в демо, форсируем недостачу
переменными `partsShortage`/`equipmentShortage=true` (читает `CheckStockDelegate`), передав их в теле
`complete` любой user task до проверки склада.

**Выдача (чеклист + списание):** форма строит чеклист из `GET /supply` (галочки на невыданном), `POST /issue`
помечает выбранные позиции `issued=true` (списание со склада в лёгкой модели — без глобального остатка).

**Фотофиксация → MinIO:** `PhotoStorageService` грузит файлы в бакет `adas-photos` (ключ
`{orderId}/{uuid}{ext}`), бакет создаётся лениво при первой загрузке (поэтому приложение и тесты
поднимаются без запущенного MinIO). Байты **проксируются через приложение** (`.../photos/{id}/content`),
а не через presigned URL — внутри docker-сети браузер не видит хост `minio:9000`. Конфиг — `minio.*`
в `application.yml` (env: `MINIO_ENDPOINT/ACCESS_KEY/SECRET_KEY/BUCKET`).

**На будущее (авторизация):** в альфе аутентификации нет. Для роли «админ» (брать на себя любую ЮТ и
завершать с нужным исходом) достаточно лёгкого Spring Security с in-memory пользователями и ролями
(`ADMIN` + группы-кандидаты). Реальная проверка assignee в альфе не нужна — `ADMIN` сможет завершать
любую задачу с любыми переменными.

**User Task → `camunda:candidateGroups`** (для наглядности в Cockpit, аутентификации в альфе нет):
`ACCEPTOR` (приёмка, фотофиксация, фиксация ТЗ, формирование списков, заказ, тест-драйв, передача),
`ENGINEER` (проверка возможности дооснащения, кодирование/калибровка), `MOUNTER` (выдача исполнителям,
монтаж), `CALIBRATOR` (вместе с ENGINEER на кодировании).

**Exclusive-гейтвеи:** на «плохих» ветках стоит `conditionExpression` (напр. `${retrofitPossible == false}`,
`${partsAvailable == false}`, `${systemHealthy == false}`, `${clientAccepted == false}`), а happy-path —
это **default flow** (`<bpmn:exclusiveGateway default="...">`). Поэтому демо проходит без передачи
переменных: достаточно завершать user task пустым телом. Чтобы свернуть на «плохую» ветку — передать
переменную в теле `complete`.

**Параллельные гейтвеи:** `Gateway_0gjwntf` (join веток «запчасти»/«оборудование») сделан **inclusive**,
а не parallel — иначе он ждал бы 4 токена, тогда как приходит 2 (внутренние exclusive выбирают по одной
подветке) → дедлок. Форк `Gateway_1x8h95c` / join `Gateway_AcceptJoin` сбалансированы (3/3) — оставлены parallel.

**Event_Success** → `camunda:executionListener` (start) `${completeOrderListener}` переводит Order в COMPLETED.

## Чего НЕ делаем в альфе

Аутентификацию/Spring Security (см. заметку в разделе форм ЮТ — это следующий шаг); реальную интеграцию
с диагностикой (всё заглушки); полную реализацию веток демонтажа/возврата (только `stubDelegate`).
Кастомный пульт (`static/index.html`) есть и покрывает happy-path; формы реализованы для 8 ЮТ
(приёмка, ТЗ, фото, диагностика, списки запчастей/оборудования, заказ недостающего, выдача),
остальные завершаются кнопкой «Завершить». Склад — лёгкая модель (флаг `issued`), без глобального остатка.

## Принцип работы

Двигаться инкрементально, после значимых шагов запускать сборку и проверять, что приложение поднимается
и Camunda разворачивает BPMN.