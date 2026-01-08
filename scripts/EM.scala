import java.util.concurrent.ThreadLocalRandom

val N = 10000
val MU1 = 5.0
val Sig1 = 2.0
val MU2 = 0.0
val Sig2 = 1.0
val P = 0.2
val NumOfSlaves = 5

val random = ThreadLocalRandom.current
val data = Array.ofDim[Double](N)

for (i <- 0 until N) {
  if (random.nextDouble <= P) {
    data(i) = MU1 + Sig1 * random.nextGaussian
  } else {
    data(i) = MU2 + Sig2 * random.nextGaussian
  }
}
var ParData = sc.parallelize(data, NumOfSlaves)
val ParDataStr = ParData.map("%.4f" format _)
ParData = ParDataStr.map(_.toDouble)
ParData.collect()

val InitialP = 0.5
var Nk = N.toDouble * InitialP
var EstMu1 = ParData.reduce((x, y) => x + y) / N.toDouble
var EstSig1 = math.sqrt(
  ParData.map(x => x * x).reduce((x, y) => x + y) / N.toDouble - EstMu1 * EstMu1
)

var EstMu2 = EstMu1 - 1.0
var EstSig2 = EstSig1
var Diff = 0.0
var OldEstMu1 = 0.0
var OldEstMu2 = 0.0
var OldEstSig1 = 0.0
var OldEstSig2 = 0.0
var ii = 0

val eps = 0.001

do {

  ii += 1
  OldEstMu1 = EstMu1
  OldEstMu2 = EstMu2
  OldEstSig1 = EstSig1
  OldEstSig2 = EstSig2

  var SufficientStatistics = ParData.map(line => {
    val x1 = -math.pow((line - EstMu2) / EstSig2, 2) / 2.0
    val x2 = -math.pow((line - EstMu1) / EstSig1, 2) / 2.0
    val gamma =
      Nk * EstSig2 / (Nk * EstSig2 + (N - Nk) * EstSig1 * math.exp(x1 - x2))
    (
      line,
      gamma,
      line * gamma,
      line * line * gamma,
      1 - gamma,
      line * (1 - gamma),
      line * line * (1 - gamma)
    )
  })

  val Results = SufficientStatistics.reduce((x, y) =>
    (
      x._1 + y._1,
      x._2 + y._2,
      x._3 + y._3,
      x._4 + y._4,
      x._5 + y._5,
      x._6 + y._6,
      x._7 + y._7
    )
  )

  Nk = Results._2
  EstMu1 = Results._3 / Nk
  EstSig1 = math.sqrt(Results._4 / Nk - EstMu1 * EstMu1)
  EstMu2 = Results._6 / Results._5
  EstSig2 = math.sqrt(Results._7 / Results._5 - EstMu2 * EstMu2)

  Diff = math.abs(EstMu1 - OldEstMu1) + math.abs(EstMu2 - OldEstMu2)
  Diff += math.abs(EstSig1 - OldEstSig1) + math.abs(EstSig2 - OldEstSig2)

} while (Diff > eps)

println(s"迭代次数: $ii")
println(f"EstMu1  = $EstMu1%.4f")
println(f"EstMu2  = $EstMu2%.4f")
println(f"EstSig1 = $EstSig1%.4f")
println(f"EstSig2 = $EstSig2%.4f")
