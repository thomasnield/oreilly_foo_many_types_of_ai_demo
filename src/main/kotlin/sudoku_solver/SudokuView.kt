import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class SudokuView : View() {

    override val root = borderpane {

        title = "Sudoku Solver"

        left = form {

            fieldset {
                field {
                    label("ALGORITHM") {
                        style {
                            fontWeight = FontWeight.BOLD
                            textFill = Color.RED
                        }
                    }
                }
                field {
                    combobox(property = GridModel.selectedSolverProperty) {
                        items.setAll(Solver.values().asList())
                        useMaxWidth = true
                    }
                }
            }

            fieldset {
                field {
                    button("Solve!") {
                        useMaxWidth = true
                        setOnAction { GridModel.solve() }
                    }
                }
                field {
                    button("Reset") {
                        useMaxWidth = true
                        setOnAction {
                            GridModel.grid.forEach { it.value = null }
                        }
                    }
                }
                field {
                    label(GridModel.statusProperty)
                }
            }
            fieldset {
                field {
                    label("""Solving a Sudoku has a surprising 
number of applications for real-world 
optimization problems. Consider scheduling 
staff workers or classrooms. You want to 
arrange them in a way to achieve constraints 
and an objective. But you only have so many 
slots and so much capacity.

Discrete optimization techniques like tree 
search and branch-and-prune can be used to 
solve these types of problems. By leveraging 
some linear programming as well, these 
problems can be solved even more efficiently.""")
                }
            }
        }

        // build GridPane view
        center = gridpane {

            (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
                    .forEach { (parentX, parentY) ->

                        val childGrid = GridPane()

                        (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                                .forEach { (x, y) ->

                                    val cell = GridModel.cellFor(parentX, parentY, x, y)

                                    val button = Button().apply {
                                        minWidth = 60.0
                                        minHeight = 60.0

                                        cell.valueProperty().onChange {
                                            text = it?.toString()
                                            if (it == null) textFill = Color.BLACK
                                        }

                                        style { fontSize = 24.px}

                                        setOnAction {
                                            cell.increment()
                                            textFill = Color.RED
                                        }
                                    }

                                    childGrid.add(button, x, y, 1, 1)
                                }

                        childGrid.paddingRight = 10.0
                        childGrid.paddingBottom = 10.0

                        add(childGrid, parentX, parentY, 1, 1)
                    }
        }
    }
}