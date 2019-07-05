
import javafx.application.Application
import javafx.scene.control.TabPane
import tornadofx.*

fun main() = Application.launch(MyApp::class.java)

class MyApp: App(MainDashboard::class)

class MainDashboard: View() {

    override val root = tabpane {

        style = "-fx-font-size: 14pt; "
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab("Traveling Salesman", TSPView().root)
        tab("Light/Dark Font Suggester", MachineLearningDemoView().root)

        primaryStage.isFullScreen = true
    }
}