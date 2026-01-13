import java.util.concurrent.ThreadLocalRandom
//设置产生数据的参数并允许各个计算节点可以读取其取值
val RowSize = sc.broadcast(200)
val ColumnSize = sc.broadcast(5)
val RowLength = sc.broadcast(4)
val ColumnLength = sc.broadcast(1000)
val NonZeroLength = 10
val p = ColumnSize.value * ColumnLength.value

val beta = (1 to p).map(_.toDouble).toArray[Double].map(i => {if(i<NonZeroLength+1) 2.0 else 0.0})

//在整个Spark系统中广播beta系数，从而使得每个计算节点都可以读取变量MyBeta中的值
val MyBeta = sc.broadcast(beta)

val sigma = 1.0
//在整个Spark系统中广播Sigma系数，从而使得每个计算节点都可以读取变量Sigma中的值
val Sigma = sc.broadcast(sigma)

var indices = 0 until RowLength.value
var ParallelIndices = sc.parallelize(indices, indices.length)

//产生数据
var lines = ParallelIndices.map(s => {
    val r = ThreadLocalRandom.current
    //nextGaussian是新产生的类对象r中的函数，可以产生服从标准正态分布的随机数
    def rn(n: Int) = (0 until n).map(x => r.nextGaussian).toArray[Double]
    //读取MyBeta和Sigma中的值
    val beta = MyBeta.value
    val sigma = Sigma.value
    val rowsize = RowSize.value
    val columnsize = ColumnSize.value
    val columnlength = ColumnLength.value
    val lines = new Array[String](rowsize)
    val p = columnsize * columnlength

    for(i <- 0 until rowsize)
    {

         var line = "";
         var y = 0.0;

         for(j <- 0 until columnlength)
         {
             var x = rn(columnsize)
             
             for(k <- 0 until columnsize) y+=beta(j*columnsize + k)*x(k)
           
             var segment = x.map("%.4f" format _).reduce(_+" "+_)
             line = line+","+segment
         }

         y+= sigma*r.nextGaussian
         lines(i) = "%.4f".format(y) + line + "\n"
    }

    lines.reduce(_+_)
})

import scala.sys.process._

var cmd = "hdfs dfs -ls /"

cmd.!!.foreach{print}

lines.saveAsTextFile("/SimData")

cmd.!!.foreach{print}

val lines_read = sc.textFile("/SimData/part-00000")

import breeze.linalg._

val transLines = lines_read.take(200).map( s => {
    val rowsize = RowSize.value
    val columnsize = ColumnSize.value
    val columnlength = ColumnLength.value
    val p = columnsize * columnlength
    val Y = DenseVector(s.map(_.split(",")(0).toDouble))
    val X = s.map(_.split(",").drop(1).map(_.split(" ").map(_.toDouble)))
    (Y,X)
}
)

val Y = lines_read.map(_.split(",")(0).toDouble)

val X = lines_read.map(_.split(",").drop(1).flatMap(_.split(" ").map(_.toDouble)))

