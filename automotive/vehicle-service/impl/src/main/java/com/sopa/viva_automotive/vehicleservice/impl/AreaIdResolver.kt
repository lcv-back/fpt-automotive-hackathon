package com.sopa.viva_automotive.vehicleservice.impl

object AreaIdResolver {

        fun resolve(declared: IntArray?, requestedAreaId: Int): List<Int> {
        if (declared == null || declared.isEmpty()) return listOf(requestedAreaId)
        if (requestedAreaId in declared) return listOf(requestedAreaId)
        if (requestedAreaId == 0) return declared.toList()

        val overlapping = declared.filter { it and requestedAreaId != 0 }
        return overlapping.ifEmpty { listOf(requestedAreaId) }
    }
}
