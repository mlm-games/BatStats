package app.batstats.battery.shizuku

import app.batstats.battery.util.BatteryStatsParser

/**
 * Small parser for "dumpsys batterystats --checkin".
 * Reads "l,pwi,uid,<mAh>..." rows and the earlier "i,uid,<uid>,<package>" map.
 * Power is UID-level: one UID may map to many packages (shared UID).
 * Returns per-UID cumulative mAh since last full charge plus package mapping.
 */
object CheckinParser {

    data class Snapshot(
        val energyByUid: Map<Int, Double>,
        val packagesByUid: Map<Int, List<String>>,
        /** Compat view: single package -> package name, otherwise "uid:<uid>". */
        val perPackageMah: Map<String, Double>
    )

    fun parse(lines: Sequence<String>): Snapshot {
        val uidToPackages = mutableMapOf<Int, LinkedHashSet<String>>()
        val energyByUid = mutableMapOf<Int, Double>()

        lines.forEach { line ->
            val p = BatteryStatsParser.splitCheckinLine(line)
            if (p.size < 4) return@forEach

            // uid map: 9,0,i,uid,1000,android (may repeat per package for shared UIDs)
            if (p.getOrNull(2) == "i" && p.getOrNull(3) == "uid" && p.size >= 6) {
                val uid = p[4].toIntOrNull() ?: return@forEach
                uidToPackages.getOrPut(uid) { linkedSetOf() }.add(p[5])
            }

            // power use item: 9,<uid>,l,pwi,uid,<mAh>,...
            // Require type == "uid" so device records (screen/wifi/bt/idle, uid 0) are ignored.
            if (p.getOrNull(2) == "l" &&
                p.getOrNull(3) == "pwi" &&
                p.getOrNull(4) == "uid" &&
                p.size >= 6
            ) {
                val uid = p[1].toIntOrNull() ?: return@forEach
                val mah = p[5].toDoubleOrNull() ?: return@forEach
                energyByUid[uid] = (energyByUid[uid] ?: 0.0) + mah
            }
        }

        val packagesByUid: Map<Int, List<String>> =
            uidToPackages.mapValues { it.value.toList() }

        // Compat map for callers still keyed by display string.
        val perPackageMah = mutableMapOf<String, Double>()
        for ((uid, mah) in energyByUid) {
            val pkgs = packagesByUid[uid].orEmpty()
            val key = pkgs.singleOrNull() ?: "uid:$uid"
            perPackageMah[key] = (perPackageMah[key] ?: 0.0) + mah
        }

        return Snapshot(energyByUid, packagesByUid, perPackageMah)
    }

    /** Display label for a UID: single package, "Shared UID <uid>", or "uid:<uid>". */
    fun displayNameFor(uid: Int, packagesByUid: Map<Int, List<String>>): String =
        BatteryStatsParser.displayNameFor(uid, packagesByUid[uid].orEmpty())
}
