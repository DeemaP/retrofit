package com.adas.retrofit.service;

import com.adas.retrofit.entity.RetrofitType;

import java.util.List;
import java.util.Map;

/**
 * Справочник: какие <b>запчасти</b> (ставятся в авто) и какое <b>оборудование</b> (оснастка цеха)
 * требуются для каждого типа дооснащения. Для альфы — фиксированные перечни.
 */
public final class RetrofitCatalog {

    private RetrofitCatalog() {
    }

    private static final Map<RetrofitType, List<String>> PARTS = Map.of(
            RetrofitType.ACC, List.of("Радар ACC", "Кронштейн радара", "Разъём питания радара", "Жгут проводов ACC"),
            RetrofitType.BSA, List.of("Радары BSA (2 шт.)", "Кронштейны бамперные", "Индикаторы в зеркалах", "Жгут BSA"),
            RetrofitType.AEB, List.of("Фронтальная камера", "Радар AEB", "Кронштейн камеры", "Жгут AEB"),
            RetrofitType.LKA, List.of("Фронтальная камера LKA", "Кронштейн камеры у зеркала", "Жгут LKA"),
            RetrofitType.COMBINED, List.of("Радар ACC/AEB", "Фронтальная камера", "Радары BSA", "Комплект кронштейнов", "Универсальный жгут"));

    private static final Map<RetrofitType, List<String>> EQUIPMENT = Map.of(
            RetrofitType.ACC, List.of("Диагностический сканер", "ПО кодирования блоков", "Калибровочный стенд радара"),
            RetrofitType.BSA, List.of("Диагностический сканер", "ПО кодирования блоков", "Стенд калибровки радаров BSA"),
            RetrofitType.AEB, List.of("Диагностический сканер", "ПО кодирования блоков", "Мишень калибровки камеры", "Калибровочный стенд"),
            RetrofitType.LKA, List.of("Диагностический сканер", "ПО кодирования блоков", "Мишень калибровки камеры"),
            RetrofitType.COMBINED, List.of("Диагностический сканер", "ПО кодирования блоков", "Калибровочный стенд радара", "Мишени калибровки камеры"));

    public static List<String> partsFor(RetrofitType type) {
        return PARTS.getOrDefault(type, List.of());
    }

    public static List<String> equipmentFor(RetrofitType type) {
        return EQUIPMENT.getOrDefault(type, List.of());
    }
}