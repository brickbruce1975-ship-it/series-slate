package slate

private fun fails(block: () -> Unit) = check(runCatching(block).exceptionOrNull() is IllegalArgumentException)
fun main() {
    val pool=parsePool("TOP\tFlex\t5\nTOP\tSolo\t4\nJUNGLE\tFlex\t5")
    val s=Series()
    val ranked=choices(s,pool,Side.BLUE,Role.TOP)
    check(ranked.first().option.champion=="Solo" && ranked.first().covered==1 && ranked.last().covered==0)
    s.apply(Action(Kind.BAN,Side.RED,Role.TOP," Flex "))
    check(choices(s,pool,Side.BLUE,Role.TOP).single().option.champion=="Solo")
    fails { s.apply(Action(Kind.PICK,Side.BLUE,Role.TOP,"flex")) }
    fails { s.apply(Action(Kind.NEXT)) }
    for(side in Side.entries) for(role in Role.entries) s.apply(Action(Kind.PICK,side,role,"${side.name}-${role.name}"))
    fails { s.apply(Action(Kind.PICK,Side.BLUE,Role.TOP,"Extra")) }
    s.apply(Action(Kind.NEXT));check(s.round==2 && "flex" !in s.unavailable())
    fails { s.apply(Action(Kind.PICK,Side.RED,Role.JUNGLE,"blue-top")) }
    check(Series.restore(s.export()).history==s.history)
    s.undo();check(s.round==1 && "flex" in s.unavailable())
    fails { parsePool("TOP\tFlex\t6") }; fails { parsePool("TOP\tFlex\t5\nTOP\tflex\t4") }
    fails { Series.restore("PICK\tBLUE\tMID\tA\nPICK\tRED\tMID\ta") }
    val bans=Series()
    repeat(5){bans.apply(Action(Kind.BAN,Side.BLUE,Role.TOP,"Ban$it"))}
    fails { bans.apply(Action(Kind.BAN,Side.BLUE,Role.TOP,"Sixth")) }
    val end=Series()
    repeat(5){game ->
        for(side in Side.entries) for(role in Role.entries) end.apply(Action(Kind.PICK,side,role,"$game-$side-$role"))
        if(game<4) end.apply(Action(Kind.NEXT))
    }
    fails { end.apply(Action(Kind.NEXT)) }
    println("Passed flex coverage, ranking, cross-side locks, round-local bans, roster limits, scoring, replay, undo, invalid input and five-game limit.")
}
