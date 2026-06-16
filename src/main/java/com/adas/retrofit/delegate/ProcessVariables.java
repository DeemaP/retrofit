package com.adas.retrofit.delegate;

import java.util.HashMap;
import java.util.Map;

/**
 * Имена переменных процесса Camunda и их значения по умолчанию.
 *
 * <p>Exclusive-гейтвеи настроены на default flow «в успех», но Camunda всё равно
 * вычисляет условия не-default веток. Если переменная не задана, JUEL бросает
 * исключение. Поэтому все решающие переменные инициализируются при старте процесса
 * безопасными значениями (happy-path), а пользователю достаточно завершать задачи
 * без передачи переменных. Чтобы свернуть на «плохую» ветку — передать переменную
 * в теле запроса complete.
 */
public final class ProcessVariables {

    private ProcessVariables() {
    }

    /** id заявки (Order) в виде строки UUID. */
    public static final String ORDER_ID = "orderId";

    /** Номер акта о приёме, проставляемый FormAcceptanceActDelegate. */
    public static final String ACCEPTANCE_ACT_NUMBER = "acceptanceActNumber";

    /** Наличие запчастей на складе (ставит CheckStockDelegate). */
    public static final String PARTS_AVAILABLE = "partsAvailable";

    /** Наличие оборудования на складе (ставит CheckStockDelegate). */
    public static final String EQUIPMENT_AVAILABLE = "equipmentAvailable";

    /** Возможно ли дооснащение (после первичной диагностики). */
    public static final String RETROFIT_POSSIBLE = "retrofitPossible";

    /** Есть ли список запчастей в БД. */
    public static final String PARTS_LIST_IN_DB = "partsListInDb";

    /** Есть ли список оборудования в БД. */
    public static final String EQUIPMENT_LIST_IN_DB = "equipmentListInDb";

    /** Исправна ли система после кодирования/калибровки. */
    public static final String SYSTEM_HEALTHY = "systemHealthy";

    /** Принял ли клиент работу. */
    public static final String CLIENT_ACCEPTED = "clientAccepted";

    /** Успешна ли поставка недостающих запчастей. */
    public static final String SUPPLY_OK = "supplyOk";

    /** Тип неисправности при доработке (CODING / DISMANTLE / иное → перемонтаж). */
    public static final String FAULT_TYPE = "faultType";

    /** Прошла ли система тест-драйв (после «Проверка системы и тест-драйв»). */
    public static final String TEST_DRIVE_PASSED = "testDrivePassed";

    /** Демо-override: принудительно считать запчасти отсутствующими (для ветки заказа). */
    public static final String PARTS_SHORTAGE = "partsShortage";

    /** Демо-override: принудительно считать оборудование отсутствующим (для ветки заказа). */
    public static final String EQUIPMENT_SHORTAGE = "equipmentShortage";

    /**
     * Переменные старта процесса с happy-path значениями по умолчанию.
     * Здесь только те решающие переменные, которые на happy-path никто не выставляет
     * (их условия иначе были бы невычислимы). Переменные снабжения
     * ({@code partsListInDb}, {@code equipmentListInDb}, {@code partsAvailable},
     * {@code equipmentAvailable}) проставляют делегаты проверок перед своими гейтвеями.
     */
    public static Map<String, Object> initialVariables(String orderId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ORDER_ID, orderId);
        vars.put(RETROFIT_POSSIBLE, true);
        vars.put(SYSTEM_HEALTHY, true);
        vars.put(CLIENT_ACCEPTED, true);
        vars.put(SUPPLY_OK, true);
        vars.put(FAULT_TYPE, "NONE");
        vars.put(TEST_DRIVE_PASSED, true);
        return vars;
    }
}