package com.adas.retrofit.entity;

/**
 * Фаза слесарно-монтажных работ, к которой относится чек-лист.
 * <ul>
 *     <li>{@link #ASSEMBLY} — монтажные и слесарные работы при дооснащении (Activity_1asctpd).</li>
 *     <li>{@link #DISASSEMBLY} — демонтаж установленной системы (Activity_05mqhxj).</li>
 * </ul>
 */
public enum WorkPhase {
    ASSEMBLY,
    DISASSEMBLY
}