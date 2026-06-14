package com.adas.retrofit.entity;

/** Тип дооснащаемой системы ADAS. */
public enum RetrofitType {
    /** Adaptive Cruise Control. */
    ACC,
    /** Blind Spot Assist. */
    BSA,
    /** Autonomous Emergency Braking. */
    AEB,
    /** Lane Keeping Assist. */
    LKA,
    /** Комбинированное дооснащение несколькими системами. */
    COMBINED
}