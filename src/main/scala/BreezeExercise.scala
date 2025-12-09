import breeze.linalg.{DenseVector,DenseMatrix,sum,norm}

object BreezeExercises{
  def q1():Unit = {
    val a = DenseVector[Double](1,3,5,7,9,7,5,3,1)

    val col = a.toDenseMatrix.t        // 9×1
    val row = a.toDenseMatrix          // 1×9
    val A = col * DenseMatrix.ones[Double](1, a.length)+
            DenseMatrix.ones[Double](a.length, 1) * row
    println(A)
  }

  def main(args:Array[String]):Unit = {
    q1()
    // q2()
    // q3()
    // q4()
    // q5()
    // q6()
  }
}

