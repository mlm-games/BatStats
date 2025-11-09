package app.batstats.enhanced

/**
 * Small parser for "dumpsys batterystats --checkin".
 * reads "l,pwi,uid,<mAh>..." rows and the earlier "i,uid,<uid>,<package>" map.
 * Returns per-package cumulative mAh since last full charge.
 */
object CheckinParser {

    data class Snapshot(val perPackageMah: Map<String, Double>)

    fun parse(lines: Sequence<String>): Snapshot {
        val uidToPkg = mutableMapOf<Int, String>()
        val energyByPkg = mutableMapOf<String, Double>()

        lines.forEach { line ->
            val p = line.split(',') // checkin is comma-separated
            if (p.size < 4) return@forEach

            // uid map: 9,0,i,uid,1000,android
            if (p[2] == "i" && p[3] == "uid" && p.size >= 6) {
                val uid = p[4].toIntOrNull() ?: return@forEach
                val pkg = p[5]
                // prefer non-system packages; but keep all
                uidToPkg[uid] = pkg
            }

            // power use item: 9,<uid>,l,pwi,uid,<mAh>,...
            if (p[2] == "l" && p[3] == "pwi" && p.size >= 6) {
                val uid = p[1].toIntOrNull() ?: return@forEach
                val mah = p[5].toDoubleOrNull() ?: return@forEach
                val pkg = uidToPkg[uid] ?: "uid:$uid"
                energyByPkg[pkg] = (energyByPkg[pkg] ?: 0.0) + mah
            }
        }
        return Snapshot(energyByPkg)
    }
}
