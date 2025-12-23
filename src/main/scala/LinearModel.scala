import org.apache.spark.sql.SparkSession
import scala.sys.process._

object LinearModel {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("LinearModel")
      .master("local[*]")   // 课程实验一般这样
      .getOrCreate()

    val sc = spark.sparkContext

    val RowSize = sc.broadcast(200)
    val ColumnSize = sc.broadcast(5)
    val RowLength = sc.broadcast(4)
    val ColumnLength = sc.broadcast(1000)

    val NonZeroLength = 10
    val p = ColumnSize.value * ColumnLength.value

    val beta = (1 to p).map(_.toDouble).toArray
      .map(i => if (i < NonZeroLength + 1) 2.0 else 0.0)

    val MyBeta = sc.broadcast(beta)

    val sigma = 1.0
    val Sigma = sc.broadcast(sigma)

    val indices = 0 until RowLength.value
    val ParallelIndices = sc.parallelize(indices, indices.length)

    val lines = ParallelIndices.map { s =>
      s"$s,${beta.mkString(" ")}"
    }

    lines.saveAsTextFile("/SimData")

    spark.stop()
  }
}
