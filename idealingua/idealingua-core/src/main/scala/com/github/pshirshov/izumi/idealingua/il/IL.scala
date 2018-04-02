package com.github.pshirshov.izumi.idealingua.il

import com.github.pshirshov.izumi.idealingua.model.il.ast.raw.ILAstParsed
import com.github.pshirshov.izumi.idealingua.model.il.ast.raw.ILAstParsed._
import com.github.pshirshov.izumi.idealingua.model.il.ast.typed.DomainId

object IL {

  sealed trait Val

  case class ILDomainId(id: DomainId) extends Val

  case class ILService(v: Service) extends Val

  case class ILInclude(i: String) extends Val

  case class ILDef(v: ILAstParsed) extends Val

}
