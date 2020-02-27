package com.project.instagram.Navigation.model

data class AlarmDTO(
    var destinationUid: String? = null,
    var userId: String? = null,
    var uid: String? = null,
    var kind: Int? = null,
    var message: String? = null,
    var timestamp: Long? = null
) {
    companion object {
        const val ALARM_LIKE = 0
        const val ALARM_COMMENT = 1
        const val ALARM_FOLLOW = 2
    }
}