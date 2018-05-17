/*
 * Copyright © 2002-2018 Neo4j Sweden AB (http://neo4j.com)
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
package org.opencypher.v9_1.rewriting.rewriters

import org.opencypher.v9_1.expressions.{Equals, NotEquals}
import org.opencypher.v9_0.util.{Rewriter, topDown}
import org.opencypher.v9_1.expressions.{Equals, Not}

case object normalizeNotEquals extends Rewriter {

  override def apply(that: AnyRef): AnyRef = instance(that)

  private val instance: Rewriter = topDown(Rewriter.lift {
    case p @ NotEquals(lhs, rhs) =>
      Not(Equals(lhs, rhs)(p.position))(p.position)   // not(1 = 2)  <!===!>     1 != 2
  })
}
