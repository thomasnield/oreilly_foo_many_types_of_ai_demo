import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*


class MachineLearningDemoView: View() {

    val backgroundColor = SimpleObjectProperty(Color.GRAY)

    fun assignRandomColor() = randomColor()
            .also { backgroundColor.set(it) }

    override val root = splitpane {
        style = "-fx-font-size: 16pt; "
        orientation = Orientation.VERTICAL

        splitpane {

            title = "Light/Dark Text Suggester"
            orientation = Orientation.HORIZONTAL

            borderpane {

                top = label("TRAINING DATA") {
                    style {
                        textFill =  Color.RED
                        fontWeight = FontWeight.BOLD
                    }
                }

                center = tableview(PredictorModel.inputs) {
                    readonlyColumn("Color", LabeledColor::label)
                    readonlyColumn("Red", LabeledColor::red)
                    readonlyColumn("Green", LabeledColor::green)
                    readonlyColumn("Blue", LabeledColor::blue)
                    readonlyColumn("Output", LabeledColor::outputValue)
                }

            }

            borderpane {

                top = label("PREDICT") {
                    style {
                        textFill =  Color.RED
                        fontWeight = FontWeight.BOLD
                    }
                }

                center = form {
                    fieldset {
                        field("Model") {
                            combobox(PredictorModel.selectedPredictor) {

                                PredictorModel.Predictor.values().forEach { items.add(it) }
                            }
                        }
                    }
                    fieldset {

                        field("Background") {
                            colorpicker {
                                valueProperty().onChange {
                                    backgroundColor.set(it)
                                }

                                customColors.forEach { println(it) }
                            }
                        }
                        field("Result") {
                            label("LOREM IPSUM") {
                                backgroundProperty().bind(
                                        backgroundColor.select { ReadOnlyObjectWrapper(Background(BackgroundFill(it, CornerRadii.EMPTY, Insets.EMPTY))) }
                                )

                                backgroundColor.onChange {
                                    val result = PredictorModel.predict(it!!)

                                    text = result.toString()
                                    textFill = result.color
                                }

                            }
                        }
                    }
                }
            }
        }

        label("""This is a simple machine learning demo to train and predict a light/dark font for a given background color.
            |
            |There are many ways to do this, from logistic regressions and decision trees to neural networks. All of these 
            |machine learning algorithms were written from scratch, and trained with hill climbing or simulated annealing 
            |rather than gradient descent. 
            |
            |Play with a few of these algorithms and use the right panel to predict a light or dark font! 
        """.trimMargin())
    }
}
