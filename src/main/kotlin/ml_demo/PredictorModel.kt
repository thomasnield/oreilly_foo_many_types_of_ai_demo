import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import org.apache.commons.math3.distribution.NormalDistribution
import org.nield.kotlinstatistics.countBy
import org.nield.kotlinstatistics.random
import org.nield.kotlinstatistics.randomFirst
import tornadofx.style
import java.net.URL
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

object PredictorModel {

    val inputs = FXCollections.observableArrayList<LabeledColor>()

    val selectedPredictor = SimpleObjectProperty(Predictor.DECISION_TREE)

    fun predict(color: Color) = selectedPredictor.get().predict(color)

    operator fun plusAssign(labeledColor: LabeledColor)  {
        inputs += labeledColor
        Predictor.values().forEach { it.retrainFlag = true }
    }
    operator fun plusAssign(categorizedInput: Pair<Color,FontShade>)  {
        inputs += categorizedInput.let { LabeledColor(it.first, it.second) }
        Predictor.values().forEach { it.retrainFlag = true }
    }

    init { preTrainData() }

    fun preTrainData() {

        URL("https://tinyurl.com/y2qmhfsr")
                .readText().split(Regex("\\r?\\n"))
                .asSequence()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { s ->
                    s.split(",").map { it.toInt() }
                }
                .map { Color.rgb(it[0], it[1], it[2]) }
                .map { LabeledColor(it, Predictor.FORMULAIC.predict(it))  }
                .toList()
                .forEach {
                    inputs += it
                }

        Predictor.values().forEach { it.retrainFlag = true }
    }


    enum class Predictor {

        /**
         * Uses a simple formula to classify colors as LIGHT or DARK
         */
        FORMULAIC {
            override fun predict(color: Color) =  (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
                        .let { if (it > .5) FontShade.DARK else FontShade.LIGHT }
        },

        LINEAR_REGRESSION_HILL_CLIMBING {

            override fun predict(color: Color): FontShade {

                var redWeightCandidate = 0.0
                var greenWeightCandidate = 0.0
                var blueWeightCandidate = 0.0

                var currentLoss = Double.MAX_VALUE

                val normalDistribution = NormalDistribution(0.0, 1.0)

                fun predict(color: Color) =
                        (redWeightCandidate * color.red + greenWeightCandidate * color.green + blueWeightCandidate * color.blue)

                repeat(10000) {

                    val selectedColor = (0..2).asSequence().randomFirst()
                    val adjust = normalDistribution.sample()

                    // make random adjustment to two of the colors
                    when {
                        selectedColor == 0 -> redWeightCandidate += adjust
                        selectedColor == 1 -> greenWeightCandidate += adjust
                        selectedColor == 2 -> blueWeightCandidate += adjust
                    }

                    // Calculate the loss, which is sum of squares
                    val newLoss = inputs.asSequence()
                            .map { (color, fontShade) ->
                                (predict(color) - fontShade.intValue).pow(2)
                            }.sum()

                    // If improvement doesn't happen, undo the move
                    if (newLoss < currentLoss) {
                        currentLoss = newLoss
                    } else {
                        // revert if no improvement happens
                        when {
                            selectedColor == 0 -> redWeightCandidate -= adjust
                            selectedColor == 1 -> greenWeightCandidate -= adjust
                            selectedColor == 2 -> blueWeightCandidate -= adjust
                        }
                    }
                }

                println("${redWeightCandidate}R + ${greenWeightCandidate}G + ${blueWeightCandidate}B")

                val formulasLoss = inputs.asSequence()
                        .map { (color, fontShade) ->
                            ( (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) - fontShade.intValue).pow(2)
                        }.average()

                println("BEST LOSS: $currentLoss, FORMULA'S LOSS: $formulasLoss \r\n")

                return predict(color)
                        .let { if (it > .5) FontShade.DARK else FontShade.LIGHT }
            }
        },

        LOGISTIC_REGRESSION_HILL_CLIMBING {


            var b0 = .01 // constant
            var b1 = .01 // red beta
            var b2 = .01 // green beta
            var b3 = .01 // blue beta


            fun predictProbability(color: Color) = 1.0 / (1 + exp(-(b0 + b1 * color.red + b2 * color.green + b3 * color.blue)))

            // Helpful Resources:
            // StatsQuest on YouTube: https://www.youtube.com/watch?v=yIYKR4sgzI8&list=PLblh5JKOoLUKxzEP5HA2d-Li7IJkHfXSe
            // Brandon Foltz on YouTube: https://www.youtube.com/playlist?list=PLIeGtxpvyG-JmBQ9XoFD4rs-b3hkcX7Uu
            override fun predict(color: Color): FontShade {


                if (retrainFlag) {
                    var bestLikelihood = -10_000_000.0

                    // use hill climbing for optimization
                    val normalDistribution = NormalDistribution(0.0, 1.0)

                    b0 = .01 // constant
                    b1 = .01 // red beta
                    b2 = .01 // green beta
                    b3 = .01 // blue beta

                    // 1 = DARK FONT, 0 = LIGHT FONT

                    repeat(50000) {

                        val selectedBeta = (0..3).asSequence().randomFirst()
                        val adjust = normalDistribution.sample()

                        // make random adjustment to two of the colors
                        when {
                            selectedBeta == 0 -> b0 += adjust
                            selectedBeta == 1 -> b1 += adjust
                            selectedBeta == 2 -> b2 += adjust
                            selectedBeta == 3 -> b3 += adjust
                        }

                        // calculate maximum likelihood
                        val darkEstimates = inputs.asSequence()
                                .filter { it.fontShade == FontShade.DARK }
                                .map { ln(predictProbability(it.color)) }
                                .sum()

                        val lightEstimates = inputs.asSequence()
                                .filter { it.fontShade == FontShade.LIGHT }
                                .map { ln(1 - predictProbability(it.color)) }
                                .sum()

                        val likelihood = darkEstimates + lightEstimates

                        if (bestLikelihood < likelihood) {
                            bestLikelihood = likelihood
                        } else {
                            // revert if no improvement happens
                            when {
                                selectedBeta == 0 -> b0 -= adjust
                                selectedBeta == 1 -> b1 -= adjust
                                selectedBeta == 2 -> b2 -= adjust
                                selectedBeta == 3 -> b3 -= adjust
                            }
                        }
                    }

                    println("1.0 / (1 + exp(-($b0 + $b1*R + $b2*G + $b3*B))")
                    println("BEST LIKELIHOOD: $bestLikelihood")
                    retrainFlag = false
                }

                return predictProbability(color)
                        .let { if (it > .5) FontShade.DARK else FontShade.LIGHT }
            }
        },

        DECISION_TREE {

            // Helpful Resources:
            // StatusQuest on YouTube: https://www.youtube.com/watch?v=7VeUPuFGJHk

            inner class Feature(val name: String, val mapper: (Color) -> Double) { 
                override fun toString() = name 
            }

            val features = listOf(
                    Feature("Red") { it.red * 255.0 },
                    Feature("Green") { it.green * 255.0 },
                    Feature("Blue") { it.blue * 255.0 }
            )

            fun giniImpurity(samples: List<LabeledColor>): Double {

                val totalSampleCount = samples.count().toDouble()

                return 1.0 - (samples.count { it.fontShade == FontShade.DARK }.toDouble() / totalSampleCount).pow(2) -
                        (samples.count {  it.fontShade == FontShade.LIGHT }.toDouble() / totalSampleCount).pow(2)
            }

            fun giniImpurityForSplit(feature: Feature, splitValue: Double, samples: List<LabeledColor>): Double {
                val positiveFeatureSamples = samples.filter { feature.mapper(it.color) >= splitValue }
                val negativeFeatureSamples = samples.filter { feature.mapper(it.color) < splitValue }

                val positiveImpurity = giniImpurity(positiveFeatureSamples)
                val negativeImpurity = giniImpurity(negativeFeatureSamples)

                return (positiveImpurity * (positiveFeatureSamples.count().toDouble() / samples.count().toDouble())) +
                        (negativeImpurity * (negativeFeatureSamples.count().toDouble() / samples.count().toDouble()))
            }

            fun splitContinuousVariable(feature: Feature, samples: List<LabeledColor>): Double? {

                val featureValues = samples.asSequence().map { feature.mapper(it.color) }.distinct().toList().sorted()

                val bestSplit = featureValues.asSequence().zipWithNext { value1, value2 -> (value1 + value2) / 2.0 }
                        .minBy { giniImpurityForSplit(feature, it, samples) }

                return bestSplit
            }


            inner class FeatureAndSplit(val feature: Feature, val split: Double)

            fun buildLeaf(samples: List<LabeledColor>, previousLeaf: TreeLeaf? = null, featureSampleSize: Int? = null ): TreeLeaf? {

                val fs = (if (featureSampleSize == null) features else features.random(featureSampleSize) )
                        .asSequence()
                        .filter { splitContinuousVariable(it, samples) != null }
                        .map { feature ->
                            FeatureAndSplit(feature, splitContinuousVariable(feature, samples)!!)
                        }.minBy { fs ->
                            giniImpurityForSplit(fs.feature, fs.split, samples)
                        }

                return if (previousLeaf == null ||
                        (fs != null && giniImpurityForSplit(fs.feature, fs.split, samples) < previousLeaf.giniImpurity))
                    TreeLeaf(fs!!.feature, fs.split, samples)
                else
                    null
            }


            inner class TreeLeaf(val feature: Feature,
                           val splitValue: Double,
                           val samples: List<LabeledColor>) {

                val goodWeatherItems = samples.filter { it.fontShade == FontShade.DARK }
                val badWeatherItems = samples.filter { it.fontShade == FontShade.LIGHT }

                val positiveItems = samples.filter { feature.mapper(it.color) >= splitValue }
                val negativeItems = samples.filter { feature.mapper(it.color) < splitValue }

                val giniImpurity = giniImpurityForSplit(feature, splitValue, samples)

                val featurePositiveLeaf: TreeLeaf? = buildLeaf(samples.filter { feature.mapper(it.color) >= splitValue }, this)
                val featureNegativeLeaf: TreeLeaf? = buildLeaf(samples.filter { feature.mapper(it.color) < splitValue }, this)


                fun predict(color: Color): Double {

                    val featureValue = feature.mapper(color)


                    return when {
                        featureValue >= splitValue -> when {
                            featurePositiveLeaf == null -> (goodWeatherItems.count { feature.mapper(it.color) >= splitValue }.toDouble() / samples.count { feature.mapper(it.color) >= splitValue }.toDouble())
                            else -> featurePositiveLeaf.predict(color)
                        }
                        else -> when {
                            featureNegativeLeaf == null -> (goodWeatherItems.count { feature.mapper(it.color) < splitValue }.toDouble() / samples.count { feature.mapper(it.color) < splitValue }.toDouble())
                            else -> featureNegativeLeaf.predict(color)
                        }
                    }
                }

                override fun toString() = "$feature split on $splitValue, ${negativeItems.count()}|${positiveItems.count()}, Impurity: $giniImpurity"

            }


            fun recurseAndPrintTree(leaf: TreeLeaf?, depth: Int = 0) {

                if (leaf != null) {
                    println("\t".repeat(depth) + "($depth): $leaf")
                    recurseAndPrintTree(leaf.featureNegativeLeaf, depth + 1)
                    recurseAndPrintTree(leaf.featurePositiveLeaf, depth + 1)
                }
            }


            override fun predict(color: Color): FontShade {

                val tree = buildLeaf(inputs)
                recurseAndPrintTree(tree)

                return if (tree!!.predict(color) >= .5) FontShade.DARK else FontShade.LIGHT
            }
        },

        RANDOM_FOREST {

            // Helpful Resources:
            // StatusQuest on YouTube: https://www.youtube.com/watch?v=7VeUPuFGJHk

            
            inner class Feature(val name: String, val mapper: (Color) -> Double) {
                override fun toString() = name
            }


            val features = listOf(
                    Feature("Red") { it.red * 255.0 },
                    Feature("Green") { it.green * 255.0 },
                    Feature("Blue") { it.blue * 255.0 }
            )

            fun giniImpurity(samples: List<LabeledColor>): Double {

                val totalSampleCount = samples.count().toDouble()

                return 1.0 - (samples.count { it.fontShade == FontShade.DARK }.toDouble() / totalSampleCount).pow(2) -
                        (samples.count {  it.fontShade == FontShade.LIGHT }.toDouble() / totalSampleCount).pow(2)
            }

            fun giniImpurityForSplit(feature: Feature, splitValue: Double, samples: List<LabeledColor>): Double {
                val positiveFeatureSamples = samples.filter { feature.mapper(it.color) >= splitValue }
                val negativeFeatureSamples = samples.filter { feature.mapper(it.color) < splitValue }

                val positiveImpurity = giniImpurity(positiveFeatureSamples)
                val negativeImpurity = giniImpurity(negativeFeatureSamples)

                return (positiveImpurity * (positiveFeatureSamples.count().toDouble() / samples.count().toDouble())) +
                        (negativeImpurity * (negativeFeatureSamples.count().toDouble() / samples.count().toDouble()))
            }

            fun splitContinuousVariable(feature: Feature, samples: List<LabeledColor>): Double? {

                val featureValues = samples.asSequence().map { feature.mapper(it.color) }.distinct().toList().sorted()

                val bestSplit = featureValues.asSequence().zipWithNext { value1, value2 -> (value1 + value2) / 2.0 }
                        .minBy { giniImpurityForSplit(feature, it, samples) }

                return bestSplit
            }


            inner class FeatureAndSplit(val feature: Feature, val split: Double)

            fun buildLeaf(samples: List<LabeledColor>, previousLeaf: TreeLeaf? = null, featureSampleSize: Int? = null ): TreeLeaf? {

                val fs = (if (featureSampleSize == null) features else features.random(featureSampleSize) )
                        .asSequence()
                        .filter { splitContinuousVariable(it, samples) != null }
                        .map { feature ->
                            FeatureAndSplit(feature, splitContinuousVariable(feature, samples)!!)
                        }.minBy { fs ->
                            giniImpurityForSplit(fs.feature, fs.split, samples)
                        }

                return if (previousLeaf == null ||
                        (fs != null && giniImpurityForSplit(fs.feature, fs.split, samples) < previousLeaf.giniImpurity))
                    TreeLeaf(fs!!.feature, fs.split, samples)
                else
                    null
            }

            inner class TreeLeaf(val feature: Feature,
                                 val splitValue: Double,
                                 val samples: List<LabeledColor>) {

                val darkItems = samples.filter { it.fontShade == FontShade.DARK }
                val lightItems = samples.filter { it.fontShade == FontShade.LIGHT }

                val positiveItems = samples.filter { feature.mapper(it.color) >= splitValue }
                val negativeItems = samples.filter { feature.mapper(it.color) < splitValue }

                val giniImpurity = giniImpurityForSplit(feature, splitValue, samples)

                val featurePositiveLeaf: TreeLeaf? = buildLeaf(samples.filter { feature.mapper(it.color) >= splitValue }, this)
                val featureNegativeLeaf: TreeLeaf? = buildLeaf(samples.filter { feature.mapper(it.color) < splitValue }, this)

                fun predict(color: Color): Double {

                    val featureValue = feature.mapper(color)

                    return when {
                        featureValue >= splitValue -> when {
                            featurePositiveLeaf == null -> (darkItems.count { feature.mapper(it.color) >= splitValue }.toDouble() / samples.count { feature.mapper(it.color) >= splitValue }.toDouble())
                            else -> featurePositiveLeaf.predict(color)
                        }
                        else -> when {
                            featureNegativeLeaf == null -> (darkItems.count { feature.mapper(it.color) < splitValue }.toDouble() / samples.count { feature.mapper(it.color) < splitValue }.toDouble())
                            else -> featureNegativeLeaf.predict(color)
                        }
                    }
                }

                override fun toString() = "$feature split on $splitValue, ${negativeItems.count()}|${positiveItems.count()}, Impurity: $giniImpurity"

            }

            fun recurseAndPrintTree(leaf: TreeLeaf?, depth: Int = 0) {

                if (leaf != null) {
                    println("\t".repeat(depth) + "($leaf)")
                    recurseAndPrintTree(leaf.featureNegativeLeaf, depth + 1)
                    recurseAndPrintTree(leaf.featurePositiveLeaf, depth + 1)
                }
            }


            lateinit var randomForest: List<TreeLeaf>

            override fun predict(color: Color): FontShade {

                val bootStrapSampleCount = (inputs.count() * (2.0 / 3.0)).toInt()

                if (retrainFlag) {
                    randomForest = (1..300).asSequence()
                            .map {
                                buildLeaf(samples = inputs.random(bootStrapSampleCount), featureSampleSize = 2)!!
                            }.toList()
                    
                    retrainFlag = false
                }

                val votes = randomForest.asSequence().countBy {
                    if (it.predict(color) >= .5) FontShade.DARK else FontShade.LIGHT
                }
                println(votes)
                return votes.maxBy { it.value }!!.key
            }
        },

        NEURAL_NETWORK_HILL_CLIMBING {

            lateinit var artificialNeuralNetwork: NeuralNetwork

            override fun predict(color: Color): FontShade {

                if (retrainFlag) {
                    artificialNeuralNetwork = neuralnetwork {
                        inputlayer(3)
                        hiddenlayer(3, ActivationFunction.TANH)
                        outputlayer(2, ActivationFunction.SOFTMAX)
                    }

                    val trainingData = inputs.map { colorAttributes(it.color) to it.fontShade.outputArray }

                    artificialNeuralNetwork.trainEntriesHillClimbing(trainingData)
                    retrainFlag = false
                }
                return artificialNeuralNetwork.predictEntry(colorAttributes(color)).let {
                    println("${it[0]} ${it[1]}")
                    if (it[0] > it[1]) FontShade.LIGHT else FontShade.DARK
                }
            }
        },

        NEURAL_NETWORK_SIMULATED_ANNEALING {

            lateinit var artificialNeuralNetwork: NeuralNetwork

            override fun predict(color: Color): FontShade {

                if (retrainFlag) {
                    artificialNeuralNetwork = neuralnetwork {
                        inputlayer(3)
                        hiddenlayer(3, ActivationFunction.TANH)
                        outputlayer(2, ActivationFunction.SOFTMAX)
                    }

                    val trainingData = inputs.map { colorAttributes(it.color) to it.fontShade.outputArray }

                    artificialNeuralNetwork.trainEntriesSimulatedAnnealing(trainingData)
                    retrainFlag = false
                }
                return artificialNeuralNetwork.predictEntry(colorAttributes(color)).let {
                    println("${it[0]} ${it[1]}")
                    if (it[0] > it[1]) FontShade.LIGHT else FontShade.DARK
                }
            }
        };

        var retrainFlag = true

        abstract fun predict(color: Color): FontShade
        override fun toString() = name.replace("_", " ")
    }

}

data class LabeledColor(
        val color: Color,
        val fontShade: FontShade
) {
    val red get() = color.red * 255.0
    val green get() = color.green * 255.0
    val blue get() = color.blue * 255.0

    val outputValue get() = fontShade.intValue

    val label get() = Label(fontShade.toString()).apply {
        style {
            textFill =  fontShade.color
            backgroundColor += color
            fontWeight = FontWeight.BOLD
        }
    }
}

enum class FontShade(val color: Color, val intValue: Double, val outputArray: DoubleArray){
    DARK(Color.BLACK, 1.0, doubleArrayOf(0.0, 1.0)),
    LIGHT(Color.WHITE, 0.0, doubleArrayOf(1.0,0.0))
}

// UTILITIES

fun randomInt(lower: Int, upper: Int) = ThreadLocalRandom.current().nextInt(lower, upper + 1)


fun randomColor() = (1..3).asSequence()
        .map { randomInt(0,255) }
        .toList()
        .let { Color.rgb(it[0], it[1], it[2]) }

fun colorAttributes(c: Color) = doubleArrayOf(
        c.red,
        c.green,
        c.blue
)
