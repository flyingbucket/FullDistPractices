import java.util.concurrent.ThreadLocalRandom

import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD

object EM {
  // 数据生成函数
  def generateData(
      N: Int,
      mu1: Double,
      sig1: Double,
      mu2: Double,
      sig2: Double,
      p: Double
  ): Array[Double] = {

    val r = ThreadLocalRandom.current

    Array.fill(N) {
      if (r.nextDouble() <= p)
        mu1 + sig1 * r.nextGaussian()
      else
        mu2 + sig2 * r.nextGaussian()
    }
  }

  def main(args: Array[String]): Unit = {

    // Spark 初始化
    val spark = SparkSession
      .builder()
      .appName("GaussianMixtureEM")
      .master("local[*]") // 本地运行；提交集群时删掉
      .getOrCreate()

    val sc = spark.sparkContext

    // 参数设置
    val N = 10000
    val MU1 = 5.0
    val Sig1 = 2.0
    val MU2 = 0.0
    val Sig2 = 1.0
    val P = 0.2
    val NumOfSlaves = 5
    val eps = 1e-3

    // 生成混合高斯数据
    val data = generateData(N, MU1, Sig1, MU2, Sig2, P)

    var parData = sc.parallelize(data, NumOfSlaves)

    // 保留你原来“格式化再转回 Double”的逻辑
    val parDataStr = parData.map("%.4f".format(_))
    parData = parDataStr.map(_.toDouble)

    // EM 初始化
    val InitialP = 0.5
    var Nk = N.toDouble * InitialP

    var EstMu1 = parData.reduce(_ + _) / N.toDouble
    var EstSig1 = math.sqrt(
      parData.map(x => x * x).reduce(_ + _) / N.toDouble - EstMu1 * EstMu1
    )

    var EstMu2 = EstMu1 - 1.0
    var EstSig2 = EstSig1

    var Diff = 0.0
    var OldEstMu1 = 0.0
    var OldEstMu2 = 0.0
    var OldEstSig1 = 0.0
    var OldEstSig2 = 0.0

    var iter = 0

    // EM 主循环
    do {

      iter += 1

      OldEstMu1 = EstMu1
      OldEstMu2 = EstMu2
      OldEstSig1 = EstSig1
      OldEstSig2 = EstSig2

      val sufficientStatistics = parData.map { x =>
        val x1 = -math.pow((x - EstMu2) / EstSig2, 2) / 2.0
        val x2 = -math.pow((x - EstMu1) / EstSig1, 2) / 2.0

        val gamma =
          Nk * EstSig2 /
            (Nk * EstSig2 + (N - Nk) * EstSig1 * math.exp(x1 - x2))

        (
          gamma,
          x * gamma,
          x * x * gamma,
          1.0 - gamma,
          x * (1.0 - gamma),
          x * x * (1.0 - gamma)
        )
      }

      val res = sufficientStatistics.reduce { (a, b) =>
        (
          a._1 + b._1,
          a._2 + b._2,
          a._3 + b._3,
          a._4 + b._4,
          a._5 + b._5,
          a._6 + b._6
        )
      }

      Nk = res._1
      EstMu1 = res._2 / Nk
      EstSig1 = math.sqrt(res._3 / Nk - EstMu1 * EstMu1)

      EstMu2 = res._5 / res._4
      EstSig2 = math.sqrt(res._6 / res._4 - EstMu2 * EstMu2)

      Diff = math.abs(EstMu1 - OldEstMu1) +
        math.abs(EstMu2 - OldEstMu2) +
        math.abs(EstSig1 - OldEstSig1) +
        math.abs(EstSig2 - OldEstSig2)

    } while (Diff > eps)

    // 输出结果
    println(s"迭代次数: $iter")
    println(f"EstMu1  = $EstMu1%.4f")
    println(f"EstMu2  = $EstMu2%.4f")
    println(f"EstSig1 = $EstSig1%.4f")
    println(f"EstSig2 = $EstSig2%.4f")

    spark.stop()
  }
}
