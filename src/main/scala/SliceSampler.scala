import java.util.concurrent.ThreadLocalRandom

object SliceSampler {
  def targetDensity(x: Double): Double = {
    if (x >= 0 && x <= 2) {
      math.pow(2 - x, 2) * math.pow(x * x * x + 1, 3)
    } else {
      1e-8
    }
  }
  // 定义采样函数
  def sliceSampling(iterations: Int, width: Double): List[Double] = {
    var samples: List[Double] = List.empty
    var current = 1.0 // 初始样本
    val rand = ThreadLocalRandom.current

    for (_ <- 0 until iterations) {
      val height = rand.nextDouble * targetDensity(current)
      var interval = (
        current - width * rand.nextDouble(),
        current + width * rand.nextDouble()
      )

      var newX = interval._1 + (interval._2 - interval._1) * rand.nextDouble()
      var newY = targetDensity(newX)

      while (newY < height) {
        if (newX > current) interval = (interval._1, newX)
        else interval = (newX, interval._2)

        newX = interval._1 + (interval._2 - interval._1) * rand.nextDouble()
        newY = targetDensity(newX)
      }

      current = newX
      samples = current :: samples
    }
    samples.reverse
  }
  def main(args: Array[String]): Unit = {
    val iterations = 10000 // 迭代次数
    val width = 0.1 // 切片的宽度

    val samples = sliceSampling(iterations, width)

    println(samples.take(10).mkString(", "))
  }
}
