package com.github.pshirshov.izumi.idealingua.model.problems

import com.github.pshirshov.izumi.fundamentals.platform.strings.IzString._
import com.github.pshirshov.izumi.idealingua.model.common.TypeId._
import com.github.pshirshov.izumi.idealingua.model.common.{DomainId, TypeId}
import com.github.pshirshov.izumi.idealingua.model.il.ast.typed._
import com.github.pshirshov.izumi.idealingua.model.loader.{FSPath, LoadedDomain, ModelParsingResult}
import com.github.pshirshov.izumi.idealingua.model.typespace.verification.MissingDependency

sealed trait IDLError

sealed trait TyperError extends IDLError

object TyperError {

  @deprecated("We need to improve design and get rid of this", "2018-12-06")
  final case class TyperException(message: String) extends TyperError {
    override def toString: String = s"Typer failed with exception. Message: $message"
  }

}

sealed trait RefResolverIssue extends IDLError

object RefResolverIssue {

  final case class DuplicatedDomains(imported: DomainId, paths: List[FSPath]) extends RefResolverIssue {
    override def toString: String = s"$imported: multiple domains have the same identifier: ${paths.niceList()}"
  }

  final case class UnparseableInclusion(domain: DomainId, stack: List[String], failure: ModelParsingResult.Failure) extends RefResolverIssue {
    override def toString: String = s"$domain: can't parse inclusion ${failure.path}, inclusion chain: $domain->${stack.mkString("->")}. Message: ${failure.message}"
  }

  final case class MissingInclusion(domain: DomainId, stack: List[String], path: String, diagnostic: List[String]) extends RefResolverIssue {
    override def toString: String = s"$domain: can't find inclusion $path, inclusion chain: $domain->${stack.mkString("->")}. Available: ${diagnostic.niceList()}"
  }

  final case class UnresolvableImport(domain: DomainId, imported: DomainId, failure: LoadedDomain.Failure) extends RefResolverIssue {
    override def toString: String = s"$domain: can't resolve import $imported, problem: $failure"
  }

  final case class MissingImport(domain: DomainId, imported: DomainId, diagnostic: List[String]) extends RefResolverIssue {
    override def toString: String = s"$domain: can't find import $imported. Available: ${diagnostic.niceList()}"
  }

}


sealed trait TypespaceError extends IDLError

object TypespaceError {

  final case class PrimitiveAdtMember(t: AdtId, members: List[AdtMember]) extends TypespaceError {
    override def toString: String = s"ADT members can't be primitive (TS implementation limit): ${members.mkString(", ")}"
  }

  final case class AmbigiousAdtMember(t: AdtId, types: List[TypeId]) extends TypespaceError {
    override def toString: String = s"ADT hierarchy contains same members (TS implementation limit): ${types.mkString(", ")}"
  }

  final case class DuplicateEnumElements(t: EnumId, members: List[String]) extends TypespaceError {
    override def toString: String = s"Duplicated enumeration members: ${members.mkString(", ")}"
  }

  final case class DuplicateAdtElements(t: AdtId, members: List[String]) extends TypespaceError {
    override def toString: String = s"Duplicated ADT members: ${members.mkString(", ")}"
  }

  final case class NoncapitalizedTypename(t: TypeId) extends TypespaceError {
    override def toString: String = s"All typenames must start with a capital letter: $t"
  }

  final case class ShortName(t: TypeId) extends TypespaceError {
    override def toString: String = s"All typenames be at least 2 characters long: $t"
  }

  final case class ReservedTypenamePrefix(t: TypeId, badNames: Set[String]) extends TypespaceError {
    override def toString: String = s"Typenames can't start with reserved runtime prefixes. Type: $t, forbidden prefixes: ${badNames.niceList()}"
  }

  final case class CyclicInheritance(t: TypeId) extends TypespaceError {
    override def toString: String = s"Type is involved into cyclic inheritance: $t"
  }

  final case class CyclicUsage(t: TypeId, cycles: Set[TypeId]) extends TypespaceError {
    override def toString: String = s"Cyclic usage disabled due to serialization issues, use opt[T] to break the loop: $t. Cycle caused by: $cycles"
  }

  final case class MissingDependencies(deps: List[MissingDependency]) extends TypespaceError {
    override def toString: String = s"Missing dependencies: ${deps.mkString(", ")}"
  }

  @deprecated("We need to improve design and get rid of this", "2018-12-06")
  final case class VerificationException(message: String) extends TypespaceError {
    override def toString: String = s"Verification failed with exception. Message: $message"
  }

}