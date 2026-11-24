package slate

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

class Studio {
    private val window by lazy { JFrame("Series Slate · Fearless draft rehearsal") }
    private val side = JComboBox(Side.entries.toTypedArray())
    private val role = JComboBox(Role.entries.toTypedArray())
    private val name = JTextField(15)
    private val status = JLabel("Choose a role to rehearse a pick. Comfort ratings start as illustrative examples.")
    private val heading = JLabel()
    private val recommendations = DefaultListModel<String>()
    private val candidates = JList(recommendations)
    private val record = JTextArea()
    private var series = Series()
    private var poolText = examplePool
    private var pool = parsePool(poolText)
    private var displayed = emptyList<Choice>()

    private fun actionButton(label: String, action: () -> Unit) = JButton(label).apply {
        addActionListener {
            try { action(); refresh() }
            catch (error: Exception) { status.text = error.message ?: "Action failed." }
        }
    }

    fun content(): JPanel {
        val body = JPanel(BorderLayout(12, 12)).apply { border = BorderFactory.createEmptyBorder(16,16,16,16) }
        val form = JPanel(FlowLayout(FlowLayout.LEFT))
        form.add(JLabel("Side")); form.add(side); form.add(JLabel("Role")); form.add(role)
        form.add(JLabel("Champion")); form.add(name)
        form.add(actionButton("Pick") { log(Kind.PICK) }); form.add(actionButton("Ban") { log(Kind.BAN) })
        val north = JPanel(BorderLayout()).apply { add(heading, BorderLayout.NORTH); add(form, BorderLayout.CENTER) }
        body.add(north, BorderLayout.NORTH)
        record.isEditable = false
        val options = JScrollPane(candidates).apply { border = BorderFactory.createTitledBorder("Coach pool · select to fill champion") }
        val timeline = JScrollPane(record).apply { border = BorderFactory.createTitledBorder("Series record") }
        body.add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, options, timeline).apply { resizeWeight = .62 }, BorderLayout.CENTER)
        val controls = JPanel(FlowLayout(FlowLayout.LEFT))
        controls.add(actionButton("Undo last action") { series.undo(); status.text="Last action removed." })
        controls.add(actionButton("Next game") { series.apply(Action(Kind.NEXT)); status.text="Previous picks carry forward. Bans reset." })
        controls.add(actionButton("Edit coach pool") { editPool() })
        controls.add(actionButton("Save series") { save() }); controls.add(actionButton("Open series") { load() })
        body.add(JPanel(BorderLayout()).apply { add(controls, BorderLayout.NORTH); add(status, BorderLayout.SOUTH) }, BorderLayout.SOUTH)
        side.addActionListener { refresh() }; role.addActionListener { refresh() }
        candidates.addListSelectionListener {
            if (!it.valueIsAdjusting) displayed.getOrNull(candidates.selectedIndex)?.let { pick -> name.text=pick.option.champion }
        }
        refresh()
        return body
    }

    fun open() {
        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        window.contentPane = content(); window.minimumSize = Dimension(865, 410)
        window.setSize(1050, 550); window.setLocationRelativeTo(null); refresh(); window.isVisible=true
    }

    private fun log(kind: Kind) {
        series.apply(Action(kind, side.selectedItem as Side, role.selectedItem as Role, name.text))
        status.text = "Recorded ${name.text.trim()}."; name.text=""
    }

    private fun refresh() {
        heading.text="Game ${series.round} of 5 · both sides share series-wide pick exclusions"
        displayed=choices(series, pool, side.selectedItem as Side, role.selectedItem as Role)
        recommendations.clear()
        for (c in displayed) recommendations.addElement("${c.option.champion} · ${c.option.comfort}/5 comfort · ${c.covered}/${c.remaining} other roles covered · lineup ${c.totalComfort}")
        if(displayed.isEmpty()) recommendations.addElement("No available pool option for this role.")
        var game=1
        record.text=buildString {
            appendLine("Game 1")
            for(a in series.history) {
                if(a.kind==Kind.NEXT) {game++;appendLine();appendLine("Game $game")}
                else appendLine("${a.side}  ${a.kind}  ${if(a.kind==Kind.PICK) a.role.name else ""}  ${a.champion}")
            }
        }
    }

    private fun editPool() {
        val editor=JTextArea(poolText,18,42)
        val choice=JOptionPane.showConfirmDialog(window,JScrollPane(editor),"Role, champion, comfort (tab-separated)",JOptionPane.OK_CANCEL_OPTION)
        if(choice!=JOptionPane.OK_OPTION)return
        val parsed=parsePool(editor.text)
        poolText=editor.text;pool=parsed;status.text="Updated ${pool.size} coach ratings."
    }

    private fun save() {
        val chooser=JFileChooser().apply { selectedFile=File("series-slate.tsv") }
        if(chooser.showSaveDialog(window)!=JFileChooser.APPROVE_OPTION)return
        val file=chooser.selectedFile
        if(file.exists() && JOptionPane.showConfirmDialog(window,"Replace ${file.name}?","Save series",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return
        file.writeText("SERIES-SLATE-1\n"+poolText.trim()+"\n---ACTIONS---\n"+series.export())
        status.text="Saved pool and series to ${file.name}."
    }

    private fun load() {
        val chooser=JFileChooser()
        if(chooser.showOpenDialog(window)!=JFileChooser.APPROVE_OPTION)return
        val file=chooser.selectedFile
        require(file.length()<=256_000) { "Series file is too large." }
        val data=file.readText().replace("\r\n","\n")
        require(data.startsWith("SERIES-SLATE-1\n")) { "Unrecognized series file." }
        val parts=data.removePrefix("SERIES-SLATE-1\n").split("\n---ACTIONS---\n",limit=2)
        require(parts.size==2) { "Missing actions section." }
        val verifiedPool=parsePool(parts[0]);val verifiedSeries=Series.restore(parts[1])
        poolText=parts[0];pool=verifiedPool;series=verifiedSeries;status.text="Opened ${file.name}."
    }
}

fun main(args: Array<String>) {
    if (args.contentEquals(arrayOf("--demo"))) {
        val series=Series();val pool=parsePool("TOP\tFlex\t5\nTOP\tSolo\t4\nJUNGLE\tFlex\t5")
        for(c in choices(series,pool,Side.BLUE,Role.TOP))
            println("${c.option.champion}: covers ${c.covered}/${c.remaining} remaining roles; combined comfort ${c.totalComfort}")
        return
    }
    SwingUtilities.invokeLater { Studio().open() }
}
