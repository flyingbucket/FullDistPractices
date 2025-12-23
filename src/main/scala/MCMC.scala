import breeze.linalg._
import breeze.numerics._
import breeze.stats.distributions._
import java.util.concurrent.ThreadLocalRandom

object MCMC {
  def targetDensity(x: Double): Double = {
    if (x >= 0 && x <= 2) {
      math.pow(2 - x, 2) * math.pow(x * x * x + 1, 3)
    } else {
      1e-8
    }
  }

  def metropolisHastings(
      targetDensity: Double => Double,
      initial: Double,
      iterations: Int,
      proposalStd: Double
  ): DenseVector[Double] = {
    val samples = DenseVector.zeros[Double](iterations)
    var current = initial
    val rand = ThreadLocalRandom.current

    for (i <- 0 until iterations) {
      val proposal = current + rand.nextGaussian() * proposalStd
      val acceptanceRatio = targetDensity(proposal) / targetDensity(current)

      if (acceptanceRatio >= 1 || rand.nextDouble() < acceptanceRatio) {
        current = proposal
      }

      samples(i) = current
    }

    samples
  }
  def main(args: Array[String]): Unit = {
    val initialSample = 0.0
    val iterations = 10000
    val proposalStd = 0.5

    val samples =
      metropolisHastings(targetDensity, initialSample, iterations, proposalStd)

    println(samples(0 to 10))
  }
}
