package dorr.util

import cats.{Applicative, Apply, Defer, Functor, Monad}
import cats.effect.{Async, Bracket, ConcurrentEffect, Sync, Timer}
import distage.{ModuleDef, TagK}
import izumi.distage.model.effect.{DIEffect, DIEffectAsync, DIEffectRunner}
import logstage.LogIO
import tofu.{Race, Start}
import tofu.concurrent.TryableDeferreds

object modules {
  def Misc[F[_] : TagK : LogIO : ConcurrentEffect : Timer] =
    new ModuleDef {
      addImplicit[LogIO[F]]
      addImplicit[Monad[F]]
      addImplicit[Apply[F]]
      addImplicit[Sync[F]]
      addImplicit[Async[F]]
      addImplicit[Timer[F]]
      addImplicit[Defer[F]]
      addImplicit[Start[F]]
      addImplicit[Race[F]]
      addImplicit[Functor[F]]
      addImplicit[Applicative[F]]
      addImplicit[TryableDeferreds[F]]
      addImplicit[Bracket[F, Throwable]]
      addImplicit[ConcurrentEffect[F]]
    }

  def DIEffects[F[_]: DIEffectRunner: DIEffectAsync: DIEffect: TagK] = new ModuleDef {
    addImplicit[DIEffectRunner[F]]
    addImplicit[DIEffectAsync[F]]
    addImplicit[DIEffect[F]]
  }
}
