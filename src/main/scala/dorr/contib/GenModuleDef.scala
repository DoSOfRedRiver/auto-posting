package dorr.contib

import izumi.distage.model.definition.{Module, ModuleDef}
import izumi.fundamentals.reflection.Tags.Tag
import shapeless.labelled.FieldType
import shapeless.ops.hlist.SelectAll
import shapeless.{::, HList, HNil, LabelledGeneric, Witness}
import shapeless.ops.record.{Fields, Selector}

object GenModuleDef {
  trait GenModule[A] {
    def module(a: A): Module
  }

  object GenModule {
    def apply[A](implicit ev: GenModule[A]): GenModule[A] = ev

    implicit def hnilGenModule = new GenModule[HNil] {
      override def module(a: HNil): Module = new ModuleDef {}
    }

    implicit def hconsGenModule[K, V: Tag, T <: HList : GenModule](implicit
      w: Witness.Aux[K],
      select: Selector.Aux[FieldType[K, V] :: T, K, V]
    ) =
      new GenModule[FieldType[K, V] :: T] {
        override def module(a: FieldType[K, V] :: T): Module =
          GenModule[T].module(a.tail) ++ new ModuleDef {
            val v: V = select(a)
            val name = w.value.toString.substring(1)
            println(s"@Witness: ${name}")
            make[V].named(name).from(v)
          }
      }

    implicit def productGenModule[A, L <: HList](implicit lg: LabelledGeneric.Aux[A, L], genModuleHL: GenModule[L]) = new GenModule[A] {
      override def module(a: A): Module = genModuleHL.module(lg.to(a))
    }
  }

  def fromTaggedProduct[A: GenModule](a: A) = GenModule[A].module(a)
}
