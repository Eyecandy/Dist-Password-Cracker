package server

object Range extends App{
  def start(range:String): Option[String]= {
    var end = false
    val vec = range.toVector
    if (vec(0) =='9')
      if (vec.filter(x => (x == '9')).length == 8) {
        end = true
      }
    if (end) {
      None
    }
    else {
      Some(create(vec,2).mkString(""))
    }
  }

  def create(vectorRange:Vector[Char],index:Int): Vector[Char] = {
    val newChar: Char = simpleIncr(vectorRange(index))
    if (newChar == 'A') {
      val newVectorRange = vectorRange.updated(index,newChar)
      create(newVectorRange ,(index-1))
    }
    else {
      val newVectorRange = vectorRange.updated(index,newChar)
      newVectorRange
    }
  }
  def simpleIncr(ch:Char): Char = {
    ch match {
      case 'Z' => 'a'
      case 'z' => '0'
      case '9' => 'A'
      case _ => (ch.toInt + 1).toChar
    }
  }
}
