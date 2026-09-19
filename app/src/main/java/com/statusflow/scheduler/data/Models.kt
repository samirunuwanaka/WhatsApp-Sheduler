package com.statusflow.scheduler.data

enum class ScheduleType {
    MESSAGE,
    STATUS
}

enum class ScheduleStatus {
    PENDING,
    DISPATCHING,
    SENT,
    MISSED,
    FAILED,
    CANCELLED
}
