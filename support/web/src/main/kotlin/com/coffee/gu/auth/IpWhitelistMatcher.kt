package com.coffee.gu.auth

import java.net.InetAddress

object IpWhitelistMatcher {

    fun isAllowed(clientIp: String?, rawAllowedIps: String): Boolean {
        val normalizedClientIp = clientIp?.trim()
        if (normalizedClientIp.isNullOrBlank()) return false

        val allowedEntries = rawAllowedIps.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return allowedEntries.any { rule -> matchesIpRule(normalizedClientIp, rule) }
    }

    fun matchesIpRule(clientIp: String, rule: String): Boolean {
        return try {
            if (!rule.contains("/")) {
                InetAddress.getByName(clientIp) == InetAddress.getByName(rule)
            } else {
                val parts = rule.split("/")
                val networkAddress = InetAddress.getByName(parts[0]).address
                val targetAddress = InetAddress.getByName(clientIp).address
                if (networkAddress.size != targetAddress.size) return false

                val prefixLength = parts[1].toInt()
                val fullBytes = prefixLength / 8
                val remainderBits = prefixLength % 8

                for (i in 0 until fullBytes) {
                    if (networkAddress[i] != targetAddress[i]) return false
                }
                if (remainderBits > 0) {
                    val mask = (0xFF shl (8 - remainderBits)) and 0xFF
                    if ((networkAddress[fullBytes].toInt() and mask) != (targetAddress[fullBytes].toInt() and mask)) {
                        return false
                    }
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
