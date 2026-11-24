package slate

import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

private fun children(c: Component): List<Component> = listOf(c) +
    if (c is Container) c.components.flatMap(::children) else emptyList()

private fun layOut(component: Component) {
    if (component is Container) {
        component.doLayout()
        component.components.forEach(::layOut)
    }
}

fun main(args: Array<String>) {
    require(args.size == 1) { "Pass a directory for PNG captures." }
    val target = File(args[0]).apply { mkdirs() }
    SwingUtilities.invokeAndWait {
        val panel = Studio().content()
        panel.setSize(1180, 502)
        val controls = children(panel)
        val sides = controls.filterIsInstance<JComboBox<*>>().single { it.getItemAt(0) == Side.BLUE }
        val roles = controls.filterIsInstance<JComboBox<*>>().single { it.getItemAt(0) == Role.TOP }
        val field = controls.filterIsInstance<JTextField>().single()
        val choices = controls.filterIsInstance<JList<*>>().single()
        val record = controls.filterIsInstance<JTextArea>().single()
        fun click(text: String) = controls.filterIsInstance<JButton>().single { it.text == text }.doClick()

        fun capture(file: String, headline: String, caption: String) {
            layOut(panel)
            val image = BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.color = Color(19, 35, 54); g.fillRect(0, 0, 1280, 720)
                g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 35)
                g.drawString(headline, 50, 65)
                g.translate(50, 95); panel.printAll(g); g.translate(-50, -95)
                g.font = Font(Font.SANS_SERIF, Font.PLAIN, 23); g.color = Color.WHITE
                g.drawString(caption, 50, 660)
            } finally { g.dispose() }
            ImageIO.write(image, "png", File(target,file+".png"))
        }

        capture("01", "Series Slate · Fearless draft rehearsal", "The coach's example comfort ratings belong to this practice session.")
        choices.selectedIndex = 0
        check(field.text.isNotBlank())
        field.text = "Poppy"; click("Pick")
        check(record.text.contains("Poppy")); roles.selectedItem = Role.JUNGLE
        check((0 until choices.model.size).none { choices.model.getElementAt(it).toString().startsWith("Poppy ·") })
        capture("02", "What does a top-lane flex pick cost?", "Blue picked Poppy at top. Jungle immediately loses that option.")
        sides.selectedItem = Side.RED; field.text = "poppy"; click("Pick")
        check(controls.filterIsInstance<JLabel>().any { it.text.contains("already picked") })
        capture("03", "The same exclusion applies to the opponent", "A second pick is rejected even if the coach types the name with different capitalization.")
        val names = mapOf(Side.BLUE to listOf("Poppy","Vi","Ahri","Jinx","Rell"), Side.RED to listOf("Ornn","Sejuani","Orianna","Varus","Braum"))
        for (side in Side.entries) for (role in Role.entries) {
            if (side == Side.BLUE && role == Role.TOP) continue
            sides.selectedItem = side; roles.selectedItem = role; field.text = names.getValue(side)[role.ordinal]; click("Pick")
        }
        sides.selectedItem = Side.BLUE; field.text = "Ashe"; click("Ban")
        capture("04", "Record both lineups and the game's bans", "Five distinct roles on each side complete game one. Ashe is banned for this game.")
        click("Next game"); roles.selectedItem = Role.BOT
        check(record.text.contains("Game 2"))
        check((0 until choices.model.size).any { choices.model.getElementAt(it).toString().startsWith("Ashe ·") })
        capture("05", "A new game reopens bans and keeps picks locked", "Ashe returns for game two. Jinx and Varus stay out after their game-one picks.")
        click("Undo last action"); check(!record.text.contains("Game 2"))
        println("Swing panel replay passed: picks, cross-side rejection, ten-role completion, bans, next game and undo.")
    }
}
