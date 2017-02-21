package external

import boopickle.DefaultBasic._
import utest._

object DefaultBasicTests extends TestSuite {

  sealed trait MyTrait

  case class TT1(i: Int) extends MyTrait

  case class TT2(s: String, next: MyTrait) extends MyTrait

  class TT3(val i: Int, val s: String) extends MyTrait {
    // a normal class requires an equals method to work properly
    override def equals(obj: scala.Any): Boolean = obj match {
      case t: TT3 => i == t.i && s == t.s
      case _      => false
    }
  }

  object MyTrait {
    // picklers must be created in correct order, because TT2 depends on MyTrait
    implicit val pickler = compositePickler[MyTrait]
    // use macro to generate picklers for TT1 and TT2
    implicit val pickler1 = PicklerGenerator.generatePickler[TT1]
    implicit val pickler2 = PicklerGenerator.generatePickler[TT2]
    // a pickler for TT3 cannot be generated by macro, so use a transform pickler
    implicit val pickler3 = transformPickler((t: (Int, String)) => new TT3(t._1, t._2))(t => (t.i, t.s))
    pickler.addConcreteType[TT1].addConcreteType[TT2].addConcreteType[TT3]
  }

  override def tests = TestSuite {
    'Trait {
      val t: Seq[MyTrait] = Seq(TT1(5), TT2("five", TT2("six", new TT3(42, "fortytwo"))))
      val bb              = Pickle.intoBytes(t)
      val u               = Unpickle[Seq[MyTrait]].fromBytes(bb)
      assert(u == t)
    }
  }
}