package com.github.pshirshov.izumi.idealingua.runtime.services

import scala.language.higherKinds

trait IRTUnsafeDispatcher[Ctx, R[_]] extends IRTWithSvcResultType[R] {
  def identifier: IRTServiceId

  def dispatchUnsafe(input: InContext[IRTMuxRequest[_], Ctx]): Option[Result[IRTMuxResponse[_]]]
}

case class IRTMethod(service: IRTServiceId, methodId: IRTMethodId)

//case class ReqBody(value: AnyRef) extends AnyRef
//
//case class ResBody(value: AnyRef) extends AnyRef
//
//case class MuxResponse[T <: AnyRef](v: T, method: Method) {
//  def body: ResBody = ResBody(v)
//}
//
//case class MuxRequest[T <: AnyRef](v: T, method: Method) {
//  def body: ReqBody = ReqBody(v)
//}

case class IRTReqBody(value: Any) extends AnyRef

case class IRTResBody(value: Any) extends AnyRef

case class IRTMuxResponse[T](v: T, method: IRTMethod) {
  def body: IRTResBody = IRTResBody(v)
}

case class IRTMuxRequest[T](v: T, method: IRTMethod) {
  def body: IRTReqBody = IRTReqBody(v)
}

case class IRTServiceId(value: String) extends AnyVal

case class IRTMethodId(value: String) extends AnyVal


class IRTServerMultiplexor[R[_], Ctx](dispatchers: List[IRTUnsafeDispatcher[Ctx, R]]) extends IRTDispatcher[InContext[IRTMuxRequest[_], Ctx], IRTMuxResponse[_], R] {
  override def dispatch(input: InContext[IRTMuxRequest[_], Ctx]): Result[IRTMuxResponse[_]] = {
    dispatchers.foreach {
      d =>
        d.dispatchUnsafe(input) match {
          case Some(v) =>
            return v
          case None =>
        }
    }
    throw new IRTMultiplexingException(s"Cannot handle $input, services: $dispatchers", input)
  }
}
