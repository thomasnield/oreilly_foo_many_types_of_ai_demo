
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import tornadofx.*

fun main() = Application.launch(MyApp::class.java)

class MyApp: App(MainDashboard::class)

class MainDashboard: View() {

    override val root = tabpane {

        style = "-fx-font-size: 14pt; "
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab("Traveling Salesman", TSPView().root)
        tab("Sudoku Solver", SudokuView().root.let {
            val v = VBox()
            v.alignment = Pos.CENTER
            val h = HBox()
            h.alignment = Pos.CENTER
            h += it
            v += h
            v.useMaxSize = true
            h.useMaxSize = true
            v
        })
        tab("Light/Dark Font Suggester", MachineLearningDemoView().root)

        primaryStage.isFullScreen = true
    }
}