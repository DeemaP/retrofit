package com.adas.retrofit.entity;

/**
 * Направление программирования ЭБУ.
 * <ul>
 *     <li>{@link #INSTALL} — прошивка/активация при дооснащении (Кодирование и калибровка).</li>
 *     <li>{@link #RESTORE} — возврат к исходному ПО (Возврат авто в изначальное состояние).</li>
 * </ul>
 */
public enum ProgrammingDirection {
    INSTALL,
    RESTORE
}