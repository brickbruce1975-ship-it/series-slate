package slate

import java.util.Locale

enum class Role { TOP, JUNGLE, MID, BOT, SUPPORT }
enum class Side { BLUE, RED }
enum class Kind { PICK, BAN, NEXT }
data class Action(val kind: Kind, val side: Side = Side.BLUE, val role: Role = Role.TOP, val champion: String = "")
data class Option(val role: Role, val champion: String, val comfort: Int)
data class Choice(val option: Option, val covered: Int, val remaining: Int, val totalComfort: Int)

private fun key(name: String) = name.trim().lowercase(Locale.ROOT)
private fun validName(name: String) = name.isNotBlank() && name.length <= 50 && name.none { it.isISOControl() }

class Series {
    val history = mutableListOf<Action>()
    val round get() = 1 + history.count { it.kind == Kind.NEXT }
    val current: List<Action> get() = history.drop(history.indexOfLast { it.kind == Kind.NEXT } + 1)

    fun unavailable(): Set<String> =
        (history.filter { it.kind == Kind.PICK } + current.filter { it.kind == Kind.BAN }).map { key(it.champion) }.toSet()

    fun pickedRoles(side: Side): Set<Role> = current.filter { it.kind == Kind.PICK && it.side == side }.map { it.role }.toSet()

    fun apply(action: Action) {
        if (action.kind == Kind.NEXT) {
            require(round < 5) { "The five-game series is complete." }
            require(Side.entries.all { pickedRoles(it).size == Role.entries.size }) { "Record five different roles for each side before advancing." }
        } else {
            require(validName(action.champion)) { "Enter a champion name (up to 50 characters)." }
            require(key(action.champion) !in unavailable()) { "That champion is already picked in the series or banned this game." }
            if (action.kind == Kind.PICK) {
                require(action.role !in pickedRoles(action.side)) { "This side already has a pick for that role." }
            } else {
                require(current.count { it.side == action.side && it.kind == Kind.BAN } < 5) { "This side already has five bans." }
            }
        }
        history.add(action.copy(champion = action.champion.trim()))
    }

    fun undo() {
        require(history.isNotEmpty()) { "There are no actions to undo." }
        history.removeAt(history.lastIndex)
    }

    fun export(): String = history.joinToString("\n") { listOf(it.kind.name, it.side.name, it.role.name, it.champion).joinToString("\t") }

    companion object {
        fun restore(text: String): Series {
            val result = Series()
            for ((index, line) in text.lineSequence().filter { it.isNotBlank() }.withIndex()) {
                try {
                    val f = line.split('\t')
                    require(f.size == 4) { "Expected four tab-separated fields." }
                    result.apply(Action(Kind.valueOf(f[0]), Side.valueOf(f[1]), Role.valueOf(f[2]), f[3]))
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException("Action ${index + 1}: ${error.message}")
                }
            }
            return result
        }
    }
}

fun parsePool(text: String): List<Option> {
    val options = text.lineSequence().filter { it.isNotBlank() }.mapIndexed { index, line ->
        val f = line.split('\t')
        require(f.size == 3) { "Pool row ${index + 1} needs ROLE, champion and comfort separated by tabs." }
        val role = Role.entries.find { it.name.equals(f[0].trim(), ignoreCase = true) }
        val name = f[1].trim()
        val rating = f[2].trim().toIntOrNull()
        require(role != null && validName(name) && rating != null && rating in 1..5) { "Invalid role, champion or comfort on pool row ${index + 1}." }
        Option(role, name, rating)
    }.toList()
    require(options.isNotEmpty() && options.size <= 250) { "Use 1–250 pool rows." }
    require(options.distinctBy { it.role to key(it.champion) }.size == options.size) { "Duplicate champion within a role." }
    return options
}

private data class Score(val slots: Int, val comfort: Int)
private val scoreOrder = compareBy<Score> { it.slots }.thenBy { it.comfort }

// A five-bit role mask tracks the best distinct assignment so a flex champion cannot cover two lanes at once.
private fun coverage(options: List<Option>, roles: Set<Role>): Score {
    val best = mutableMapOf(0 to 0)
    for (champion in options.filter { it.role in roles }.groupBy { key(it.champion) }.values) {
        val previous = best.toMap()
        for (choice in champion) {
            val bit = 1 shl choice.role.ordinal
            for ((mask, value) in previous) {
                if ((mask and bit) != 0) continue
                val next = mask or bit
                best[next] = maxOf(best[next] ?: 0, value + choice.comfort)
            }
        }
    }
    return best.map { Score(Integer.bitCount(it.key), it.value) }.maxWith(scoreOrder)
}

fun choices(series: Series, pool: List<Option>, side: Side, role: Role): List<Choice> {
    val openRoles = Role.entries.toSet() - series.pickedRoles(side)
    if (role !in openRoles) return emptyList()
    val unavailable = series.unavailable()
    val available = pool.filter { key(it.champion) !in unavailable }
    val remaining = openRoles - role
    return available.filter { it.role == role }.map { pick ->
        val rest = coverage(available.filter { key(it.champion) != key(pick.champion) }, remaining)
        Choice(pick, rest.slots, remaining.size, rest.comfort + pick.comfort)
    }.sortedWith(compareByDescending<Choice> { it.covered }
        .thenByDescending { it.totalComfort }.thenByDescending { it.option.comfort }.thenBy { key(it.option.champion) })
}

val examplePool = """
TOP	Ornn	5
TOP	Poppy	4
TOP	Gnar	4
TOP	Sion	3
JUNGLE	Poppy	5
JUNGLE	Vi	4
JUNGLE	Sejuani	4
JUNGLE	Maokai	3
MID	Ahri	5
MID	Orianna	4
MID	Taliyah	4
MID	Tristana	3
BOT	Tristana	5
BOT	Ashe	4
BOT	Jinx	4
BOT	Varus	3
SUPPORT	Maokai	5
SUPPORT	Ashe	4
SUPPORT	Rell	4
SUPPORT	Braum	3
""".trim()
