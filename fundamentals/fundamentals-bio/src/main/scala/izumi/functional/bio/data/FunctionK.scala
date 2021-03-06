package izumi.functional.bio.data

import scala.language.implicitConversions

/**
  * Note: if you're using Scala 2.12 and getting "no such method" or implicit-related errors when interacting with FunctionK,
  * you must enable `-Xsource:2.13` compiler option. BIO does not work without this option on 2.12.
  */
object FunctionK {
  private[data] type FunctionK[-F[_], +G[_]]

  @inline def apply[F[_], G[_]](polyFunction: F[UnknownA] => G[UnknownA]): FunctionK[F, G] = polyFunction.asInstanceOf[FunctionK[F, G]]

  @inline def id[F[_]]: FunctionK[F, F] = (identity[Any] _).asInstanceOf[FunctionK[F, F]]

  implicit final class FunctionKOps[-F[_], +G[_]](private val self: FunctionK[F, G]) extends AnyVal {
    @inline def apply[A](f: F[A]): G[A] = self.asInstanceOf[F[A] => G[A]](f)

    @inline def compose[F1[_]](f: FunctionK[F1, F]): FunctionK[F1, G] = FunctionK(g => apply(f(g)))
    @inline def andThen[H[_]](f: FunctionK[G, H]): FunctionK[F, H] = f.compose(self)
  }

  implicit def fromCats[F[_], G[_]](fn: cats.arrow.FunctionK[F, G]): FunctionK[F, G] = FunctionK(fn(_))

  implicit final class FunctionKCatsOps[F[_], G[_]](private val self: FunctionK[F, G]) extends AnyVal {
    @inline def toCats: cats.arrow.FunctionK[F, G] = Lambda[cats.arrow.FunctionK[F, G]](self(_))
  }

  /**
    * When it's more convenient to write a polymorphic function using a class or kind-projector's lambda syntax:
    *
    * {{{
    *   Lambda[FunctionK.Instance[F, G]](a => f(b(a)))
    * }}}
    */
  trait Instance[-F[_], +G[_]] {
    def apply[A](fa: F[A]): G[A]

    @inline final def compose[F1[_]](f: FunctionK[F1, F]): FunctionK[F1, G] = f.andThen(this)
    @inline final def andThen[H[_]](f: FunctionK[G, H]): FunctionK[F, H] = f.compose(this)
  }
  object Instance {
    @inline implicit def asFunctionK[F[_], G[_]](functionKKInstance: FunctionK.Instance[F, G]): FunctionK[F, G] =
      FunctionK(functionKKInstance.apply)
  }

  private[FunctionK] type UnknownA

  implicit def conversion2To1[F[_, _], G[_, _], E](f: FunctionKK[F, G]): FunctionK[F[E, ?], G[E, ?]] = f.asInstanceOf[FunctionK[F[E, ?], G[E, ?]]]
  implicit def Convert2To1[F[_, _], G[_, _], E](implicit f: FunctionKK[F, G]): FunctionK[F[E, ?], G[E, ?]] = f.asInstanceOf[FunctionK[F[E, ?], G[E, ?]]]

  // workaround for inference issues with `E=Nothing`
  implicit def conversion2To1Nothing[F[_, _], G[_, _]](f: FunctionKK[F, G]): FunctionK[F[Nothing, ?], G[Nothing, ?]] =
    f.asInstanceOf[FunctionK[F[Nothing, ?], G[Nothing, ?]]]
  implicit def Convert2To1Nothing[F[_, _], G[_, _]](implicit f: FunctionKK[F, G]): FunctionK[F[Nothing, ?], G[Nothing, ?]] =
    f.asInstanceOf[FunctionK[F[Nothing, ?], G[Nothing, ?]]]
}
