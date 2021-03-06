/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.ast.AdministrationCommand
import org.opencypher.v9_0.ast.Statement
import org.opencypher.v9_0.ast.UnaliasedReturnItem
import org.opencypher.v9_0.ast.semantics.SemanticState
import org.opencypher.v9_0.expressions.NotEquals
import org.opencypher.v9_0.rewriting.RewriterCondition
import org.opencypher.v9_0.rewriting.RewriterStep.enableCondition
import org.opencypher.v9_0.rewriting.RewriterStepSequencer
import org.opencypher.v9_0.rewriting.conditions.containsNoNodesOfType
import org.opencypher.v9_0.rewriting.conditions.containsNoReturnAll
import org.opencypher.v9_0.rewriting.conditions.noDuplicatesInReturnItems
import org.opencypher.v9_0.rewriting.conditions.noReferenceEqualityAmongVariables
import org.opencypher.v9_0.rewriting.conditions.noUnnamedPatternElementsInMatch
import org.opencypher.v9_0.rewriting.conditions.noUnnamedPatternElementsInPatternComprehension
import org.opencypher.v9_0.rewriting.conditions.normalizedEqualsArguments
import org.opencypher.v9_0.rewriting.rewriters.AddUniquenessPredicates
import org.opencypher.v9_0.rewriting.rewriters.InnerVariableNamer
import org.opencypher.v9_0.rewriting.rewriters.LiteralExtraction
import org.opencypher.v9_0.rewriting.rewriters.addImplicitExistToPatternExpressions
import org.opencypher.v9_0.rewriting.rewriters.desugarMapProjection
import org.opencypher.v9_0.rewriting.rewriters.expandStar
import org.opencypher.v9_0.rewriting.rewriters.foldConstants
import org.opencypher.v9_0.rewriting.rewriters.inlineNamedPathsInPatternComprehensions
import org.opencypher.v9_0.rewriting.rewriters.literalReplacement
import org.opencypher.v9_0.rewriting.rewriters.moveWithPastMatch
import org.opencypher.v9_0.rewriting.rewriters.nameMatchPatternElements
import org.opencypher.v9_0.rewriting.rewriters.namePatternComprehensionPatternElements
import org.opencypher.v9_0.rewriting.rewriters.nameUpdatingClauses
import org.opencypher.v9_0.rewriting.rewriters.normalizeArgumentOrder
import org.opencypher.v9_0.rewriting.rewriters.normalizeComparisons
import org.opencypher.v9_0.rewriting.rewriters.normalizeMatchPredicates
import org.opencypher.v9_0.rewriting.rewriters.normalizeNotEquals
import org.opencypher.v9_0.rewriting.rewriters.normalizeSargablePredicates
import org.opencypher.v9_0.rewriting.rewriters.parameterValueTypeReplacement
import org.opencypher.v9_0.rewriting.rewriters.recordScopes
import org.opencypher.v9_0.rewriting.rewriters.replaceLiteralDynamicPropertyLookups
import org.opencypher.v9_0.rewriting.rewriters.sensitiveLiteralReplacement
import org.opencypher.v9_0.util.CypherExceptionFactory
import org.opencypher.v9_0.util.symbols.CypherType

class ASTRewriter(rewriterSequencer: String => RewriterStepSequencer,
                  literalExtraction: LiteralExtraction,
                  getDegreeRewriting: Boolean,
                  innerVariableNamer: InnerVariableNamer) {

  def rewrite(statement: Statement,
              semanticState: SemanticState,
              parameterTypeMapping: Map[String, CypherType],
              cypherExceptionFactory: CypherExceptionFactory): (Statement, Map[String, Any], Set[RewriterCondition]) = {

    val contract = rewriterSequencer("ASTRewriter")(
      recordScopes(semanticState),
      desugarMapProjection(semanticState),
      moveWithPastMatch,
      normalizeComparisons,
      enableCondition(noReferenceEqualityAmongVariables),
      enableCondition(containsNoNodesOfType[UnaliasedReturnItem]),
      enableCondition(noDuplicatesInReturnItems),
      expandStar(semanticState),
      enableCondition(containsNoReturnAll),
      foldConstants(cypherExceptionFactory),
      nameMatchPatternElements,
      nameUpdatingClauses,
      enableCondition(noUnnamedPatternElementsInMatch),
      normalizeMatchPredicates(getDegreeRewriting),
      normalizeNotEquals,
      enableCondition(containsNoNodesOfType[NotEquals]),
      normalizeArgumentOrder,
      normalizeSargablePredicates,
      enableCondition(normalizedEqualsArguments),
      AddUniquenessPredicates(innerVariableNamer),
      replaceLiteralDynamicPropertyLookups,
      namePatternComprehensionPatternElements,
      enableCondition(noUnnamedPatternElementsInPatternComprehension),
      inlineNamedPathsInPatternComprehensions,
      addImplicitExistToPatternExpressions(semanticState)
    )

    val rewrittenStatement = statement.endoRewrite(contract.rewriter)

    val replaceParameterValueTypes = parameterValueTypeReplacement(rewrittenStatement, parameterTypeMapping)
    val rewrittenStatementWithParameterTypes = rewrittenStatement.endoRewrite(replaceParameterValueTypes)
    val (extractParameters, extractedParameters) = statement match {
      case _ : AdministrationCommand => sensitiveLiteralReplacement(rewrittenStatementWithParameterTypes)
      case _ => literalReplacement(rewrittenStatementWithParameterTypes, literalExtraction)
    }

    (rewrittenStatementWithParameterTypes.endoRewrite(extractParameters), extractedParameters, contract.postConditions)
  }
}
